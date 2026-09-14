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
    val ideas: List<BackupIdea> = emptyList()
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
