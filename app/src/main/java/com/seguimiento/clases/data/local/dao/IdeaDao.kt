package com.seguimiento.clases.data.local.dao

import androidx.room.*
import com.seguimiento.clases.data.local.entity.IdeaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas WHERE subjectId = :subjectId AND isUsed = 0 ORDER BY createdAt DESC")
    fun getPendingIdeasForSubject(subjectId: Long): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE subjectId = :subjectId AND isUsed = 1 ORDER BY updatedAt DESC")
    fun getUsedIdeasForSubject(subjectId: Long): Flow<List<IdeaEntity>>

    @Query("SELECT COUNT(*) FROM ideas WHERE subjectId = :subjectId AND isUsed = 0")
    fun getPendingIdeasCount(subjectId: Long): Flow<Int>

    @Query("SELECT * FROM ideas WHERE subjectId = :subjectId ORDER BY isUsed ASC, createdAt DESC")
    fun getAllIdeasForSubject(subjectId: Long): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas ORDER BY createdAt DESC")
    suspend fun getAllIdeas(): List<IdeaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdea(idea: IdeaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdeas(ideas: List<IdeaEntity>): List<Long>

    @Update
    suspend fun updateIdea(idea: IdeaEntity)

    @Query("UPDATE ideas SET text = :text, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateIdeaText(id: Long, text: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ideas SET isUsed = :isUsed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setIdeaUsedState(id: Long, isUsed: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteIdea(idea: IdeaEntity)

    @Query("DELETE FROM ideas WHERE id = :id")
    suspend fun deleteIdeaById(id: Long)

    @Query("DELETE FROM ideas")
    suspend fun deleteAllIdeas()
}
