package com.seguimiento.clases.ui.screens.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.data.local.entity.IdeaEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.data.repository.ClassRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SubjectDetailUiState(
    val subject: SubjectEntity? = null,
    val logs: List<ClassLogEntity> = emptyList(),
    val pendingIdeas: List<IdeaEntity> = emptyList(),
    val usedIdeas: List<IdeaEntity> = emptyList(),
    val selectedTab: Int = 0, // 0 = Registro, 1 = Ideas
    val showUsedIdeas: Boolean = false,
    val isLoading: Boolean = true
)

class SubjectDetailViewModel(
    private val subjectId: Long,
    private val repository: ClassRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    private val _showUsedIdeas = MutableStateFlow(false)

    val uiState: StateFlow<SubjectDetailUiState> = combine(
        combine(
            repository.getSubjectById(subjectId),
            repository.getAllLogsForSubject(subjectId)
        ) { subject, logs -> Pair(subject, logs) },
        combine(
            repository.getPendingIdeasForSubject(subjectId),
            repository.getUsedIdeasForSubject(subjectId)
        ) { pending, used -> Pair(pending, used) },
        _selectedTab,
        _showUsedIdeas
    ) { (subject, logs), (pendingIdeas, usedIdeas), tab, showUsed ->
        SubjectDetailUiState(
            subject = subject,
            logs = logs,
            pendingIdeas = pendingIdeas,
            usedIdeas = usedIdeas,
            selectedTab = tab,
            showUsedIdeas = showUsed,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubjectDetailUiState()
    )

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun toggleShowUsedIdeas() {
        _showUsedIdeas.update { !it }
    }

    // --- ACCIONES REGISTRO ---
    fun saveOrUpdateLog(initialLog: ClassLogEntity?, date: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            if (initialLog != null && initialLog.date != date) {
                repository.deleteClassLog(initialLog)
            }
            repository.saveClassLog(subjectId, date, content.trim())
        }
    }

    fun saveLog(date: String, content: String) = saveOrUpdateLog(null, date, content)

    fun deleteLog(log: ClassLogEntity) {
        viewModelScope.launch {
            repository.deleteClassLog(log)
        }
    }

    fun restoreLog(log: ClassLogEntity) {
        viewModelScope.launch {
            repository.insertClassLog(log)
        }
    }

    // --- ACCIONES IDEAS ---
    fun addIdea(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addIdea(subjectId, text.trim())
        }
    }

    fun updateIdea(id: Long, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.updateIdeaText(id, text)
        }
    }

    fun setIdeaUsedState(id: Long, isUsed: Boolean) {
        viewModelScope.launch {
            repository.setIdeaUsedState(id, isUsed)
        }
    }

    fun deleteIdea(idea: IdeaEntity) {
        viewModelScope.launch {
            repository.deleteIdea(idea)
        }
    }

    fun restoreIdea(idea: IdeaEntity) {
        viewModelScope.launch {
            repository.insertIdea(idea)
        }
    }

    // --- ACCIÓN ARCHIVAR ---
    fun toggleArchived() {
        val subject = uiState.value.subject ?: return
        viewModelScope.launch {
            repository.setSubjectArchived(subject.id, !subject.isArchived)
        }
    }

    companion object {
        fun provideFactory(subjectId: Long, repository: ClassRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SubjectDetailViewModel(subjectId, repository) as T
                }
            }
    }
}
