package com.dena.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dena.data.debt.Debt
import com.dena.data.debt.DebtDao
import com.dena.data.transaction.Transaction
import com.dena.data.transaction.TransactionDao

// v1 -> v2: add isClosed flag; repair legacy data (negatives blocked going forward,
// so clamp old overpaid balances to 0 and close fully-paid rows)
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE debts ADD COLUMN isClosed INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE debts SET remainingBalance = 0 WHERE remainingBalance < 0")
        db.execSQL("UPDATE debts SET isClosed = 1 WHERE remainingBalance <= 0.005")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE debts ADD COLUMN contactPhone TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE debts ADD COLUMN relationship TEXT NOT NULL DEFAULT 'other'")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS message_templates (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, body TEXT NOT NULL, createdAt INTEGER NOT NULL)"
        )
    }
}

@Database(
    entities = [Debt::class, Transaction::class, com.dena.data.template.MessageTemplate::class],
    version = 4,
    exportSchema = false,
)
abstract class DenaDatabase : RoomDatabase() {
    abstract fun debtDao(): DebtDao
    abstract fun transactionDao(): TransactionDao
    abstract fun templateDao(): com.dena.data.template.MessageTemplateDao
}