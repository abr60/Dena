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
            ).build().also { instance = it }
        }
}