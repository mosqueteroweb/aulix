package com.seguimiento.clases.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String, // ej. "DI", "AFH", "SI", "ASI"
    val name: String = "", // Nombre extendido opcional
    val colorHex: String = "#3B82F6", // Color identificativo
    val isArchived: Boolean = false,
    val displayOrder: Int = 0
)
