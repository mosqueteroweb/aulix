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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.ui.components.SubjectBadge
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
        var codeValue by remember(currentSubject) {
            mutableStateOf(TextFieldValue(currentSubject.code, TextRange(currentSubject.code.length)))
        }
        var nameValue by remember(currentSubject) {
            mutableStateOf(TextFieldValue(currentSubject.name, TextRange(currentSubject.name.length)))
        }

        AlertDialog(
            onDismissRequest = { subjectToEdit = null },
            title = { Text("Editar Módulo") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = codeValue,
                        onValueChange = {
                            if (it.text.length <= 8) {
                                codeValue = it
                            }
                        },
                        label = { Text("Siglas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameValue,
                        onValueChange = { nameValue = it },
                        label = { Text("Nombre descriptivo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSubject(currentSubject, codeValue.text.trim(), nameValue.text.trim())
                        subjectToEdit = null
                    },
                    enabled = codeValue.text.isNotBlank()
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
                text = "Gestión de módulos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Modifica nombres o archiva módulos sin perder sus anotaciones pasadas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            subjects.forEach { subject ->
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
                        SubjectBadge(
                            code = subject.code,
                            colorHex = subject.colorHex,
                            size = 36.dp,
                            isArchived = subject.isArchived
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subject.code + if (subject.isArchived) " (Archivado)" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (subject.name.isNotBlank()) {
                                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { subjectToEdit = subject },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onToggleArchive(subject) },
                            modifier = Modifier.size(36.dp)
                        ) {
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
