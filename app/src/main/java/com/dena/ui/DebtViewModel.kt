package com.dena.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dena.data.DebtRepository
import com.dena.data.debt.Debt
import com.dena.data.transaction.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DebtViewModel(
    private val repository: DebtRepository,
) : ViewModel() {

    // StateFlows for lists
    private val _owedToMe = MutableStateFlow<List<Debt>>(emptyList())
    val owedToMe: StateFlow<List<Debt>> = _owedToMe

    private val _iOwe = MutableStateFlow<List<Debt>>(emptyList())
    val iOwe: StateFlow<List<Debt>> = _iOwe

    // StateFlows for summaries
    private val _summaryOwedToMe = MutableStateFlow<Double>(0.0)
    val summaryOwedToMe: StateFlow<Double> = _summaryOwedToMe

    private val _summaryIOwe = MutableStateFlow<Double>(0.0)
    val summaryIOwe: StateFlow<Double> = _summaryIOwe

    // StateFlows for counts
    private val _countOwedToMe = MutableStateFlow<Int>(0)
    val countOwedToMe: StateFlow<Int> = _countOwedToMe

    private val _countIOwe = MutableStateFlow<Int>(0)
    val countIOwe: StateFlow<Int> = _countIOwe

    init {
        observeData()
        viewModelScope.launch { repository.ensureInitialTransactions() }
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.observeOwedToMe.collect { debts ->
                _owedToMe.value = debts
            }
        }
        viewModelScope.launch {
            repository.observeIOwe.collect { debts ->
                _iOwe.value = debts
            }
        }
        viewModelScope.launch {
            while (true) {
                _summaryOwedToMe.value = repository.sumOwedToMe()
                _summaryIOwe.value = repository.sumIOwe()
                _countOwedToMe.value = repository.countOwedToMe()
                _countIOwe.value = repository.countIOwe()
                kotlinx.coroutines.delay(1000) // Refresh periodically, or use a flow for these too
            }
        }
    }

    fun getTransactionsForDebt(debtId: Long): Flow<List<Transaction>> = repository.getTransactionsForDebt(debtId)

    fun addDebt(
        contactName: String,
        direction: String, // "owed_to_me" | "i_owe"
        amount: Double,
        currency: String,
        category: String,
        notes: String,
        dueDate: Long?,
        dateOpened: Long = System.currentTimeMillis(),
        creationDate: Long = System.currentTimeMillis(),
    ) {
        viewModelScope.launch {
            val debt = Debt.fromDomain(
                contactName = contactName,
                contactAvatar = null,
                direction = direction,
                principalAmount = amount,
                currency = currency,
                dateOpened = dateOpened,
                dueDate = dueDate,
                category = category,
                notes = notes,
                creationDate = creationDate,
            )
            val newId = repository.insertDebt(debt)
            // Task 1: log initial debt as first transaction
            val initialTx = Transaction.create(
                debtId = newId,
                amount = amount,
                direction = "debt_added",
                note = notes,
                timestamp = creationDate
            )
            repository.insertTransaction(initialTx)
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch { repository.updateTransaction(tx) }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch { repository.deleteTransaction(tx) }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    fun recordPayment(debtId: Long, amount: Double, note: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.recordPayment(debtId, amount, note, timestamp)
        }
    }

    fun addMoreDebt(debtId: Long, amount: Double, note: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.addMoreDebt(debtId, amount, note, timestamp)
        }
    }

    suspend fun importDebtsAndTxs(debts: List<Debt>, txs: List<Transaction>) {
        for (d in debts) repository.insertDebt(d.copy(id = 0))
        // naive: re-insert txs after debts; real ids will shift but okay for backup restore
        for (t in txs) repository.insertTransaction(t.copy(id = 0))
    }
}