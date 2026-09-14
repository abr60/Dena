package com.dena.data.transaction

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Double, // Positive number
    val direction: String, // "debt_added" | "payment_received" (they paid you) | "payment_made" (you paid them)
    val timestamp: Long,
    val note: String,
    val stableId: String, // For dedup
) {
    companion object {
        fun create(
            debtId: Long,
            amount: Double,
            direction: String,
            note: String = "",
            timestamp: Long = System.currentTimeMillis(),
        ): Transaction = Transaction(
            debtId = debtId,
            amount = amount,
            direction = direction,
            timestamp = timestamp,
            note = note,
            stableId = generateStableId(),
        )

        private fun generateStableId(): String {
            val timestamp = System.currentTimeMillis()
            val random = (0..999).random()
            return "$timestamp-$random"
        }
    }
}