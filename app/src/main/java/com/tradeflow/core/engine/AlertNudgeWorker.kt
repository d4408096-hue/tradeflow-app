package com.tradeflow.core.engine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/** One-shot 30-min re-nudge: fires only if the escalation is still unhandled. */
class AlertNudgeWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val phone = inputData.getString("phone") ?: return Result.success()
        val repo = appRepo(applicationContext)
        val c = repo.convo(phone)
        if (c.needsHuman) {
            BotNotify.priorityAlert(applicationContext, phone,
                "Reminder: ${c.problem.ifBlank { "customer" }} still waiting on you.")
            repo.log("BOT", "$phone re-nudged")
        }
        return Result.success()
    }

    companion object {
        fun scheduleOnce(ctx: Context, phone: String) {
            val req = OneTimeWorkRequestBuilder<AlertNudgeWorker>()
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setInputData(workDataOf("phone" to phone))
                .addTag("nudge-$phone")
                .build()
            WorkManager.getInstance(ctx).enqueue(req)
        }
    }
}
