package com.tradeflow.core.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import kotlinx.coroutines.launch

/**
 * Tracks RINGING -> IDLE transitions. OFFHOOK (answered) cancels tracking.
 * Heavy work runs in EngineScope with goAsync() so the broadcast stays alive.
 */
class CallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val now = System.currentTimeMillis()
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val num = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Prefs.setLastRinging(ctx, num, now)
                EngineScope.launch {
                    runCatching { appRepo(ctx).log("CALL", "RINGING ${mask(num)}") }
                }
            }
            // Patch #10: log human-answered calls so the log has no mystery gaps.
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val hadRinging = Prefs.getLastRinging(ctx).first != null
                Prefs.clearLastRinging(ctx)
                if (hadRinging) {
                    EngineScope.launch {
                        runCatching { appRepo(ctx).log("CALL", "answered by human, staying silent") }
                    }
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val (num, at) = Prefs.getLastRinging(ctx)
                Prefs.clearLastRinging(ctx)
                if (num != null && now - at < 5 * 60 * 1000) {
                    val pending = goAsync()
                    EngineScope.launch {
                        runCatching { MissedCallHandler.onCallEnded(ctx.applicationContext, num) }
                            .onFailure {
                                runCatching { appRepo(ctx.applicationContext).log("CALL", "ERR " + it.message) }
                            }
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun mask(n: String?): String =
        if (n.isNullOrBlank()) "unknown" else "••" + n.filter { it.isDigit() }.takeLast(4)
}
