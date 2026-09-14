package com.seguimiento.clases

import android.app.Application
import com.seguimiento.clases.data.backup.BackupManager
import com.seguimiento.clases.data.local.AppDatabase
import com.seguimiento.clases.data.repository.ClassRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy {
        AppDatabase.getDatabase(this, applicationScope)
    }

    val backupManager by lazy {
        BackupManager(this, database)
    }

    val repository by lazy {
        ClassRepository(database, backupManager)
    }

    override fun onCreate() {
        super.onCreate()
        // Asegurar que la base de datos se inicializa en segundo plano al arrancar
        database
    }
}
