package com.tradeflow.core.engine

import com.tradeflow.core.data.MsgTemplate
import com.tradeflow.core.data.Repo

/** First-run seeds. Called once from MainActivity (wired in M3). */
object Defaults {
    val SERVICES = listOf("Faucet/Leak", "Drain", "Toilet", "Water Heater", "Other")

    suspend fun ensureSeeded(repo: Repo) {
        if (repo.template(MsgTemplate.ON_MY_WAY) != null) return
        repo.saveTemplate(MsgTemplate(MsgTemplate.ON_MY_WAY, "On my way",
            "On my way, {name}! See you at {slot}. - {biz}"))
        repo.saveTemplate(MsgTemplate(MsgTemplate.BILL_REVIEW, "Bill + review ask",
            "All done, {name}! Total: {amount}. Pay here: {link}\nIf you were happy, a quick review means a lot: {review}"))
        repo.saveTemplate(MsgTemplate(MsgTemplate.DECLINE, "Can't help",
            "Thanks for reaching out! Unfortunately this isn't something we handle. Sorry about that! - {biz}"))
        repo.saveTemplate(MsgTemplate(MsgTemplate.NUDGE, "Stalled nudge",
            "Hi {name}, just checking — still need help with {service}? Reply YES and I'll get you booked. - {biz}"))
        repo.saveTemplate(MsgTemplate(MsgTemplate.SLOTS, "Slot offer",
            "Got it — {service} 👍 I'm free at:\nA. {slotA}\nB. {slotB}\n\nReply A or B."))
        awaitLog(repo)
    }

    private suspend fun awaitLog(repo: Repo) {
        repo.log("SYS", "default templates seeded")
    }
}
