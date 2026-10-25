package com.seguimiento.clases

import android.app.Application
import com.seguimiento.clases.data.backup.AutoBackupManager
import com.seguimiento.clases.data.backup.AutoBackupWorker
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

    val autoBackupManager by lazy {
        AutoBackupManager(this, database, backupManager)
    }

    val repository by lazy {
        ClassRepository(database, backupManager, this, autoBackupManager)
    }

    override fun onCreate() {
        super.onCreate()
        // Crear la instancia de la base de datos al arrancar
        database
        // Copia automática cada 2 semanas en Descargas/Aulix (la primera, en cuanto se pueda)
        AutoBackupWorker.schedule(this)
    }
}
