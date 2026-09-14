package com.seguimiento.clases.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    object Today : Screen(
        route = "today",
        title = "Hoy",
        selectedIcon = Icons.Filled.CalendarToday,
        unselectedIcon = Icons.Outlined.CalendarToday
    )

    object Subjects : Screen(
        route = "subjects",
        title = "Asignaturas",
        selectedIcon = Icons.Filled.School,
        unselectedIcon = Icons.Outlined.School
    )

    object Settings : Screen(
        route = "settings",
        title = "Ajustes",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    object SubjectDetail : Screen(
        route = "subject_detail/{subjectId}",
        title = "Detalle Asignatura"
    ) {
        fun createRoute(subjectId: Long): String = "subject_detail/$subjectId"
    }

    companion object {
        val bottomNavItems = listOf(Today, Subjects, Settings)
    }
}
