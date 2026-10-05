package com.dena.core

import android.content.Context
import android.util.Log
import com.dena.data.DenaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-time cleanup for orphan transactions (rows whose debtId has no matching
 * debt row — left behind by pre-cascade deletes). Runs once per install,
 * guarded by [DenaPreferences.KEY_ORPHAN_SWEEP_DONE].
 */
object OrphanTransactionSweeper {
    private const val TAG = "OrphanSweep"

    suspend fun sweepOnce(context: Context, db: DenaDatabase): Int =
        withContext(Dispatchers.IO) {
            val prefs = DenaPreferences(context)
            if (prefs.isOrphanSweepDone()) return@withContext 0
            val removed = try {
                db.transactionDao().deleteOrphans()
            } catch (e: Exception) {
                Log.w(TAG, "sweep failed: ${e.message}")
                return@withContext 0
            }
            prefs.setOrphanSweepDone(true)
            if (removed > 0) Log.i(TAG, "removed $removed orphan transaction(s)")
            removed
        }
}
