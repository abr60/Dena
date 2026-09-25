package com.dena.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.provider.ContactsContract
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
import com.dena.ui.ContactSuggestion
import com.dena.ui.queryDeviceContacts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
import com.dena.data.debt.Debt
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
    var relationship by remember { mutableStateOf("other") }
    var notes by remember { mutableStateOf("") }
    var noDueDate by remember { mutableStateOf(true) }
    var creationDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val prefs = remember(context) { DenaPreferences(context) }
    var currencyCode by remember { mutableStateOf(prefs.getCurrency()) }
    val showPhoneField = prefs.showManualPhoneField()
    // existing contacts for suggestions — carry phone for auto-fill (Task 2)
    val owedList by viewModel.owedToMe.collectAsStateWithLifecycle()
    val iOweList by viewModel.iOwe.collectAsStateWithLifecycle()
    val allSuggestions = remember(owedList, iOweList) {
        val map = LinkedHashMap<String, String?>()
        (owedList + iOweList).forEach { d ->
            val key = d.contactName.trim()
            if (key.isNotEmpty() && !map.containsKey(key)) map[key] = d.contactPhone?.takeIf { it.isNotBlank() }
        }
        map.map { (n, p) -> ContactSuggestion(n, p) }
    }
    // Device contacts inline as user types — merged with DB suggestions
    var deviceSuggestions by remember { mutableStateOf<List<ContactSuggestion>>(emptyList()) }
    var contactsGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    val devicePermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> contactsGranted = granted }
    // Request on form open if not yet granted (one prompt for first-install users)
    LaunchedEffect(Unit) {
        if (!contactsGranted) devicePermLauncher.launch(android.Manifest.permission.READ_CONTACTS)
    }
    LaunchedEffect(name) {
        if (name.isBlank()) { deviceSuggestions = emptyList(); return@LaunchedEffect }
        if (!contactsGranted) return@LaunchedEffect
        delay(250)
        val q = name.trim()
        if (q.isEmpty()) { deviceSuggestions = emptyList(); return@LaunchedEffect }
        deviceSuggestions = withContext(Dispatchers.IO) { queryDeviceContacts(context, q) }
    }
    val filteredSuggestions = remember(name, allSuggestions, deviceSuggestions) {
        if (name.length < 1) emptyList()
        else {
            val dbMatches = allSuggestions.filter { it.name.contains(name.trim(), ignoreCase = true) && !it.name.equals(name.trim(), ignoreCase = true) }
            val seen = dbMatches.map { it.name.lowercase(Locale.US) }.toMutableSet()
            val deviceFiltered = deviceSuggestions.filter { it.name.lowercase(Locale.US) !in seen && !it.name.equals(name.trim(), ignoreCase = true) }
            (dbMatches + deviceFiltered).take(6)
        }
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
                // Try to get display name + phone in one pass
                var resolvedName: String? = null
                var resolvedPhone: String? = null
                val proj = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                )
                context.contentResolver.query(uri, proj, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val nIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val pIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (nIdx >= 0) resolvedName = c.getString(nIdx)
                        if (pIdx >= 0) resolvedPhone = c.getString(pIdx)
                    }
                }
                if (resolvedName.isNullOrBlank()) {
                    context.contentResolver.query(uri, null, null, null, null)?.use { c2 ->
                        if (c2.moveToFirst()) {
                            val idx2 = c2.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                            if (idx2 >= 0) resolvedName = c2.getString(idx2)
                        }
                    }
                }
                if (!resolvedName.isNullOrBlank()) name = resolvedName!!
                if (!resolvedPhone.isNullOrBlank()) phone = resolvedPhone!!
                // Fallback: if phone still empty, lookup by name from local debts
                if (phone.isBlank() && !name.isBlank()) {
                    val match = allSuggestions.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
                    if (!match?.phone.isNullOrBlank()) phone = match!!.phone!!
                    else {
                        val devMatch = deviceSuggestions.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
                        if (!devMatch?.phone.isNullOrBlank()) phone = devMatch!!.phone!!
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
                                    .clickable {
                                        name = suggestion.name
                                        if (!suggestion.phone.isNullOrBlank() && phone.isBlank()) phone = suggestion.phone!!
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(suggestion.name, style = MaterialTheme.typography.bodyMedium)
                                    if (!suggestion.phone.isNullOrBlank()) {
                                        Text(suggestion.phone!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Direction toggle — compact chip pair (terminology-aware)
            run {
                val termMode = prefs.getTerminologyMode()
                val term = com.dena.core.Terminology.labels(termMode)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DirectionChip(
                        label = term.directionOwedToMe,
                        selected = isOwedToMe,
                        onClick = { isOwedToMe = true },
                        modifier = Modifier.weight(1f),
                    )
                    DirectionChip(
                        label = term.directionIOwe,
                        selected = !isOwedToMe,
                        onClick = { isOwedToMe = false },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Relationship tag selector (Task 5)
            RelationshipSelector(selected = relationship, onSelect = { relationship = it })

            // Phone (optional) — hidden when toggle off (Task 3)
            if (showPhoneField) {
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
            }

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
                    relationship = relationship,
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
private fun RelationshipSelector(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Debt.RELATIONSHIPS.forEach { key ->
            val isSel = selected == key
            Box(
                modifier = Modifier.weight(1f).height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(Debt.labelForRelationship(key), style = MaterialTheme.typography.labelSmall, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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