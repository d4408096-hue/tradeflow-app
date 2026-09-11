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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tradeflow.core.ui.Fmt
import com.tradeflow.core.ui.vm.JobVm
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(vm: JobVm = viewModel()) {
    val jobs by vm.jobs.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    val zone = remember { ZoneId.systemDefault() }

    fun dayOf(at: Long): LocalDate =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(at), zone).toLocalDate()

    val jobDays = remember(jobs) { jobs.map { dayOf(it.startAt) }.toSet() }
    val dayJobs = remember(jobs, selected) {
        jobs.filter { dayOf(it.startAt) == selected }.sortedBy { it.startAt }
    }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹", fontSize = 28.sp) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)), fontSize = 19.sp)
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("›", fontSize = 28.sp) }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(d, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        val firstDow = month.atDay(1).dayOfWeek.value % 7 // Sunday-first: Sun=0
        val cells = firstDow + month.lengthOfMonth()
        val rows = (cells + 6) / 7
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val num = row * 7 + col - firstDow + 1
                    Box(Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (num in 1..month.lengthOfMonth()) {
                            val date = month.atDay(num)
                            val isSel = date == selected
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selected = date }
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        CircleShape
                                    ).padding(vertical = 6.dp, horizontal = 10.dp)
                            ) {
                                Text("$num", fontSize = 15.sp,
                                    color = if (isSel) Color.White else Color.Unspecified)
                                Text(if (jobDays.contains(date)) "•" else " ",
                                    fontSize = 10.sp,
                                    color = if (isSel) Color.White else Color(0xFFF59E0B))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(Fmt.day(selected.atStartOfDay(zone).toInstant().toEpochMilli()), fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
