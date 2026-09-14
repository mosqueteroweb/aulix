package com.seguimiento.clases.ui.screens.today.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.ui.components.SubjectBadge
import com.seguimiento.clases.ui.screens.subjects.components.AddEditLogDialog
import com.seguimiento.clases.ui.screens.today.SessionCardUiState
import com.seguimiento.clases.ui.theme.DayThemes
import com.seguimiento.clases.ui.theme.parseColor

@Composable
fun ClassSessionCard(
    cardState: SessionCardUiState,
    onContentChange: (String) -> Unit,
    onClearCurrentLog: () -> Unit,
    onToggleExpandPrior: () -> Unit,
    onEditPriorLog: (log: ClassLogEntity, newDate: String, newContent: String) -> Unit,
    onDeletePriorLog: (log: ClassLogEntity) -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenIdeasBottomSheet: () -> Unit,
    modifier: Modifier = Modifier,
    dayOfWeek: Int = 1
) {
    val subject = cardState.session.subject
    val sessionId = cardState.session.session.id
    val subjectId = subject.id
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Control preciso de cursor y selección mediante TextFieldValue
    var textFieldValue by remember(sessionId, subjectId, dayOfWeek) {
        mutableStateOf(
            TextFieldValue(
                text = cardState.currentLogContent,
                selection = TextRange(cardState.currentLogContent.length)
            )
        )
    }

    // Sincronizar solo cuando el texto cambie desde el exterior (ej. carga Room o borrado),
    // sin reescribir la posición del cursor si el texto coincide con lo que el usuario está tecleando.
    LaunchedEffect(cardState.currentLogContent) {
        if (cardState.currentLogContent != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = cardState.currentLogContent,
                selection = TextRange(cardState.currentLogContent.length)
            )
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("¿Borrar anotación de clase?") },
            text = { Text("Se borrará el texto anotado para la sesión de hoy.") },
            confirmButton = {
                Button(
                    onClick = {
                        textFieldValue = TextFieldValue("", TextRange.Zero)
                        onClearCurrentLog()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val dayConfig = DayThemes.forDay(dayOfWeek)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(width = 1.5.dp, color = dayConfig.currentBorder()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabecera: Siglas, Nombre y Botón Historial
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge de color con las siglas
                SubjectBadge(
                    code = subject.code,
                    colorHex = subject.colorHex,
                    size = 46.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (subject.name.isNotBlank() && subject.name != subject.code) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Acceso directo a historial completo de la asignatura
                TextButton(
                    onClick = onNavigateToHistory,
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Historial",
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sección: Última(s) anotación(es) anterior(es)
            PriorNotesSection(
                priorLogs = cardState.recentPriorLogs,
                isExpanded = cardState.isExpandedPriorLogs,
                onToggleExpand = onToggleExpandPrior,
                onEditLog = onEditPriorLog,
                onDeleteLog = onDeletePriorLog
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo de texto: «Lo visto en clase»
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lo visto en clase",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (cardState.currentLogContent.isNotBlank() || textFieldValue.text.isNotBlank()) {
                    TextButton(
                        onClick = { showClearConfirmDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Borrar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    if (newValue.text != cardState.currentLogContent) {
                        onContentChange(newValue.text)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Anotar temas tratados, ejercicios o estado de la sesión...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                minLines = 3,
                maxLines = 8,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botón/Chip para acceder a las ideas pendientes de esta asignatura
            val ideasCount = cardState.pendingIdeas.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onOpenIdeasBottomSheet,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (ideasCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (ideasCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (ideasCount > 0) "$ideasCount ideas pendientes" else "Ideas pendientes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorNotesSection(
    priorLogs: List<ClassLogEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditLog: (ClassLogEntity, String, String) -> Unit,
    onDeleteLog: (ClassLogEntity) -> Unit
) {
    if (priorLogs.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                text = "Sin anotaciones anteriores registradas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp)
            )
        }
        return
    }

    val latest = priorLogs.first()
    var editingLog by remember { mutableStateOf<ClassLogEntity?>(null) }
    var logToDelete by remember { mutableStateOf<ClassLogEntity?>(null) }

    if (editingLog != null) {
        AddEditLogDialog(
            initialLog = editingLog,
            onDismiss = { editingLog = null },
            onSave = { date, content ->
                editingLog?.let { onEditLog(it, date, content) }
                editingLog = null
            }
        )
    }

    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("¿Eliminar anotación?") },
            text = { Text("Se eliminará permanentemente la anotación de la fecha ${logToDelete?.date}.") },
            confirmButton = {
                Button(
                    onClick = {
                        logToDelete?.let { onDeleteLog(it) }
                        logToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Última anotación visible
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Última clase (${latest.date}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (priorLogs.size > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(onClick = onToggleExpand)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "Ocultar" else "Ver 3 últimas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    IconButton(
                        onClick = { editingLog = latest },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar anotación",
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = { logToDelete = latest },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Eliminar anotación",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = latest.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) 10 else 2,
                overflow = TextOverflow.Ellipsis
            )

            // Desplegable de las 3 últimas
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    priorLogs.drop(1).forEach { log ->
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sesión ${log.date}:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { editingLog = log },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Editar anotación",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { logToDelete = log },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Eliminar anotación",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = log.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
