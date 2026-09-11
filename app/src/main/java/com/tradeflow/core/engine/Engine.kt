package com.tradeflow.core.engine

import android.content.Context
import com.tradeflow.core.TradeFlowApp
import com.tradeflow.core.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Background scope for receivers/services. IO-bound engine work only. */
val EngineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Repo accessor for receivers/services (UI uses ViewModels in M3). */
fun appRepo(ctx: Context): Repo = (ctx.applicationContext as TradeFlowApp).repo

/** Min gap between two auto-replies to the same number. */
const val COOLDOWN_MS = 2L * 60 * 60 * 1000
