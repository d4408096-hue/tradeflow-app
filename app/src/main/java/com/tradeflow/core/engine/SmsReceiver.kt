package com.tradeflow.core.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.tradeflow.core.data.ChatMessage
import com.tradeflow.core.data.Repo
import kotlinx.coroutines.launch

/** Incoming SMS -> log as IN message -> route to BookingBot. Never blocks other apps. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pdus = intent.extras?.get("pdus") as? Array<*> ?: return
        val format = intent.getStringExtra("format")
        val pending = goAsync()
        EngineScope.launch {
            try {
                val appCtx = ctx.applicationContext
                val msgs = pdus.mapNotNull { SmsMessage.createFromPdu(it as ByteArray, format) }
                val from = msgs.firstOrNull()?.originatingAddress ?: return@launch
                val body = msgs.joinToString("") { it.messageBody ?: "" }
                if (body.isBlank()) return@launch
                val repo = appRepo(appCtx)
                val phone = Repo.norm(from)
                // Patch #14: alphanumeric/bank senders ("KOTAKB") norm to "" — nothing
                // dialable to reply to. Skip WITHOUT creating a ghost thread.
                if (phone.length < 7) {
                    repo.log("SMS", "IN from non-dialable sender, ignored")
                    return@launch
                }
                repo.addMessage(phone, ChatMessage.IN, body)
                repo.log("SMS", "IN $phone: ${body.take(60)}")
                BookingBot.handleReply(appCtx, phone, body)
            } finally {
                pending.finish()
            }
        }
    }
}
