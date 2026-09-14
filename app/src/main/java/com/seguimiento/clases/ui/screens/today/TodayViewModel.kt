package com.seguimiento.clases.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seguimiento.clases.data.local.dao.SessionWithSubjectInfo
import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.data.local.entity.IdeaEntity
import com.seguimiento.clases.data.repository.ClassRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SessionCardUiState(
    val session: SessionWithSubjectInfo,
    val currentLogContent: String = "",
    val recentPriorLogs: List<ClassLogEntity> = emptyList(),
    val pendingIdeas: List<IdeaEntity> = emptyList(),
    val isExpandedPriorLogs: Boolean = false
)

data class TodayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val dayOfWeek: Int = 1,
    val isToday: Boolean = true,
    val dayName: String = "",
    val dateSubtitle: String = "",
    val formattedDate: String = "",
    val sessions: List<SessionCardUiState> = emptyList(),
    val isLoading: Boolean = true,
    val activeBottomSheetSubjectId: Long? = null // Para el BottomSheet de ideas
)

class TodayViewModel(
    private val repository: ClassRepository
) : ViewModel() {

    private fun adjustToWeekday(date: LocalDate): LocalDate {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> date.plusDays(2)
            DayOfWeek.SUNDAY -> date.plusDays(1)
            else -> date
        }
    }

    private val _selectedDate = MutableStateFlow(adjustToWeekday(LocalDate.now()))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _activeBottomSheetSubjectId = MutableStateFlow<Long?>(null)
    val activeBottomSheetSubjectId: StateFlow<Long?> = _activeBottomSheetSubjectId.asStateFlow()

    // Map para controlar expansión de las 3 últimas notas por subjectId
    private val _expandedSubjectIds = MutableStateFlow<Set<Long>>(emptySet())

    // Mapa temporal de textos editados para debouncing
    private val _draftLogs = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val saveDebounceJobs = mutableMapOf<Long, Job>()

    private val spanishLocale = Locale("es", "ES")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = combine(
        _selectedDate,
        _expandedSubjectIds,
        _draftLogs,
        _activeBottomSheetSubjectId
    ) { date, expandedIds, drafts, bottomSheetId ->
        Tuple4(date, expandedIds, drafts, bottomSheetId)
    }.flatMapLatest { (date, expandedIds, drafts, bottomSheetId) ->
        val dayOfWeek = date.dayOfWeek.value // 1 = Lunes, 5 = Viernes
        val dateString = date.toString() // "YYYY-MM-DD"
        val currentSchoolToday = adjustToWeekday(LocalDate.now())
        val isToday = date.isEqual(currentSchoolToday)

        val dayName = date.format(DateTimeFormatter.ofPattern("EEEE", spanishLocale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(spanishLocale) else it.toString() }
        val dateSubtitle = date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", spanishLocale))
        val formattedDate = "$dayName, $dateSubtitle"

        repository.getSessionsForDay(dayOfWeek).flatMapLatest { sessionsList ->
            if (sessionsList.isEmpty()) {
                flowOf(
                    TodayUiState(
                        selectedDate = date,
                        dayOfWeek = dayOfWeek,
                        isToday = isToday,
                        dayName = dayName,
                        dateSubtitle = dateSubtitle,
                        formattedDate = formattedDate,
                        sessions = emptyList(),
                        isLoading = false,
                        activeBottomSheetSubjectId = bottomSheetId
                    )
                )
            } else {
                // Para cada sesión combinamos su log actual, logs previos e ideas pendientes
                val sessionFlows = sessionsList.map { sessionInfo ->
                    val subjectId = sessionInfo.subject.id
                    combine(
                        repository.getLogForSubjectAndDate(subjectId, dateString),
                        repository.getRecentLogsPriorToDate(subjectId, dateString, limit = 3),
                        repository.getPendingIdeasForSubject(subjectId)
                    ) { currentLog, priorLogs, pendingIdeas ->
                        val text = drafts[subjectId] ?: currentLog?.content ?: ""
                        SessionCardUiState(
                            session = sessionInfo,
                            currentLogContent = text,
                            recentPriorLogs = priorLogs,
                            pendingIdeas = pendingIdeas,
                            isExpandedPriorLogs = expandedIds.contains(subjectId)
                        )
                    }
                }

                combine(sessionFlows) { cardsArray ->
                    TodayUiState(
                        selectedDate = date,
                        dayOfWeek = dayOfWeek,
                        isToday = isToday,
                        dayName = dayName,
                        dateSubtitle = dateSubtitle,
                        formattedDate = formattedDate,
                        sessions = cardsArray.toList(),
                        isLoading = false,
                        activeBottomSheetSubjectId = bottomSheetId
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    fun onPreviousDay() {
        _draftLogs.value = emptyMap()
        _selectedDate.update { current ->
            var prev = current.minusDays(1)
            while (prev.dayOfWeek == DayOfWeek.SATURDAY || prev.dayOfWeek == DayOfWeek.SUNDAY) {
                prev = prev.minusDays(1)
            }
            prev
        }
    }

    fun onNextDay() {
        _draftLogs.value = emptyMap()
        _selectedDate.update { current ->
            var next = current.plusDays(1)
            while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
                next = next.plusDays(1)
            }
            next
        }
    }

    fun onGoToToday() {
        _draftLogs.value = emptyMap()
        _selectedDate.value = adjustToWeekday(LocalDate.now())
    }

    fun toggleExpandPriorLogs(subjectId: Long) {
        _expandedSubjectIds.update { current ->
            if (current.contains(subjectId)) current - subjectId else current + subjectId
        }
    }

    fun onLogContentChanged(subjectId: Long, newText: String) {
        _draftLogs.update { current ->
            current + (subjectId to newText)
        }

        // Debounce de guardado automático (500 ms)
        saveDebounceJobs[subjectId]?.cancel()
        saveDebounceJobs[subjectId] = viewModelScope.launch {
            delay(500)
            saveLogToDb(subjectId, newText)
        }
    }

    fun forceSaveCurrentLog(subjectId: Long) {
        val currentText = _draftLogs.value[subjectId] ?: return
        saveDebounceJobs[subjectId]?.cancel()
        viewModelScope.launch {
            saveLogToDb(subjectId, currentText)
        }
    }

    private suspend fun saveLogToDb(subjectId: Long, content: String) {
        val dateString = _selectedDate.value.toString()
        repository.saveClassLog(subjectId, dateString, content)
    }

    fun clearCurrentLog(subjectId: Long) {
        saveDebounceJobs[subjectId]?.cancel()
        _draftLogs.update { current -> current - subjectId }
        val dateString = _selectedDate.value.toString()
        viewModelScope.launch {
            repository.deleteLogForSubjectAndDate(subjectId, dateString)
        }
    }

    fun saveOrUpdatePriorLog(initialLog: ClassLogEntity?, newDate: String, newContent: String, subjectId: Long) {
        if (newContent.isBlank()) return
        viewModelScope.launch {
            if (initialLog != null && initialLog.date != newDate) {
                repository.deleteClassLog(initialLog)
            }
            repository.saveClassLog(subjectId, newDate, newContent.trim())
        }
    }

    fun deletePriorLog(log: ClassLogEntity) {
        viewModelScope.launch {
            repository.deleteClassLog(log)
        }
    }

    fun openBottomSheetForSubject(subjectId: Long) {
        _activeBottomSheetSubjectId.value = subjectId
    }

    fun closeBottomSheet() {
        _activeBottomSheetSubjectId.value = null
    }

    fun markIdeaAsUsed(ideaId: Long) {
        viewModelScope.launch {
            repository.setIdeaUsedState(ideaId, true)
        }
    }

    fun updateIdeaText(ideaId: Long, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            repository.updateIdeaText(ideaId, newText.trim())
        }
    }

    fun deleteIdea(ideaId: Long) {
        viewModelScope.launch {
            repository.deleteIdeaById(ideaId)
        }
    }

    fun addQuickIdea(subjectId: Long, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addIdea(subjectId, text)
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    companion object {
        fun provideFactory(repository: ClassRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TodayViewModel(repository) as T
                }
            }
    }
}
