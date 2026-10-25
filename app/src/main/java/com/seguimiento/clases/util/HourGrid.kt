package com.seguimiento.clases.util

import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity

/**
 * Horario de un día como cuadrícula de horas lectivas: la posición i es la hora i+1 (1ª a 6ª)
 * y contiene el id del módulo que se imparte o null si es hora libre.
 * En la base de datos, las horas seguidas del mismo módulo se guardan como una sola sesión.
 */
object HourGrid {
    const val HOURS_PER_DAY = 6
    val SCHOOL_DAYS = 1..5 // Lunes a viernes

    fun emptyDay(): List<Long?> = List(HOURS_PER_DAY) { null }

    /** Convierte las sesiones guardadas de un día en su cuadrícula de horas. */
    fun fromSessions(sessions: List<ScheduleSessionEntity>): List<Long?> {
        val hours = MutableList<Long?>(HOURS_PER_DAY) { null }
        sessions.forEach { session ->
            for (hour in session.startHour..session.endHour) {
                if (hour in 1..HOURS_PER_DAY) hours[hour - 1] = session.subjectId
            }
        }
        return hours
    }

    /**
     * Convierte la cuadrícula de un día en sesiones: cada tramo de horas seguidas del mismo módulo
     * es una sesión, ordenadas por hora de inicio.
     */
    fun toSessions(dayOfWeek: Int, hours: List<Long?>): List<ScheduleSessionEntity> {
        val sessions = mutableListOf<ScheduleSessionEntity>()
        var hourIndex = 0
        while (hourIndex < hours.size) {
            val subjectId = hours[hourIndex]
            if (subjectId == null) {
                hourIndex++
                continue
            }
            var endIndex = hourIndex
            while (endIndex + 1 < hours.size && hours[endIndex + 1] == subjectId) endIndex++
            sessions += ScheduleSessionEntity(
                dayOfWeek = dayOfWeek,
                subjectId = subjectId,
                orderIndex = sessions.size,
                startHour = hourIndex + 1,
                endHour = endIndex + 1
            )
            hourIndex = endIndex + 1
        }
        return sessions
    }

    /** Etiqueta corta de las horas de una sesión: «2ª h» o «2ª–3ª h». Vacía si no tiene horas. */
    fun label(startHour: Int, endHour: Int): String = when {
        startHour <= 0 -> ""
        endHour <= startHour -> "${startHour}ª h"
        else -> "${startHour}ª–${endHour}ª h"
    }
}
