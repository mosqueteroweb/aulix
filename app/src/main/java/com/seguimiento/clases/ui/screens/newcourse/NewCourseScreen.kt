package com.seguimiento.clases.ui.screens.newcourse

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.ui.components.HourGridEditor
import com.seguimiento.clases.ui.components.HourOption
import com.seguimiento.clases.ui.components.SubjectBadge
import com.seguimiento.clases.ui.theme.DayThemes
import com.seguimiento.clases.ui.theme.SubjectColorPresets
import com.seguimiento.clases.ui.theme.parseColor
import com.seguimiento.clases.util.HourGrid
import com.seguimiento.clases.util.SubjectCodes
import java.time.LocalDate

/**
 * Asistente para empezar un curso nuevo: copia del curso que termina, módulos del curso nuevo,
 * horario por horas de lunes a viernes y confirmación. Solo al confirmar se borra el curso anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCourseScreen(
    viewModel: NewCourseViewModel,
    onClose: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.finished) {
        if (uiState.finished) onFinished()
    }

    BackHandler {
        if (!viewModel.back()) onClose()
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Empezar el curso nuevo?") },
            text = {
                Text(
                    if (uiState.needsBackup) {
                        "Se borrarán los módulos, el horario, las anotaciones, las ideas y la nota fija del curso actual. " +
                            "La copia que has guardado permite recuperarlos."
                    } else {
                        "Se crearán los módulos y el horario que has configurado."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.startCourse()
                    },
                    colors = if (uiState.needsBackup) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text("Empezar curso")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }

    val stepNumber = uiState.steps.indexOf(uiState.step) + 1
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nuevo curso", fontWeight = FontWeight.Bold)
                        if (!uiState.isLoading) {
                            Text(
                                text = "Paso $stepNumber de ${uiState.steps.size} · ${stepTitle(uiState.step)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (!viewModel.back()) onClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = {
            if (!uiState.isLoading) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (!viewModel.back()) onClose() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (stepNumber == 1) "Cancelar" else "Atrás")
                        }
                        if (uiState.step == NewCourseStep.SUMMARY) {
                            Button(
                                onClick = { showConfirmDialog = true },
                                enabled = !uiState.isSaving,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Empezar curso")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.next() },
                                enabled = uiState.canGoNext,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Siguiente")
                            }
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (uiState.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            when (uiState.step) {
                NewCourseStep.BACKUP -> BackupStep(uiState, onExport = viewModel::exportBackup)
                NewCourseStep.MODULES -> ModulesStep(uiState, viewModel)
                NewCourseStep.SCHEDULE -> ScheduleStep(uiState, viewModel)
                NewCourseStep.SUMMARY -> SummaryStep(uiState)
            }
        }
    }
}

private fun stepTitle(step: NewCourseStep): String = when (step) {
    NewCourseStep.BACKUP -> "Copia del curso actual"
    NewCourseStep.MODULES -> "Módulos"
    NewCourseStep.SCHEDULE -> "Horario"
    NewCourseStep.SUMMARY -> "Resumen"
}

@Composable
private fun BackupStep(uiState: NewCourseUiState, onExport: (android.net.Uri) -> Unit) {
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(onExport) }

    Text(
        text = "Antes de empezar, guarda una copia de todo el curso actual: módulos, horario, anotaciones, " +
            "ideas y nota fija. Con ella podrás consultarlo o recuperarlo más adelante. " +
            "Guárdala fuera del móvil (por ejemplo, en Drive).",
        style = MaterialTheme.typography.bodyMedium
    )
    if (uiState.backupDone) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
            Spacer(Modifier.width(8.dp))
            Text("Copia guardada", fontWeight = FontWeight.SemiBold)
        }
    }
    if (uiState.isExporting) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    FilledTonalButton(
        onClick = {
            val today = LocalDate.now().toString().replace("-", "")
            exportLauncher.launch("aulix_copia_fin_de_curso_$today.json")
        },
        enabled = !uiState.isExporting,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Filled.FileUpload, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (uiState.backupDone) "Guardar otra copia" else "Guardar copia del curso actual")
    }
}

@Composable
private fun ModulesStep(uiState: NewCourseUiState, viewModel: NewCourseViewModel) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var color by remember(uiState.modules.size) { mutableStateOf(viewModel.suggestedColor()) }
    var formError by remember { mutableStateOf<String?>(null) }
    val isDuplicate = code.isNotBlank() && viewModel.isCodeTaken(code)

    Text(
        text = "¿Qué módulos vas a impartir este curso? Añádelos uno a uno (por ejemplo DI, ASI, ISO, FH).",
        style = MaterialTheme.typography.bodyMedium
    )

    uiState.modules.forEach { module ->
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SubjectBadge(code = module.code, colorHex = module.colorHex, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(module.code, fontWeight = FontWeight.SemiBold)
                    if (module.name.isNotBlank()) {
                        Text(module.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = { viewModel.removeModule(module.key) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Quitar ${module.code}", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Añadir módulo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it.take(8)
                    formError = null
                },
                label = { Text("Siglas (ej. DI)") },
                isError = isDuplicate || formError != null,
                supportingText = when {
                    isDuplicate -> ({ Text(SubjectCodes.DUPLICATE_MESSAGE) })
                    formError != null -> ({ Text(formError!!) })
                    else -> null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SubjectColorPresets.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(parseColor(hex))
                            .clickable { color = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (hex == color) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val error = viewModel.addModule(code, name, color)
                    if (error == null) {
                        code = ""
                        name = ""
                    } else {
                        formError = error
                    }
                },
                enabled = code.isNotBlank() && !isDuplicate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Añadir módulo")
            }
        }
    }
}

@Composable
private fun ScheduleStep(uiState: NewCourseUiState, viewModel: NewCourseViewModel) {
    Text(
        text = "Para cada día, elige qué módulo das en cada hora. Las horas seguidas del mismo módulo " +
            "serán una sola clase en «Hoy». Puedes cambiarlo después en Ajustes.",
        style = MaterialTheme.typography.bodyMedium
    )
    HourGridEditor(
        selectedDay = uiState.selectedDay,
        onSelectDay = viewModel::selectDay,
        hours = uiState.hoursByDay.getValue(uiState.selectedDay),
        options = uiState.modules.map { HourOption(it.key, it.code, it.name, it.colorHex) },
        onSetHour = viewModel::setHour
    )
}

@Composable
private fun SummaryStep(uiState: NewCourseUiState) {
    val modulesByKey = uiState.modules.associateBy { it.key }

    Text("Módulos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    uiState.modules.forEach { module ->
        val weekly = uiState.weeklyHours(module.key)
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectBadge(code = module.code, colorHex = module.colorHex, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${module.code}: $weekly ${if (weekly == 1) "hora" else "horas"} a la semana",
                color = if (weekly == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    Text("Horario", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    HourGrid.SCHOOL_DAYS.forEach { day ->
        val sessions = HourGrid.toSessions(day, uiState.hoursByDay.getValue(day))
        val description = if (sessions.isEmpty()) {
            "Sin clases"
        } else {
            sessions.joinToString(" · ") { session ->
                "${modulesByKey[session.subjectId]?.code} ${HourGrid.label(session.startHour, session.endHour)}"
            }
        }
        Row {
            Text(
                text = DayThemes.forDay(day).name,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(96.dp)
            )
            Text(description, modifier = Modifier.weight(1f))
        }
    }

    if (uiState.needsBackup) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(
                text = "Al empezar se borrará todo el curso actual. Tu copia ya está guardada.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
