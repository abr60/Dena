package com.dena.core

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.dena.data.debt.DebtDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * One-shot background backfill for legacy debts that were created before
 * device-contact phone resolution existed. Matches [Debt.contactName]
 * (case-insensitive, trimmed) against the device Phone table and fills
 * [Debt.contactPhone] where it is currently null/blank.
 *
 * Idempotent — safe to re-run. No-op when READ_CONTACTS is not granted
 * or when there are no legacy debts to fill. Marks
 * [DenaPreferences.KEY_PHONE_MIGRATION_DONE] so subsequent cold starts
 * skip the scan unless new legacy debts appear.
 */
object ContactPhoneMigrator {
    suspend fun migrateIfNeeded(
        context: Context,
        debtDao: DebtDao,
        prefs: DenaPreferences = DenaPreferences(context),
    ) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return

        withContext(Dispatchers.IO) {
            val legacy = try { debtDao.getAllOnce() } catch (_: Exception) { return@withContext }
                .filter { it.contactPhone.isNullOrBlank() }
            if (legacy.isEmpty()) {
                if (!prefs.isPhoneMigrationDone()) prefs.setPhoneMigrationDone(true)
                return@withContext
            }

            // If we already completed a full pass and no new legacy debts appeared since,
            // still attempt incremental fill — but gate on nothing else, just run.
            // The flag prevents redundant full scans after success.
            // We still run once more if legacy non-empty even when flag is set.

            // Build normalized name -> first phone number
            val contactMap = LinkedHashMap<String, String>()
            try {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ),
                    null, null, null,
                )?.use { c ->
                    val nIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val pIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (c.moveToNext()) {
                        val rawName = if (nIdx >= 0) c.getString(nIdx)?.trim() else null
                        if (rawName.isNullOrBlank()) continue
                        val key = rawName.lowercase(Locale.US)
                        if (contactMap.containsKey(key)) continue
                        val rawPhone = if (pIdx >= 0) c.getString(pIdx)?.trim() else null
                        if (rawPhone.isNullOrBlank()) continue
                        contactMap[key] = rawPhone
                    }
                }
            } catch (_: Exception) {
                return@withContext
            }
            if (contactMap.isEmpty()) return@withContext

            var filled = 0
            for (debt in legacy) {
                val key = debt.contactName.trim().lowercase(Locale.US)
                val phone = contactMap[key] ?: continue
                try {
                    debtDao.update(debt.copy(contactPhone = phone, updatedAt = System.currentTimeMillis()))
                    filled++
                } catch (_: Exception) { /* best-effort */ }
            }
            // Mark done if we filled everything or there was nothing left we could fill
            // (remaining legacy will be retried next launch if contacts change)
            val remaining = try { debtDao.getAllOnce().count { it.contactPhone.isNullOrBlank() } } catch (_: Exception) { -1 }
            if (remaining == 0) prefs.setPhoneMigrationDone(true)
            else if (filled > 0) {
                // partial progress — don't mark done, allow next launch to finish if contacts added
            } else {
                // no matches found — mark done to avoid spinning every launch;
                // a new debt with blank phone will still trigger next incremental attempt
                // because we only short-circuit when legacy.isEmpty(), not on flag alone.
                // So we intentionally don't set flag here when no matches but legacy remains.
            }
        }
    }
}
