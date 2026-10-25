package com.seguimiento.clases.ui.screens.newcourse

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seguimiento.clases.data.local.entity.SubjectEntity
import com.seguimiento.clases.data.repository.ClassRepository
import com.seguimiento.clases.data.repository.NewCourseModule
import com.seguimiento.clases.ui.theme.SubjectColorPresets
import com.seguimiento.clases.util.HourGrid
import com.seguimiento.clases.util.SubjectCodes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NewCourseStep { BACKUP, MODULES, SCHEDULE, SUMMARY }

/** Módulo del curso nuevo mientras se configura; [key] es provisional hasta guardarlo. */
data class ModuleDraft(
    val key: Long,
    val code: String,
    val name: String,
    val colorHex: String
)

data class NewCourseUiState(
    val isLoading: Boolean = true,
    val step: NewCourseStep = NewCourseStep.MODULES,
    // Hay datos del curso actual: hay que guardar una copia antes de borrarlos
    val needsBackup: Boolean = false,
    val backupDone: Boolean = false,
    val isExporting: Boolean = false,
    val reportSaved: Boolean = false,
    val isExportingReport: Boolean = false,
    val modules: List<ModuleDraft> = emptyList(),
    val hoursByDay: Map<Int, List<Long?>> = HourGrid.SCHOOL_DAYS.associateWith { HourGrid.emptyDay() },
    val selectedDay: Int = 1,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val finished: Boolean = false
) {
    val steps: List<NewCourseStep>
        get() = if (needsBackup) NewCourseStep.entries else NewCourseStep.entries - NewCourseStep.BACKUP

    val canGoNext: Boolean
        get() = when (step) {
            NewCourseStep.BACKUP -> backupDone
            NewCourseStep.MODULES -> modules.isNotEmpty()
            NewCourseStep.SCHEDULE, NewCourseStep.SUMMARY -> true
        }

    /** Horas semanales asignadas a cada módulo, para el resumen. */
    fun weeklyHours(key: Long): Int = hoursByDay.values.sumOf { day -> day.count { it == key } }
}

class NewCourseViewModel(
    private val repository: ClassRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewCourseUiState())
    val uiState: StateFlow<NewCourseUiState> = _uiState.asStateFlow()

    private var nextKey = 1L

    init {
        viewModelScope.launch {
            val hasData = repository.hasAnyData()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    needsBackup = hasData,
                    step = if (hasData) NewCourseStep.BACKUP else NewCourseStep.MODULES
                )
            }
        }
    }

    // --- Paso 1: copia del curso que termina ---
    fun exportBackup(targetUri: Uri) {
        if (_uiState.value.isExporting) return
        _uiState.update { it.copy(isExporting = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.exportBackup(targetUri)
            _uiState.update {
                it.copy(
                    isExporting = false,
                    backupDone = it.backupDone || result.isSuccess,
                    errorMessage = result.exceptionOrNull()?.let { error -> "No se pudo guardar la copia: ${error.message}" }
                )
            }
        }
    }

    /** Opcional: guarda también el registro legible del curso que termina, en PDF. */
    fun exportReport(targetUri: Uri) {
        if (_uiState.value.isExportingReport) return
        _uiState.update { it.copy(isExportingReport = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.exportCourseReport(targetUri)
            _uiState.update {
                it.copy(
                    isExportingReport = false,
                    reportSaved = it.reportSaved || result.isSuccess,
                    errorMessage = result.exceptionOrNull()?.let { error -> "No se pudo guardar el registro: ${error.message}" }
                )
            }
        }
    }

    // --- Paso 2: módulos ---
    /** Añade un módulo; devuelve un mensaje de error si no es válido. */
    fun addModule(code: String, name: String, colorHex: String): String? {
        if (code.isBlank()) return "Escribe las siglas del módulo"
        if (isCodeTaken(code)) return SubjectCodes.DUPLICATE_MESSAGE
        _uiState.update {
            it.copy(modules = it.modules + ModuleDraft(nextKey++, SubjectCodes.normalize(code), name.trim(), colorHex))
        }
        return null
    }

    fun isCodeTaken(code: String): Boolean =
        SubjectCodes.isTaken(code, _uiState.value.modules.map { SubjectEntity(id = it.key, code = it.code) })

    /** Primer color que aún no usa ningún módulo, para proponerlo al añadir otro. */
    fun suggestedColor(): String {
        val used = _uiState.value.modules.map { it.colorHex }.toSet()
        return SubjectColorPresets.firstOrNull { it !in used } ?: SubjectColorPresets.first()
    }

    fun removeModule(key: Long) {
        _uiState.update { state ->
            state.copy(
                modules = state.modules.filterNot { it.key == key },
                // Las horas que tenía ese módulo quedan libres
                hoursByDay = state.hoursByDay.mapValues { (_, hours) -> hours.map { if (it == key) null else it } }
            )
        }
    }

    // --- Paso 3: horario ---
    fun selectDay(day: Int) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun setHour(hourIndex: Int, key: Long?) {
        _uiState.update { state ->
            val day = state.selectedDay
            val hours = state.hoursByDay.getValue(day).toMutableList()
            if (hourIndex !in hours.indices) return@update state
            hours[hourIndex] = key
            state.copy(hoursByDay = state.hoursByDay + (day to hours))
        }
    }

    // --- Navegación entre pasos ---
    fun next() {
        val state = _uiState.value
        if (!state.canGoNext) return
        val steps = state.steps
        val index = steps.indexOf(state.step)
        if (index < steps.lastIndex) _uiState.update { it.copy(step = steps[index + 1], errorMessage = null) }
    }

    /** Vuelve al paso anterior; devuelve false si ya está en el primero. */
    fun back(): Boolean {
        val state = _uiState.value
        val index = state.steps.indexOf(state.step)
        if (index <= 0) return false
        _uiState.update { it.copy(step = state.steps[index - 1], errorMessage = null) }
        return true
    }

    // --- Paso 4: crear el curso ---
    fun startCourse() {
        val state = _uiState.value
        if (state.isSaving || state.modules.isEmpty() || (state.needsBackup && !state.backupDone)) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val indexByKey = state.modules.mapIndexed { index, module -> module.key to index }.toMap()
            val result = runCatching {
                repository.startNewCourse(
                    modules = state.modules.map { NewCourseModule(it.code, it.name, it.colorHex) },
                    hoursByDay = state.hoursByDay.mapValues { (_, hours) -> hours.map { key -> key?.let(indexByKey::get) } }
                )
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    finished = result.isSuccess,
                    errorMessage = result.exceptionOrNull()?.let { error -> "No se pudo crear el curso: ${error.message}" }
                )
            }
        }
    }

    companion object {
        fun provideFactory(repository: ClassRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NewCourseViewModel(repository) as T
                }
            }
    }
}
