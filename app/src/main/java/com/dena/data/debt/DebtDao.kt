package com.dena.data.debt

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: Debt): Long

    @Update
    suspend fun update(debt: Debt)

    @Delete
    suspend fun delete(debt: Debt)

    @Query("SELECT * FROM debts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE direction = 'owed_to_me' ORDER BY updatedAt DESC")
    fun observeOwedToMe(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE direction = 'i_owe' ORDER BY updatedAt DESC")
    fun observeIOwe(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Long): Debt?

    @Query("SELECT * FROM debts")
    suspend fun getAllOnce(): List<Debt>

    @Query("DELETE FROM debts")
    suspend fun deleteAll()
}