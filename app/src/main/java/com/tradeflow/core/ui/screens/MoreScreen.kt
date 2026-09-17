package com.tradeflow.core.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tradeflow.core.engine.Prefs
import com.tradeflow.core.ui.Fmt
import com.tradeflow.core.ui.vm.DashVm

private fun missingPerms(ctx: Context): List<String> {
    val need = mutableListOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS
    )
    if (Build.VERSION.SDK_INT >= 33) need.add(Manifest.permission.POST_NOTIFICATIONS)
    return need.filter {
        ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun MoreScreen(onSettings: () -> Unit, onDiag: () -> Unit, vm: DashVm = viewModel()) {
    val ctx = LocalContext.current
    val caught by vm.caught.collectAsState()
    val booked by vm.booked.collectAsState()
    val revenue by vm.revenue.collectAsState()
    val busy by vm.busy.collectAsState()
    val biz = remember { Prefs.bizName(ctx).ifBlank { "My Business" } }
    val bizPhone = remember { Prefs.bizPhone(ctx) }
    var permTick by remember { mutableStateOf(0) }
    val missing = remember(permTick) { missingPerms(ctx) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permTick++ }

    LaunchedEffect(Unit) { vm.load(ctx) }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(biz, fontSize = 20.sp)
        if (bizPhone.isNotBlank()) Text(bizPhone, fontSize = 15.sp, color = Color.Gray)
        if (missing.isNotEmpty()) {
            Card(Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEC))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⚠️ ${missing.size} permissions missing", fontSize = 16.sp)
                    Text("Auto-reply can't work until Calls + SMS are granted.",
                        fontSize = 14.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { permLauncher.launch(missing.toTypedArray()) }) {
                            Text("Grant")
                        }
                        TextButton(onClick = {
                            ctx.startActivity(Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", ctx.packageName, null)
                            ))
                        }) { Text("Settings") }
                        TextButton(onClick = { permTick++ }) { Text("Recheck") }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoiCard("📞", "$caught", "Calls caught", Modifier.weight(1f))
            RoiCard("📅", "$booked", "Jobs booked", Modifier.weight(1f))
            RoiCard("💰", Fmt.money(revenue), "Collected", Modifier.weight(1f))
        }
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("🟢 Busy Mode", fontSize = 17.sp)
                    Text(if (busy) "Auto-reply active" else "Off — calls ring normally",
                        fontSize = 14.sp, color = Color.Gray)
                }
                Switch(checked = busy, onCheckedChange = { vm.setBusy(ctx, it) })
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(6.dp)) {
                TextButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("⚙️ Settings", fontSize = 16.sp)
                        Text("›", fontSize = 16.sp)
                    }
                }
                TextButton(onClick = onDiag, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🩺 Diagnostics", fontSize = 16.sp)
                        Text("›", fontSize = 16.sp)
                    }
                }
            }
        }
        Text("TradeFlow v0.1.0", fontSize = 13.sp, color = Color.Gray)
    }
}

@Composable
private fun RoiCard(icon: String, big: String, lbl: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 20.sp)
            Text(big, fontSize = 18.sp)
            Text(lbl, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
