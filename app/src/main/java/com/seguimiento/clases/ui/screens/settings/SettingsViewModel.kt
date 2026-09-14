package com.seguimiento.clases.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seguimiento.clases.data.local.dao.SessionWithSubjectInfo
import com.seguimiento.clases.data.local.entity.ScheduleSessionEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.data.repository.ClassRepository
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

    val uiState: StateFlow<SettingsUiState> = combine(
        _selectedScheduleDay,
        repository.getAllSubjects(),
        _feedbackMessage,
        _errorMessage
    ) { day, subjects, feedback, error ->
        Tuple4(day, subjects, feedback, error)
    }.flatMapLatest { (day, subjects, feedback, error) ->
        repository.getSessionsForDay(day).map { sessions ->
            SettingsUiState(
                selectedScheduleDay = day,
                scheduleSessions = sessions,
                allSubjects = subjects,
                activeSubjects = subjects.filter { !it.isArchived },
                feedbackMessage = feedback,
                errorMessage = error
            )
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
    fun addSessionToDay(subjectId: Long) {
        val currentDay = _selectedScheduleDay.value
        viewModelScope.launch {
            repository.addSession(currentDay, subjectId)
        }
    }

    fun removeSession(session: ScheduleSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun moveSessionUp(index: Int) {
        if (index <= 0) return
        val currentDay = _selectedScheduleDay.value
        viewModelScope.launch {
            repository.reorderSession(currentDay, index, index - 1)
        }
    }

    fun moveSessionDown(index: Int) {
        val sessions = uiState.value.scheduleSessions
        if (index >= sessions.size - 1) return
        val currentDay = _selectedScheduleDay.value
        viewModelScope.launch {
            repository.reorderSession(currentDay, index, index + 1)
        }
    }

    // --- ASIGNATURAS ---
    fun updateSubject(subject: SubjectEntity, newCode: String, newName: String) {
        if (newCode.isBlank()) return
        viewModelScope.launch {
            repository.updateSubject(
                subject.copy(
                    code = newCode.trim().uppercase(),
                    name = newName.trim()
                )
            )
            _feedbackMessage.value = "Asignatura actualizada"
        }
    }

    fun toggleArchiveSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.setSubjectArchived(subject.id, !subject.isArchived)
            _feedbackMessage.value = if (!subject.isArchived) "Asignatura archivada" else "Asignatura desarchivada"
        }
    }

    // --- COPIA DE SEGURIDAD ---
    fun exportBackup(targetUri: Uri) {
        viewModelScope.launch {
            val result = repository.exportBackup(targetUri)
            if (result.isSuccess) {
                _feedbackMessage.value = "Copia de seguridad guardada con éxito."
            } else {
                _errorMessage.value = "Error al exportar: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun importBackup(sourceUri: Uri) {
        viewModelScope.launch {
            val result = repository.importBackup(sourceUri)
            if (result.isSuccess) {
                _feedbackMessage.value = "Copia restaurada con éxito. Se importaron los registros correctamente."
            } else {
                _errorMessage.value = "Error al restaurar: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearMessages() {
        _feedbackMessage.value = null
        _errorMessage.value = null
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    companion object {
        fun provideFactory(repository: ClassRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository) as T
                }
            }
    }
}
