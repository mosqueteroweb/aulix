package com.seguimiento.clases.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.ui.theme.parseColor

@Composable
fun SubjectManageSection(
    subjects: List<SubjectEntity>,
    onUpdateSubject: (subject: SubjectEntity, newCode: String, newName: String) -> Unit,
    onToggleArchive: (SubjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }

    if (subjectToEdit != null) {
        val currentSubject = subjectToEdit!!
        var code by remember(currentSubject) { mutableStateOf(currentSubject.code) }
        var name by remember(currentSubject) { mutableStateOf(currentSubject.name) }

        AlertDialog(
            onDismissRequest = { subjectToEdit = null },
            title = { Text("Editar Asignatura") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.take(8) },
                        label = { Text("Siglas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre descriptivo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSubject(currentSubject, code, name)
                        subjectToEdit = null
                    },
                    enabled = code.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToEdit = null }) {
                    Text("Cancelar")
                }
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
                text = "Gestión de asignaturas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Modifica nombres o archiva asignaturas sin perder sus anotaciones pasadas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            subjects.forEach { subject ->
                val color = parseColor(subject.colorHex)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (subject.isArchived)
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (subject.isArchived) Color.Gray else color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = subject.code,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subject.code + if (subject.isArchived) " (Archivada)" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (subject.name.isNotBlank()) {
                                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { subjectToEdit = subject }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = { onToggleArchive(subject) }) {
                            Icon(
                                imageVector = if (subject.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                contentDescription = if (subject.isArchived) "Desarchivar" else "Archivar",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
