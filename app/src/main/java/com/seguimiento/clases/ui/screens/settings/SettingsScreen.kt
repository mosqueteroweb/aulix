package com.seguimiento.clases.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.ui.screens.settings.components.BackupSection
import com.seguimiento.clases.ui.screens.settings.components.ScheduleEditorSection
import com.seguimiento.clases.ui.screens.settings.components.SubjectManageSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(message = err, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección 1: Horario Semanal
            ScheduleEditorSection(
                selectedDay = uiState.selectedScheduleDay,
                sessions = uiState.scheduleSessions,
                activeSubjects = uiState.activeSubjects,
                onSelectDay = { viewModel.selectScheduleDay(it) },
                onAddSession = { viewModel.addSessionToDay(it) },
                onRemoveSession = { viewModel.removeSession(it) },
                onMoveUp = { viewModel.moveSessionUp(it) },
                onMoveDown = { viewModel.moveSessionDown(it) }
            )

            // Sección 2: Gestión de Asignaturas
            SubjectManageSection(
                subjects = uiState.allSubjects,
                onUpdateSubject = { subject, code, name ->
                    viewModel.updateSubject(subject, code, name)
                },
                onToggleArchive = { viewModel.toggleArchiveSubject(it) }
            )

            // Sección 3: Copia de Seguridad JSON
            BackupSection(
                onExport = { uri -> viewModel.exportBackup(uri) },
                onImport = { uri -> viewModel.importBackup(uri) }
            )

            // Información sobre la app
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Aulix - Seguimiento de Clases",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Versión 0.1.9 • 100% Sin conexión • Almacenamiento local SQLite/Room.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
