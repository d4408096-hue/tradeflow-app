package com.tradeflow.core.engine

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tradeflow.core.MainActivity

/** All Mike-facing notifications. Two channels: chat (quiet) + alerts (🚨 loud). */
object BotNotify {
    private const val CHAT = "chat"
    private const val ALERT = "alerts"

    private fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensure(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val m = ctx.getSystemService(NotificationManager::class.java)
        m.createNotificationChannel(
            NotificationChannel(CHAT, "Messages", NotificationManager.IMPORTANCE_DEFAULT))
        val a = NotificationChannel(ALERT, "Needs you", NotificationManager.IMPORTANCE_HIGH)
        a.enableVibration(true)
        m.createNotificationChannel(a)
    }

    private fun openApp(ctx: Context, phone: String?): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).putExtra("phone", phone)
        return PendingIntent.getActivity(
            ctx, (phone ?: "x").hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun post(ctx: Context, id: Int, channel: String, title: String, text: String,
                     phone: String?, alert: Boolean) {
        ensure(ctx)
        if (!canPost(ctx)) return
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(if (alert) android.R.drawable.ic_dialog_alert
                          else android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(ctx, phone))
            .setAutoCancel(true)
            .setPriority(if (alert) NotificationCompat.PRIORITY_HIGH
                         else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    fun missedIdle(ctx: Context, phone: String) =
        post(ctx, phone.hashCode(), CHAT, "Missed call",
            "$phone — Busy Mode off, no auto-reply.", phone, false)

    fun autoReplied(ctx: Context, phone: String, name: String?) =
        post(ctx, phone.hashCode(), CHAT, "Auto-replied ✅",
            "${name ?: phone}: menu sent, bot handling it.", phone, false)

    fun newText(ctx: Context, phone: String, preview: String) =
        post(ctx, phone.hashCode(), CHAT, "New text — $phone", preview.take(120), phone, false)

    fun pendingConfirm(ctx: Context, phone: String, name: String, service: String, slot: String) =
        post(ctx, phone.hashCode(), CHAT, "📅 Tap to confirm booking",
            "$name · $service · $slot · $phone", phone, false)

    fun priorityAlert(ctx: Context, phone: String, preview: String) =
        post(ctx, -phone.hashCode(), ALERT, "🚨 NEEDS YOU",
            "$phone: ${preview.take(140)}", phone, true)

    fun sendFailed(ctx: Context, phone: String) =
        post(ctx, -phone.hashCode() + 1, ALERT, "⚠️ Auto-reply FAILED",
            "Couldn't text $phone — call back manually!", phone, true)

    fun permNeeded(ctx: Context) {
        val now = System.currentTimeMillis()
        if (now - Prefs.lastPermNag(ctx) < 24 * 60 * 60 * 1000) return
        Prefs.setLastPermNag(ctx, now)
        post(ctx, 999001, ALERT, "⚠️ Permissions needed",
            "Open the app and grant Calls + SMS permissions, or auto-reply can't work.",
            null, true)
    }

    fun clearAlert(ctx: Context, phone: String) {
        NotificationManagerCompat.from(ctx).cancel(-phone.hashCode())
    }
}
