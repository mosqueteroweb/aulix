package com.seguimiento.clases

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.seguimiento.clases.data.backup.AutoBackupManager
import com.seguimiento.clases.data.backup.BackupData
import com.seguimiento.clases.data.backup.BackupManager
import com.seguimiento.clases.data.local.AppDatabase
import com.seguimiento.clases.data.local.entity.SubjectEntity
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/** La copia automática se escribe en Descargas/Aulix y solo se conservan las 3 más recientes. */
@RunWith(AndroidJUnit4::class)
class AutoBackupTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private lateinit var autoBackup: AutoBackupManager

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        autoBackup = AutoBackupManager(context, database, BackupManager(context, database))
        deleteAllCopies()
    }

    @After
    fun tearDown() {
        deleteAllCopies()
        database.close()
    }

    @Test
    fun withoutDataNoCopyIsCreated() = runBlocking {
        assertNull(autoBackup.createBackup().getOrThrow())
        assertTrue(autoBackup.listCopies().isEmpty())
    }

    @Test
    fun keepsOnlyTheThreeMostRecentCopies() = runBlocking {
        database.subjectDao().insertSubject(SubjectEntity(id = 1, code = "DI"))

        // Cuatro copias, cada una dos semanas después de la anterior
        val dates = (0..3).map { weeks ->
            Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 1, 9, 0, 0); add(Calendar.WEEK_OF_YEAR, weeks * 2) }.timeInMillis
        }
        dates.forEach { autoBackup.createBackup(it).getOrThrow() }

        val remaining = autoBackup.listCopies().map { it.second }.sorted()
        assertEquals(dates.drop(1).map { AutoBackupManager.fileNameFor(it) }, remaining)
        assertEquals(dates.last(), autoBackup.lastBackupMillis)

        // El contenido es una copia válida y completa
        val newestId = autoBackup.listCopies().maxByOrNull { it.second }!!.first
        val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, newestId)
        val content = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        val backup = Json { ignoreUnknownKeys = true }.decodeFromString(BackupData.serializer(), content)
        assertEquals(listOf("DI"), backup.subjects.map { it.code })
    }

    private fun deleteAllCopies() {
        autoBackup.listCopies().forEach { (id, _) ->
            context.contentResolver.delete(ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id), null, null)
        }
    }
}
