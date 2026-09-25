package com.dena.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dena.core.CurrencyRegistry
import com.dena.core.DenaPreferences
import com.dena.core.formatCurrencyRaw
import com.dena.core.formatSigned
import com.dena.data.debt.Debt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun isToday(millis: Long): Boolean {
    val cal = Calendar.getInstance()
    val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val debtDay = Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    return today.timeInMillis == debtDay.timeInMillis
}

private fun sectionHeader(millis: Long): String {
    if (isToday(millis)) return "TODAY"
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val debtDay = Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    if (yesterday.timeInMillis == debtDay.timeInMillis) return "YESTERDAY"
    return SimpleDateFormat("MMM d, yyyy", Locale.US).format(millis).uppercase(Locale.US)
}

enum class DebtSort(val label: String) {
    NEWEST("Newest first"),
    AMOUNT_ASC("Low to high"),
    AMOUNT_DESC("High to low"),
    CATEGORY("Category"),
}

@Composable
fun SortMenuButton(
    sort: DebtSort,
    onSortChange: (DebtSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                Icons.Filled.Sort,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DebtSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = if (option == sort) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else null,
                    enabled = option != DebtSort.CATEGORY,
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun DebtList(
    debts: List<Debt>,
    onDebtClick: (Debt) -> Unit,
    searchQuery: String = "",
    closed: Boolean = false,
    modifier: Modifier = Modifier,
    sort: DebtSort = DebtSort.NEWEST,
    showDateHeaders: Boolean = false,
    listState: LazyListState = rememberLazyListState(),
    relationshipFilter: String? = null,
) {
    val context = LocalContext.current
    val prefs = remember(context) { DenaPreferences(context) }
    val currencySymbol = prefs.getCurrencySymbol()
    val showPercentage = prefs.showPercentage()
    val showDecimals = prefs.showDecimals()
    val showContactNumber = prefs.showContactNumber()
    val dateFmt = SimpleDateFormat("M/d/yy", Locale.US)

    val filtered = remember(debts, searchQuery, relationshipFilter) {
        var list = debts
        if (relationshipFilter != null) list = list.filter { it.relationship == relationshipFilter }
        if (searchQuery.isNotBlank()) list = list.filter { it.contactName.contains(searchQuery, ignoreCase = true) }
        list
    }
    if (filtered.isEmpty() && searchQuery.isNotBlank()) {
        Text(
            text = "No results for \"$searchQuery\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        )
        return
    }
    val sorted = remember(filtered, sort) {
        when (sort) {
            DebtSort.NEWEST, DebtSort.CATEGORY -> filtered.sortedByDescending { it.dateOpened }
            DebtSort.AMOUNT_ASC -> filtered.sortedBy { it.remainingBalance }
            DebtSort.AMOUNT_DESC -> filtered.sortedByDescending { it.remainingBalance }
        }
    }
    val grouped = remember(sorted) {
        sorted.groupBy { sectionHeader(it.dateOpened) }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = 24.dp),
    ) {
        grouped.forEach { (header, groupDebts) ->
            if (showDateHeaders) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = 14.dp, bottom = 6.dp),
                )
            }
            groupDebts.forEach { debt ->
                DebtCardItem(
                    debt = debt,
                    onDebtClick = onDebtClick,
                    closed = closed,
                    currencySymbol = currencySymbol,
                    showPercentage = showPercentage,
                    showDecimals = showDecimals,
                    showContactNumber = showContactNumber,
                    dateFmt = dateFmt,
                )
            }
        }
    }
}

@Composable
private fun DebtCardItem(
    debt: Debt,
    onDebtClick: (Debt) -> Unit,
    closed: Boolean,
    currencySymbol: String,
    showPercentage: Boolean,
    showDecimals: Boolean,
    showContactNumber: Boolean,
    dateFmt: SimpleDateFormat,
) {
    val isIOwe = debt.direction == "i_owe"
    val progress = if (debt.principalAmount > 0) ((debt.principalAmount - debt.remainingBalance) / debt.principalAmount * 100).toInt().coerceIn(0, 100) else 0
    val targetContainer = when {
        closed -> MaterialTheme.colorScheme.surfaceContainer
        isSystemInDarkTheme() -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val cardContainer by animateColorAsState(targetValue = targetContainer, animationSpec = tween(250), label = "SettleCard")
    val targetAvatar = if (closed) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary
    val avatarBg by animateColorAsState(targetValue = targetAvatar, animationSpec = tween(250), label = "SettleAvatar")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        onClick = { onDebtClick(debt) },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = cardContainer,
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = if (closed) 0.dp else 2.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val letter = debt.contactName.firstOrNull()?.uppercase() ?: "?"
            val avatarFg = if (closed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(avatarBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter.toString(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = avatarFg)
            }
            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 10.dp),
            ) {
                    Text(
                        debt.contactName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (debt.relationship != "other") {
                        Text(
                            text = Debt.labelForRelationship(debt.relationship),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                    if (showContactNumber && !debt.contactPhone.isNullOrBlank()) {
                    Text(
                        text = debt.contactPhone,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                if (debt.dueDate != null) {
                    Text(
                        text = "due ${dateFmt.format(debt.dueDate)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                if (showPercentage) {
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val sym = if (debt.currency.isNotBlank()) CurrencyRegistry.symbolFor(debt.currency) else currencySymbol
                Text(
                    text = formatCurrencyRaw(debt.principalAmount, sym, showDecimals),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                if (closed) {
                    Text(
                        text = "Paid off",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                } else {
                    val raw = formatSigned(debt.remainingBalance, sym, negative = isIOwe, showDecimals = showDecimals)
                    val balText = if (isIOwe) raw else "+ $raw"
                    val moneyPalette = com.dena.ui.theme.LocalMoneyPalette.current
                    val balColor = if (isIOwe) moneyPalette.negative else moneyPalette.positive
                    Text(
                        text = balText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = balColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
