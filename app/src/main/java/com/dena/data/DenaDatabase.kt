package com.dena.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dena.data.debt.Debt
import com.dena.data.debt.DebtDao
import com.dena.data.transaction.Transaction
import com.dena.data.transaction.TransactionDao

@Database(
    entities = [Debt::class, Transaction::class],
    version = 1,
    exportSchema = false,
)
abstract class DenaDatabase : RoomDatabase() {
    abstract fun debtDao(): DebtDao
    abstract fun transactionDao(): TransactionDao
}