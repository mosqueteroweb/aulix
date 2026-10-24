package com.seguimiento.clases

import com.seguimiento.clases.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class DateUtilsTest {

    @Test
    fun testFormatShortDate() {
        assertEquals("27/09/26", DateUtils.formatShortDate("2026-09-27"))
        assertEquals("15/09/26", DateUtils.formatShortDate("2026-09-15"))
        assertEquals("05/01/26", DateUtils.formatShortDate("2026-01-05"))
        assertEquals("29/02/24", DateUtils.formatShortDate("2024-02-29"))
    }

    @Test
    fun testFormatShortDateEdgeCases() {
        assertEquals("", DateUtils.formatShortDate(null))
        assertEquals("", DateUtils.formatShortDate(""))
        assertEquals("", DateUtils.formatShortDate("   "))
        // Si no es una fecha válida ISO, devuelve la cadena original sin fallar
        assertEquals("no-es-fecha", DateUtils.formatShortDate("no-es-fecha"))
        assertEquals("27/09/26", DateUtils.formatShortDate("27/09/26"))
    }

    @Test
    fun testOnlyTodayAndPastDatesAreSelectableForLogs() {
        val today = LocalDate.of(2026, 10, 25)
        fun utc(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        assertTrue(DateUtils.isSelectableLogDate(utc(today), today))
        assertTrue(DateUtils.isSelectableLogDate(utc(today.minusDays(400)), today))
        assertFalse(DateUtils.isSelectableLogDate(utc(today.plusDays(1)), today))
    }
}
