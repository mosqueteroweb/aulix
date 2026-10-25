package com.seguimiento.clases.ui.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.data.local.dao.SessionWithSubjectInfo
import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.ui.components.HourGridEditor
import com.seguimiento.clases.ui.components.HourOption
import com.seguimiento.clases.util.HourGrid

@Composable
fun ScheduleEditorSection(
    selectedDay: Int,
    sessions: List<SessionWithSubjectInfo>,
    activeSubjects: List<SubjectEntity>,
    onSelectDay: (Int) -> Unit,
    onSetHour: (hourIndex: Int, subjectId: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = remember(sessions) { HourGrid.fromSessions(sessions.map { it.session }) }
    val options = remember(activeSubjects) {
        activeSubjects.map { HourOption(key = it.id, code = it.code, name = it.name, colorHex = it.colorHex) }
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
                text = "Elige qué módulo das en cada una de las ${HourGrid.HOURS_PER_DAY} horas de cada día. " +
                    "Las horas seguidas del mismo módulo cuentan como una sola clase.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            HourGridEditor(
                selectedDay = selectedDay,
                onSelectDay = onSelectDay,
                hours = hours,
                options = options,
                onSetHour = onSetHour
            )
        }
    }
}
