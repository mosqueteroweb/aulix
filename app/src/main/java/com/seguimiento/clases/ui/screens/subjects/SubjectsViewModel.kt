package com.seguimiento.clases.ui.screens.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.data.repository.ClassRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubjectsUiState(
    val activeSubjects: List<SubjectEntity> = emptyList(),
    val archivedSubjects: List<SubjectEntity> = emptyList(),
    val isLoading: Boolean = true
)

class SubjectsViewModel(
    private val repository: ClassRepository
) : ViewModel() {

    val uiState: StateFlow<SubjectsUiState> = repository.getAllSubjects()
        .map { list ->
            SubjectsUiState(
                activeSubjects = list.filter { !it.isArchived },
                archivedSubjects = list.filter { it.isArchived },
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubjectsUiState()
        )

    fun createSubject(code: String, name: String, colorHex: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            repository.saveSubject(
                SubjectEntity(
                    code = code.trim().uppercase(),
                    name = name.trim(),
                    colorHex = colorHex
                )
            )
        }
    }

    companion object {
        fun provideFactory(repository: ClassRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SubjectsViewModel(repository) as T
                }
            }
    }
}
