package com.tradeflow.core.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tradeflow.core.MainActivity

/** Foreground service: the persistent "🟢 active" notification. Toggled from the app. */
class BusyModeService : Service() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("busy", "Service", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(1001, note(this))
        return START_STICKY
    }

    private fun note(ctx: Context): Notification {
        val pi = PendingIntent.getActivity(
            ctx, 7, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(ctx, "busy")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🟢 ${Prefs.bizName(ctx).ifBlank { "TradeFlow" }} is active")
            .setContentText(if (Prefs.isBusy(ctx)) "Busy Mode ON — auto-reply watching"
                            else "App running")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.tradeflow.core.STOP_BUSY"

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, BusyModeService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, BusyModeService::class.java))
        }
    }
}
