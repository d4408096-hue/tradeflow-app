package com.tradeflow.core.engine

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch

/** Result of each SmsSender.sendNow. Failures raise the ⚠️ alert (throttled). */
class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != SmsSender.ACTION_SMS_STATUS) return
        val phone = intent.getStringExtra("phone") ?: return
        val ok = resultCode == Activity.RESULT_OK
        val pending = goAsync()
        EngineScope.launch {
            val appCtx = ctx.applicationContext
            val repo = appRepo(appCtx)
            if (ok) {
                repo.log("SMS", "sent ✓ $phone")
            } else {
                repo.log("SMS", "FAILED $phone code=$resultCode")
                val now = System.currentTimeMillis()
                if (now - Prefs.lastFailAt(appCtx) > 10 * 60 * 1000) {
                    Prefs.setLastFailAt(appCtx, now)
                    BotNotify.sendFailed(appCtx, phone)
                }
            }
            pending.finish()
        }
    }
}
