package com.dena.data

import androidx.room.RoomDatabase
import com.dena.data.debt.Debt
import com.dena.data.debt.DebtDao
import com.dena.data.transaction.Transaction
import com.dena.data.transaction.TransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext


class DebtRepository(
    private val debtDao: DebtDao,
    private val transactionDao: TransactionDao,
    private val database: RoomDatabase,
) {

    // Observations
    val observeOwedToMe: Flow<List<Debt>> = debtDao.observeOwedToMe()
    val observeIOwe: Flow<List<Debt>> = debtDao.observeIOwe()
    val observeAll: Flow<List<Debt>> = debtDao.observeAll()

    // Debt CRUD
    suspend fun insertDebt(debt: Debt): Long = debtDao.insert(debt)
    suspend fun updateDebt(debt: Debt) = debtDao.update(debt)
    suspend fun deleteDebt(debt: Debt) = debtDao.delete(debt)

    // Transaction operations
    suspend fun insertTransaction(tx: Transaction): Long = transactionDao.insert(tx)
    fun getTransactionsForDebt(debtId: Long): Flow<List<Transaction>> = transactionDao.getByDebtId(debtId)

    suspend fun updateTransaction(tx: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.update(tx)
        recalculateDebtBalance(tx.debtId)
    }

    suspend fun deleteTransaction(tx: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.delete(tx)
        recalculateDebtBalance(tx.debtId)
    }

    suspend fun ensureInitialTransactions() = withContext(Dispatchers.IO) {
        val debts = debtDao.getAllOnce()
        for (debt in debts) {
            val count = transactionDao.countInitialTransactions(debt.id, debt.creationDate)
            if (count == 0) {
                val initialTx = Transaction.create(
                    debtId = debt.id,
                    amount = debt.principalAmount,
                    direction = "debt_added",
                    note = debt.notes,
                    timestamp = debt.creationDate
                )
                transactionDao.insert(initialTx)
            }
        }
    }

    suspend fun recalculateDebtBalance(debtId: Long) = withContext(Dispatchers.IO) {
        val debt = debtDao.getById(debtId) ?: return@withContext
        val transactions = transactionDao.getAllForDebtOnce(debtId)
        
        var newPrincipal = 0.0
        var totalPayments = 0.0
        
        for (tx in transactions) {
            if (tx.direction == "debt_added") {
                newPrincipal += tx.amount
            } else if (tx.direction == "payment_received" || tx.direction == "payment_made") {
                totalPayments += tx.amount
            }
        }
        
        // Balance invariant: never negative (overpayment is blocked at entry);
        // clamp defensively so no UI path can ever render an "overpaid" state
        val newRemaining = maxOf(newPrincipal - totalPayments, 0.0)

        // Auto close/reopen: settled at ~0, reopens the moment balance rises above 0
        val closed = newRemaining <= CLOSE_EPSILON

        val updatedDebt = debt.copy(
            principalAmount = newPrincipal,
            remainingBalance = newRemaining,
            isClosed = closed,
            updatedAt = System.currentTimeMillis()
        )
        debtDao.update(updatedDebt)
    }

    // Record payment: insert transaction, then recompute from the ledger (single choke point
    // keeps balance + isClosed consistent). Overpayments are refused entirely.
    suspend fun recordPayment(debtId: Long, amount: Double, note: String, timestamp: Long = System.currentTimeMillis()): Boolean {
        val debt = debtDao.getById(debtId) ?: return false
        if (amount <= 0 || amount > debt.remainingBalance + CLOSE_EPSILON) return false

        val txDirection = if (debt.direction == "owed_to_me") {
            "payment_received" // they paid you
        } else {
            "payment_made" // you paid them
        }

        val transaction = Transaction.create(debtId, amount, txDirection, note, timestamp)
        transactionDao.insert(transaction)
        recalculateDebtBalance(debtId)
        return true
    }

    // Add more debt: logged as debt_added, then recompute (reopens a settled debt automatically)
    suspend fun addMoreDebt(debtId: Long, amount: Double, note: String, timestamp: Long = System.currentTimeMillis()): Boolean {
        val debt = debtDao.getById(debtId) ?: return false
        if (amount <= 0) return false
        val transaction = Transaction.create(debtId, amount, "debt_added", note, timestamp)
        transactionDao.insert(transaction)
        recalculateDebtBalance(debtId)
        return true
    }

    companion object {
        // Half a paisa/cent: ledger amounts are 2-dp, so anything at/below this is "fully paid"
        const val CLOSE_EPSILON = 0.005
    }
}