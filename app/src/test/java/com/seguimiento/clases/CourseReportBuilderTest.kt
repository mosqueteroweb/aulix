package com.seguimiento.clases

import com.seguimiento.clases.data.export.CourseReportBuilder
import com.seguimiento.clases.data.export.LogEntry
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.data.local.entity.IdeaEntity
import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseReportBuilderTest {

    private val subjects = listOf(
        SubjectEntity(id = 2, code = "FH", name = "", displayOrder = 2),
        SubjectEntity(id = 1, code = "DI", name = "Desarrollo de Interfaces", displayOrder = 1),
        SubjectEntity(id = 9, code = "OLD", name = "Archivado", isArchived = true, displayOrder = 0)
    )

    @Test
    fun testModulesInOrderWithArchivedLastAndLogsChronological() {
        val report = CourseReportBuilder.build(
            subjects = subjects,
            sessions = emptyList(),
            logs = listOf(
                ClassLogEntity(subjectId = 1, date = "2026-10-20", content = "Tema 2"),
                ClassLogEntity(subjectId = 1, date = "2026-10-13", content = "Tema 1"),
                ClassLogEntity(subjectId = 1, date = "2026-10-27", content = "   "), // vacía y no impartida: no sale
                ClassLogEntity(subjectId = 1, date = "2026-11-03", content = "", isCompleted = true)
            ),
            ideas = emptyList(),
            pinnedNote = "  ",
            generatedOn = "25/10/26"
        )

        assertEquals(listOf("DI · Desarrollo de Interfaces", "FH", "OLD · Archivado"), report.modules.map { it.title })
        assertTrue(report.modules.last().isArchived)
        assertEquals(
            listOf(
                LogEntry("13/10/26", "Tema 1"),
                LogEntry("20/10/26", "Tema 2"),
                LogEntry("03/11/26", CourseReportBuilder.COMPLETED_WITHOUT_NOTE)
            ),
            report.modules.first().entries
        )
        assertEquals("Del 13/10/26 al 03/11/26", report.period)
        assertNull(report.pinnedNote)
    }

    @Test
    fun testIdeasSplitIntoPendingAndUsed() {
        val report = CourseReportBuilder.build(
            subjects = subjects,
            sessions = emptyList(),
            logs = emptyList(),
            ideas = listOf(
                IdeaEntity(subjectId = 2, text = "Usada", isUsed = true, createdAt = 1),
                IdeaEntity(subjectId = 2, text = "Pendiente B", createdAt = 3),
                IdeaEntity(subjectId = 2, text = "Pendiente A", createdAt = 2)
            ),
            pinnedNote = "Nota",
            generatedOn = "25/10/26"
        )
        val fh = report.modules.first { it.title == "FH" }
        assertEquals(listOf("Pendiente A", "Pendiente B"), fh.pendingIdeas)
        assertEquals(listOf("Usada"), fh.usedIdeas)
        assertNull(report.period)
        assertEquals("Nota", report.pinnedNote)
    }

    @Test
    fun testScheduleSummary() {
        val sessions = listOf(
            ScheduleSessionEntity(dayOfWeek = 2, subjectId = 1, orderIndex = 0, startHour = 4, endHour = 4),
            ScheduleSessionEntity(dayOfWeek = 1, subjectId = 1, orderIndex = 0, startHour = 2, endHour = 3)
        )
        assertEquals("Lunes 2ª–3ª h · Martes 4ª h", CourseReportBuilder.scheduleSummary(sessions))
        assertNull(CourseReportBuilder.scheduleSummary(emptyList()))
    }
}
