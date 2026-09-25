package com.dena.core

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.documentfile.provider.DocumentFile
import com.dena.data.DenaDatabaseProvider
import java.util.concurrent.TimeUnit

class BackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = DenaPreferences(ctx)
        val dirUri = prefs.getBackupDirUri()
        if (dirUri.isBlank()) return Result.success()
        return try {
            val db = DenaDatabaseProvider.get(ctx)
            val debts = db.debtDao().getAllOnce()
            val txs = db.transactionDao().getAllOnce()
            val templates = try { db.templateDao().getAllOnce() } catch (_: Exception) { emptyList() }
            val prefsSnap = BackupHelper.capturePreferences(ctx)
            val json = BackupHelper.exportProfileToJson(debts, txs, templates, prefsSnap)
            val treeUri = android.net.Uri.parse(dirUri)
            val dir = DocumentFile.fromTreeUri(ctx, treeUri) ?: return Result.failure()
            var file = dir.findFile("dena-backup.json")
            if (file == null) file = dir.createFile("application/json", "dena-backup.json")
            if (file == null) return Result.failure()
            ctx.contentResolver.openOutputStream(file.uri, "w")?.use { it.write(json.toByteArray()) }
            prefs.setLastBackupTime(System.currentTimeMillis())
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }

    companion object {
        const val WORK_NAME = "dena_scheduled_backup"
        fun schedule(context: Context) {
            val prefs = DenaPreferences(context)
            val schedule = prefs.getBackupSchedule()
            val wm = WorkManager.getInstance(context)
            if (schedule == DenaPreferences.SCHEDULE_DISABLED) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val repeatHours = when (schedule) {
                DenaPreferences.SCHEDULE_DAILY -> 24L
                DenaPreferences.SCHEDULE_WEEKLY -> 24L * 7
                else -> 24L * 30
            }
            val req = PeriodicWorkRequestBuilder<BackupWorker>(repeatHours, TimeUnit.HOURS)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun backupNow(context: Context) {
            val req = OneTimeWorkRequestBuilder<BackupWorker>().build()
            WorkManager.getInstance(context).enqueue(req)
        }
    }
}
