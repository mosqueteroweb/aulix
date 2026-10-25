package com.seguimiento.clases

import android.app.Application
import com.seguimiento.clases.data.backup.BackupManager
import com.seguimiento.clases.data.local.AppDatabase
import com.seguimiento.clases.data.repository.ClassRepository

class AppApplication : Application() {
    val database by lazy {
        AppDatabase.getDatabase(this)
    }

    val backupManager by lazy {
        BackupManager(this, database)
    }

    val repository by lazy {
        ClassRepository(database, backupManager, this)
    }

    override fun onCreate() {
        super.onCreate()
        // Crear la instancia de la base de datos al arrancar
        database
    }
}
