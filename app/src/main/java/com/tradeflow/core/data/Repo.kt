package com.tradeflow.core.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thin helper over the DAOs. UI + engine talk to Repo, never to DAOs directly.
 * Access via (context.applicationContext as TradeFlowApp).repo
 */
class Repo(private val db: TradeFlowDb) {

    companion object {
        /** Normalize to US 10-digit: "+1 (512) 555-0134" -> "5125550134". */
        fun norm(p: String): String {
            val d = p.filter { it.isDigit() }
            return if (d.length == 11 && d.startsWith("1")) d.drop(1) else d
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
        db.messages().insert(ChatMessage(phone = p, dir = dir, body = body, auto = auto))
        val c = convo(p)
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
