package com.dena.data.template

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageTemplateDao {
    @Query("SELECT * FROM message_templates ORDER BY id ASC")
    fun observeAll(): Flow<List<MessageTemplate>>

    @Query("SELECT * FROM message_templates ORDER BY id ASC")
    suspend fun getAllOnce(): List<MessageTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: MessageTemplate): Long

    @Update
    suspend fun update(t: MessageTemplate)

    @Delete
    suspend fun delete(t: MessageTemplate)

    @Query("SELECT COUNT(*) FROM message_templates")
    suspend fun count(): Int

    @Query("DELETE FROM message_templates")
    suspend fun deleteAll()
}
