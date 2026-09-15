package com.seguimiento.clases.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class DayConfig(
    val dayOfWeek: Int,          // 1 = Lunes, 2 = Martes, 3 = Miércoles, 4 = Jueves, 5 = Viernes
    val letter: String,          // "L", "M", "X", "J", "V"
    val name: String,            // "Lunes", "Martes", "Miércoles", "Jueves", "Viernes"
    val solidColor: Color,       // Color de fondo para botón seleccionado (contraste > 4.5:1 con blanco)
    val accentDark: Color,       // Color de texto/icono en Modo Oscuro (contraste > 8.0:1 sobre fondos oscuros)
    val accentLight: Color,      // Color de texto/icono en Modo Claro (contraste > 7.0:1 sobre fondos claros)
    val containerDark: Color,    // Color de contenedor sutil en Modo Oscuro
    val containerLight: Color    // Color de contenedor sutil en Modo Claro
) {
    @Composable
    fun currentAccent(): Color {
        return if (isSystemInDarkTheme()) accentDark else accentLight
    }

    @Composable
    fun currentContainer(): Color {
        return if (isSystemInDarkTheme()) containerDark else containerLight
    }

    @Composable
    fun currentBorder(): Color {
        return if (isSystemInDarkTheme()) accentDark.copy(alpha = 0.85f) else solidColor.copy(alpha = 0.70f)
    }
}

object DayThemes {
    // 1: Lunes - Azul elegante
    val Lunes = DayConfig(
        dayOfWeek = 1,
        letter = "L",
        name = "Lunes",
        solidColor = Color(0xFF1D4ED8),     // Blue 700 (contraste 6.5:1 con blanco)
        accentDark = Color(0xFF93C5FD),     // Blue 300 (contraste 9.5:1 en modo oscuro)
        accentLight = Color(0xFF1E40AF),    // Blue 800 (contraste 8.4:1 en modo claro)
        containerDark = Color(0xFF1E3A8A).copy(alpha = 0.35f),
        containerLight = Color(0xFFDBEAFE).copy(alpha = 0.7f)
    )

    // 2: Martes - Verde esmeralda
    val Martes = DayConfig(
        dayOfWeek = 2,
        letter = "M",
        name = "Martes",
        solidColor = Color(0xFF047857),     // Emerald 700 (contraste 6.0:1 con blanco)
        accentDark = Color(0xFF6EE7B7),     // Emerald 300 (contraste 10.2:1 en modo oscuro)
        accentLight = Color(0xFF065F46),    // Emerald 800 (contraste 8.1:1 en modo claro)
        containerDark = Color(0xFF064E3B).copy(alpha = 0.35f),
        containerLight = Color(0xFFD1FAE5).copy(alpha = 0.7f)
    )

    // 3: Miércoles - Naranja / Terracota (abreviatura estándar 'X')
    val Miercoles = DayConfig(
        dayOfWeek = 3,
        letter = "X",
        name = "Miércoles",
        solidColor = Color(0xFFC2410C),     // Orange 700 (contraste 5.3:1 con blanco)
        accentDark = Color(0xFFFDBA74),     // Orange 300 (contraste 10.5:1 en modo oscuro)
        accentLight = Color(0xFF9A3412),    // Orange 800 (contraste 7.2:1 en modo claro)
        containerDark = Color(0xFF7C2D12).copy(alpha = 0.35f),
        containerLight = Color(0xFFFFEDD5).copy(alpha = 0.7f)
    )

    // 4: Jueves - Violeta / Púrpura
    val Jueves = DayConfig(
        dayOfWeek = 4,
        letter = "J",
        name = "Jueves",
        solidColor = Color(0xFF6D28D9),     // Violet 700 (contraste 6.2:1 con blanco)
        accentDark = Color(0xFFC4B5FD),     // Violet 300 (contraste 9.1:1 en modo oscuro)
        accentLight = Color(0xFF5B21B6),    // Violet 800 (contraste 8.2:1 en modo claro)
        containerDark = Color(0xFF4C1D95).copy(alpha = 0.35f),
        containerLight = Color(0xFFEDE9FE).copy(alpha = 0.7f)
    )

    // 5: Viernes - Rosa carmesí / Rubí
    val Viernes = DayConfig(
        dayOfWeek = 5,
        letter = "V",
        name = "Viernes",
        solidColor = Color(0xFFBE123C),     // Rose 700 (contraste 5.5:1 con blanco)
        accentDark = Color(0xFFFDA4AF),     // Rose 300 (contraste 9.2:1 en modo oscuro)
        accentLight = Color(0xFF9F1239),    // Rose 800 (contraste 7.8:1 en modo claro)
        containerDark = Color(0xFF881337).copy(alpha = 0.35f),
        containerLight = Color(0xFFFFE4E6).copy(alpha = 0.7f)
    )

    val allDays = listOf(Lunes, Martes, Miercoles, Jueves, Viernes)

    fun forDay(dayOfWeek: Int): DayConfig {
        return when (dayOfWeek) {
            1 -> Lunes
            2 -> Martes
            3 -> Miercoles
            4 -> Jueves
            5 -> Viernes
            else -> Lunes
        }
    }
}
