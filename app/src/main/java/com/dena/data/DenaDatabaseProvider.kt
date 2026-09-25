package com.dena.data

import android.content.Context
import androidx.room.Room

object DenaDatabaseProvider {
    @Volatile
    private var instance: DenaDatabase? = null

    fun get(context: Context): DenaDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                DenaDatabase::class.java,
                "dena.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }
}