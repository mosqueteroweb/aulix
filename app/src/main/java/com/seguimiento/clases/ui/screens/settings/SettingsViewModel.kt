package com.seguimiento.clases.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seguimiento.clases.data.backup.ImportSummary
import com.seguimiento.clases.data.local.dao.SessionWithSubjectInfo
import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.data.repository.ClassRepository
import com.seguimiento.clases.util.HourGrid
import com.seguimiento.clases.util.SubjectCodes
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedScheduleDay: Int = 1, // 1 = Lunes, ..., 5 = Viernes
    val scheduleSessions: List<SessionWithSubjectInfo> = emptyList(),
    val allSubjects: List<SubjectEntity> = emptyList(),
    val activeSubjects: List<SubjectEntity> = emptyList(),
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val repository: ClassRepository
) : ViewModel() {

    private val _selectedScheduleDay = MutableStateFlow(1)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Copia de seguridad en curso (para mostrar progreso y bloquear los botones)
    private val _backupOperation = MutableStateFlow<BackupOperation?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SettingsUiState> = combine(
        _selectedScheduleDay,
        repository.getAllSubjects(),
        _feedbackMessage,
        _errorMessage,
        _backupOperation
    ) { day, subjects, feedback, error, operation ->
        SettingsUiState(
            selectedScheduleDay = day,
            allSubjects = subjects,
            activeSubjects = subjects.filter { !it.isArchived },
            isExporting = operation == BackupOperation.EXPORTING,
            isImporting = operation == BackupOperation.IMPORTING,
            feedbackMessage = feedback,
            errorMessage = error
        )
    }.flatMapLatest { baseState ->
        repository.getSessionsForDay(baseState.selectedScheduleDay).map { sessions ->
            baseState.copy(scheduleSessions = sessions)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun selectScheduleDay(day: Int) {
        _selectedScheduleDay.value = day
    }

    // --- HORARIO ---
    /** Asigna un módulo (o deja libre, con null) una hora del día seleccionado. */
    fun setHour(hourIndex: Int, subjectId: Long?) {
        val day = _selectedScheduleDay.value
        val hours = HourGrid.fromSessions(uiState.value.scheduleSessions.map { it.session }).toMutableList()
        if (hourIndex !in hours.indices || hours[hourIndex] == subjectId) return
        hours[hourIndex] = subjectId
        viewModelScope.launch {
            repository.replaceDaySchedule(day, hours)
        }
    }

    // --- ASIGNATURAS ---
    fun updateSubject(subject: SubjectEntity, newCode: String, newName: String) {
        if (newCode.isBlank()) return
        if (SubjectCodes.isTaken(newCode, uiState.value.allSubjects, editingSubjectId = subject.id)) {
            _errorMessage.value = SubjectCodes.DUPLICATE_MESSAGE
            return
        }
        viewModelScope.launch {
            repository.updateSubject(
                subject.copy(
                    code = SubjectCodes.normalize(newCode),
                    name = newName.trim()
                )
            )
            _feedbackMessage.value = "Módulo actualizado"
        }
    }

    fun toggleArchiveSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.setSubjectArchived(subject.id, !subject.isArchived)
            _feedbackMessage.value = if (!subject.isArchived) "Módulo archivado" else "Módulo desarchivado"
        }
    }

    // --- COPIA DE SEGURIDAD ---
    fun exportBackup(targetUri: Uri) {
        if (_backupOperation.value != null) return
        _backupOperation.value = BackupOperation.EXPORTING
        viewModelScope.launch {
            val result = repository.exportBackup(targetUri)
            _backupOperation.value = null
            if (result.isSuccess) {
                _feedbackMessage.value = "Copia de seguridad guardada con éxito."
            } else {
                _errorMessage.value = "Error al exportar: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun importBackup(sourceUri: Uri) {
        if (_backupOperation.value != null) return
        _backupOperation.value = BackupOperation.IMPORTING
        viewModelScope.launch {
            val result = repository.importBackup(sourceUri)
            _backupOperation.value = null
            result.onSuccess { summary ->
                _feedbackMessage.value = importSuccessMessage(summary)
            }.onFailure { error ->
                _errorMessage.value = "Error al restaurar: ${error.message}"
            }
        }
    }

    fun clearMessages() {
        _feedbackMessage.value = null
        _errorMessage.value = null
    }

    private enum class BackupOperation { EXPORTING, IMPORTING }

    companion object {
        /** Mensaje tras restaurar, con lo importado: «Copia restaurada: 4 módulos, 35 anotaciones y 1 idea.» */
        fun importSuccessMessage(summary: ImportSummary): String {
            fun count(n: Int, singular: String, plural: String) = "$n ${if (n == 1) singular else plural}"
            return "Copia restaurada: ${count(summary.subjects, "módulo", "módulos")}, " +
                "${count(summary.classLogs, "anotación", "anotaciones")} y ${count(summary.ideas, "idea", "ideas")}."
        }

        fun provideFactory(repository: ClassRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository) as T
                }
            }
    }
}
