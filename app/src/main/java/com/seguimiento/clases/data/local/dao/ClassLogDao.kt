package com.seguimiento.clases.data.local.dao

import androidx.room.*
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassLogDao {
    @Query("SELECT * FROM class_logs WHERE subjectId = :subjectId AND date = :date LIMIT 1")
    fun getLogForSubjectAndDate(subjectId: Long, date: String): Flow<ClassLogEntity?>

    @Query("SELECT * FROM class_logs WHERE subjectId = :subjectId AND date = :date LIMIT 1")
    suspend fun getLogForSubjectAndDateOnce(subjectId: Long, date: String): ClassLogEntity?

    @Query("SELECT * FROM class_logs WHERE subjectId = :subjectId AND date < :beforeDate AND TRIM(content) != '' ORDER BY date DESC LIMIT :limit")
    fun getRecentLogsPriorToDate(subjectId: Long, beforeDate: String, limit: Int = 3): Flow<List<ClassLogEntity>>

    @Query("SELECT * FROM class_logs WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getAllLogsForSubject(subjectId: Long): Flow<List<ClassLogEntity>>

    @Query("SELECT * FROM class_logs ORDER BY date DESC")
    suspend fun getAllLogs(): List<ClassLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: ClassLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<ClassLogEntity>): List<Long>

    @Delete
    suspend fun deleteLog(log: ClassLogEntity)

    @Query("DELETE FROM class_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM class_logs WHERE subjectId = :subjectId AND date = :date")
    suspend fun deleteLogForSubjectAndDate(subjectId: Long, date: String)

    @Query("DELETE FROM class_logs")
    suspend fun deleteAllLogs()
}
