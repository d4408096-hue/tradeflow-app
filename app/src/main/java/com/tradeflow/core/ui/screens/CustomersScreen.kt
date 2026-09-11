package com.tradeflow.core.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tradeflow.core.data.Customer
import com.tradeflow.core.ui.vm.CustomerVm

@Composable
fun CustomersScreen(vm: CustomerVm = viewModel()) {
    val list by vm.customers.collectAsState()
    var q by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Customer?>(null) }
    var deleting by remember { mutableStateOf<Customer?>(null) }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        OutlinedTextField(
            value = q, onValueChange = { q = it; vm.setQuery(it) },
            label = { Text("🔍 Search customers") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.id }) { c ->
                Card(onClick = { editing = c }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(c.name, fontSize = 18.sp)
                        Text(c.phone, fontSize = 14.sp)
                        if (c.address.isNotBlank()) Text(c.address, fontSize = 14.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FloatingActionButton(onClick = { showAdd = true }) { Text("+", fontSize = 26.sp) }
        }
    }

    if (showAdd) CustomerDialog(
        title = "➕ Add customer", initial = null,
        onSave = { vm.save(it) { showAdd = false } }, onClose = { showAdd = false }
    )
    editing?.let { c ->
        CustomerDialog(
            title = "Edit customer", initial = c,
            onSave = { vm.save(it) { editing = null } }, onClose = { editing = null },
            onDelete = { deleting = c; editing = null }
        )
    }
    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${c.name}?") },
            text = { Text("This also removes their jobs. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(c); deleting = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CustomerDialog(
    title: String, initial: Customer?,
    onSave: (Customer) -> Unit, onClose: () -> Unit, onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name *") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone *") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(address, { address = it }, label = { Text("Address") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank() || phone.isBlank()) return@Button
                onSave((initial ?: Customer(name = "", phone = "")).copy(
                    name = name.trim(), phone = phone.trim(),
                    address = address.trim(), notes = notes.trim()
                ))
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        }
    )
}
