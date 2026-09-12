package com.tradeflow.core.engine

import android.content.Context
import com.tradeflow.core.data.ChatMessage
import com.tradeflow.core.data.Conversation
import com.tradeflow.core.data.Customer
import com.tradeflow.core.data.Job
import com.tradeflow.core.data.MsgTemplate
import com.tradeflow.core.data.Repo

/**
 * The booking brain. Routes every incoming text by conversation stage.
 * Mike's in-app actions (✅ / 📞 / ❌ / confirm) call resolveYes/resolveNo/confirmBooking.
 */
object BookingBot {

    suspend fun handleReply(ctx: Context, phone: String, raw: String) {
        val repo = appRepo(ctx)
        val c = repo.convo(phone)
        val t = raw.trim()
        if (t.isEmpty()) return
        // Global human-escape (but not while already escalated — avoid loops)
        if (c.stage != Conversation.ESCALATED &&
            Regex("(?i)\\b(call( me)?|human|help|person|agent|stop)\\b").containsMatchIn(t)
        ) {
            escalate(ctx, phone, c, "customer asked for human: \"${t.take(80)}\"")
            val msg = "On it — ${Prefs.firstName(ctx)} will call you back shortly. 👍"
            SmsSender.sendNow(ctx, phone, msg)
            repo.addMessage(phone, ChatMessage.OUT, msg, auto = true)
            return
        }
        when (c.stage) {
            Conversation.AWAIT_SERVICE -> onService(ctx, phone, c, t)
            Conversation.AWAIT_SLOT -> onSlot(ctx, phone, c, t)
            Conversation.AWAIT_DETAILS -> onDetails(ctx, phone, c, t)
            Conversation.AWAIT_PROBLEM -> onProblem(ctx, phone, c, t)
            Conversation.IDLE -> onIdleText(ctx, phone, c, t)
            else -> BotNotify.newText(ctx, phone, t) // PENDING/ESCALATED/DONE: human loop
        }
    }

    // ---------- stage handlers ----------

    private suspend fun onService(ctx: Context, phone: String, c: Conversation, t: String) {
        val repo = appRepo(ctx)
        val svc = Prefs.services(ctx)
        val pick = t.filter { it.isDigit() }.firstOrNull()?.digitToIntOrNull()
        if (pick == null || pick < 1 || pick > svc.size) {
            // Long text instead of a number? Treat as a problem description (no dead-ends).
            if (t.split("\\s+".toRegex()).size >= 3) {
                onProblem(ctx, phone, c.copy(stage = Conversation.AWAIT_PROBLEM), t)
                return
            }
            resendMenu(ctx, phone, c, "Sorry — just reply with a number (1-${svc.size}).")
            return
        }
        val name = svc[pick - 1]
        if (pick == svc.size) { // last item = Other -> escalation path
            repo.updateConvo(c.copy(stage = Conversation.AWAIT_PROBLEM, service = name))
            val msg = "No problem — tell me about it. 👇\nJust describe your problem in one text " +
                "and I'll have ${Prefs.firstName(ctx)} take a look personally."
            SmsSender.sendNow(ctx, phone, msg)
            repo.addMessage(phone, ChatMessage.OUT, msg, auto = true)
            repo.log("BOT", "$phone chose OTHER")
            return
        }
        val known = repo.customerByPhone(phone)
        val slots = Availability.nextTwoSlots(ctx)
        if (slots.size < 2) {
            escalate(ctx, phone, c.copy(service = name), "no free slots for $name")
            SmsSender.sendNow(ctx, phone,
                "Thanks — ${Prefs.firstName(ctx)} is fully booked this week. He'll text you shortly with options. 👍")
            return
        }
        repo.updateConvo(c.copy(
            stage = Conversation.AWAIT_SLOT, service = name,
            customerId = known?.id ?: c.customerId
        ))
        val msg = repo.render(MsgTemplate.SLOTS,
            mapOf("service" to name, "slotA" to slots[0].label, "slotB" to slots[1].label))
            .ifBlank { "Got it — $name 👍 I'm free at:\nA. ${slots[0].label}\nB. ${slots[1].label}\n\nReply A or B." }
        SmsSender.sendNow(ctx, phone, msg)
        repo.addMessage(phone, ChatMessage.OUT, msg, auto = true)
        repo.log("BOT", "$phone service=$name")
    }

