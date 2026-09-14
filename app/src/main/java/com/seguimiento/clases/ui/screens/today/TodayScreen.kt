package com.seguimiento.clases.ui.screens.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.ui.screens.today.components.ClassSessionCard
import com.seguimiento.clases.ui.screens.today.components.DateNavigationHeader
import com.seguimiento.clases.ui.screens.today.components.PendingIdeasBottomSheet

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onNavigateToSubjectDetail: (Long) -> Unit,
    onNavigateToSubjectsList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            DateNavigationHeader(
                formattedDate = uiState.formattedDate,
                isToday = uiState.isToday,
                onPreviousDay = { viewModel.onPreviousDay() },
                onNextDay = { viewModel.onNextDay() },
                onGoToToday = { viewModel.onGoToToday() }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.sessions.isEmpty()) {
                // Estado vacío cuando no hay clases programadas
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay clases programadas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No tienes asignaturas en el horario para este día.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onNavigateToSubjectsList) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver asignaturas")
                    }
                }
            } else {
                // Lista de tarjetas de clase
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = uiState.sessions,
                        key = { it.session.session.id }
                    ) { cardState ->
                        val subjectId = cardState.session.subject.id
                        ClassSessionCard(
                            cardState = cardState,
                            onContentChange = { newText ->
                                viewModel.onLogContentChanged(subjectId, newText)
                            },
                            onToggleExpandPrior = {
                                viewModel.toggleExpandPriorLogs(subjectId)
                            },
                            onNavigateToHistory = {
                                onNavigateToSubjectDetail(subjectId)
                            },
                            onOpenIdeasBottomSheet = {
                                viewModel.openBottomSheetForSubject(subjectId)
                            }
                        )
                    }
                }
            }

            // BottomSheet de ideas pendientes si hay una asignatura seleccionada
            val activeSubjectId = uiState.activeBottomSheetSubjectId
            if (activeSubjectId != null) {
                val activeSession = uiState.sessions.firstOrNull { it.session.subject.id == activeSubjectId }
                if (activeSession != null) {
                    PendingIdeasBottomSheet(
                        subject = activeSession.session.subject,
                        pendingIdeas = activeSession.pendingIdeas,
                        onDismiss = { viewModel.closeBottomSheet() },
                        onMarkIdeaUsed = { ideaId -> viewModel.markIdeaAsUsed(ideaId) },
                        onAddQuickIdea = { text -> viewModel.addQuickIdea(activeSubjectId, text) },
                        onNavigateToSubjectDetail = { onNavigateToSubjectDetail(activeSubjectId) }
                    )
                }
            }
        }
    }
}
