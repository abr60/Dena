package com.dena.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import com.dena.ui.components.DenaDatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dena.R
import com.dena.core.DenaPreferences
import com.dena.core.StatementExport
import com.dena.core.formatCurrencyRaw
import com.dena.core.formatRelativeDate
import com.dena.core.formatSigned
import com.dena.data.transaction.Transaction
import com.dena.ui.DebtViewModel
import com.dena.ui.theme.LocalMoneyPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DebtDetailScreen(
    debtId: Long,
    viewModel: DebtViewModel,
    onBack: () -> Unit,
) {
    val owedToMeList by viewModel.owedToMe.collectAsStateWithLifecycle()
    val iOweList by viewModel.iOwe.collectAsStateWithLifecycle()
    val debt = (owedToMeList + iOweList).find { it.id == debtId }

    val transactions by viewModel.getTransactionsForDebt(debtId).collectAsStateWithLifecycle(initialValue = emptyList())

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPaymentModal by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showMessageSheet by remember { mutableStateOf(false) }
    var showSwapConfirm by remember { mutableStateOf(false) }
    var showPhoneEdit by remember { mutableStateOf(false) }
    var paymentIsAddMore by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<Transaction?>(null) }
    if (debt == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(R.string.detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.debt_not_found), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val isOwedToMe = debt.direction == "owed_to_me"
    val context = LocalContext.current
    var editingName by remember(debt.id) { mutableStateOf(false) }
    var editName by remember(debt.id) { mutableStateOf(debt.contactName) }
    fun commitRename() {
        editingName = false
        viewModel.renameDebt(debt, editName)
        editName = editName.trim().ifBlank { debt.contactName }
    }
    val prefs = remember(context) { DenaPreferences(context) }
    val sym = prefs.getCurrencySymbol()
    val showDecimals = prefs.showDecimals()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.CenterAlignedTopAppBar(
            title = {
                if (editingName) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commitRename() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                } else {
                    Text(
                        debt.contactName,
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onDoubleClick = {
                                editName = debt.contactName
                                editingName = true
                            },
                        ),
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showSwapConfirm = true }) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Switch I Lent / I Borrowed")
                }
                IconButton(onClick = { showMessageSheet = true }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                }
                IconButton(onClick = { showExportSheet = true }) {
                    Icon(Icons.Filled.Share, contentDescription = "Export")
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // B. Summary card — reference-style elevated neutral
            val detailCardContainer = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = detailCardContainer,
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(
                    defaultElevation = 2.dp,
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                ) {
                    val settled = debt.principalAmount - debt.remainingBalance
                    Text(
                        text = "Remaining: ${formatSigned(debt.remainingBalance, sym, negative = !isOwedToMe, showDecimals = showDecimals)}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (debt.isClosed) {
                        Text(
                            text = "Paid off",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "Total: ${formatCurrencyRaw(debt.principalAmount, sym, showDecimals)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Settled: ${formatCurrencyRaw(settled, sym, showDecimals)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Opened: ${formatRelativeDate(debt.dateOpened)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (debt.dueDate != null) {
                        Text(
                            text = "Due: ${formatRelativeDate(debt.dueDate!!)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showPhoneEdit = true }.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (!debt.contactPhone.isNullOrBlank()) "Phone: ${debt.contactPhone}" else "Add phone number",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!debt.contactPhone.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (debt.notes.isNotBlank()) {
                        Text(
                            text = debt.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            // C. Balanced action buttons — same hierarchy, distinct semantics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { paymentIsAddMore = false; showPaymentModal = true },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("\u2212 Log Payment", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                }
                Button(
                    onClick = { paymentIsAddMore = true; showPaymentModal = true },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (isOwedToMe) "+ Lend More" else "+ Borrow More", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            // D. Transaction history section
            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium,
            )
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No transactions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    transactions.forEach { tx ->
                        TransactionRow(
                            transaction = tx,
                            debtDirection = debt.direction,
                            showDecimals = showDecimals,
                            onClick = { editingTx = tx }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_debt)) },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebt(debt)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showPaymentModal) {
        PaymentModal(
            isAddMore = paymentIsAddMore,
            isOwedToMe = isOwedToMe,
            remainingBalance = debt.remainingBalance,
            onDismiss = { showPaymentModal = false },
            onSave = { amount, note, ts ->
                if (paymentIsAddMore) viewModel.addMoreDebt(debtId, amount, note, ts)
                else viewModel.recordPayment(debtId, amount, note, ts)
                showPaymentModal = false
            },
        )
    }

    if (showExportSheet) {
        ModalBottomSheet(onDismissRequest = { showExportSheet = false }) {
            val ctx = LocalContext.current
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Export Statement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = { StatementExport.exportPdf(ctx, debt, transactions); showExportSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Export as PDF") }
                OutlinedButton(
                    onClick = { StatementExport.exportCsv(ctx, debt, transactions); showExportSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Export as CSV") }
            }
        }
    }

    if (showSwapConfirm) {
        val targetLabel = if (isOwedToMe) "I Borrowed" else "I Lent"
        AlertDialog(
            onDismissRequest = { showSwapConfirm = false },
            title = { Text("Switch direction?") },
            text = { Text("Move \"${debt.contactName}\" to $targetLabel? Amount and history stay the same.") },
            confirmButton = {
                TextButton(onClick = { viewModel.swapDirection(debt); showSwapConfirm = false }) { Text("Switch") }
            },
            dismissButton = { TextButton(onClick = { showSwapConfirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showPhoneEdit) {
        var phoneDraft by remember(debt.id, debt.contactPhone) { mutableStateOf(debt.contactPhone ?: "") }
        AlertDialog(
            onDismissRequest = { showPhoneEdit = false },
            title = { Text("Phone number") },
            text = {
                OutlinedTextField(
                    value = phoneDraft,
                    onValueChange = { phoneDraft = it },
                    label = { Text("Phone") },
                    placeholder = { Text("017... or +880...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.updateContactPhone(debt, phoneDraft); showPhoneEdit = false }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showPhoneEdit = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showMessageSheet) {
        MessageSheet(
            debt = debt,
            remainingText = formatCurrencyRaw(debt.remainingBalance, sym, showDecimals),
            dueDate = debt.dueDate,
            onDismiss = { showMessageSheet = false },
            onSavePhone = { newPhone -> viewModel.updateContactPhone(debt, newPhone) },
        )
    }

    editingTx?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            debtDirection = debt.direction,
            remainingBalance = debt.remainingBalance,
            onDismiss = { editingTx = null },
            onSave = { updated ->
                viewModel.updateTransaction(updated)
                editingTx = null
            },
            onDelete = {
                viewModel.deleteTransaction(tx)
                editingTx = null
            }
        )
    }
}

private fun buildReminderMessage(firstName: String, amount: String, isOwedToMe: Boolean, dueDate: Long?): String {
    val duePart = dueDate?.let { " — due ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(it))}" } ?: ""
    return if (isOwedToMe) {
        if (dueDate != null) "Hi $firstName,\n\njust a friendly reminder that $amount to me is still outstanding — it was due ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(dueDate))}. Could you settle when convenient?\n\nThank you!"
        else "Hi $firstName,\n\njust a friendly reminder that $amount to me is still outstanding. Could you settle when convenient?\n\nThank you!"
    } else {
        "Hi $firstName,\n\nquick heads-up — I owe you $amount$duePart. Repaying soon, thanks for your patience!"
    }
}

private fun sanitizeForSms(raw: String): String = raw.trim()
private fun sanitizeForWa(raw: String): String {
    var d = raw.filter { it.isDigit() }
    if (d.startsWith("0") && d.length >= 11) d = "880" + d.drop(1)
    return d
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageSheet(
    debt: com.dena.data.debt.Debt,
    remainingText: String,
    dueDate: Long?,
    onDismiss: () -> Unit,
    onSavePhone: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val isOwedToMe = debt.direction == "owed_to_me"
    val firstName = debt.contactName.trim().substringBefore(" ").takeIf { it.isNotBlank() } ?: debt.contactName
    var phone by remember(debt.id) { mutableStateOf(debt.contactPhone ?: "") }
    var channel by remember { mutableStateOf("sms") } // sms | wa
    var message by remember(debt.id, dueDate, remainingText) { mutableStateOf(buildReminderMessage(firstName, remainingText, isOwedToMe, dueDate)) }
    val hasPhone = phone.trim().isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Message $firstName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number") },
                placeholder = { Text("017... or +880...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val smsSel = channel == "sms"
                val waSel = channel == "wa"
                OutlinedButton(
                    onClick = { channel = "sms" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (smsSel) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors(),
                ) { Text("SMS") }
                OutlinedButton(
                    onClick = { channel = "wa" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (waSel) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors(),
                ) { Text("WhatsApp") }
            }
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 6,
            )
            Button(
                onClick = {
                    val trimmedPhone = phone.trim()
                    if (trimmedPhone.isNotBlank() && trimmedPhone != debt.contactPhone) onSavePhone(trimmedPhone)
                    try {
                        if (channel == "wa") {
                            val waDigits = sanitizeForWa(trimmedPhone)
                            if (waDigits.isBlank()) throw IllegalArgumentException("Missing phone")
                            val enc = java.net.URLEncoder.encode(message, "UTF-8")
                            val uri = android.net.Uri.parse("https://wa.me/$waDigits?text=$enc")
                            ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                        } else {
                            val smsPhone = sanitizeForSms(trimmedPhone)
                            val uri = if (smsPhone.isNotBlank()) android.net.Uri.parse("smsto:$smsPhone") else android.net.Uri.parse("smsto:")
                            val i = android.content.Intent(android.content.Intent.ACTION_SENDTO, uri).apply { putExtra("sms_body", message) }
                            ctx.startActivity(i)
                        }
                    } catch (_: Exception) {}
                    onDismiss()
                },
                enabled = hasPhone && message.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) { Text(if (channel == "wa") "Send via WhatsApp" else "Send via SMS") }
            if (!hasPhone) Text("Add a phone number to send directly to $firstName.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    debtDirection: String,
    showDecimals: Boolean,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember(context) { DenaPreferences(context) }
    val currencySymbol = prefs.getCurrencySymbol()
    val isDebtAdded = transaction.direction == "debt_added"
    val label = when (transaction.direction) {
        "debt_added" -> "Debt added"
        "payment_received" -> "Payment received"
        "payment_made" -> "Payment made"
        else -> transaction.direction
    }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val txCardContainer = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = txCardContainer,
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val raw = formatCurrencyRaw(transaction.amount, currencySymbol, showDecimals)
                val moneyPalette = LocalMoneyPalette.current
                val amountColor = if (isDebtAdded) moneyPalette.positive else moneyPalette.negative
                Text(
                    text = if (isDebtAdded) "+ $raw" else "− $raw",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor,
                )
                Text(
                    text = dateFormat.format(transaction.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    debtDirection: String,
    remainingBalance: Double = 0.0,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: () -> Unit,
) {
    var amountText by remember { mutableStateOf(transaction.amount.toString().let { if (it.endsWith(".0")) it.dropLast(2) else it }) }
    var noteText by remember { mutableStateOf(transaction.note) }
    var txDate by remember { mutableStateOf(transaction.timestamp) }
    var txDirection by remember { mutableStateOf(transaction.direction) }
    val context = LocalContext.current
    val df = SimpleDateFormat("MMM d, yyyy", Locale.US)
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val typeOptions = if (debtDirection == "owed_to_me") listOf("Debt added", "Payment received") else listOf("Debt added", "Payment made")
    val dirValues = if (debtDirection == "owed_to_me") listOf("debt_added", "payment_received") else listOf("debt_added", "payment_made")
    val selectedIdx = dirValues.indexOf(txDirection).coerceAtLeast(0)
    // Cap edited payments so no edit can create an overpaid balance:
    // removing this row first would leave `remainingBalance + oldAmount` payable at most
    val oldWasPayment = transaction.direction == "payment_received" || transaction.direction == "payment_made"
    val nowIsPayment = txDirection != "debt_added"
    val enteredEditAmount = amountText.toDoubleOrNull() ?: 0.0
    val maxPayable = remainingBalance + if (oldWasPayment) transaction.amount else 0.0
    val editExceeds = nowIsPayment && enteredEditAmount > maxPayable + com.dena.data.DebtRepository.CLOSE_EPSILON
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Edit entry", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Change the amount, note, date or type. Balance recalculates on save.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                dirValues.forEachIndexed { idx, _ ->
                    val sel = idx == selectedIdx
                    if (sel) {
                        Button(
                            onClick = { txDirection = dirValues[idx] },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        ) { Text(typeOptions[idx], fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    } else {
                        OutlinedButton(
                            onClick = { txDirection = dirValues[idx] },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text(typeOptions[idx], fontSize = 12.sp) }
                    }
                }
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amountText = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = editExceeds,
                supportingText = if (editExceeds) {
                    { Text("Amount exceeds remaining debt", color = MaterialTheme.colorScheme.error) }
                } else null,
            )
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(stringResource(R.string.note_optional)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showDatePicker = true },
            ) {
                OutlinedTextField(
                    value = df.format(txDate),
                    onValueChange = {},
                    label = { Text("Date") },
                    leadingIcon = { Icon(Icons.Filled.DateRange, null) },
                    readOnly = true,
                    enabled = false,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete", fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0 && !editExceeds) onSave(transaction.copy(amount = amount, note = noteText, timestamp = txDate, direction = txDirection))
                    },
                    enabled = enteredEditAmount > 0 && !editExceeds,
                    shape = RoundedCornerShape(12.dp),
                ) { Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold) }
            }
        }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete entry?") },
            text = { Text("This will remove this history entry and recalculate the balance.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (showDatePicker) {
        DenaDatePickerDialog(
            initialMillis = txDate,
            onPick = { txDate = it },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
fun PaymentModal(
    isAddMore: Boolean,
    isOwedToMe: Boolean,
    remainingBalance: Double = 0.0,
    onDismiss: () -> Unit,
    onSave: (Double, String, Long) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var txDate by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val df = SimpleDateFormat("MMM d, yyyy", Locale.US)
    var showDatePicker by remember { mutableStateOf(false) }

    val title = if (isAddMore) (if (isOwedToMe) "Lend More" else "Borrow More") else "Log Payment"
    val amountLabel = if (isAddMore) (if (isOwedToMe) "Amount to lend" else "Amount to borrow") else "Amount to log"
    // Live overpayment guard: payments can never exceed what is left
    val enteredAmount = amountText.toDoubleOrNull() ?: 0.0
    val exceedsBalance = !isAddMore && enteredAmount > remainingBalance + com.dena.data.DebtRepository.CLOSE_EPSILON

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = it
                        }
                    },
                    label = { Text(amountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = exceedsBalance,
                    supportingText = if (exceedsBalance) {
                        { Text("Amount exceeds remaining debt", color = MaterialTheme.colorScheme.error) }
                    } else null,
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.note_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
) { showDatePicker = true },
                ) {
                    OutlinedTextField(
                        value = df.format(txDate),
                        onValueChange = {},
                        label = { Text("Date") },
                        leadingIcon = { Icon(Icons.Filled.DateRange, null) },
                        readOnly = true,
                        enabled = false,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && !exceedsBalance) onSave(amount, noteText, txDate)
                },
                enabled = enteredAmount > 0 && !exceedsBalance,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
    if (showDatePicker) {
        DenaDatePickerDialog(
            initialMillis = txDate,
            onPick = { txDate = it },
            onDismiss = { showDatePicker = false },
        )
    }
}
