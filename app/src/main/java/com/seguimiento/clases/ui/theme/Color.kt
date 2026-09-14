package com.seguimiento.clases.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta clara
val PrimaryLight = Color(0xFF1E3A8A)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFDBEAFE)
val OnPrimaryContainerLight = Color(0xFF1E3A8A)

val SecondaryLight = Color(0xFF334155)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFF1F5F9)
val OnSecondaryContainerLight = Color(0xFF0F172A)

val BackgroundLight = Color(0xFFF8FAFC)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val OnSurfaceVariantLight = Color(0xFF475569)

// Paleta oscura
val PrimaryDark = Color(0xFF93C5FD)
val OnPrimaryDark = Color(0xFF1E3A8A)
val PrimaryContainerDark = Color(0xFF1E40AF)
val OnPrimaryContainerDark = Color(0xFFDBEAFE)

val SecondaryDark = Color(0xFFCBD5E1)
val OnSecondaryDark = Color(0xFF1E293B)
val SecondaryContainerDark = Color(0xFF334155)
val OnSecondaryContainerDark = Color(0xFFF1F5F9)

val BackgroundDark = Color(0xFF0F172A)
val OnBackgroundDark = Color(0xFFF8FAFC)
val SurfaceDark = Color(0xFF1E293B)
val OnSurfaceDark = Color(0xFFF8FAFC)
val SurfaceVariantDark = Color(0xFF334155)
val OnSurfaceVariantDark = Color(0xFF94A3B8)

// Utilidad para parsear colores hexadecimales de las asignaturas
fun parseColor(hex: String, defaultColor: Color = Color(0xFF3B82F6)): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val fullHex = if (cleanHex.length == 6) "FF$cleanHex" else cleanHex
        Color(fullHex.toLong(16))
    } catch (e: Exception) {
        defaultColor
    }
}
