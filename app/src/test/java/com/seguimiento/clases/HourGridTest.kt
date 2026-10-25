package com.seguimiento.clases

import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.util.HourGrid
import org.junit.Assert.assertEquals
import org.junit.Test

class HourGridTest {

    private val fh = 1L
    private val di = 2L
    private val asi = 3L

    @Test
    fun testConsecutiveHoursOfSameModuleBecomeOneSession() {
        // Lunes: libre, FH, FH, DI, libre, ASI
        val sessions = HourGrid.toSessions(1, listOf(null, fh, fh, di, null, asi))

        assertEquals(
            listOf(
                ScheduleSessionEntity(dayOfWeek = 1, subjectId = fh, orderIndex = 0, startHour = 2, endHour = 3),
                ScheduleSessionEntity(dayOfWeek = 1, subjectId = di, orderIndex = 1, startHour = 4, endHour = 4),
                ScheduleSessionEntity(dayOfWeek = 1, subjectId = asi, orderIndex = 2, startHour = 6, endHour = 6)
            ),
            sessions
        )
    }

    @Test
    fun testSameModuleSeparatedByAnotherIsTwoSessions() {
        val sessions = HourGrid.toSessions(2, listOf(di, asi, di, null, null, null))
        assertEquals(listOf(1 to 1, 2 to 2, 3 to 3), sessions.map { it.startHour to it.endHour })
        assertEquals(listOf(di, asi, di), sessions.map { it.subjectId })
    }

    @Test
    fun testGridRoundTrip() {
        val grid = listOf(fh, fh, fh, null, di, di)
        assertEquals(grid, HourGrid.fromSessions(HourGrid.toSessions(3, grid)))
        assertEquals(HourGrid.emptyDay(), HourGrid.fromSessions(HourGrid.toSessions(3, HourGrid.emptyDay())))
    }

    @Test
    fun testLabel() {
        assertEquals("2ª h", HourGrid.label(2, 2))
        assertEquals("2ª–3ª h", HourGrid.label(2, 3))
        assertEquals("", HourGrid.label(0, 0))
    }
}
