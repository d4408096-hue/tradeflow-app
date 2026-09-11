package com.tradeflow.core.ui.vm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradeflow.core.TradeFlowApp
import com.tradeflow.core.data.MsgTemplate
import com.tradeflow.core.engine.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUi(
    val name: String, val phone: String,
    val services: List<String>,
    val days: Set<Int>, val startH: Int, val endH: Int, val typical: Int,
    val payLink: String, val reviewLink: String,
    val autoConfirm: Boolean,
    val tplWay: String, val tplBill: String, val tplDecline: String
)

class SettingsVm(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TradeFlowApp).repo

    private val _ui = MutableStateFlow<SettingsUi?>(null)
    val ui: StateFlow<SettingsUi?> = _ui

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice
    fun clearNotice() { _notice.value = null }

    fun load(ctx: Context) = viewModelScope.launch {
        val appCtx = ctx.applicationContext
        _ui.value = SettingsUi(
            name = Prefs.bizName(appCtx), phone = Prefs.bizPhone(appCtx),
            services = Prefs.services(appCtx),
            days = Prefs.workDays(appCtx),
            startH = Prefs.workStart(appCtx), endH = Prefs.workEnd(appCtx),
            typical = Prefs.typicalJobMins(appCtx),
            payLink = Prefs.paymentLink(appCtx), reviewLink = Prefs.reviewLink(appCtx),
            autoConfirm = Prefs.autoConfirm(appCtx),
            tplWay = repo.template(MsgTemplate.ON_MY_WAY)?.body ?: "",
            tplBill = repo.template(MsgTemplate.BILL_REVIEW)?.body ?: "",
            tplDecline = repo.template(MsgTemplate.DECLINE)?.body ?: ""
        )
    }

    fun save(ctx: Context, u: SettingsUi) = viewModelScope.launch {
        val appCtx = ctx.applicationContext
        Prefs.setBiz(appCtx, u.name, u.phone)
        Prefs.setServices(appCtx, u.services.filter { it.isNotBlank() })
        Prefs.setWork(appCtx, u.days.ifEmpty { setOf(2, 3, 4, 5, 6, 7) }, u.startH, u.endH)
        Prefs.setTypicalJobMins(appCtx, u.typical.coerceIn(30, 480))
        Prefs.setLinks(appCtx, u.payLink, u.reviewLink)
        Prefs.setAutoConfirm(appCtx, u.autoConfirm)
        if (u.tplWay.isNotBlank()) repo.saveTemplate(MsgTemplate(MsgTemplate.ON_MY_WAY, "On my way", u.tplWay))
        if (u.tplBill.isNotBlank()) repo.saveTemplate(MsgTemplate(MsgTemplate.BILL_REVIEW, "Bill + review ask", u.tplBill))
        if (u.tplDecline.isNotBlank()) repo.saveTemplate(MsgTemplate(MsgTemplate.DECLINE, "Can't help", u.tplDecline))
        repo.log("SYS", "settings saved")
        _notice.value = "Saved ✅"
    }
}
