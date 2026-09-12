package com.tradeflow.core.ui.vm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradeflow.core.TradeFlowApp
import com.tradeflow.core.data.ChatMessage
import com.tradeflow.core.data.Conversation
import com.tradeflow.core.data.Customer
import com.tradeflow.core.engine.BookingBot
import com.tradeflow.core.engine.SmsSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MsgVm(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TradeFlowApp).repo

    val convos: StateFlow<List<Conversation>> = repo.conversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Patch #12: customer names for thread titles (fallback when convo has no name yet).
    val customers: StateFlow<List<Customer>> = repo.customers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice
    fun clearNotice() { _notice.value = null }
    private fun say(s: String) { _notice.value = s }

    fun thread(phone: String): Flow<List<ChatMessage>> = repo.messagesFor(phone)

    fun open(phone: String) = viewModelScope.launch { repo.clearUnread(phone) }

    fun sendText(ctx: Context, phone: String, body: String) = viewModelScope.launch {
        val appCtx = ctx.applicationContext
        SmsSender.sendNow(appCtx, phone, body)
        repo.addMessage(phone, ChatMessage.OUT, body)
    }

    suspend fun renderTemplate(key: String, vars: Map<String, String>): String =
        repo.render(key, vars)

    fun resolveYes(ctx: Context, phone: String) = viewModelScope.launch {
        BookingBot.resolveYes(ctx.applicationContext, phone)
        say("Slot offer sent 👍")
    }

    fun resolveNo(ctx: Context, phone: String) = viewModelScope.launch {
        BookingBot.resolveNo(ctx.applicationContext, phone)
        say("Decline sent")
    }

    fun confirmBooking(ctx: Context, phone: String) = viewModelScope.launch {
        val id = BookingBot.confirmBooking(ctx.applicationContext, phone)
        say(if (id != null) "✅ Booked! Job created." else "Couldn't book — check details.")
    }
}
