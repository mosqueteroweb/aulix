package com.seguimiento.clases.ui.screens.settings.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun BackupSection(
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var showImportWarningDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher para exportar (guardar archivo JSON)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { onExport(it) }
    }

    // Launcher para seleccionar archivo JSON a importar
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImportUri = uri
            showImportWarningDialog = true
        }
    }

    // Diálogo de confirmación explícita al importar
    if (showImportWarningDialog && pendingImportUri != null) {
        val uriToImport = pendingImportUri!!
        AlertDialog(
            onDismissRequest = {
                showImportWarningDialog = false
                pendingImportUri = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Confirmar restauración") },
            text = {
                Text(
                    "Esta acción sustituirá todos los datos locales actuales por los contenidos en el archivo de copia. " +
                    "¿Deseas exportar una copia de seguridad antes de continuar?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportWarningDialog = false
                        pendingImportUri = null
                        onImport(uriToImport)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sobrescribir e importar")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            // Ofrecer exportar primero
                            val today = LocalDate.now().toString().replace("-", "")
                            exportLauncher.launch("seguimiento_clases_previo_$today.json")
                        }
                    ) {
                        Text("Exportar antes")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            showImportWarningDialog = false
                            pendingImportUri = null
                        }
                    ) {
                        Text("Cancelar")
                    }
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
                text = "Copia de seguridad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Exporta o restaura tus módulos, horario, registros de clase e ideas en un archivo JSON local.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Botón Exportar
                FilledTonalButton(
                    onClick = {
                        val today = LocalDate.now().toString().replace("-", "")
                        exportLauncher.launch("seguimiento_clases_backup_$today.json")
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Exportar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Botón Importar
                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/*"))
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Restaurar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
