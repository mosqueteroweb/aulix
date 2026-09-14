package com.seguimiento.clases.data.local.dao

import androidx.room.*
import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

data class SessionWithSubjectInfo(
    @Embedded val session: ScheduleSessionEntity,
    @Relation(
        parentColumn = "subjectId",
        entityColumn = "id"
    )
    val subject: SubjectEntity
)

@Dao
interface ScheduleDao {
    @Transaction
    @Query("SELECT * FROM schedule_sessions WHERE dayOfWeek = :dayOfWeek ORDER BY orderIndex ASC")
    fun getSessionsForDay(dayOfWeek: Int): Flow<List<SessionWithSubjectInfo>>

    @Transaction
    @Query("SELECT * FROM schedule_sessions ORDER BY dayOfWeek ASC, orderIndex ASC")
    fun getAllSessionsWithSubject(): Flow<List<SessionWithSubjectInfo>>

    @Query("SELECT * FROM schedule_sessions WHERE dayOfWeek = :dayOfWeek ORDER BY orderIndex ASC")
    suspend fun getRawSessionsForDay(dayOfWeek: Int): List<ScheduleSessionEntity>

    @Query("SELECT * FROM schedule_sessions ORDER BY dayOfWeek ASC, orderIndex ASC")
    suspend fun getAllRawSessions(): List<ScheduleSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScheduleSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ScheduleSessionEntity>): List<Long>

    @Update
    suspend fun updateSession(session: ScheduleSessionEntity)

    @Delete
    suspend fun deleteSession(session: ScheduleSessionEntity)

    @Query("DELETE FROM schedule_sessions WHERE dayOfWeek = :dayOfWeek")
    suspend fun deleteSessionsForDay(dayOfWeek: Int)

    @Query("DELETE FROM schedule_sessions")
    suspend fun deleteAllSessions()
}
