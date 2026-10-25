package com.seguimiento.clases

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.seguimiento.clases.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Crea una base de datos idéntica a la de la versión 2 (la de las apps 1.0.x instaladas), con datos,
 * y la abre con la versión actual: Room aplica la migración 2→3 y valida el esquema resultante.
 * Si la migración fallara o dejara el esquema distinto del esperado, abrirla lanzaría una excepción.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migracion_test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrationFrom2To3KeepsAllDataAndAssignsHours() = runBlocking {
        createVersion2Database()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
        try {
            val subjects = database.subjectDao().getAllSubjects().first()
            assertEquals(listOf("DI", "SI"), subjects.map { it.code })

            // Lunes: DI (orden 0) y SI (orden 1) pasan a 1ª y 2ª hora
            val monday = database.scheduleDao().getRawSessionsForDay(1)
            assertEquals(listOf(1 to 1, 2 to 2), monday.map { it.startHour to it.endHour })
            assertEquals(listOf(1L, 2L), monday.map { it.subjectId })

            val logs = database.classLogDao().getAllLogs()
            assertEquals(1, logs.size)
            assertEquals("Tema 1", logs.single().content)
            assertTrue(logs.single().isCompleted)

            val ideas = database.ideaDao().getAllIdeas()
            assertEquals("Idea pendiente", ideas.single().text)
        } finally {
            database.close()
        }
    }

    /** Esquema exacto que generaba Room para la versión 2 de AppDatabase. */
    private fun createVersion2Database() {
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            listOf(
                "CREATE TABLE IF NOT EXISTS `subjects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `isArchived` INTEGER NOT NULL, `displayOrder` INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS `schedule_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dayOfWeek` INTEGER NOT NULL, `subjectId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_schedule_sessions_subjectId` ON `schedule_sessions` (`subjectId`)",
                "CREATE INDEX IF NOT EXISTS `index_schedule_sessions_dayOfWeek_orderIndex` ON `schedule_sessions` (`dayOfWeek`, `orderIndex`)",
                "CREATE TABLE IF NOT EXISTS `class_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectId` INTEGER NOT NULL, `date` TEXT NOT NULL, `content` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_class_logs_subjectId_date` ON `class_logs` (`subjectId`, `date`)",
                "CREATE INDEX IF NOT EXISTS `index_class_logs_date` ON `class_logs` (`date`)",
                "CREATE TABLE IF NOT EXISTS `ideas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectId` INTEGER NOT NULL, `text` TEXT NOT NULL, `isUsed` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_ideas_subjectId` ON `ideas` (`subjectId`)",
                "CREATE INDEX IF NOT EXISTS `index_ideas_subjectId_isUsed` ON `ideas` (`subjectId`, `isUsed`)",
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3bca3cdd03a04fe0e22592557ee22215')",
                "INSERT INTO subjects VALUES (1, 'DI', 'Desarrollo de Interfaces', '#2563EB', 0, 1)",
                "INSERT INTO subjects VALUES (2, 'SI', 'Sistemas Informáticos', '#059669', 0, 2)",
                "INSERT INTO schedule_sessions VALUES (1, 1, 1, 0)",
                "INSERT INTO schedule_sessions VALUES (2, 1, 2, 1)",
                "INSERT INTO class_logs VALUES (1, 1, '2026-10-19', 'Tema 1', 1, 1000)",
                "INSERT INTO ideas VALUES (1, 2, 'Idea pendiente', 0, 10, 11)"
            ).forEach(db::execSQL)
            db.version = 2
        }
    }
}
