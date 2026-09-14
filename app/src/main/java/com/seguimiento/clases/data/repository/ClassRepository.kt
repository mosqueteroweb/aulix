package com.seguimiento.clases.data.repository

import android.net.Uri
import com.seguimiento.clases.data.backup.BackupManager
import com.seguimiento.clases.data.local.AppDatabase
import com.seguimiento.clases.data.local.dao.SessionWithSubjectInfo
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.data.local.entity.IdeaEntity
import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

class ClassRepository(
    private val database: AppDatabase,
    private val backupManager: BackupManager
) {
    private val subjectDao = database.subjectDao()
    private val scheduleDao = database.scheduleDao()
    private val classLogDao = database.classLogDao()
    private val ideaDao = database.ideaDao()

    // --- ASIGNATURAS ---
    fun getAllSubjects(): Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()
    fun getActiveSubjects(): Flow<List<SubjectEntity>> = subjectDao.getActiveSubjects()
    fun getSubjectById(id: Long): Flow<SubjectEntity?> = subjectDao.getSubjectById(id)

    suspend fun saveSubject(subject: SubjectEntity): Long = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: SubjectEntity) = subjectDao.updateSubject(subject)
    suspend fun setSubjectArchived(id: Long, isArchived: Boolean) = subjectDao.setArchived(id, isArchived)
    suspend fun deleteSubject(subject: SubjectEntity) = subjectDao.deleteSubject(subject)

    // --- HORARIO ---
    fun getSessionsForDay(dayOfWeek: Int): Flow<List<SessionWithSubjectInfo>> =
        scheduleDao.getSessionsForDay(dayOfWeek)

    fun getAllSessionsWithSubject(): Flow<List<SessionWithSubjectInfo>> =
        scheduleDao.getAllSessionsWithSubject()

    suspend fun addSession(dayOfWeek: Int, subjectId: Long) {
        val currentSessions = scheduleDao.getRawSessionsForDay(dayOfWeek)
        val nextOrder = if (currentSessions.isEmpty()) 0 else currentSessions.maxOf { it.orderIndex } + 1
        scheduleDao.insertSession(
            ScheduleSessionEntity(
                dayOfWeek = dayOfWeek,
                subjectId = subjectId,
                orderIndex = nextOrder
            )
        )
    }

    suspend fun deleteSession(session: ScheduleSessionEntity) {
        scheduleDao.deleteSession(session)
        // Reordenar las restantes del día
        val remaining = scheduleDao.getRawSessionsForDay(session.dayOfWeek)
        remaining.forEachIndexed { index, item ->
            if (item.orderIndex != index) {
                scheduleDao.updateSession(item.copy(orderIndex = index))
            }
        }
    }

    suspend fun reorderSession(dayOfWeek: Int, fromIndex: Int, toIndex: Int) {
        val sessions = scheduleDao.getRawSessionsForDay(dayOfWeek).toMutableList()
        if (fromIndex in sessions.indices && toIndex in sessions.indices) {
            val moved = sessions.removeAt(fromIndex)
            sessions.add(toIndex, moved)
            sessions.forEachIndexed { index, item ->
                scheduleDao.updateSession(item.copy(orderIndex = index))
            }
        }
    }

    // --- REGISTRO DE CLASES ---
    fun getLogForSubjectAndDate(subjectId: Long, date: String): Flow<ClassLogEntity?> =
        classLogDao.getLogForSubjectAndDate(subjectId, date)

    suspend fun getLogForSubjectAndDateOnce(subjectId: Long, date: String): ClassLogEntity? =
        classLogDao.getLogForSubjectAndDateOnce(subjectId, date)

    fun getRecentLogsPriorToDate(subjectId: Long, beforeDate: String, limit: Int = 3): Flow<List<ClassLogEntity>> =
        classLogDao.getRecentLogsPriorToDate(subjectId, beforeDate, limit)

    fun getAllLogsForSubject(subjectId: Long): Flow<List<ClassLogEntity>> =
        classLogDao.getAllLogsForSubject(subjectId)

    suspend fun saveClassLog(subjectId: Long, date: String, content: String) {
        val existing = classLogDao.getLogForSubjectAndDateOnce(subjectId, date)
        val log = if (existing != null) {
            existing.copy(content = content, updatedAt = System.currentTimeMillis())
        } else {
            ClassLogEntity(
                subjectId = subjectId,
                date = date,
                content = content,
                updatedAt = System.currentTimeMillis()
            )
        }
        classLogDao.upsertLog(log)
    }

    suspend fun toggleLogCompleted(subjectId: Long, date: String, draftContent: String? = null) {
        val existing = classLogDao.getLogForSubjectAndDateOnce(subjectId, date)
        if (existing != null) {
            val newContent = draftContent ?: existing.content
            classLogDao.upsertLog(
                existing.copy(
                    content = newContent,
                    isCompleted = !existing.isCompleted,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            val content = draftContent ?: ""
            classLogDao.upsertLog(
                ClassLogEntity(
                    subjectId = subjectId,
                    date = date,
                    content = content,
                    isCompleted = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteClassLog(log: ClassLogEntity) = classLogDao.deleteLog(log)
    suspend fun deleteClassLogById(id: Long) = classLogDao.deleteLogById(id)
    suspend fun deleteLogForSubjectAndDate(subjectId: Long, date: String) =
        classLogDao.deleteLogForSubjectAndDate(subjectId, date)

    // --- IDEAS ---
    fun getPendingIdeasForSubject(subjectId: Long): Flow<List<IdeaEntity>> =
        ideaDao.getPendingIdeasForSubject(subjectId)

    fun getUsedIdeasForSubject(subjectId: Long): Flow<List<IdeaEntity>> =
        ideaDao.getUsedIdeasForSubject(subjectId)

    fun getPendingIdeasCount(subjectId: Long): Flow<Int> =
        ideaDao.getPendingIdeasCount(subjectId)

    fun getAllIdeasForSubject(subjectId: Long): Flow<List<IdeaEntity>> =
        ideaDao.getAllIdeasForSubject(subjectId)

    suspend fun addIdea(subjectId: Long, text: String): Long {
        return ideaDao.insertIdea(
            IdeaEntity(
                subjectId = subjectId,
                text = text.trim()
            )
        )
    }

    suspend fun updateIdeaText(id: Long, text: String) {
        ideaDao.updateIdeaText(id, text.trim())
    }

    suspend fun setIdeaUsedState(id: Long, isUsed: Boolean) {
        ideaDao.setIdeaUsedState(id, isUsed)
    }

    suspend fun deleteIdea(idea: IdeaEntity) = ideaDao.deleteIdea(idea)
    suspend fun deleteIdeaById(id: Long) = ideaDao.deleteIdeaById(id)

    // --- BACKUP ---
    suspend fun exportBackup(targetUri: Uri): Result<Unit> = backupManager.exportBackup(targetUri)
    suspend fun importBackup(sourceUri: Uri): Result<Int> = backupManager.importBackup(sourceUri)
}
