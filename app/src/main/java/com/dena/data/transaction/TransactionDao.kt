package com.dena.data.transaction

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction): Long

    @Query("SELECT * FROM transactions WHERE debtId = :debtId ORDER BY timestamp DESC")
    fun getByDebtId(debtId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE debtId = :debtId AND direction = 'payment_received'")
    suspend fun sumPaymentsReceived(debtId: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE debtId = :debtId AND direction = 'payment_made'")
    suspend fun sumPaymentsMade(debtId: Long): Double?

    @Query("SELECT * FROM transactions")
    suspend fun getAllOnce(): List<Transaction>
}