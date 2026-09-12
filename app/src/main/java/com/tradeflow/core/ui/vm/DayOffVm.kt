package com.tradeflow.core.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradeflow.core.TradeFlowApp
import com.tradeflow.core.data.DayOff
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DayOffVm(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TradeFlowApp).repo

    val offs: StateFlow<List<DayOff>> = repo.dayOffs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRange(startIso: String, days: Int, reason: String) = viewModelScope.launch {
        val s = LocalDate.parse(startIso)
        val e = s.plusDays((days.coerceIn(1, 30) - 1).toLong())
        repo.addDayOff(DayOff(startDate = s.toString(), endDate = e.toString(), reason = reason.trim()))
        repo.log("SYS", "day off $s..$e ${reason.trim()}")
    }

    fun removeOn(iso: String) = viewModelScope.launch {
        repo.removeDayOffOn(iso)
        repo.log("SYS", "day off removed $iso")
    }
}
