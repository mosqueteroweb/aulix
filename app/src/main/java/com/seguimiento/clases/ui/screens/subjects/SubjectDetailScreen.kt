package com.seguimiento.clases.ui.screens.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.ui.components.SubjectBadge
import com.seguimiento.clases.ui.screens.subjects.components.AddEditLogDialog
import com.seguimiento.clases.ui.screens.subjects.components.SubjectHistoryTab
import com.seguimiento.clases.ui.screens.subjects.components.SubjectIdeasTab
import com.seguimiento.clases.ui.theme.parseColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    viewModel: SubjectDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<ClassLogEntity?>(null) }
    var showArchiveConfirmDialog by remember { mutableStateOf(false) }

    val subject = uiState.subject

    if (showAddEditDialog || editingLog != null) {
        AddEditLogDialog(
            initialLog = editingLog,
            onDismiss = {
                showAddEditDialog = false
                editingLog = null
            },
            onSave = { date, content ->
                viewModel.saveOrUpdateLog(editingLog, date, content)
            }
        )
    }

    if (showArchiveConfirmDialog && subject != null) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirmDialog = false },
            title = {
                Text(if (subject.isArchived) "¿Desarchivar módulo?" else "¿Archivar módulo?")
            },
            text = {
                Text(
                    if (subject.isArchived)
                        "El módulo volverá a estar disponible en el horario y listas activas."
                    else
                        "El módulo dejará de mostrarse en el horario activo, pero se conservarán intactas todas sus anotaciones e ideas."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.toggleArchived()
                    showArchiveConfirmDialog = false
                }) {
                    Text(if (subject.isArchived) "Desarchivar" else "Archivar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (subject != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SubjectBadge(
                                code = subject.code,
                                colorHex = subject.colorHex,
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = subject.code,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (subject.name.isNotBlank() && subject.name != subject.code) {
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                },
                actions = {
                    if (subject != null) {
                        IconButton(onClick = { showArchiveConfirmDialog = true }) {
                            Icon(
                                imageVector = if (subject.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                contentDescription = if (subject.isArchived) "Desarchivar" else "Archivar"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddEditDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Añadir al registro") }
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Pestañas: Registro e Ideas
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Registro (${uiState.logs.size})") }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Ideas (${uiState.pendingIdeas.size})") }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (uiState.selectedTab) {
                    0 -> SubjectHistoryTab(
                        logs = uiState.logs,
                        onEditLog = { log -> editingLog = log },
                        onDeleteLog = { log -> viewModel.deleteLog(log) }
                    )
                    1 -> SubjectIdeasTab(
                        pendingIdeas = uiState.pendingIdeas,
                        usedIdeas = uiState.usedIdeas,
                        showUsedIdeas = uiState.showUsedIdeas,
                        onToggleShowUsed = { viewModel.toggleShowUsedIdeas() },
                        onAddIdea = { text -> viewModel.addIdea(text) },
                        onUpdateIdea = { id, text -> viewModel.updateIdea(id, text) },
                        onSetIdeaUsed = { id, isUsed -> viewModel.setIdeaUsedState(id, isUsed) },
                        onDeleteIdea = { idea -> viewModel.deleteIdea(idea) }
                    )
                }
            }
        }
    }
}
