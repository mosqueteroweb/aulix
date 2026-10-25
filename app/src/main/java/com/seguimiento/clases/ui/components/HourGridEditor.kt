package com.seguimiento.clases.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.ui.theme.DayThemes
import com.seguimiento.clases.util.HourGrid

/** Módulo que se puede asignar a una hora: [key] es el id del módulo (o uno provisional en el asistente). */
data class HourOption(
    val key: Long,
    val code: String,
    val name: String,
    val colorHex: String
)

/**
 * Editor del horario de un día por horas lectivas: selector de día (L a V) y una fila por hora
 * (1ª a 6ª) con un desplegable para elegir el módulo o dejarla libre.
 */
@Composable
fun HourGridEditor(
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    hours: List<Long?>,
    options: List<HourOption>,
    onSetHour: (hourIndex: Int, key: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DaySelector(selectedDay = selectedDay, onSelectDay = onSelectDay)

        Spacer(modifier = Modifier.height(10.dp))

        val dayConfig = DayThemes.forDay(selectedDay)
        val dayAccent = if (isSystemInDarkTheme()) dayConfig.accentDark else dayConfig.accentLight
        val busyHours = hours.count { it != null }
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
                        .background(dayConfig.solidColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dayConfig.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = dayAccent
                )
            }
            Text(
                text = "$busyHours de ${HourGrid.HOURS_PER_DAY} horas con clase",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        hours.forEachIndexed { hourIndex, key ->
            HourRow(
                hourNumber = hourIndex + 1,
                selected = options.firstOrNull { it.key == key },
                options = options,
                onSelect = { newKey -> onSetHour(hourIndex, newKey) }
            )
        }
    }
}

@Composable
fun DaySelector(
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DayThemes.allDays.forEach { config ->
            val isSelected = config.dayOfWeek == selectedDay
            val accent = if (isDark) config.accentDark else config.accentLight
            Surface(
                onClick = { onSelectDay(config.dayOfWeek) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp),
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
                    modifier = Modifier.padding(vertical = 10.dp),
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
}

@Composable
private fun HourRow(
    hourNumber: Int,
    selected: HourOption?,
    options: List<HourOption>,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${hourNumber}ª",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(40.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(10.dp),
                color = if (selected != null) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selected != null) {
                        SubjectBadge(code = selected.code, colorHex = selected.colorHex, size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (selected.name.isNotBlank()) "${selected.code} · ${selected.name}" else selected.code,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = "Libre",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 6.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Elegir módulo para la ${hourNumber}ª hora"
                    )
                }
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(if (option.name.isNotBlank()) "${option.code} · ${option.name}" else option.code) },
                        leadingIcon = { SubjectBadge(code = option.code, colorHex = option.colorHex, size = 28.dp) },
                        onClick = {
                            onSelect(option.key)
                            expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Libre") },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    }
                )
            }
        }
    }
}
