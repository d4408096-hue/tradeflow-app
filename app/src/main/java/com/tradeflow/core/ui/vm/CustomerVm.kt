package com.tradeflow.core.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradeflow.core.TradeFlowApp
import com.tradeflow.core.data.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerVm(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TradeFlowApp).repo
    private val q = MutableStateFlow("")

    fun setQuery(s: String) { q.value = s }

    val customers: StateFlow<List<Customer>> = q.flatMapLatest { query ->
        if (query.isBlank()) repo.customers() else repo.searchCustomers(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(c: Customer, done: () -> Unit = {}) = viewModelScope.launch {
        repo.saveCustomer(c)
        repo.log("SYS", "customer saved: ${c.name}")
        done()
    }

    fun delete(c: Customer) = viewModelScope.launch {
        repo.deleteCustomer(c)
        repo.log("SYS", "customer deleted: ${c.name}")
    }
}
