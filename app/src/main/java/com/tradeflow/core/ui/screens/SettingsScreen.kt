package com.tradeflow.core.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tradeflow.core.ui.vm.SettingsUi
import com.tradeflow.core.ui.vm.SettingsVm

private fun hour12(h: Int): String {
    val ap = if (h < 12) "am" else "pm"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$h12:00$ap"
}

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsVm = viewModel()) {
    val ctx = LocalContext.current
    val saved by vm.ui.collectAsState()
    val notice by vm.notice.collectAsState()
    LaunchedEffect(Unit) { vm.load(ctx) }
    LaunchedEffect(notice) {
        if (notice != null) {
            Toast.makeText(ctx, notice, Toast.LENGTH_SHORT).show()
            vm.clearNotice()
        }
    }
    if (saved == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val s = saved!!
    var name by remember(s) { mutableStateOf(s.name) }
    var phone by remember(s) { mutableStateOf(s.phone) }
    var svcs by remember(s) { mutableStateOf(s.services) }
    var days by remember(s) { mutableStateOf(s.days) }
    var startH by remember(s) { mutableStateOf(s.startH) }
    var endH by remember(s) { mutableStateOf(s.endH) }
    var typical by remember(s) { mutableStateOf(s.typical) }
    var payLink by remember(s) { mutableStateOf(s.payLink) }
    var reviewLink by remember(s) { mutableStateOf(s.reviewLink) }
    var autoConfirm by remember(s) { mutableStateOf(s.autoConfirm) }
    var tplWay by remember(s) { mutableStateOf(s.tplWay) }
    var tplBill by remember(s) { mutableStateOf(s.tplBill) }
    var tplDecline by remember(s) { mutableStateOf(s.tplDecline) }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back", fontSize = 16.sp) }
            Text("⚙️ Settings", fontSize = 19.sp)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Business", fontSize = 17.sp)
            OutlinedTextField(name, { name = it }, label = { Text("Business name") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text("Business phone") },
                singleLine = true, modifier = Modifier.fillMaxWidth())

            Text("Services (menu 1-5)", fontSize = 17.sp)
            svcs.forEachIndexed { i, svc ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(svc, { v ->
                        svcs = svcs.toMutableList().also { it[i] = v }
                    }, label = { Text("Option ${i + 1}") }, singleLine = true,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        svcs = svcs.toMutableList().also { it.removeAt(i) }
                    }) { Text("✕") }
                }
            }
            OutlinedButton(onClick = { svcs = svcs + "" }) { Text("+ Add service") }
            Text("Last item is always the 'Other / describe it' option.",
                fontSize = 13.sp, color = Color.Gray)

            Text("Working days (swipe →)", fontSize = 17.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1 to "S", 2 to "M", 3 to "T", 4 to "W", 5 to "T", 6 to "F", 7 to "S")
                    .forEach { (d, label) ->
                        if (days.contains(d)) Button(onClick = { days = days - d }) { Text(label) }
                        else OutlinedButton(onClick = { days = days + d }) { Text(label) }
                    }
            }
            Stepper("Day starts", startH, 0, 23, ::hour12, { startH = it })
            Stepper("Day ends", endH, 1, 23, ::hour12, { endH = it })
            Stepper("Typical job (min)", typical, 30, 480, { "$it" }, { typical = it })

            Text("Links", fontSize = 17.sp)
            OutlinedTextField(payLink, { payLink = it }, label = { Text("Payment link") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(reviewLink, { reviewLink = it }, label = { Text("Review link") },
                singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Auto-confirm bookings", fontSize = 16.sp)
                    Text("Skip your ✅ tap — jobs book themselves",
                        fontSize = 13.sp, color = Color.Gray)
                }
                Switch(autoConfirm, { autoConfirm = it })
            }

            Text("Message wording", fontSize = 17.sp)
            Text("Placeholders: {name} {amount} {link} {review} {slot} {service} {biz}",
                fontSize = 13.sp, color = Color.Gray)
            OutlinedTextField(tplWay, { tplWay = it }, label = { Text("On my way") },
                minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(tplBill, { tplBill = it }, label = { Text("Bill + review ask") },
                minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(tplDecline, { tplDecline = it }, label = { Text("Can't help") },
                minLines = 2, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
        }
        Button(
            onClick = {
                vm.save(ctx, SettingsUi(
                    name.trim(), phone.trim(), svcs, days,
                    startH.coerceIn(0, 23), endH.coerceIn(1, 23).coerceAtLeast(startH + 1),
                    typical, payLink.trim(), reviewLink.trim(), autoConfirm,
                    tplWay, tplBill, tplDecline
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save", fontSize = 17.sp) }
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int,
                    fmt: (Int) -> String, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }) { Text("−", fontSize = 22.sp) }
            Text(fmt(value), fontSize = 16.sp)
            TextButton(onClick = { onChange((value + 1).coerceAtMost(max)) }) { Text("+", fontSize = 22.sp) }
        }
    }
}
