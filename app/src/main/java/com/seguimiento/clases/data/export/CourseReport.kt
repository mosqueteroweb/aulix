package com.seguimiento.clases.data.export

import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.data.local.entity.IdeaEntity
import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.util.DateUtils
import com.seguimiento.clases.util.HourGrid

/** Registro del curso listo para imprimir: un apartado por módulo y la nota fija. */
data class CourseReport(
    val generatedOn: String,
    val period: String?,
    val modules: List<ModuleReport>,
    val pinnedNote: String?
)

data class ModuleReport(
    val title: String,
    val isArchived: Boolean,
    val schedule: String?,
    val entries: List<LogEntry>,
    val pendingIdeas: List<String>,
    val usedIdeas: List<String>
)

data class LogEntry(
    val date: String,
    val text: String
)

object CourseReportBuilder {
    private val dayNames = mapOf(1 to "Lunes", 2 to "Martes", 3 to "Miércoles", 4 to "Jueves", 5 to "Viernes")
    const val COMPLETED_WITHOUT_NOTE = "Clase impartida (sin anotación)"

    /**
     * Construye el registro a partir de los datos guardados. Los módulos salen en su orden
     * (los archivados al final) y las anotaciones en orden cronológico, de la más antigua a la más
     * reciente, para leerlo como un diario del curso.
     */
    fun build(
        subjects: List<SubjectEntity>,
        sessions: List<ScheduleSessionEntity>,
        logs: List<ClassLogEntity>,
        ideas: List<IdeaEntity>,
        pinnedNote: String?,
        generatedOn: String
    ): CourseReport {
        val logsBySubject = logs.groupBy { it.subjectId }
        val ideasBySubject = ideas.groupBy { it.subjectId }
        val sessionsBySubject = sessions.groupBy { it.subjectId }

        val modules = subjects
            .sortedWith(compareBy<SubjectEntity> { it.isArchived }.thenBy { it.displayOrder }.thenBy { it.code })
            .map { subject ->
                val subjectIdeas = ideasBySubject[subject.id].orEmpty()
                ModuleReport(
                    title = if (subject.name.isNotBlank() && subject.name != subject.code) "${subject.code} · ${subject.name}" else subject.code,
                    isArchived = subject.isArchived,
                    schedule = scheduleSummary(sessionsBySubject[subject.id].orEmpty()),
                    entries = logsBySubject[subject.id].orEmpty()
                        .filter { it.content.isNotBlank() || it.isCompleted }
                        .sortedBy { it.date }
                        .map { log ->
                            LogEntry(
                                date = DateUtils.formatShortDate(log.date),
                                text = log.content.trim().ifEmpty { COMPLETED_WITHOUT_NOTE }
                            )
                        },
                    pendingIdeas = subjectIdeas.filter { !it.isUsed && it.text.isNotBlank() }.sortedBy { it.createdAt }.map { it.text.trim() },
                    usedIdeas = subjectIdeas.filter { it.isUsed && it.text.isNotBlank() }.sortedBy { it.createdAt }.map { it.text.trim() }
                )
            }

        val datedLogs = logs.filter { it.content.isNotBlank() || it.isCompleted }.map { it.date }.sorted()
        val period = if (datedLogs.isEmpty()) null else {
            "Del ${DateUtils.formatShortDate(datedLogs.first())} al ${DateUtils.formatShortDate(datedLogs.last())}"
        }

        return CourseReport(
            generatedOn = generatedOn,
            period = period,
            modules = modules,
            pinnedNote = pinnedNote?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    /** «Lunes 2ª–3ª h · Martes 4ª h», o null si el módulo no tiene horas en el horario. */
    fun scheduleSummary(sessions: List<ScheduleSessionEntity>): String? {
        val parts = sessions
            .sortedWith(compareBy<ScheduleSessionEntity> { it.dayOfWeek }.thenBy { it.startHour }.thenBy { it.orderIndex })
            .map { session ->
                val day = dayNames[session.dayOfWeek] ?: return@map null
                val hours = HourGrid.label(session.startHour, session.endHour)
                if (hours.isEmpty()) day else "$day $hours"
            }
            .filterNotNull()
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }
}
