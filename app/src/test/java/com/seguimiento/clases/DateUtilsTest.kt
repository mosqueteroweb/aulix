package com.seguimiento.clases

import com.seguimiento.clases.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test

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
}
