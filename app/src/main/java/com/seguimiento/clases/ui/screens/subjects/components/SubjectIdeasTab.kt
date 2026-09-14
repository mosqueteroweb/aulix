package com.seguimiento.clases.ui.screens.subjects.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.data.local.entity.IdeaEntity

@Composable
fun SubjectIdeasTab(
    pendingIdeas: List<IdeaEntity>,
    usedIdeas: List<IdeaEntity>,
    showUsedIdeas: Boolean,
    onToggleShowUsed: () -> Unit,
    onAddIdea: (String) -> Unit,
    onUpdateIdea: (id: Long, newText: String) -> Unit,
    onSetIdeaUsed: (id: Long, isUsed: Boolean) -> Unit,
    onDeleteIdea: (IdeaEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var newIdeaText by remember { mutableStateOf("") }
    var editingIdea by remember { mutableStateOf<IdeaEntity?>(null) }
    var ideaToDelete by remember { mutableStateOf<IdeaEntity?>(null) }

    // Diálogo para editar idea
    if (editingIdea != null) {
        var editText by remember(editingIdea) { mutableStateOf(editingIdea?.text ?: "") }
        AlertDialog(
            onDismissRequest = { editingIdea = null },
            title = { Text("Editar idea") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingIdea?.let { onUpdateIdea(it.id, editText) }
                        editingIdea = null
                    },
                    enabled = editText.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIdea = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para confirmar borrado de idea
    if (ideaToDelete != null) {
        AlertDialog(
            onDismissRequest = { ideaToDelete = null },
            title = { Text("¿Eliminar idea?") },
            text = { Text("Se eliminará esta idea de forma definitiva.") },
            confirmButton = {
                Button(
                    onClick = {
                        ideaToDelete?.let { onDeleteIdea(it) }
                        ideaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { ideaToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Campo superior para añadir ideas rápidamente
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Nueva idea para futuras sesiones",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newIdeaText,
                        onValueChange = { newIdeaText = it },
                        placeholder = { Text("Ej. Debate sobre APIs, práctica de Docker...") },
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (newIdeaText.isNotBlank()) {
                                onAddIdea(newIdeaText)
                                newIdeaText = ""
                            }
                        },
                        enabled = newIdeaText.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Añadir idea"
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sección: Ideas Pendientes
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pendientes (${pendingIdeas.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (pendingIdeas.isEmpty()) {
                item {
                    Text(
                        text = "No tienes ideas pendientes registradas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(pendingIdeas, key = { it.id }) { idea ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = idea.text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Marcar como utilizada
                            IconButton(
                                onClick = { onSetIdeaUsed(idea.id, true) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Marcar como utilizada",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Editar
                            IconButton(
                                onClick = { editingIdea = idea },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Editar idea",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Eliminar
                            IconButton(
                                onClick = { ideaToDelete = idea },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Eliminar idea",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Sección colapsable: Ideas Utilizadas
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Utilizadas (${usedIdeas.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(onClick = onToggleShowUsed) {
                        Text(if (showUsedIdeas) "Ocultar" else "Ver")
                        Icon(
                            imageVector = if (showUsedIdeas) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }
            }

            if (showUsedIdeas) {
                if (usedIdeas.isEmpty()) {
                    item {
                        Text(
                            text = "Aún no has marcado ninguna idea como utilizada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(usedIdeas, key = { it.id }) { idea ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = idea.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Botón reactivar idea
                                OutlinedButton(
                                    onClick = { onSetIdeaUsed(idea.id, false) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reactivar", style = MaterialTheme.typography.labelSmall)
                                }

                                IconButton(
                                    onClick = { ideaToDelete = idea },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
