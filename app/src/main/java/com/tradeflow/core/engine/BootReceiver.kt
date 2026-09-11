package com.tradeflow.core.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** After reboot: restart the service if Busy Mode was left ON. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Prefs.isBusy(ctx)) BusyModeService.start(ctx.applicationContext)
    }
}
