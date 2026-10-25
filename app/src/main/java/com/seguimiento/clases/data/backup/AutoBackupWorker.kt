package com.seguimiento.clases.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.seguimiento.clases.AppApplication
import java.util.concurrent.TimeUnit

/** Tarea en segundo plano que hace la copia automática cada [INTERVAL_DAYS] días. */
class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val autoBackupManager = (applicationContext as AppApplication).autoBackupManager
        if (!autoBackupManager.isSupported) return Result.success()
        return if (autoBackupManager.createBackup().isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val INTERVAL_DAYS = 14L
        private const val WORK_NAME = "aulix_copia_automatica"

        /** Programa la copia periódica; si ya estaba programada, la deja como está. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(INTERVAL_DAYS, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
