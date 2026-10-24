package com.seguimiento.clases.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val app: String = "SeguimientoClases",
    val exportedAt: Long = System.currentTimeMillis(),
    val subjects: List<BackupSubject> = emptyList(),
    val scheduleSessions: List<BackupScheduleSession> = emptyList(),
    val classLogs: List<BackupClassLog> = emptyList(),
    val ideas: List<BackupIdea> = emptyList(),
    val pinnedNote: String? = null,
    // Ausente en copias anteriores a la 1.0.2: en ese caso la alarma del móvil no se toca
    val alarm: BackupAlarm? = null
)

@Serializable
data class BackupAlarm(
    val hour: Int,
    val minute: Int,
    val isActive: Boolean = false,
    val triggerMillis: Long = 0L
)

@Serializable
data class BackupSubject(
    val id: Long,
    val code: String,
    val name: String = "",
    val colorHex: String = "#3B82F6",
    val isArchived: Boolean = false,
    val displayOrder: Int = 0
)

@Serializable
data class BackupScheduleSession(
    val id: Long,
    val dayOfWeek: Int,
    val subjectId: Long,
    val orderIndex: Int
)

@Serializable
data class BackupClassLog(
    val id: Long,
    val subjectId: Long,
    val date: String,
    val content: String,
    val isCompleted: Boolean = false,
    val updatedAt: Long
)

@Serializable
data class BackupIdea(
    val id: Long,
    val subjectId: Long,
    val text: String,
    val isUsed: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

/** Recuento de lo restaurado, para informar al usuario al terminar la importación. */
data class ImportSummary(
    val subjects: Int,
    val classLogs: Int,
    val ideas: Int
)
