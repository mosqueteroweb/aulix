package com.seguimiento.clases.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.seguimiento.clases.data.local.AppDatabase
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.data.local.entity.IdeaEntity
import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportBackup(targetUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val subjects = database.subjectDao().getAllSubjects().first()
            val sessions = database.scheduleDao().getAllRawSessions()
            val logs = database.classLogDao().getAllLogs()
            val ideas = database.ideaDao().getAllIdeas()

            val backup = BackupData(
                version = 1,
                app = "SeguimientoClases",
                exportedAt = System.currentTimeMillis(),
                subjects = subjects.map {
                    BackupSubject(
                        id = it.id,
                        code = it.code,
                        name = it.name,
                        colorHex = it.colorHex,
                        isArchived = it.isArchived,
                        displayOrder = it.displayOrder
                    )
                },
                scheduleSessions = sessions.map {
                    BackupScheduleSession(
                        id = it.id,
                        dayOfWeek = it.dayOfWeek,
                        subjectId = it.subjectId,
                        orderIndex = it.orderIndex
                    )
                },
                classLogs = logs.map {
                    BackupClassLog(
                        id = it.id,
                        subjectId = it.subjectId,
                        date = it.date,
                        content = it.content,
                        isCompleted = it.isCompleted,
                        updatedAt = it.updatedAt
                    )
                },
                ideas = ideas.map {
                    BackupIdea(
                        id = it.id,
                        subjectId = it.subjectId,
                        text = it.text,
                        isUsed = it.isUsed,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                }
            )

            val jsonString = json.encodeToString(backup)

            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo para escritura"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(sourceUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Lectura con límite de tamaño para prevenir OutOfMemoryError (máximo 15 MB)
            val maxChars = 15 * 1024 * 1024
            val content = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val buffer = CharArray(8192)
                val sb = StringBuilder()
                var totalCharsRead = 0
                var charsRead: Int
                while (reader.read(buffer).also { charsRead = it } != -1) {
                    totalCharsRead += charsRead
                    if (totalCharsRead > maxChars) {
                        return@withContext Result.failure(Exception("El archivo supera el tamaño máximo permitido de 15 MB."))
                    }
                    sb.append(buffer, 0, charsRead)
                }
                sb.toString()
            } ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo de origen"))

            // Validación estricta previa a modificar la base de datos
            val backup = try {
                json.decodeFromString<BackupData>(content)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("El archivo seleccionado no tiene un formato de copia válido: ${e.localizedMessage}"))
            }

            if (backup.version > 1) {
                return@withContext Result.failure(Exception("La versión del archivo de copia (${backup.version}) es superior a la soportada por esta app."))
            }

            // Validación de integridad referencial: comprobar que no existan registros huérfanos
            val validSubjectIds = backup.subjects.map { it.id }.toSet()
            val orphanSessions = backup.scheduleSessions.filter { it.subjectId !in validSubjectIds }
            val orphanLogs = backup.classLogs.filter { it.subjectId !in validSubjectIds }
            val orphanIdeas = backup.ideas.filter { it.subjectId !in validSubjectIds }

            if (orphanSessions.isNotEmpty() || orphanLogs.isNotEmpty() || orphanIdeas.isNotEmpty()) {
                return@withContext Result.failure(
                    Exception("La copia de seguridad contiene datos inconsistentes o huérfanos (sesiones o anotaciones que apuntan a módulos inexistentes). Restauración cancelada.")
                )
            }

            // Si pasa todas las validaciones, realizamos la restauración dentro de una transacción Room
            database.withTransaction {
                // Limpiar datos antiguos
                database.scheduleDao().deleteAllSessions()
                database.classLogDao().deleteAllLogs()
                database.ideaDao().deleteAllIdeas()
                database.subjectDao().deleteAllSubjects()

                // Insertar datos restaurados
                val subjectEntities = backup.subjects.map {
                    SubjectEntity(
                        id = it.id,
                        code = it.code,
                        name = it.name,
                        colorHex = it.colorHex,
                        isArchived = it.isArchived,
                        displayOrder = it.displayOrder
                    )
                }
                database.subjectDao().insertSubjects(subjectEntities)

                val sessionEntities = backup.scheduleSessions.map {
                    ScheduleSessionEntity(
                        id = it.id,
                        dayOfWeek = it.dayOfWeek,
                        subjectId = it.subjectId,
                        orderIndex = it.orderIndex
                    )
                }
                database.scheduleDao().insertSessions(sessionEntities)

                val logEntities = backup.classLogs.map {
                    ClassLogEntity(
                        id = it.id,
                        subjectId = it.subjectId,
                        date = it.date,
                        content = it.content,
                        isCompleted = it.isCompleted,
                        updatedAt = it.updatedAt
                    )
                }
                database.classLogDao().insertLogs(logEntities)

                val ideaEntities = backup.ideas.map {
                    IdeaEntity(
                        id = it.id,
                        subjectId = it.subjectId,
                        text = it.text,
                        isUsed = it.isUsed,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                }
                database.ideaDao().insertIdeas(ideaEntities)
            }

            Result.success(backup.classLogs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
