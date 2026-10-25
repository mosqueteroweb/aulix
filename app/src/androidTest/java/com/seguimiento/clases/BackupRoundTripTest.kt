package com.seguimiento.clases

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.seguimiento.clases.alarm.AlarmScheduler
import com.seguimiento.clases.data.backup.BackupManager
import com.seguimiento.clases.data.backup.ImportSummary
import com.seguimiento.clases.data.local.AppDatabase
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.data.local.entity.IdeaEntity
import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Exporta una base de datos con todos los tipos de datos de la app, la vacía, importa la copia
 * y comprueba que vuelve exactamente lo mismo: módulos, horario, anotaciones, ideas, nota fija
 * y configuración de la alarma. Se ejecuta en un dispositivo o emulador (connectedDebugAndroidTest).
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private lateinit var backupManager: BackupManager
    private lateinit var backupFile: File

    private val prefsFiles = listOf("aulix_pinned_notes", "aulix_alarm_prefs")
    private val savedPrefs = mutableMapOf<String, Map<String, *>>()

    private val subjects = listOf(
        SubjectEntity(id = 1, code = "DI", name = "Desarrollo de Interfaces", colorHex = "#2563EB", displayOrder = 1),
        SubjectEntity(id = 2, code = "ASI", name = "Administración de Sistemas", colorHex = "#D97706", displayOrder = 2),
        SubjectEntity(id = 7, code = "OLD", name = "Módulo archivado", colorHex = "#123456", isArchived = true, displayOrder = 9)
    )
    private val sessions = listOf(
        ScheduleSessionEntity(id = 1, dayOfWeek = 1, subjectId = 1, orderIndex = 0, startHour = 2, endHour = 3),
        ScheduleSessionEntity(id = 2, dayOfWeek = 1, subjectId = 2, orderIndex = 1, startHour = 4, endHour = 4),
        ScheduleSessionEntity(id = 5, dayOfWeek = 5, subjectId = 2, orderIndex = 0, startHour = 1, endHour = 2)
    )
    private val logs = listOf(
        ClassLogEntity(id = 1, subjectId = 1, date = "2026-10-19", content = "Tema 1: «layouts»\nEjercicio 3 ✅", isCompleted = true, updatedAt = 1_000L),
        ClassLogEntity(id = 2, subjectId = 1, date = "2026-10-20", content = "", isCompleted = true, updatedAt = 2_000L),
        ClassLogEntity(id = 3, subjectId = 7, date = "2025-05-02", content = "Anotación de un módulo archivado", updatedAt = 3_000L)
    )
    private val ideas = listOf(
        IdeaEntity(id = 1, subjectId = 1, text = "Idea pendiente con ñ y tildes", createdAt = 10L, updatedAt = 11L),
        IdeaEntity(id = 2, subjectId = 2, text = "Idea ya utilizada", isUsed = true, createdAt = 20L, updatedAt = 21L),
        IdeaEntity(id = 3, subjectId = 7, text = "Idea de módulo archivado", createdAt = 30L, updatedAt = 31L)
    )

    @Before
    fun setUp() {
        // Guardar las preferencias reales de la app para dejarlas como estaban al terminar
        prefsFiles.forEach { name -> savedPrefs[name] = prefs(name).all.toMap() }

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        backupManager = BackupManager(context, database)
        backupFile = File(context.cacheDir, "copia_test.json")
    }

    @After
    fun tearDown() {
        database.close()
        backupFile.delete()
        AlarmScheduler(context).cancelAlarm()
        prefsFiles.forEach { name ->
            val editor = prefs(name).edit().clear()
            savedPrefs[name]?.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                }
            }
            editor.commit()
        }
    }

    @Test
    fun exportThenImportRestoresEveryPieceOfData() = runBlocking {
        fillDatabase()
        prefs("aulix_pinned_notes").edit().putString("key_pinned_note", "Nota fija\ncon dos líneas 📌").commit()
        val scheduler = AlarmScheduler(context)
        val triggerMillis = scheduler.scheduleAlarm(7, 45)

        assertTrue(backupManager.exportBackup(Uri.fromFile(backupFile)).isSuccess)

        // Simular otro móvil: sin datos, otra nota y sin alarma
        wipeDatabase()
        prefs("aulix_pinned_notes").edit().putString("key_pinned_note", "otra nota").commit()
        scheduler.cancelAlarm()

        val summary = backupManager.importBackup(Uri.fromFile(backupFile)).getOrThrow()
        assertEquals(ImportSummary(subjects = 3, classLogs = 3, ideas = 3), summary)

        assertEquals(subjects, database.subjectDao().getAllSubjects().first().sortedBy { it.id })
        assertEquals(sessions, database.scheduleDao().getAllRawSessions().sortedBy { it.id })
        assertEquals(logs, database.classLogDao().getAllLogs().sortedBy { it.id })
        assertEquals(ideas, database.ideaDao().getAllIdeas().sortedBy { it.id })
        assertEquals("Nota fija\ncon dos líneas 📌", prefs("aulix_pinned_notes").getString("key_pinned_note", null))
        assertTrue(scheduler.isAlarmActive())
        assertEquals(triggerMillis, scheduler.getTriggerMillis())
        assertEquals(7, scheduler.getSavedHour())
        assertEquals(45, scheduler.getSavedMinute())
    }

    @Test
    fun invalidFileLeavesExistingDataUntouched() = runBlocking {
        fillDatabase()
        backupFile.writeText("{ esto no es una copia válida")

        assertFalse(backupManager.importBackup(Uri.fromFile(backupFile)).isSuccess)

        assertEquals(subjects, database.subjectDao().getAllSubjects().first().sortedBy { it.id })
        assertEquals(logs, database.classLogDao().getAllLogs().sortedBy { it.id })
        assertEquals(ideas, database.ideaDao().getAllIdeas().sortedBy { it.id })
    }

    private suspend fun fillDatabase() {
        database.subjectDao().insertSubjects(subjects)
        database.scheduleDao().insertSessions(sessions)
        database.classLogDao().insertLogs(logs)
        database.ideaDao().insertIdeas(ideas)
    }

    private suspend fun wipeDatabase() {
        database.scheduleDao().deleteAllSessions()
        database.classLogDao().deleteAllLogs()
        database.ideaDao().deleteAllIdeas()
        database.subjectDao().deleteAllSubjects()
    }

    private fun prefs(name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)
}