    private suspend fun onSlot(ctx: Context, phone: String, c: Conversation, t: String) {
        val repo = appRepo(ctx)
        val slots = Availability.nextTwoSlots(ctx)
        val idx = when (t.trim().uppercase().firstOrNull()) {
            'A', '1' -> 0
            'B', '2' -> 1
            else -> -1
        }
        if (idx < 0 || slots.size < 2) {
            if (slots.size < 2) {
                escalate(ctx, phone, c, "slots filled mid-flow")
                SmsSender.sendNow(ctx, phone,
                    "Hmm, those just filled up! ${Prefs.firstName(ctx)} will text you fresh times shortly. 👍")
            } else {
                val msg = "Just reply A or B 🙂\nA. ${slots[0].label}\nB. ${slots[1].label}"
                SmsSender.sendNow(ctx, phone, msg)
                repo.addMessage(phone, ChatMessage.OUT, msg, auto = true)
            }
            return
        }
        val known = repo.customerByPhone(phone)
        if (known != null) { // known customer: skip name/address, straight to confirm
            repo.updateConvo(c.copy(
                stage = Conversation.PENDING_CONFIRM,
                slotLabel = slots[idx].label, slotAt = slots[idx].at,
                name = known.name, address = known.address, customerId = known.id
            ))
            BotNotify.pendingConfirm(ctx, phone, known.name, c.service, slots[idx].label)
            repo.log("BOT", "$phone known ${known.name}, awaiting confirm")
            if (Prefs.autoConfirm(ctx)) confirmBooking(ctx, phone)
            return
        }
        repo.updateConvo(c.copy(
            stage = Conversation.AWAIT_DETAILS,
            slotLabel = slots[idx].label, slotAt = slots[idx].at
        ))
        val msg = "Perfect — ${slots[idx].label}. Just reply with your name + address."
        SmsSender.sendNow(ctx, phone, msg)
        repo.addMessage(phone, ChatMessage.OUT, msg, auto = true)
    }

    private suspend fun onDetails(ctx: Context, phone: String, c: Conversation, t: String) {
        val repo = appRepo(ctx)
        val parts = t.split(",", limit = 2).map { it.trim() }.filter { it.isNotEmpty() }
        val (name, addr) = if (parts.size == 2) parts[0] to parts[1] else "New customer" to t
        repo.updateConvo(c.copy(
            stage = Conversation.PENDING_CONFIRM,
            name = name.take(60), address = addr.take(200)
        ))
        BotNotify.pendingConfirm(ctx, phone, name, c.service, c.slotLabel)
        repo.log("BOT", "$phone details -> pending confirm")
        if (Prefs.autoConfirm(ctx)) confirmBooking(ctx, phone)
    }

    private suspend fun onProblem(ctx: Context, phone: String, c: Conversation, t: String) {
        val repo = appRepo(ctx)
        repo.updateConvo(c.copy(
            stage = Conversation.ESCALATED, problem = t.take(500), needsHuman = true
        ))
        val msg = "Thanks! I've sent this straight to ${Prefs.firstName(ctx)}. 📲 " +
            Availability.freeAfterLabel(ctx) + "."
        SmsSender.sendNow(ctx, phone, msg)
        repo.addMessage(phone, ChatMessage.OUT, msg, auto = true)
        BotNotify.priorityAlert(ctx, phone, t)
        AlertNudgeWorker.scheduleOnce(ctx, phone)
        repo.log("BOT", "$phone ESCALATED: ${t.take(80)}")
    }

    /** Customer texts first (no missed call). Day off / Busy + cooled down -> auto-reply. */
    private suspend fun onIdleText(ctx: Context, phone: String, c: Conversation, t: String) {
        val repo = appRepo(ctx)
        val now = System.currentTimeMillis()
        if (repo.offOn(Repo.todayStr()) != null) {
            if (now - c.lastAutoReplyAt > COOLDOWN_MS) startOffLead(ctx, phone)
            else BotNotify.newText(ctx, phone, t)
            return
        }
        if (Prefs.isBusy(ctx) && now - c.lastAutoReplyAt > COOLDOWN_MS) {
            val known = repo.customerByPhone(phone)
            val menu = if (known != null) Prefs.menuText(ctx, known.name.substringBefore(" "))
            else Prefs.menuText(ctx, null)
            SmsSender.sendNow(ctx, phone, menu)
            repo.addMessage(phone, ChatMessage.OUT, menu, auto = true)
            repo.updateConvo(c.copy(
                stage = Conversation.AWAIT_SERVICE,
                lastAutoReplyAt = now, customerId = known?.id
            ))
            BotNotify.autoReplied(ctx, phone, known?.name)
        } else {
            BotNotify.newText(ctx, phone, t)
        }
    }

    /**
     * Day-off lead capture: unavailability notice (no menu), lead parked quietly
     * for Mike's return. No loud alert, no nudge — he's on holiday.
     */
    suspend fun startOffLead(ctx: Context, phone: String) {
        val repo = appRepo(ctx)
        val off = repo.offOn(Repo.todayStr())
        val span = if (off == null || off.startDate == off.endDate) "today"
        else "from ${Repo.prettyDay(off.startDate)} to ${Repo.prettyDay(off.endDate)}"
        val msg = Prefs.offText(ctx, span, off?.reason ?: "")
        SmsSender.sendNow(ctx, phone, msg)
        repo.addMessage(phone, ChatMessage.OUT, msg, auto = true)
        val c = repo.convo(phone)
        repo.updateConvo(c.copy(
            stage = Conversation.ESCALATED, needsHuman = true,
            lastAutoReplyAt = System.currentTimeMillis()
        ))
        BotNotify.newText(ctx, phone, "Day-off lead ($span) — waiting on customer reply.")
        repo.log("BOT", "$phone off-lead captured ($span)")
    }

