package com.seguimiento.clases.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["dayOfWeek", "orderIndex"])
    ]
)
data class ScheduleSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: Int, // 1 = Lunes, 2 = Martes, 3 = Miércoles, 4 = Jueves, 5 = Viernes
    val subjectId: Long,
    val orderIndex: Int, // Orden dentro del día (0, 1, 2...)
    // Horas lectivas que ocupa la sesión (1ª a 6ª). Horas seguidas del mismo módulo = una sesión.
    @ColumnInfo(defaultValue = "0")
    val startHour: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val endHour: Int = 0
)
