package com.dena.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dena.core.DenaPreferences
import com.dena.data.debt.Debt
import com.dena.ui.DebtViewModel
import com.dena.ui.components.DebtList
import com.dena.ui.components.EmptyState
import com.dena.ui.components.ScreenContainer
import com.dena.ui.components.SummaryBanner

@Composable
fun IOweScreen(
    viewModel: DebtViewModel,
    summaryTotal: Double,
    summaryCount: Int,
    onDebtClick: (Debt) -> Unit,
) {
    val debts by viewModel.iOwe.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val prefs = remember(context) { DenaPreferences(context) }
    val symbol = prefs.getCurrencySymbol()

    ScreenContainer(
        title = "I Owe",
        searchQuery = searchQuery,
        onSearchChange = { searchQuery = it },
        searchPlaceholder = "Search debtor…",
    ) {
        SummaryBanner(
            total = summaryTotal,
            count = summaryCount,
            currency = symbol,
            label = "you owe",
            isOwedToMe = false,
        )
        if (debts.isEmpty()) {
            EmptyState(message = "You don't owe anyone yet. Tap + to add a debt.")
        } else {
            DebtList(debts = debts, onDebtClick = onDebtClick, searchQuery = searchQuery)
        }
    }
}
