package com.tradeflow.core.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Thin helper over the DAOs. UI + engine talk to Repo, never to DAOs directly.
 * Access via (context.applicationContext as TradeFlowApp).repo
 */
class Repo(private val db: TradeFlowDb) {

    companion object {
        /**
         * Normalize to last-10-digits: "+1 (512) 555-0134" -> "5125550134",
         * "+91 72764 05063" -> "7276405063", short codes untouched.
         * Keeps call-log and SMS sender formats matched on all carriers.
         */
        fun norm(p: String): String {
            val d = p.filter { it.isDigit() }
            return if (d.length > 10) d.takeLast(10) else d
        }

        fun todayStr(): String = LocalDate.now().toString() // ISO yyyy-MM-dd

        /** "2026-09-14" -> "Mon 14" */
        fun prettyDay(iso: String): String = try {
            LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEE d", Locale.US))
        } catch (_: Exception) {
            iso
        }
    }

    // ---------- customers ----------
    fun customers(): Flow<List<Customer>> = db.customers().all()
    fun searchCustomers(q: String): Flow<List<Customer>> = db.customers().search(q)
    suspend fun customerByPhone(phone: String): Customer? = db.customers().byPhone(norm(phone))
    suspend fun saveCustomer(c: Customer): Long = db.customers().upsert(c.copy(phone = norm(c.phone)))
    suspend fun deleteCustomer(c: Customer) = db.customers().delete(c)

    // ---------- jobs ----------
    fun jobs(): Flow<List<Job>> = db.jobs().all()
    fun jobsInRange(from: Long, to: Long): Flow<List<Job>> = db.jobs().inRange(from, to)
    fun jobsForCustomer(cid: Long): Flow<List<Job>> = db.jobs().forCustomer(cid)
    suspend fun currentJob(): Job? = db.jobs().current()
    suspend fun clashingJobs(from: Long, to: Long) = db.jobs().clashing(from, to)
    suspend fun saveJob(j: Job): Long = db.jobs().upsert(j)
    suspend fun setJobStatus(id: Long, s: String) = db.jobs().setStatus(id, s)
    suspend fun markJobPaid(id: Long) = db.jobs().markPaid(id)
    suspend fun deleteJob(j: Job) = db.jobs().delete(j)
    fun paidTotal(): Flow<Long> = db.jobs().paidTotal()
    fun bookedCount(): Flow<Int> = db.jobs().bookedCount()

    // ---------- conversations + messages ----------
    fun conversations(): Flow<List<Conversation>> = db.conversations().all()
    fun escalated(): Flow<List<Conversation>> = db.conversations().escalated()
    fun messagesFor(phone: String): Flow<List<ChatMessage>> = db.messages().forPhone(norm(phone))
    fun autoMsgCount(): Flow<Int> = db.messages().autoCount()

    // Patch #11: truthful ROI counter — unique leads caught, not texts sent.
    fun caughtCount(): Flow<Int> = db.conversations().autoRepliedCount()

    suspend fun convo(phone: String): Conversation {
        val p = norm(phone)
        return db.conversations().byPhone(p) ?: Conversation(phone = p).also {
            db.conversations().upsert(it)
        }
    }

    suspend fun updateConvo(c: Conversation) =
        db.conversations().upsert(c.copy(lastMsgAt = System.currentTimeMillis()))

    suspend fun addMessage(phone: String, dir: String, body: String, auto: Boolean = false) {
        val p = norm(phone)
        // Patch #13: parent FIRST — messages FK to conversations. Inserting before the
        // thread exists crashed on first-contact texts (walk-ins, OTPs, any new number).
        val c = convo(p)
        db.messages().insert(ChatMessage(phone = p, dir = dir, body = body, auto = auto))
        db.conversations().upsert(
            c.copy(lastMsgAt = System.currentTimeMillis(), unread = c.unread + 1)
        )
    }

    suspend fun clearUnread(phone: String) = db.conversations().clearUnread(norm(phone))

    /** Testing only: wipes conversations + messages (jobs/customers stay). */
    suspend fun clearTestData() {
        db.messages().clearAll()
        db.conversations().clearAll()
        log("SYS", "test data reset")
    }

    // ---------- days off ----------
    fun dayOffs(): Flow<List<DayOff>> = db.daysoff().all()
    suspend fun offOn(date: String): DayOff? = db.daysoff().offOn(date)
    suspend fun addDayOff(d: DayOff): Long = db.daysoff().add(d)
    suspend fun removeDayOffOn(date: String) = db.daysoff().deleteOn(date)

    // ---------- templates ----------
    suspend fun template(key: String): MsgTemplate? = db.templates().byKey(key)
    suspend fun saveTemplate(t: MsgTemplate) = db.templates().upsert(t)

    /** Fill {name} {amount} {link} {slot} {biz} placeholders. Missing keys stay as-is. */
    suspend fun render(key: String, vars: Map<String, String>): String {
        val body = db.templates().byKey(key)?.body ?: return ""
        var out = body
        vars.forEach { (k, v) -> out = out.replace("{$k}", v) }
        return out
    }

    // ---------- diagnostics ----------
    fun diag(n: Int): Flow<List<DiagEvent>> = db.diag().latest(n)

    suspend fun log(tag: String, text: String) {
        db.diag().insert(DiagEvent(tag = tag, text = text))
        db.diag().prune(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)
    }

    /** Formatted log for the Copy Log button. Newest first. */
    suspend fun diagText(n: Int): String {
        val f = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)
        return db.diag().latestNow(n).reversed().joinToString("\n") {
            "${f.format(Date(it.at))} [${it.tag}] ${it.text}"
        }
    }
}
