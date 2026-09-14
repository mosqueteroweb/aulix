package com.seguimiento.clases.data.local

import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity

object DatabaseInitializer {

    suspend fun populateInitialData(database: AppDatabase) {
        val subjectDao = database.subjectDao()
        val scheduleDao = database.scheduleDao()

        // Asignaturas iniciales con sus siglas y colores distintivos
        val subjects = listOf(
            SubjectEntity(id = 1, code = "DI", name = "Desarrollo de Interfaces", colorHex = "#2563EB", displayOrder = 1),
            SubjectEntity(id = 2, code = "AFH", name = "Acceso a Fuentes Horizontales", colorHex = "#7C3AED", displayOrder = 2),
            SubjectEntity(id = 3, code = "SI", name = "Sistemas Informáticos", colorHex = "#059669", displayOrder = 3),
            SubjectEntity(id = 4, code = "ASI", name = "Administración de Sistemas", colorHex = "#D97706", displayOrder = 4)
        )
        subjectDao.insertSubjects(subjects)

        // Horario semanal según especificación exacta:
        // Lunes: DI, AFH, SI
        // Martes: SI, ASI, DI
        // Miércoles: SI, DI
        // Jueves: SI, ASI
        // Viernes: AFH, SI
        val schedule = listOf(
            // Lunes (1)
            ScheduleSessionEntity(dayOfWeek = 1, subjectId = 1, orderIndex = 0), // DI
            ScheduleSessionEntity(dayOfWeek = 1, subjectId = 2, orderIndex = 1), // AFH
            ScheduleSessionEntity(dayOfWeek = 1, subjectId = 3, orderIndex = 2), // SI

            // Martes (2)
            ScheduleSessionEntity(dayOfWeek = 2, subjectId = 3, orderIndex = 0), // SI
            ScheduleSessionEntity(dayOfWeek = 2, subjectId = 4, orderIndex = 1), // ASI
            ScheduleSessionEntity(dayOfWeek = 2, subjectId = 1, orderIndex = 2), // DI

            // Miércoles (3)
            ScheduleSessionEntity(dayOfWeek = 3, subjectId = 3, orderIndex = 0), // SI
            ScheduleSessionEntity(dayOfWeek = 3, subjectId = 1, orderIndex = 1), // DI

            // Jueves (4)
            ScheduleSessionEntity(dayOfWeek = 4, subjectId = 3, orderIndex = 0), // SI
            ScheduleSessionEntity(dayOfWeek = 4, subjectId = 4, orderIndex = 1), // ASI

            // Viernes (5)
            ScheduleSessionEntity(dayOfWeek = 5, subjectId = 2, orderIndex = 0), // AFH
            ScheduleSessionEntity(dayOfWeek = 5, subjectId = 3, orderIndex = 1)  // SI
        )
        scheduleDao.insertSessions(schedule)
    }
}
