package com.tradeflow.core.ui.vm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradeflow.core.TradeFlowApp
import com.tradeflow.core.data.ChatMessage
import com.tradeflow.core.data.Customer
import com.tradeflow.core.data.Job
import com.tradeflow.core.data.MsgTemplate
import com.tradeflow.core.engine.Prefs
import com.tradeflow.core.engine.SmsSender
import com.tradeflow.core.ui.Fmt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JobVm(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TradeFlowApp).repo

    val jobs: StateFlow<List<Job>> =
        repo.jobs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers: StateFlow<List<Customer>> =
        repo.customers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun advance(job: Job) = viewModelScope.launch {
        val next = when (job.status) {
            Job.SCHEDULED -> Job.IN_PROGRESS
            Job.IN_PROGRESS -> Job.COMPLETED
            else -> return@launch
        }
        repo.setJobStatus(job.id, next)
        repo.log("JOB", "#${job.id} -> $next")
    }

    fun completeWithAmount(job: Job, amountCents: Int, paid: Boolean) = viewModelScope.launch {
        repo.saveJob(job.copy(status = Job.COMPLETED, amountCents = amountCents, paid = paid))
        repo.log("JOB", "#${job.id} completed ${Fmt.money(amountCents.toLong())} paid=$paid")
    }

    fun markPaid(job: Job) = viewModelScope.launch {
        repo.markJobPaid(job.id)
        repo.log("JOB", "#${job.id} marked paid")
    }

    fun delete(job: Job) = viewModelScope.launch {
        repo.deleteJob(job)
        repo.log("JOB", "#${job.id} deleted")
    }

    fun create(ctx: Context, customerId: Long, service: String, title: String, startAt: Long) {
        val dur = Prefs.typicalJobMins(ctx) * 60_000L
        viewModelScope.launch {
            repo.saveJob(Job(customerId = customerId, title = title, service = service,
                startAt = startAt, endAt = startAt + dur, status = Job.SCHEDULED))
            repo.log("JOB", "created: $title")
        }
    }

    private fun vars(ctx: Context, job: Job, cust: Customer) = mapOf(
        "name" to cust.name.substringBefore(" "),
        "amount" to Fmt.money(job.amountCents.toLong()),
        "link" to Prefs.paymentLink(ctx).ifBlank { "(payment link not set)" },
        "review" to Prefs.reviewLink(ctx).ifBlank { "(review link not set)" },
        "biz" to Prefs.bizName(ctx),
        "slot" to Fmt.dt(job.startAt),
        "service" to job.service
    )

    fun sendBill(ctx: Context, job: Job, cust: Customer) = viewModelScope.launch {
        val appCtx = ctx.applicationContext
        val msg = repo.render(MsgTemplate.BILL_REVIEW, vars(ctx, job, cust))
            .ifBlank { "Total: ${Fmt.money(job.amountCents.toLong())}" }
        SmsSender.sendNow(appCtx, cust.phone, msg)
        repo.addMessage(cust.phone, ChatMessage.OUT, msg)
        repo.log("JOB", "#${job.id} bill sent")
    }

    fun sendOnMyWay(ctx: Context, job: Job, cust: Customer) = viewModelScope.launch {
        val appCtx = ctx.applicationContext
        val msg = repo.render(MsgTemplate.ON_MY_WAY, vars(ctx, job, cust))
            .ifBlank { "On my way!" }
        SmsSender.sendNow(appCtx, cust.phone, msg)
        repo.addMessage(cust.phone, ChatMessage.OUT, msg)
        repo.log("JOB", "#${job.id} on-my-way sent")
    }
}
