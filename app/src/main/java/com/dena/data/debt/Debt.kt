package com.dena.data.debt

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val contactAvatar: String?,
    val direction: String, // "owed_to_me" | "i_owe"
    val principalAmount: Double,
    val remainingBalance: Double,
    val currency: String, // e.g., "BDT", "USD"
    val dateOpened: Long,
    val dueDate: Long?, // Null = indefinite debt
    val category: String, // e.g., "Rent", "Groceries"
    val notes: String,
    val contactPhone: String? = null,
    val creationDate: Long = System.currentTimeMillis(), // Manually set date
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false, // true when fully paid (remainingBalance <= 0.005); auto-reopens on new debt
) {
    companion object {
        fun fromDomain(
            contactName: String,
            contactAvatar: String? = null,
            direction: String,
            principalAmount: Double,
            currency: String,
            dateOpened: Long,
            dueDate: Long? = null,
            category: String = "Other",
            notes: String = "",
            contactPhone: String? = null,
            creationDate: Long = System.currentTimeMillis(),
        ): Debt = Debt(
            principalAmount = principalAmount,
            remainingBalance = principalAmount,
            contactName = contactName,
            contactAvatar = contactAvatar,
            direction = direction,
            currency = currency,
            dateOpened = dateOpened,
            dueDate = dueDate,
            category = category,
            notes = notes,
            contactPhone = contactPhone,
            creationDate = creationDate,
        )
    }
}