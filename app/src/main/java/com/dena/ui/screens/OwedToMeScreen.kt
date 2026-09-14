package com.dena.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dena.core.DenaPreferences
import com.dena.data.debt.Debt
import com.dena.ui.DebtViewModel
import com.dena.ui.components.DebtList
import com.dena.ui.components.EmptyState
import com.dena.ui.components.ScreenContainer
import com.dena.ui.components.SummaryBanner

@Composable
fun OwedToMeScreen(
    viewModel: DebtViewModel,
    summaryTotal: Double,
    summaryCount: Int,
    onDebtClick: (Debt) -> Unit,
) {
    val debts by viewModel.owedToMe.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val prefs = remember(context) { DenaPreferences(context) }
    val symbol = prefs.getCurrencySymbol()

    ScreenContainer(
        title = "Owed to Me",
        searchQuery = searchQuery,
        onSearchChange = { searchQuery = it },
        searchPlaceholder = "Search debtor…",
    ) {
        SummaryBanner(
            total = summaryTotal,
            count = summaryCount,
            currency = symbol,
            label = "owe you",
            isOwedToMe = true,
        )
        val (openDebts, closedDebts) = debts.partition { !it.isClosed }
        if (debts.isEmpty()) {
            EmptyState(message = "No debts owed to you yet. Tap + to add one.")
        } else {
            if (openDebts.isNotEmpty()) {
                DebtList(debts = openDebts, onDebtClick = onDebtClick, searchQuery = searchQuery, modifier = Modifier.weight(1f))
            }
            if (closedDebts.isNotEmpty()) {
                Text(
                    text = "SETTLED",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                )
                DebtList(debts = closedDebts, onDebtClick = onDebtClick, searchQuery = searchQuery, closed = true, modifier = Modifier.weight(1f))
            }
        }
    }
}
