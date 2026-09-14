package com.seguimiento.clases.ui.screens.alarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.seguimiento.clases.ui.screens.alarm.components.WheelTimePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Inicializar ruedas a la hora y minutos actuales cada vez que se entra en la pantalla
    LaunchedEffect(Unit) {
        viewModel.initializeToCurrentTime()
    }

    // Mostrar feedback si existe
    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    // Gestor de permisos para Android 13+ (POST_NOTIFICATIONS)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.activateAlarm()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Alarma",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sonido y vibración en segundo plano",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Visualización digital grande de la hora seleccionada
            val displayHour = String.format("%02d", uiState.selectedHour)
            val displayMinute = String.format("%02d", uiState.selectedMinute)

            Text(
                text = "$displayHour:$displayMinute",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (uiState.isAlarmActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )

            // Indicador de tiempo restante o previsión
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (uiState.isAlarmActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                },
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.isAlarmActive) Icons.Filled.NotificationsActive else Icons.Outlined.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (uiState.isAlarmActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.remainingTimeText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (uiState.isAlarmActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (uiState.isAlarmActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Selector de dos ruedas independientes
            WheelTimePicker(
                selectedHour = uiState.selectedHour,
                selectedMinute = uiState.selectedMinute,
                onTimeChanged = { hour, minute ->
                    viewModel.onTimeChanged(hour, minute)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón principal de Activar / Desactivar
            Button(
                onClick = {
                    if (!uiState.isAlarmActive) {
                        // Verificar permiso de notificaciones en Android 13+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@Button
                            }
                        }
                        viewModel.activateAlarm()
                    } else {
                        viewModel.cancelAlarm()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = if (uiState.isAlarmActive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Icon(
                    imageVector = if (uiState.isAlarmActive) Icons.Filled.AlarmOff else Icons.Filled.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (uiState.isAlarmActive) "Desactivar Alarma" else "Activar Alarma",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta explicativa de funcionamiento
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cómo funciona la alarma",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FeatureRow(
                        icon = Icons.Filled.NotificationsActive,
                        title = "Sonido y vibración continuos",
                        description = "Suena por el altavoz de alarma con volumen propio, incluso si el móvil está silenciado o la app cerrada."
                    )

                    FeatureRow(
                        icon = Icons.Outlined.Snooze,
                        title = "Posponer 5 min o cancelar",
                        description = "Al sonar, puedes posponerla 5 minutos o cancelarla directamente desde la notificación."
                    )

                    FeatureRow(
                        icon = Icons.Outlined.Timer,
                        title = "Auto-cancelación en 1 minuto",
                        description = "Si no respondes en 1 minuto, la alarma se silencia y cancela automáticamente para no agotar la batería."
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
