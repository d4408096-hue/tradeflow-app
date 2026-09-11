package com.tradeflow.core.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tradeflow.core.data.Job
import com.tradeflow.core.engine.Prefs
import com.tradeflow.core.ui.Fmt
import com.tradeflow.core.ui.vm.JobVm
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun JobsScreen(vm: JobVm = viewModel()) {
    val jobs by vm.jobs.collectAsState()
    val custs by vm.customers.collectAsState()
    val ctx = LocalContext.current
    var completing by remember { mutableStateOf<Job?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Job?>(null) }

    val active = remember(jobs) {
        jobs.filter { it.status == Job.SCHEDULED || it.status == Job.IN_PROGRESS }
            .sortedBy { it.startAt }
    }
    val history = remember(jobs) {
        jobs.filter { it.status == Job.COMPLETED || it.status == Job.CANCELLED }
            .sortedByDescending { it.startAt }
    }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Active", fontSize = 18.sp) }
            if (active.isEmpty()) item { Text("No active jobs — enjoy the quiet. 🙂", fontSize = 15.sp) }
            items(active, key = { it.id }) { j ->
                val cust = custs.find { it.id == j.customerId }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(j.title, fontSize = 17.sp)
                        Text("${Fmt.dt(j.startAt)}${if (cust != null) " · ${cust.name}" else ""}",
                            fontSize = 14.sp)
                        if (j.amountCents > 0) Text(
                            Fmt.money(j.amountCents.toLong()) + if (j.paid) " · Paid ✓" else " · Unpaid",
                            fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (j.status == Job.SCHEDULED) {
                                OutlinedButton(onClick = {
                                    if (cust == null) Toast.makeText(ctx, "Customer missing", Toast.LENGTH_SHORT).show()
                                    else vm.sendOnMyWay(ctx, j, cust)
                                }) { Text("🚗 On my way") }
                                Button(onClick = { vm.advance(j) }) { Text("▶ Start") }
                            } else {
                                Button(onClick = { completing = j }) { Text("✔ Complete") }
                            }
                            TextButton(onClick = { deleting = j }) { Text("Delete") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(6.dp)); Text("History", fontSize = 18.sp) }
            items(history, key = { it.id }) { j ->
                val cust = custs.find { it.id == j.customerId }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(j.title, fontSize = 16.sp)
                        Text("${Fmt.dt(j.startAt)}${if (cust != null) " · ${cust.name}" else ""}",
                            fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (j.amountCents > 0) Fmt.money(j.amountCents.toLong()) else "No amount",
                                fontSize = 14.sp)
                            if (j.status == Job.COMPLETED && !j.paid) {
                                OutlinedButton(onClick = {
                                    if (cust == null) Toast.makeText(ctx, "Customer missing", Toast.LENGTH_SHORT).show()
                                    else vm.sendBill(ctx, j, cust)
                                }) { Text("💰 Bill") }
                                TextButton(onClick = { vm.markPaid(j) }) { Text("Mark paid") }
                            } else if (j.paid) {
                                Text("Paid ✓", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FloatingActionButton(onClick = { creating = true }) { Text("+", fontSize = 26.sp) }
        }
    }

    completing?.let { j ->
        var amount by remember { mutableStateOf(if (j.amountCents > 0) "${j.amountCents / 100}" else "") }
        var paid by remember { mutableStateOf(j.paid) }
        AlertDialog(
            onDismissRequest = { completing = null },
            title = { Text("Complete job") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(j.title, fontSize = 15.sp)
                    OutlinedTextField(amount, { amount = it }, label = { Text("Amount ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(paid, { paid = it })
                        Text("Already paid", fontSize = 15.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.completeWithAmount(j, Fmt.dollarsToCents(amount), paid)
                    completing = null
                }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { completing = null }) { Text("Cancel") }
            }
        )
    }

    deleting?.let { j ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete job?") },
            text = { Text(j.title) },
            confirmButton = {
                TextButton(onClick = { vm.delete(j); deleting = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            }
        )
    }

    if (creating) CreateJobDialog(
        onClose = { creating = false },
        onCreate = { cid, svc, title, startAt -> vm.create(ctx, cid, svc, title, startAt); creating = false }
    )
}

@Composable
private fun CreateJobDialog(onClose: () -> Unit, onCreate: (Long, String, String, Long) -> Unit) {
    val ctx = LocalContext.current
    val vm: JobVm = viewModel()
    val custs by vm.customers.collectAsState()
    val services = remember { Prefs.services(ctx).dropLast(1) } // exclude "Other"
    val startH = remember { Prefs.workStart(ctx) }
    val endH = remember { Prefs.workEnd(ctx) }
    val zone = remember { ZoneId.systemDefault() }

    var custId by remember { mutableStateOf(-1L) }
    var svc by remember { mutableStateOf("") }
    var dayIdx by remember { mutableStateOf(0) }
    var hour by remember { mutableStateOf(startH) }
    val days = remember { (0..13).map { LocalDate.now().plusDays(it.toLong()) } }
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEE d", Locale.US) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("➕ New job") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Customer", fontSize = 15.sp)
                custs.forEach { c ->
                    OutlinedButton(
                        onClick = { custId = c.id },
                        enabled = custId != c.id,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text((if (custId == c.id) "✓ " else "") + c.name) }
                }
                if (custs.isEmpty()) Text("Add a customer first (Customers tab).", fontSize = 14.sp)
                Text("2. Service", fontSize = 15.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(services) { s ->
                        if (svc == s) Button(onClick = {}) { Text(s) }
                        else OutlinedButton(onClick = { svc = s }) { Text(s) }
                    }
                }
                Text("3. Day", fontSize = 15.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(14) { i ->
                        val label = days[i].format(dayFmt)
                        if (dayIdx == i) Button(onClick = {}) { Text(label) }
                        else OutlinedButton(onClick = { dayIdx = i }) { Text(label) }
                    }
                }
                Text("4. Time", fontSize = 15.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((endH - startH).coerceAtLeast(1)) { i ->
                        val h = startH + i
                        val label = hourLabel(h)
                        if (hour == h) Button(onClick = {}) { Text(label) }
                        else OutlinedButton(onClick = { hour = h }) { Text(label) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val c = custs.find { it.id == custId } ?: return@Button
                    if (svc.isBlank()) return@Button
                    val at = days[dayIdx].atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
                    onCreate(c.id, svc, "$svc — ${c.name}", at)
                },
                enabled = custId != -1L && svc.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel") }
        }
    )
}

private fun hourLabel(h: Int): String {
    val ap = if (h < 12) "am" else "pm"
    val h12 = when {
        h == 0 || h == 12 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "$h12$ap"
}
