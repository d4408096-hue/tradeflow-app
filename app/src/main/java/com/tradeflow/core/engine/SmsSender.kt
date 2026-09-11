package com.tradeflow.core.engine

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

/** Sends SMS via the phone's own SIM. Delivery result lands in SmsStatusReceiver. */
object SmsSender {
    const val ACTION_SMS_STATUS = "com.tradeflow.core.SMS_STATUS"

    /** Returns false only if unsendable (no permission / exception). True = handed to radio. */
    fun sendNow(ctx: Context, to: String, body: String): Boolean {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            EngineScope.launch { appRepo(ctx).log("SMS", "SEND_SMS permission missing") }
            BotNotify.permNeeded(ctx)
            return false
        }
        return try {
            val sm = ctx.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            val parts = sm.divideMessage(body)
            val pi = PendingIntent.getBroadcast(
                ctx, to.hashCode(),
                Intent(ACTION_SMS_STATUS).setPackage(ctx.packageName).putExtra("phone", to),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (parts.size <= 1) sm.sendTextMessage(to, null, body, pi, null)
            else sm.sendMultipartTextMessage(to, null, parts, ArrayList(parts.map { pi }), null)
            EngineScope.launch { appRepo(ctx).log("SMS", "queued -> $to (${parts.size} part)") }
            true
        } catch (e: Exception) {
            EngineScope.launch { appRepo(ctx).log("SMS", "EX " + e.message) }
            false
        }
    }
}
