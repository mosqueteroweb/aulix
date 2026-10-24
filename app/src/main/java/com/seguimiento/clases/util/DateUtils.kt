package com.seguimiento.clases.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateUtils {
    private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy")

    /**
     * Indica si una fecha del selector (milisegundos UTC a medianoche, como los da el DatePicker)
     * se puede elegir para una anotación: solo hoy o días pasados, nunca futuros.
     */
    fun isSelectableLogDate(utcTimeMillis: Long, today: LocalDate = LocalDate.now()): Boolean {
        val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return !date.isAfter(today)
    }

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
