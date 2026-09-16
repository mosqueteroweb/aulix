package com.seguimiento.clases.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {
    private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy")

    /**
     * Convierte una fecha ISO "yyyy-MM-dd" (ej: "2026-09-27") a formato corto "dd/MM/yy" (ej: "27/09/26").
     * Si la cadena no coincide con el formato ISO o es nula/en blanco, devuelve la cadena original sin fallar.
     */
    fun formatShortDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val parsed = LocalDate.parse(dateStr)
            parsed.format(SHORT_DATE_FORMATTER)
        } catch (_: Exception) {
            dateStr
        }
    }
}
