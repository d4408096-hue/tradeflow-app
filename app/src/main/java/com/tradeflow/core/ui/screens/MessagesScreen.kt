package com.tradeflow.core.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tradeflow.core.data.ChatMessage
import com.tradeflow.core.data.Conversation
import com.tradeflow.core.data.MsgTemplate
import com.tradeflow.core.engine.Prefs
import com.tradeflow.core.ui.vm.MsgVm
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(initialPhone: String? = null, onOpened: () -> Unit = {}, vm: MsgVm = viewModel()) {
    val convos by vm.convos.collectAsState()
    val custs by vm.customers.collectAsState()
    // Patch #12: phone -> customer name for thread titles.
    val custNames = remember(custs) { custs.associate { it.phone to it.name } }
    var open by remember { mutableStateOf(initialPhone) }
    LaunchedEffect(Unit) { onOpened() }
    if (open == null) {
        ConvoList(convos, custNames, onOpen = { open = it; vm.open(it) })
    } else {
        ThreadView(
            phone = open!!, convos = convos,
            custName = custNames[open!!], vm = vm, onBack = { open = null }
        )
    }
}

@Composable
private fun ConvoList(
    convos: List<Conversation>,
    custNames: Map<String, String>,
    onOpen: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        if (convos.isEmpty()) {
            Text("No conversations yet.\nMissed calls + texts land here. 📲", fontSize = 16.sp)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(convos, key = { it.phone }) { c ->
                Card(
                    onClick = { onOpen(c.phone) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (c.needsHuman) CardDefaults.cardColors(containerColor = Color(0xFFFDECEC))
                    else CardDefaults.cardColors()
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            val title = c.name.takeIf { it.isNotBlank() }
                                ?: custNames[c.phone]?.takeIf { it.isNotBlank() }
                                ?: c.phone
                            Text((if (c.needsHuman) "🚨 " else "") + title,
                                fontSize = 17.sp)
                            Text(convoSubtitle(c), fontSize = 14.sp, color = Color.Gray, maxLines = 2)
                        }
                        if (c.unread > 0) {
                            Text("${c.unread}",
                                Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun convoSubtitle(c: Conversation): String = when {
    c.needsHuman && c.problem.isNotBlank() -> c.problem.take(80)
    c.needsHuman -> "Needs your decision"
    c.stage == Conversation.PENDING_CONFIRM -> "📅 Confirm: ${c.service} · ${c.slotLabel}"
    c.stage == Conversation.AWAIT_SERVICE ||
        c.stage == Conversation.AWAIT_SLOT ||
        c.stage == Conversation.AWAIT_DETAILS -> "Bot: waiting on customer…"
    // Patch #12: declined threads must not wear the Booked badge.
    c.stage == Conversation.DONE && c.outcome == Conversation.OUT_DECLINED -> "Declined 🚫"
    c.stage == Conversation.DONE -> "Booked ✅"
    else -> "Tap to open"
}

@Composable
private fun ThreadView(
    phone: String,
    convos: List<Conversation>,
    custName: String?,
    vm: MsgVm,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val msgs by vm.thread(phone).collectAsState(initial = emptyList())
    val convo = convos.find { it.phone == phone }
    val notice by vm.notice.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val title = convo?.name?.takeIf { it.isNotBlank() }
        ?: custName?.takeIf { it.isNotBlank() }
        ?: phone

    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1)
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            Toast.makeText(ctx, notice, Toast.LENGTH_SHORT).show()
            vm.clearNotice()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back", fontSize = 16.sp) }
            Text(title, fontSize = 17.sp,
                modifier = Modifier.weight(1f))
            TextButton(onClick = {
                ctx.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
            }) { Text("📞", fontSize = 22.sp) }
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp),
            state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs, key = { it.id }) { m ->
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = if (m.dir == ChatMessage.IN) Arrangement.Start
                    else Arrangement.End) {
                    Card(
                        colors = if (m.dir == ChatMessage.IN) CardDefaults.cardColors()
                        else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(m.body, fontSize = 15.sp,
                                color = if (m.dir == ChatMessage.IN) Color.Unspecified else Color.White)
                            if (m.auto) Text("auto", fontSize = 11.sp,
                                color = if (m.dir == ChatMessage.IN) Color.Gray else Color.White)
                        }
                    }
                }
            }
        }
        convo?.let { c ->
            if (c.needsHuman) {
                Card(Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEC))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🚨 " + c.problem.ifBlank { "Customer needs you." }, fontSize = 15.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.resolveYes(ctx, phone) },
                                modifier = Modifier.weight(1f)) { Text("✅ Yes") }
                            OutlinedButton(
                                onClick = {
                                    ctx.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
                                },
                                modifier = Modifier.weight(1f)) { Text("📞 Call") }
                            OutlinedButton(onClick = { vm.resolveNo(ctx, phone) },
                                modifier = Modifier.weight(1f)) { Text("❌ No") }
                        }
                    }
                }
            } else if (c.stage == Conversation.PENDING_CONFIRM) {
                Card(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📅 ${c.name} · ${c.service} · ${c.slotLabel}", fontSize = 15.sp)
                        if (c.address.isNotBlank()) Text(c.address, fontSize = 14.sp, color = Color.Gray)
                        Button(onClick = { vm.confirmBooking(ctx, phone) },
                            modifier = Modifier.fillMaxWidth()) { Text("✅ Confirm booking") }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val c = convo
            TextButton(onClick = {
                scope.launch {
                    input = vm.renderTemplate(MsgTemplate.ON_MY_WAY, tplVars(ctx, c)).ifBlank { input }
                }
            }) { Text("🚗 Way") }
            TextButton(onClick = {
                scope.launch {
                    input = vm.renderTemplate(MsgTemplate.BILL_REVIEW, tplVars(ctx, c)).ifBlank { input }
                }
            }) { Text("💰 Bill") }
            TextButton(onClick = {
                scope.launch {
                    input = vm.renderTemplate(MsgTemplate.NUDGE, tplVars(ctx, c)).ifBlank { input }
                }
            }) { Text("👋 Nudge") }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message…") })
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (input.isNotBlank()) {
                    vm.sendText(ctx, phone, input.trim())
                    input = ""
                }
            }) { Text("Send") }
        }
    }
}

private fun tplVars(ctx: Context, c: Conversation?): Map<String, String> = mapOf(
    "name" to (c?.name?.substringBefore(" ")?.ifBlank { "there" } ?: "there"),
    "service" to (c?.service ?: ""),
    "slot" to (c?.slotLabel ?: ""),
    "biz" to Prefs.bizName(ctx),
    "amount" to "",
    "link" to Prefs.paymentLink(ctx),
    "review" to Prefs.reviewLink(ctx)
)
