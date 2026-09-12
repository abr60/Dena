package com.dena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dena.R
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

@Composable
fun DebtList(
    debts: List<Debt>,
    onDebtClick: (Debt) -> Unit,
    searchQuery: String = "",
) {
    val context = LocalContext.current
    val prefs = DenaPreferences(context)
    val currencySymbol = prefs.getCurrencySymbol()
    val dateFmt = SimpleDateFormat("M/d/yy", Locale.US)
    // Filter by search
    val filtered = remember(debts, searchQuery) {
        if (searchQuery.isBlank()) debts
        else debts.filter { it.contactName.contains(searchQuery, ignoreCase = true) }
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
    // Group by day — like Debt Tracker TODAY
    val grouped = remember(filtered) {
        filtered.sortedByDescending { it.dateOpened }.groupBy { sectionHeader(it.dateOpened) }
    }
    grouped.forEach { (header, groupDebts) ->
        Text(
            text = header,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        )
        groupDebts.forEach { debt ->
            val isOverpaid = debt.remainingBalance < 0
            val isIOwe = debt.direction == "i_owe"
            val progress = if (debt.principalAmount > 0) ((debt.principalAmount - debt.remainingBalance) / debt.principalAmount * 100).toInt().coerceIn(0, 100) else 0
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp),
                onClick = { onDebtClick(debt) },
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val letter = debt.contactName.firstOrNull()?.uppercase() ?: "?"
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(letter.toString(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 12.dp, end = 10.dp),
                    ) {
                        Text(
                            debt.contactName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = if (debt.dueDate != null) "due ${dateFmt.format(debt.dueDate)}" else "loan with no due date",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Column(
                        modifier = Modifier.width(112.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = "$progress%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        // Use per-debt currency symbol if set, else passed symbol
                        val sym = if (debt.currency.isNotBlank()) CurrencyRegistry.symbolFor(debt.currency) else currencySymbol
                        val showDecimals = DenaPreferences(LocalContext.current).showDecimals()
                        Text(
                            text = formatCurrencyRaw(debt.principalAmount, sym, showDecimals),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        // Signed amount — I Owe = -$X, Owed to Me = $X
                        val balText = formatSigned(debt.remainingBalance, sym, negative = isIOwe || isOverpaid, showDecimals = showDecimals)
                        val moneyPalette = com.dena.ui.theme.LocalMoneyPalette.current
                        val balColor = if (isIOwe || isOverpaid) moneyPalette.negative else moneyPalette.positive
                        Text(
                            text = balText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = balColor,
                            maxLines = 1,
                        )
                        if (isOverpaid) {
                            Text(stringResource(R.string.overpaid), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
