package com.seguimiento.clases.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.seguimiento.clases.data.repository.ClassRepository
import com.seguimiento.clases.ui.screens.settings.SettingsScreen
import com.seguimiento.clases.ui.screens.settings.SettingsViewModel
import com.seguimiento.clases.ui.screens.subjects.SubjectDetailScreen
import com.seguimiento.clases.ui.screens.subjects.SubjectDetailViewModel
import com.seguimiento.clases.ui.screens.subjects.SubjectsScreen
import com.seguimiento.clases.ui.screens.subjects.SubjectsViewModel
import com.seguimiento.clases.ui.screens.today.TodayScreen
import com.seguimiento.clases.ui.screens.today.TodayViewModel

@Composable
fun AppNavigation(
    repository: ClassRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = Screen.bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                val icon = if (selected) screen.selectedIcon else screen.unselectedIcon
                                icon?.let { Icon(it, contentDescription = screen.title) }
                            },
                            label = { Text(screen.title) }
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Pantalla Hoy
            composable(Screen.Today.route) {
                val todayViewModel: TodayViewModel = viewModel(
                    factory = TodayViewModel.provideFactory(repository)
                )
                TodayScreen(
                    viewModel = todayViewModel,
                    onNavigateToSubjectDetail = { subjectId ->
                        navController.navigate(Screen.SubjectDetail.createRoute(subjectId))
                    },
                    onNavigateToSubjectsList = {
                        navController.navigate(Screen.Subjects.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Pantalla Listado Asignaturas
            composable(Screen.Subjects.route) {
                val subjectsViewModel: SubjectsViewModel = viewModel(
                    factory = SubjectsViewModel.provideFactory(repository)
                )
                SubjectsScreen(
                    viewModel = subjectsViewModel,
                    onNavigateToDetail = { subjectId ->
                        navController.navigate(Screen.SubjectDetail.createRoute(subjectId))
                    }
                )
            }

            // Pantalla Detalle Asignatura (Registro + Ideas)
            composable(
                route = Screen.SubjectDetail.route,
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: return@composable
                val detailViewModel: SubjectDetailViewModel = viewModel(
                    factory = SubjectDetailViewModel.provideFactory(subjectId, repository)
                )
                SubjectDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Pantalla Ajustes
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(repository)
                )
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }
        }
    }
}
