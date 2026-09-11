package com.tradeflow.core.ui.vm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradeflow.core.TradeFlowApp
import com.tradeflow.core.engine.BotNotify
import com.tradeflow.core.engine.BusyModeService
import com.tradeflow.core.engine.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashVm(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TradeFlowApp).repo

    val caught: StateFlow<Int> = repo.autoMsgCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val booked: StateFlow<Int> = repo.bookedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val revenue: StateFlow<Long> = repo.paidTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun load(ctx: Context) { _busy.value = Prefs.isBusy(ctx) }

    fun setBusy(ctx: Context, b: Boolean) {
        val appCtx = ctx.applicationContext
        try {
            Prefs.setBusy(appCtx, b)
            _busy.value = b
            if (b) BusyModeService.start(appCtx)
            else {
                BusyModeService.stop(appCtx)
                BotNotify.clearGuard(appCtx)
            }
        } catch (e: Exception) {
            // Engine gates on the pref (already saved) — toggle still "works".
            _busy.value = b
            viewModelScope.launch {
                runCatching { repo.log("SYS", "BUSY BTN ERR: ${e.javaClass.simpleName}: ${e.message}") }
            }
            return
        }
        viewModelScope.launch { repo.log("SYS", "busy=$b") }
    }
}