    private suspend fun resendMenu(ctx: Context, phone: String, c: Conversation, prefix: String) {
        val repo = appRepo(ctx)
        val msg = "$prefix\n\n" + Prefs.menuText(ctx, null)
        SmsSender.sendNow(ctx, phone, msg)
        repo.addMessage(phone, ChatMessage.OUT, msg, auto = true)
        repo.updateConvo(c.copy(stage = Conversation.AWAIT_SERVICE))
    }

    private suspend fun escalate(ctx: Context, phone: String, c: Conversation, reason: String) {
        val repo = appRepo(ctx)
        repo.updateConvo(c.copy(stage = Conversation.ESCALATED, needsHuman = true))
        BotNotify.priorityAlert(ctx, phone, reason)
        AlertNudgeWorker.scheduleOnce(ctx, phone)
        repo.log("BOT", "$phone escalated: $reason")
    }

    // ---------- Mike's in-app actions (called from UI in M3) ----------

    /** ✅ I can do this -> customer gets slot offer, flow continues. */
    suspend fun resolveYes(ctx: Context, phone: String) {
        val repo = appRepo(ctx)
        val c = repo.convo(phone)
        val slots = Availability.nextTwoSlots(ctx)
        if (slots.size < 2) {
            escalate(ctx, phone, c, "Mike said yes but no slots free")
            return
        }
        repo.updateConvo(c.copy(stage = Conversation.AWAIT_SLOT, needsHuman = false))
        val msg = "Good news — ${Prefs.firstName(ctx)} says he can handle this! 👍 He's free at:\n" +
            "A. ${slots[0].label}\nB. ${slots[1].label}\n\nReply A or B."
        SmsSender.sendNow(ctx, phone, msg)
        repo.addMessage(phone, ChatMessage.OUT, msg, auto = true)
        BotNotify.clearAlert(ctx, phone)
    }

    /** ❌ Can't help -> polite decline text, flow ends. */
    suspend fun resolveNo(ctx: Context, phone: String) {
        val repo = appRepo(ctx)
        val c = repo.convo(phone)
        val msg = repo.render(MsgTemplate.DECLINE, mapOf("biz" to Prefs.bizName(ctx).ifBlank { "us" }))
            .ifBlank { "Thanks for reaching out! Unfortunately this isn't something we handle. Sorry about that!" }
        SmsSender.sendNow(ctx, phone, msg)
        repo.addMessage(phone, ChatMessage.OUT, msg, auto = true)
        repo.updateConvo(c.copy(stage = Conversation.DONE, needsHuman = false))
        BotNotify.clearAlert(ctx, phone)
    }

    /** ✅ Confirm booking -> creates customer (if new) + job, sends booked text. Returns job id. */
    suspend fun confirmBooking(ctx: Context, phone: String): Long? {
        val repo = appRepo(ctx)
        val c = repo.convo(phone)
        if (c.stage != Conversation.PENDING_CONFIRM || c.slotAt <= 0) return null
        var cust = repo.customerByPhone(phone)
        if (cust == null) {
            repo.saveCustomer(Customer(
                name = c.name.ifBlank { "New customer" }, phone = phone, address = c.address
            ))
            cust = repo.customerByPhone(phone)
        } else if (c.address.isNotBlank() && cust.address.isBlank()) {
            repo.saveCustomer(cust.copy(address = c.address))
        }
        val cid = cust?.id ?: return null
        val durMs = Prefs.typicalJobMins(ctx) * 60_000L
        val jobId = repo.saveJob(Job(
            customerId = cid,
            title = "${c.service.ifBlank { "Service" }} — ${cust.name}",
            service = c.service,
            startAt = c.slotAt, endAt = c.slotAt + durMs,
            status = Job.SCHEDULED
        ))
        val msg = "✅ You're booked for ${c.slotLabel}, ${cust.name.substringBefore(" ")}! " +
            "I'll text when I'm on my way. - ${Prefs.firstName(ctx)}"
        SmsSender.sendNow(ctx, phone, msg)
        repo.addMessage(phone, ChatMessage.OUT, msg, auto = true)
        repo.updateConvo(c.copy(stage = Conversation.DONE, needsHuman = false, customerId = cid))
        BotNotify.clearAlert(ctx, phone)
        repo.log("BOT", "$phone BOOKED job#$jobId ${c.slotLabel}")
        return jobId
    }
}
