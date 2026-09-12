package com.tradeflow.core

import android.app.Application
import com.tradeflow.core.data.Repo
import com.tradeflow.core.data.TradeFlowDb

/** App holder: single DB + Repo for UI, services and receivers. Declared in manifest. */
class TradeFlowApp : Application() {
    val db by lazy { TradeFlowDb.get(this) }
    val repo by lazy { Repo(db) }

    override fun onCreate() {
        super.onCreate()
        // Patch #3: suicide note — any crash auto-logs its cause to Diagnostics
        // before the process dies, so Copy Log always contains the reason.
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val th = Thread {
                    runCatching {
                        kotlinx.coroutines.runBlocking {
                            repo.log("CRASH", "${e.javaClass.simpleName}: ${e.message} @ ${e.stackTrace.firstOrNull()}")
                            repo.log("CRASH", e.stackTraceToString().take(1500))
                        }
                    }
                }
                th.start()
                th.join(2500) // brief wait so the note lands before death
            } catch (_: Exception) {
            }
            prev?.uncaughtException(t, e)
        }
    }
}
