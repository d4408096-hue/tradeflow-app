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

/**
 * Foreground guard: the persistent "🟢 active" indicator.
 * HARDENED (Patch #2): every line is crash-proofed. Worst case the guard
 * silently degrades to a regular notification — the app can NEVER die here.
 * Note: the call/SMS engine does NOT depend on this service (manifest
 * receivers fire regardless), so Busy Mode works even in compat mode.
 */
class BusyModeService : Service() {

    override fun onCreate() {
        super.onCreate()
        runCatching {
            if (Build.VERSION.SDK_INT >= 26) {
                getSystemService(NotificationManager::class.java).createNotificationChannel(
                    NotificationChannel("busy", "Service", NotificationManager.IMPORTANCE_LOW))
            }
        }.onFailure { crashLog(this, "onCreate", it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val err = runCatching { startForeground(1001, note(this)) }.exceptionOrNull()
        if (err != null) {
            crashLog(this, "startForeground", err)
            runCatching { BotNotify.compatGuard(this) }
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun note(ctx: Context): Notification {
        val pi = PendingIntent.getActivity(
            ctx, 7, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val biz = runCatching { Prefs.bizName(ctx).ifBlank { "TradeFlow" } }.getOrDefault("TradeFlow")
        val on = runCatching { Prefs.isBusy(ctx) }.getOrDefault(true)
        return NotificationCompat.Builder(ctx, "busy")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🟢 $biz is active")
            .setContentText(if (on) "Busy Mode ON — auto-reply watching" else "App running")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    /**
     * Writes the failure reason to Diagnostics from a background thread
     * (Room forbids main-thread writes, and we may be mid-crash).
     */
    private fun crashLog(ctx: Context, where: String, e: Throwable) {
        val appCtx = ctx.applicationContext
        Thread {
            runCatching {
                kotlinx.coroutines.runBlocking {
                    appRepo(appCtx).log("SYS", "FGS $where ERR: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }.start()
    }

    companion object {
        const val ACTION_STOP = "com.tradeflow.core.STOP_BUSY"

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, BusyModeService::class.java))
        }

        fun stop(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, BusyModeService::class.java)) }
        }
    }
}
