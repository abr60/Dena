package com.dena.ui.screens

import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dena.R
import com.dena.core.CurrencyRegistry
import com.dena.core.DenaPreferences
import com.dena.ui.DebtViewModel
import com.dena.ui.components.CurrencyPickerDialog
import com.dena.ui.components.DenaDatePickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebtFormScreen(
    viewModel: DebtViewModel,
    onBack: () -> Unit,
    initialIsOwedToMe: Boolean? = null,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isOwedToMe by remember { mutableStateOf(initialIsOwedToMe ?: true) }
    var notes by remember { mutableStateOf("") }
    var noDueDate by remember { mutableStateOf(true) }
    var creationDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val prefs = remember(context) { DenaPreferences(context) }
    var currencyCode by remember { mutableStateOf(prefs.getCurrency()) }
    // existing contact names for suggestions
    val owedList by viewModel.owedToMe.collectAsStateWithLifecycle()
    val iOweList by viewModel.iOwe.collectAsStateWithLifecycle()
    val allNames = remember(owedList, iOweList) { (owedList + iOweList).map { it.contactName }.distinct() }
    val filteredSuggestions = remember(name, allNames) {
        if (name.length < 1) emptyList()
        else allNames.filter { it.contains(name.trim(), ignoreCase = true) && !it.equals(name.trim(), ignoreCase = true) }.take(5)
    }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf<Pair<Long, (Long) -> Unit>?>(null) }
    val symbol = CurrencyRegistry.symbolFor(currencyCode)
    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
    val shortDateFmt = SimpleDateFormat("MMM d", Locale.US)
    val canSave = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0

    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            try {
                val cursor = context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                    null, null, null,
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val idx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        if (idx >= 0) name = it.getString(idx) ?: name
                    }
                }
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
        else try { contactLauncher.launch(null) } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Compact header — back button + title + creation date picker inline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = stringResource(R.string.create_debt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            // Creation date as compact icon button
            IconButton(
                onClick = { datePickerTarget = creationDate to { creationDate = it } },
            ) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = "Set date",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = shortDateFmt.format(Date(creationDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Form content — fixed, no scroll
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Contact name — leading icon opens device contacts, field still allows manual typing + suggestions
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.contact_name)) },
                leadingIcon = {
                    IconButton(onClick = {
                        permLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                    }) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Pick contact",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            if (filteredSuggestions.isNotEmpty()) {
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        filteredSuggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { name = suggestion }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(suggestion, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Direction toggle — compact chip pair
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DirectionChip(
                    label = "I Lent",
                    selected = isOwedToMe,
                    onClick = { isOwedToMe = true },
                    modifier = Modifier.weight(1f),
                )
                DirectionChip(
                    label = "I Borrowed",
                    selected = !isOwedToMe,
                    onClick = { isOwedToMe = false },
                    modifier = Modifier.weight(1f),
                )
            }

            // Phone (optional)
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (optional)") },
                placeholder = { Text("017... or +880...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Amount + currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amountText = it
                    },
                    label = { Text(stringResource(R.string.amount)) },
                    leadingIcon = {
                        Text(symbol, style = MaterialTheme.typography.titleMedium)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedButton(
                    onClick = { showCurrencyPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .width(64.dp)
                        .height(56.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        symbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }

            // Due date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        if (!noDueDate) datePickerTarget =
                            (dueDate ?: System.currentTimeMillis()) to { picked -> dueDate = picked }
                    },
                    enabled = !noDueDate,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp),
                ) {
                    Icon(
                        Icons.Filled.DateRange,
                        null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 0.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (dueDate != null) dateFmt.format(Date(dueDate!!)) else "No due date",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        noDueDate = !noDueDate
                        if (!noDueDate && dueDate == null) dueDate = System.currentTimeMillis()
                    },
                ) {
                    Checkbox(
                        checked = noDueDate,
                        onCheckedChange = { checked ->
                            noDueDate = checked
                            if (checked) dueDate = null
                            else if (dueDate == null) dueDate = System.currentTimeMillis()
                        },
                    )
                    Text(
                        stringResource(R.string.no_due_date),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // Notes — single line
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes)) },
                placeholder = { Text("optional comment") },
                trailingIcon = if (notes.isNotEmpty()) {
                    {
                        IconButton(onClick = { notes = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
        }

        // Save button — pinned above keyboard
        Button(
            onClick = {
                val amount = amountText.toDoubleOrNull() ?: 0.0
                viewModel.addDebt(
                    contactName = name.trim(),
                    direction = if (isOwedToMe) "owed_to_me" else "i_owe",
                    amount = amount,
                    currency = currencyCode,
                    category = "Other",
                    notes = notes.trim(),
                    dueDate = if (noDueDate) null else dueDate,
                    creationDate = creationDate,
                    contactPhone = phone.trim().takeIf { it.isNotBlank() },
                )
                onBack()
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
        }
    }

    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            currentCode = currencyCode,
            onPick = { picked ->
                currencyCode = picked
                prefs.setCurrency(picked)
                showCurrencyPicker = false
            },
            onDismiss = { showCurrencyPicker = false },
        )
    }

    datePickerTarget?.let { (initial, onPick) ->
        DenaDatePickerDialog(
            initialMillis = initial,
            onPick = onPick,
            onDismiss = { datePickerTarget = null },
        )
    }
}

@Composable
private fun DirectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}