package com.seguimiento.clases.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "class_logs",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId", "date"], unique = true),
        Index(value = ["date"])
    ]
)
data class ClassLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val date: String, // Formato "YYYY-MM-DD"
    val content: String, // «Lo visto en clase»
    val isCompleted: Boolean = false, // Indica si la clase fue impartida/completada
    val updatedAt: Long = System.currentTimeMillis()
)
