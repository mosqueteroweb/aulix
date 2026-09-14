package com.seguimiento.clases

import com.seguimiento.clases.data.backup.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class BackupSerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testBackupSerializationAndDeserialization() {
        val originalBackup = BackupData(
            version = 1,
            app = "SeguimientoClases",
            exportedAt = 1700000000000L,
            subjects = listOf(
                BackupSubject(id = 1, code = "DI", name = "Desarrollo de Interfaces", colorHex = "#2563EB", isArchived = false, displayOrder = 1),
                BackupSubject(id = 2, code = "AFH", name = "Acceso a Fuentes Horizontales", colorHex = "#7C3AED", isArchived = false, displayOrder = 2),
                BackupSubject(id = 3, code = "SI", name = "Sistemas Informáticos", colorHex = "#059669", isArchived = false, displayOrder = 3),
                BackupSubject(id = 4, code = "ASI", name = "Administración de Sistemas", colorHex = "#D97706", isArchived = false, displayOrder = 4)
            ),
            scheduleSessions = listOf(
                BackupScheduleSession(id = 1, dayOfWeek = 1, subjectId = 1, orderIndex = 0),
                BackupScheduleSession(id = 2, dayOfWeek = 1, subjectId = 2, orderIndex = 1),
                BackupScheduleSession(id = 3, dayOfWeek = 1, subjectId = 3, orderIndex = 2)
            ),
            classLogs = listOf(
                BackupClassLog(id = 1, subjectId = 1, date = "2026-09-14", content = "Introducción al diseño y accesibilidad", updatedAt = 1700000010000L)
            ),
            ideas = listOf(
                BackupIdea(id = 1, subjectId = 1, text = "Práctica con Figma y Compose", isUsed = false, createdAt = 1700000000000L, updatedAt = 1700000000000L)
            )
        )

        val jsonString = json.encodeToString(originalBackup)
        assertTrue(jsonString.contains("SeguimientoClases"))
        assertTrue(jsonString.contains("Desarrollo de Interfaces"))

        val decodedBackup = json.decodeFromString<BackupData>(jsonString)
        assertEquals(originalBackup.version, decodedBackup.version)
        assertEquals(originalBackup.subjects.size, decodedBackup.subjects.size)
        assertEquals(originalBackup.scheduleSessions.size, decodedBackup.scheduleSessions.size)
        assertEquals(originalBackup.classLogs.size, decodedBackup.classLogs.size)
        assertEquals(originalBackup.ideas.size, decodedBackup.ideas.size)

        assertEquals("DI", decodedBackup.subjects[0].code)
        assertEquals("Introducción al diseño y accesibilidad", decodedBackup.classLogs[0].content)
        assertEquals("Práctica con Figma y Compose", decodedBackup.ideas[0].text)
        assertFalse(decodedBackup.ideas[0].isUsed)
    }

    @Test
    fun testCorruptedBackupFailsValidation() {
        val invalidJson = "{ \"version\": \"invalid_version_not_a_number\" }"
        try {
            json.decodeFromString<BackupData>(invalidJson)
            fail("Debería haber lanzado una excepción de deserialización")
        } catch (e: Exception) {
            // Se espera fallo de parseo JSON
            assertNotNull(e)
        }
    }
}
