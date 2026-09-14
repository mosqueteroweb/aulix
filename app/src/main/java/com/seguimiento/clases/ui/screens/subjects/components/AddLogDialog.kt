package com.seguimiento.clases.ui.screens.subjects.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLogDialog(
    initialLog: ClassLogEntity? = null,
    onDismiss: () -> Unit,
    onSave: (date: String, content: String) -> Unit
) {
    var dateString by remember {
        mutableStateOf(initialLog?.date ?: LocalDate.now().toString())
    }
    var contentValue by remember(initialLog) {
        val initialText = initialLog?.content ?: ""
        mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            val localDate = LocalDate.parse(dateString)
            localDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        dateString = localDate.toString()
                    }
                    showDatePicker = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialLog == null) "Añadir anotación" else "Editar anotación")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Selector de fecha
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Fecha de la sesión",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateString,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "Elegir fecha",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contenido de la clase
                OutlinedTextField(
                    value = contentValue,
                    onValueChange = { contentValue = it },
                    label = { Text("Lo visto en clase") },
                    placeholder = { Text("Detalla los temas tratados...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (contentValue.text.isNotBlank()) {
                        onSave(dateString, contentValue.text.trim())
                        onDismiss()
                    }
                },
                enabled = contentValue.text.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
