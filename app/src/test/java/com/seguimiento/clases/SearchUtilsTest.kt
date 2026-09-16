package com.seguimiento.clases

import com.seguimiento.clases.util.SearchUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUtilsTest {

    @Test
    fun testRemoveAccentsPreservesLength() {
        val original = "Explicación de métodos ágiles y teoría"
        val normalized = SearchUtils.removeAccents(original)
        assertEquals(original.length, normalized.length)
        assertEquals("Explicacion de metodos agiles y teoria", normalized)
    }

    @Test
    fun testFindMatchRangesCaseAndAccentInsensitive() {
        val text = "Hoy vimos la Sesión de Programación en Android"
        
        // Buscar "sesion" sin tilde y en minúsculas
        val ranges1 = SearchUtils.findMatchRanges(text, "sesion")
        assertEquals(1, ranges1.size)
        val match1 = text.substring(ranges1[0].first, ranges1[0].last + 1)
        assertEquals("Sesión", match1)

        // Buscar "PROGRAMACION"
        val ranges2 = SearchUtils.findMatchRanges(text, "PROGRAMACION")
        assertEquals(1, ranges2.size)
        val match2 = text.substring(ranges2[0].first, ranges2[0].last + 1)
        assertEquals("Programación", match2)

        // Múltiples ocurrencias
        val repeated = "Clase de SQL. Ejercicios con SQL y repaso SQL."
        val ranges3 = SearchUtils.findMatchRanges(repeated, "sql")
        assertEquals(3, ranges3.size)
    }

    @Test
    fun testFindMatchRangesEdgeCases() {
        assertTrue(SearchUtils.findMatchRanges("", "test").isEmpty())
        assertTrue(SearchUtils.findMatchRanges("hola", "").isEmpty())
        assertTrue(SearchUtils.findMatchRanges("hola", "   ").isEmpty())
        assertTrue(SearchUtils.findMatchRanges("hola mundo", "inexistente").isEmpty())
    }

    @Test
    fun testHighlightQuery() {
        val text = "Diseño de Interfaces"
        val annotated = SearchUtils.highlightQuery(text, "diseño")
        assertEquals("Diseño de Interfaces", annotated.text)
        assertEquals(1, annotated.spanStyles.size)
        assertEquals(0, annotated.spanStyles[0].start)
        assertEquals(6, annotated.spanStyles[0].end)
    }
}
