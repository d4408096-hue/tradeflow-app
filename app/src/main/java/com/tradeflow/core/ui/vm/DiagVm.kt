package com.tradeflow.core.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradeflow.core.TradeFlowApp
import com.tradeflow.core.data.DiagEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DiagVm(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TradeFlowApp).repo

    val events: StateFlow<List<DiagEvent>> = repo.diag(200)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun copyText(): String = repo.diagText(200)

    /** Testing only. Called from the confirm dialog's coroutine scope. */
    suspend fun reset() = repo.clearTestData()
}
