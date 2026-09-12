package com.tradeflow.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tradeflow.core.data.DayOff
import com.tradeflow.core.ui.Fmt
import com.tradeflow.core.ui.vm.DayOffVm
import com.tradeflow.core.ui.vm.JobVm
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Patch #8: compact grid on top (~40%) + roomy detail section (~60%) that always fits. */
@Composable
fun CalendarScreen(vm: JobVm = viewModel(), offVm: DayOffVm = viewModel()) {
    val jobs by vm.jobs.collectAsState()
    val offs by offVm.offs.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    val zone = remember { ZoneId.systemDefault() }

    fun dayOf(at: Long): LocalDate =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(at), zone).toLocalDate()

    val jobDays = remember(jobs) { jobs.map { dayOf(it.startAt) }.toSet() }
    val offMap = remember(offs) {
        buildMap<LocalDate, DayOff> {
            offs.forEach { o ->
                var d = LocalDate.parse(o.startDate)
                val end = LocalDate.parse(o.endDate)
                var guard = 0
                while (!d.isAfter(end) && guard++ < 60) {
                    put(d, o)
                    d = d.plusDays(1)
                }
            }
        }
    }
    val dayJobs = remember(jobs, selected) {
        jobs.filter { dayOf(it.startAt) == selected }.sortedBy { it.startAt }
    }
    val selOff = offMap[selected]

    var showForm by remember(selected) { mutableStateOf(false) }
    var offN by remember(selected) { mutableStateOf(1) }
    var reason by remember(selected) { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).padding(top = 2.dp)) {
        // ---------- compact month grid ----------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹", fontSize = 24.sp) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)), fontSize = 17.sp)
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("›", fontSize = 24.sp) }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(d, fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
        val firstDow = month.atDay(1).dayOfWeek.value % 7 // Sunday-first: Sun=0
        val cells = firstDow + month.lengthOfMonth()
        val rows = (cells + 6) / 7
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                repeat(7) { col ->
                    val num = row * 7 + col - firstDow + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (num in 1..month.lengthOfMonth()) {
                            val date = month.atDay(num)
                            val isSel = date == selected
                            val isOff = offMap.contains(date)
                            val hasJob = jobDays.contains(date)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { selected = date }
                            ) {
                                Text("$num", fontSize = 13.sp,
                                    color = when {
                                        isSel -> Color.White
                                        isOff -> Color(0xFFDC2626)
                                        else -> Color.Unspecified
                                    })
                                if (!isSel && (isOff || hasJob)) {
                                    Box(Modifier.align(Alignment.BottomCenter)
                                        .padding(bottom = 5.dp)
                                        .size(4.dp)
                                        .background(
                                            if (isOff) Color(0xFFDC2626) else Color(0xFFF59E0B),
                                            CircleShape
                                        ))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // ---------- detail section: ALL remaining space, always fits ----------
        Column(Modifier.weight(1f).fillMaxWidth()) {
            Text(Fmt.day(selected.atStartOfDay(zone).toInstant().toEpochMilli()), fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))

            if (selOff != null) {
                Card(Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEC))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("🏖️ Day off" + (if (selOff.reason.isNotBlank()) " (${selOff.reason})" else ""),
                            fontSize = 15.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { offVm.removeOn(selected.toString()) }) { Text("Remove") }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (dayJobs.isNotEmpty()) {
                    Text("⚠️ ${dayJobs.size} job(s) scheduled on your day off!", fontSize = 14.sp,
                        color = Color(0xFFDC2626))
                    Spacer(Modifier.height(6.dp))
                }
            } else if (showForm) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Days off from ${selected.dayOfMonth}/${selected.monthValue}",
                                fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { offN = (offN - 1).coerceAtLeast(1) }) {
                                    Text("−", fontSize = 22.sp)
                                }
                                Text("$offN", fontSize = 16.sp)
                                TextButton(onClick = { offN = (offN + 1).coerceAtMost(30) }) {
                                    Text("+", fontSize = 22.sp)
                                }
                            }
                        }
                        OutlinedTextField(reason, { reason = it },
                            label = { Text("Reason (optional)") },
                            minLines = 2, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    offVm.addRange(selected.toString(), offN, reason)
                                    showForm = false
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Save") }
                            TextButton(onClick = { showForm = false }) { Text("Cancel") }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            } else {
                TextButton(onClick = { showForm = true }) {
                    Text("+ Mark days off", fontSize = 15.sp)
                }
            }

            LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayJobs, key = { it.id }) { j ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(j.title, fontSize = 16.sp)
                            Text("${Fmt.time(j.startAt)} · ${j.status.lowercase().replaceFirstChar { it.uppercase() }}",
                                fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
