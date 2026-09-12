package com.dena.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
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

    // Summaries
    suspend fun sumOwedToMe(): Double = debtDao.sumOwedToMe() ?: 0.0
    suspend fun sumIOwe(): Double = debtDao.sumIOwe() ?: 0.0

    // Counts (fixed: simple COUNT with WHERE, no GROUP BY)
    suspend fun countOwedToMe(): Int = countDebtsByDirection("owed_to_me")
    suspend fun countIOwe(): Int = countDebtsByDirection("i_owe")

    private suspend fun countDebtsByDirection(direction: String): Int = withContext(Dispatchers.IO) {
        val query = SimpleSQLiteQuery("SELECT COUNT(*) FROM debts WHERE direction = ?", arrayOf(direction))
        val db: SupportSQLiteDatabase = database.openHelper.writableDatabase
        val cursor = db.query(query)
        try {
            if (cursor.moveToNext()) cursor.getInt(0) else 0
        } finally {
            cursor.close()
        }
    }

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
        
        val newRemaining = newPrincipal - totalPayments
        
        val updatedDebt = debt.copy(
            principalAmount = newPrincipal,
            remainingBalance = newRemaining,
            updatedAt = System.currentTimeMillis()
        )
        debtDao.update(updatedDebt)
    }

    // Record payment: insert transaction + update debt remainingBalance (allows negative for overpayment)
    suspend fun recordPayment(debtId: Long, amount: Double, note: String, timestamp: Long = System.currentTimeMillis()): Boolean {
        val debt = debtDao.getById(debtId) ?: return false

        val txDirection = if (debt.direction == "owed_to_me") {
            "payment_received" // they paid you
        } else {
            "payment_made" // you paid them
        }

        val transaction = Transaction.create(debtId, amount, txDirection, note, timestamp)
        transactionDao.insert(transaction)

        // Allow negative balance to track overpayment; do not clamp to 0
        val newBalance = debt.remainingBalance - amount

        val updatedDebt = debt.copy(
            remainingBalance = newBalance,
            updatedAt = System.currentTimeMillis()
        )
        debtDao.update(updatedDebt)
        return true
    }

    // Add more debt: increases remainingBalance + principalAmount, logged as debt_added
    suspend fun addMoreDebt(debtId: Long, amount: Double, note: String, timestamp: Long = System.currentTimeMillis()): Boolean {
        val debt = debtDao.getById(debtId) ?: return false
        val transaction = Transaction.create(debtId, amount, "debt_added", note, timestamp)
        transactionDao.insert(transaction)
        val updatedDebt = debt.copy(
            principalAmount = debt.principalAmount + amount,
            remainingBalance = debt.remainingBalance + amount,
            updatedAt = System.currentTimeMillis()
        )
        debtDao.update(updatedDebt)
        return true
    }
}