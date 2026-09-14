package com.seguimiento.clases.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import com.seguimiento.clases.data.local.dao.SessionWithSubjectInfo
import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.ui.components.SubjectBadge
import com.seguimiento.clases.ui.theme.DayThemes
import com.seguimiento.clases.ui.theme.parseColor

@Composable
fun ScheduleEditorSection(
    selectedDay: Int,
    sessions: List<SessionWithSubjectInfo>,
    activeSubjects: List<SubjectEntity>,
    onSelectDay: (Int) -> Unit,
    onAddSession: (subjectId: Long) -> Unit,
    onRemoveSession: (ScheduleSessionEntity) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddSessionDialog(
            activeSubjects = activeSubjects,
            onDismiss = { showAddDialog = false },
            onSelect = { subjectId ->
                onAddSession(subjectId)
                showAddDialog = false
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Horario semanal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configura el orden de las sesiones lectivas de cada día.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de día con 1 letra por botón (L, M, X, J, V) y colores de alto contraste
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DayThemes.allDays.forEach { config ->
                    val isSelected = config.dayOfWeek == selectedDay
                    val isDark = isSystemInDarkTheme()
                    val accent = if (isDark) config.accentDark else config.accentLight

                    Surface(
                        onClick = { onSelectDay(config.dayOfWeek) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            config.solidColor
                        } else {
                            if (isDark) Color(0xFF20232B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) config.solidColor else accent.copy(alpha = 0.4f)
                        ),
                        tonalElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = config.letter,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else accent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subcabecera descriptiva del día seleccionado con indicador de color y contraste verificado
            val currentDayConfig = DayThemes.forDay(selectedDay)
            val isDarkTheme = isSystemInDarkTheme()
            val currentDayAccent = if (isDarkTheme) currentDayConfig.accentDark else currentDayConfig.accentLight

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(currentDayConfig.solidColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentDayConfig.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = currentDayAccent
                    )
                }

                Text(
                    text = "${sessions.size} ${if (sessions.size == 1) "sesión" else "sesiones"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay sesiones asignadas a este día",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                sessions.forEachIndexed { index, item ->
                    val subject = item.subject
                    val session = item.session

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}ª",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = currentDayAccent,
                                modifier = Modifier.width(28.dp)
                            )

                            SubjectBadge(
                                code = subject.code,
                                colorHex = subject.colorHex,
                                size = 32.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = subject.code + if (subject.name.isNotBlank()) " - ${subject.name}" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            // Botón subir orden
                            IconButton(
                                onClick = { onMoveUp(index) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Subir",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Botón bajar orden
                            IconButton(
                                onClick = { onMoveDown(index) },
                                enabled = index < sessions.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Bajar",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Botón eliminar sesión
                            IconButton(
                                onClick = { onRemoveSession(session) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Quitar sesión",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = activeSubjects.isNotEmpty(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Añadir sesión a este día")
            }
        }
    }
}

@Composable
fun AddSessionDialog(
    activeSubjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir módulo") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeSubjects.forEach { subject ->
                    OutlinedCard(
                        onClick = { onSelect(subject.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubjectBadge(
                                code = subject.code,
                                colorHex = subject.colorHex,
                                size = 30.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = subject.code + if (subject.name.isNotBlank()) " - ${subject.name}" else "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
