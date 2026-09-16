package com.seguimiento.clases.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object SearchUtils {

    /**
     * Reemplaza las vocales acentuadas por sus equivalentes sin acento,
     * manteniendo exactamente la misma longitud y posición 1:1 de los caracteres.
     */
    fun removeAccents(str: String): String {
        return str.map { char ->
            when (char) {
                'á', 'à', 'ä', 'â' -> 'a'
                'é', 'è', 'ë', 'ê' -> 'e'
                'í', 'ì', 'ï', 'î' -> 'i'
                'ó', 'ò', 'ö', 'ô' -> 'o'
                'ú', 'ù', 'ü', 'û' -> 'u'
                'Á', 'À', 'Ä', 'Â' -> 'A'
                'É', 'È', 'Ë', 'Ê' -> 'E'
                'Í', 'Ì', 'Ï', 'Î' -> 'I'
                'Ó', 'Ò', 'Ö', 'Ô' -> 'O'
                'Ú', 'Ù', 'Ü', 'Û' -> 'U'
                else -> char
            }
        }.joinToString("")
    }

    /**
     * Encuentra todos los rangos [start, end) de coincidencias en el texto original,
     * ignorando tildes y diferencias de mayúsculas/minúsculas.
     */
    fun findMatchRanges(text: String, query: String): List<IntRange> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank() || text.isBlank()) return emptyList()

        val normalizedText = removeAccents(text).lowercase()
        val normalizedQuery = removeAccents(trimmedQuery).lowercase()

        val ranges = mutableListOf<IntRange>()
        var startIndex = 0
        while (startIndex < normalizedText.length) {
            val foundIndex = normalizedText.indexOf(normalizedQuery, startIndex)
            if (foundIndex == -1) break
            val endIndex = foundIndex + normalizedQuery.length
            ranges.add(foundIndex until endIndex)
            startIndex = endIndex
        }
        return ranges
    }

    /**
     * Construye un AnnotatedString con los rangos coincidentes resaltados.
     */
    fun highlightQuery(
        text: String,
        query: String,
        highlightBackground: Color = Color(0xFFFFD54F), // Amarillo / ámbar suave
        highlightColor: Color = Color.Black
    ): AnnotatedString {
        val ranges = findMatchRanges(text, query)
        if (ranges.isEmpty()) return AnnotatedString(text)

        return buildAnnotatedString {
            append(text)
            for (range in ranges) {
                if (range.first in 0 until text.length && range.last < text.length) {
                    addStyle(
                        style = SpanStyle(
                            background = highlightBackground,
                            color = highlightColor,
                            fontWeight = FontWeight.Bold
                        ),
                        start = range.first,
                        end = range.last + 1
                    )
                }
            }
        }
    }
}
