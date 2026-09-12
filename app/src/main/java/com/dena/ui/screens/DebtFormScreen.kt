package com.dena.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dena.R
import com.dena.core.DenaPreferences
import com.dena.ui.DebtViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormScreen(
    viewModel: DebtViewModel,
    onBack: () -> Unit,
    initialIsOwedToMe: Boolean? = null,
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isOwedToMe by remember { mutableStateOf(initialIsOwedToMe ?: true) }
    var notes by remember { mutableStateOf("") }
    var noDueDate by remember { mutableStateOf(true) }
    var creationDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val prefs = remember(context) { DenaPreferences(context) }
    val symbol = prefs.getCurrencySymbol()

    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
    val canSave = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0

    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            try {
                val cursor = context.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME), null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val idx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        if (idx >= 0) name = it.getString(idx) ?: name
                    }
                }
                // fallback: try StructuredName
                if (name.isBlank()) {
                    val c2 = context.contentResolver.query(uri, null, null, null, null)
                    c2?.use {
                        if (it.moveToFirst()) {
                            val idx2 = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                            if (idx2 >= 0) name = it.getString(idx2) ?: name
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) contactLauncher.launch(null)
        else {
            // fallback: open picker anyway (some devices allow without perm)
            try { contactLauncher.launch(null) } catch (_: Exception) {}
        }
    }

    fun showDatePicker(initial: Long, onPick: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initial }
        DatePickerDialog(context, { _, y, m, d ->
            val c = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0); set(Calendar.MILLISECOND, 0) }
            onPick(c.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.create_debt)) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.weight(1f).clickable { isOwedToMe = true }, verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.RadioButton(selected = isOwedToMe, onClick = { isOwedToMe = true })
                    Text("I Lent", fontSize = 14.sp)
                }
                Row(modifier = Modifier.weight(1f).clickable { isOwedToMe = false }, verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.RadioButton(selected = !isOwedToMe, onClick = { isOwedToMe = false })
                    Text("I Borrowed", fontSize = 14.sp)
                }
            }
            // Creation date — tappable
            OutlinedTextField(
                value = dateFmt.format(Date(creationDate)),
                onValueChange = {},
                label = { Text("Creation date") },
                trailingIcon = { Icon(Icons.Filled.DateRange, null, modifier = Modifier.clickable { showDatePicker(creationDate) { creationDate = it } }) },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.contact_name)) },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    permLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                }) {
                    Icon(Icons.Filled.Person, contentDescription = "Pick contact")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amountText = it },
                    label = { Text(stringResource(R.string.amount)) },
                    leadingIcon = { Icon(Icons.Filled.Info, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Text(symbol, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
            }
            // Due date row: picker + checkbox
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if (dueDate != null) dateFmt.format(Date(dueDate!!)) else "—",
                    onValueChange = {},
                    label = { Text("Due date") },
                    leadingIcon = { Icon(Icons.Filled.DateRange, null) },
                    readOnly = true,
                    enabled = !noDueDate,
                    modifier = Modifier.weight(1f).clickable(enabled = !noDueDate) {
                        showDatePicker(dueDate ?: System.currentTimeMillis()) { picked -> dueDate = picked }
                    },
                )
                IconButton(onClick = {
                    if (!noDueDate) showDatePicker(dueDate ?: System.currentTimeMillis()) { picked -> dueDate = picked }
                }, enabled = !noDueDate) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Pick due date")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = noDueDate, onCheckedChange = { checked ->
                    noDueDate = checked
                    if (checked) dueDate = null else if (dueDate == null) dueDate = System.currentTimeMillis()
                })
                Text(stringResource(R.string.no_due_date), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.clickable { noDueDate = !noDueDate; if (!noDueDate && dueDate == null) dueDate = System.currentTimeMillis() })
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes)) },
                leadingIcon = { Icon(Icons.Filled.Info, null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    viewModel.addDebt(
                        contactName = name.trim(),
                        direction = if (isOwedToMe) "owed_to_me" else "i_owe",
                        amount = amount,
                        currency = prefs.getCurrency(),
                        category = "Other",
                        notes = notes.trim(),
                        dueDate = if (noDueDate) null else dueDate,
                        creationDate = creationDate,
                    )
                    onBack()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}
