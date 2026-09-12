package com.tradeflow.core.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.tradeflow.core.data.ChatMessage
import com.tradeflow.core.data.Conversation
import com.tradeflow.core.data.Repo
import java.util.concurrent.TimeUnit

/** Verifies a RINGING->IDLE was truly missed, then fires the auto-reply (if Busy + cooled down). */
object MissedCallHandler {

    suspend fun onCallEnded(ctx: Context, ringNumber: String?) {
        val repo = appRepo(ctx)
        val num = ringNumber?.let { Repo.norm(it) } ?: ""
        if (num.length < 7) {
            repo.log("CALL", "IDLE ignored (no caller id)")
            return
        }
        if (!verifyMissed(ctx, num)) {
            repo.log("CALL", "IDLE $num was answered/diverted")
            return
        }
        repo.log("CALL", "MISSED $num")
        val c = repo.convo(num)
        val now = System.currentTimeMillis()
        if (now - c.lastAutoReplyAt < COOLDOWN_MS) {
            repo.log("BOT", "cooldown skip $num")
            BotNotify.missedIdle(ctx, num)
            return
        }
        if (repo.offOn(Repo.todayStr()) != null) { // day off beats the Busy toggle
            BookingBot.startOffLead(ctx, num)
            return
        }
        if (!Prefs.isBusy(ctx)) {
            BotNotify.missedIdle(ctx, num)
            return
        }
        val known = repo.customerByPhone(num)
        val text = if (known != null) Prefs.menuText(ctx, known.name.substringBefore(" "))
        else Prefs.menuText(ctx, null)
        val ok = SmsSender.sendNow(ctx, num, text)
        if (ok) {
            repo.addMessage(num, ChatMessage.OUT, text, auto = true)
            repo.updateConvo(c.copy(
                stage = Conversation.AWAIT_SERVICE,
                service = "",
                lastAutoReplyAt = now,
                customerId = known?.id ?: c.customerId,
                needsHuman = false
            ))
            BotNotify.autoReplied(ctx, num, known?.name)
            repo.log("SMS", "menu sent to $num")
        } else {
            BotNotify.sendFailed(ctx, num)
            repo.log("SMS", "menu FAILED to $num")
        }
    }

    /** True only if a fresh MISSED_TYPE row exists for this number. Marks it handled. */
    private suspend fun verifyMissed(ctx: Context, num: String): Boolean {
        if (queryLogOnce(ctx, num)) return true
        // IDLE can beat the call-log write by ~1-2s — one retry before giving up.
        appRepo(ctx).log("CALL", "verify retry for $num")
        Thread.sleep(2500) // IO thread (EngineScope), safe to block briefly
        return queryLogOnce(ctx, num)
    }

    /** Single call-log pass for verifyMissed. Marks the matched row handled. */
    private fun queryLogOnce(ctx: Context, num: String): Boolean {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            BotNotify.permNeeded(ctx)
            return false
        }
        val cur = try {
            ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                CallLog.Calls.TYPE + "=?",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString()),
                // Patch #9: sortOrder must be a PURE order clause. LIMIT here throws
                // SQLiteException "Invalid token LIMIT" and kills the whole missed-call flow.
                CallLog.Calls.DATE + " DESC"
            )
        } catch (_: Exception) {
            null
        } ?: return true // unreadable log -> trust the RINGING->IDLE signal
        cur.use {
            val ni = it.getColumnIndex(CallLog.Calls.NUMBER)
            val di = it.getColumnIndex(CallLog.Calls.DATE)
            val since = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(6)
            val lastHandled = Prefs.lastHandledCallDate(ctx)
            var checked = 0
            while (it.moveToNext() && checked++ < 6) { // row cap lives here now, not in SQL
                val d = it.getLong(di)
                if (d <= lastHandled || d < since) continue
                val n = Repo.norm(it.getString(ni) ?: "")
                if (n == num || (n.length >= 7 && num.endsWith(n)) || (num.length >= 7 && n.endsWith(num))) {
                    Prefs.setLastHandledCallDate(ctx, d)
                    return true
                }
            }
        }
        return false
    }
}
