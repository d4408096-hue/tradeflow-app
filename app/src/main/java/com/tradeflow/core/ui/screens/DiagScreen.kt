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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tradeflow.core.ui.vm.DiagVm
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagScreen(onBack: () -> Unit, vm: DiagVm = viewModel()) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val events by vm.events.collectAsState()
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.US) }
    var showReset by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back", fontSize = 16.sp) }
            Text("🩺 Diagnostics", fontSize = 19.sp, modifier = Modifier.weight(1f))
        }
        Button(
            onClick = {
                scope.launch {
                    val t = vm.copyText()
                    clipboard.setText(AnnotatedString(t.ifBlank { "(log empty)" }))
                    Toast.makeText(ctx, "Log copied — paste it to support", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("📋 Copy log") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showReset = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("🧪 Reset test data") }
        Spacer(Modifier.height(10.dp))
        if (events.isEmpty()) Text("Nothing logged yet. Make a test call! 📞", fontSize = 15.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(events, key = { it.id }) { e ->
                Text("${fmt.format(Date(e.at))} [${e.tag}] ${e.text}",
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }

    if (showReset) AlertDialog(
        onDismissRequest = { showReset = false },
        title = { Text("Reset test data?") },
        text = { Text("Deletes ALL conversations + messages (cooldowns too). Jobs and customers stay. Testing only!") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    vm.reset()
                    showReset = false
                    Toast.makeText(ctx, "Test data cleared", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Reset") }
        },
        dismissButton = {
            TextButton(onClick = { showReset = false }) { Text("Cancel") }
        }
    )
}
