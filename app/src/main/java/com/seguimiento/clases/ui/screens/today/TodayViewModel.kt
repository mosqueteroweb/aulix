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
    val isExpandedPriorLogs: Boolean = false,
    val isCompleted: Boolean = false
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

private data class SessionDbData(
    val session: SessionWithSubjectInfo,
    val savedLog: ClassLogEntity?,
    val recentPriorLogs: List<ClassLogEntity>,
    val pendingIdeas: List<IdeaEntity>
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

    // Flujo de datos persistentes (sesiones, logs guardados, logs previos e ideas)
    // Solo reacciona a cambios de fecha seleccionada o cambios en la base de datos Room,
    // NUNCA se reinicia ni re-consulta Room al escribir en las cajas de texto.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _dbDataFlow: Flow<List<SessionDbData>> = _selectedDate.flatMapLatest { date ->
        val dayOfWeek = date.dayOfWeek.value
        val dateString = date.toString()
        repository.getSessionsForDay(dayOfWeek).flatMapLatest { sessionsList ->
            if (sessionsList.isEmpty()) {
                flowOf(emptyList())
            } else {
                val sessionFlows = sessionsList.map { sessionInfo ->
                    val subjectId = sessionInfo.subject.id
                    combine(
                        repository.getLogForSubjectAndDate(subjectId, dateString),
                        repository.getRecentLogsPriorToDate(subjectId, dateString, limit = 3),
                        repository.getPendingIdeasForSubject(subjectId)
                    ) { currentLog, priorLogs, pendingIdeas ->
                        SessionDbData(
                            session = sessionInfo,
                            savedLog = currentLog,
                            recentPriorLogs = priorLogs,
                            pendingIdeas = pendingIdeas
                        )
                    }
                }
                combine(sessionFlows) { it.toList() }
            }
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(
        _selectedDate,
        _dbDataFlow,
        _expandedSubjectIds,
        _draftLogs,
        _activeBottomSheetSubjectId
    ) { date, dbSessions, expandedIds, drafts, bottomSheetId ->
        val dayOfWeek = date.dayOfWeek.value // 1 = Lunes, 5 = Viernes
        val currentSchoolToday = adjustToWeekday(LocalDate.now())
        val isToday = date.isEqual(currentSchoolToday)

        val dayName = date.format(DateTimeFormatter.ofPattern("EEEE", spanishLocale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(spanishLocale) else it.toString() }
        val dateSubtitle = date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", spanishLocale))
        val formattedDate = "$dayName, $dateSubtitle"

        val cardStates = dbSessions.map { dbItem ->
            val subjectId = dbItem.session.subject.id
            // Si hay un borrador activo escrito por el usuario en memoria, se usa de inmediato;
            // si no, se muestra el contenido guardado en Room.
            val text = drafts[subjectId] ?: dbItem.savedLog?.content ?: ""
            val isCompleted = dbItem.savedLog?.isCompleted ?: false
            SessionCardUiState(
                session = dbItem.session,
                currentLogContent = text,
                recentPriorLogs = dbItem.recentPriorLogs,
                pendingIdeas = dbItem.pendingIdeas,
                isExpandedPriorLogs = expandedIds.contains(subjectId),
                isCompleted = isCompleted
            )
        }.sortedWith(
            compareBy<SessionCardUiState> { it.isCompleted } // false primero, true al final
                .thenBy { it.session.session.orderIndex }
        )

        TodayUiState(
            selectedDate = date,
            dayOfWeek = dayOfWeek,
            isToday = isToday,
            dayName = dayName,
            dateSubtitle = dateSubtitle,
            formattedDate = formattedDate,
            sessions = cardStates,
            isLoading = false,
            activeBottomSheetSubjectId = bottomSheetId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    private fun flushPendingDrafts() {
        val currentDateString = _selectedDate.value.toString()
        val draftsToSave = _draftLogs.value
        // Cancelar todos los jobs de debounce pendientes
        saveDebounceJobs.values.forEach { it.cancel() }
        saveDebounceJobs.clear()
        _draftLogs.value = emptyMap()

        if (draftsToSave.isNotEmpty()) {
            viewModelScope.launch {
                draftsToSave.forEach { (subjectId, content) ->
                    repository.saveClassLog(subjectId, currentDateString, content)
                }
            }
        }
    }

    fun onPreviousDay() {
        flushPendingDrafts()
        _selectedDate.update { current ->
            var prev = current.minusDays(1)
            while (prev.dayOfWeek == DayOfWeek.SATURDAY || prev.dayOfWeek == DayOfWeek.SUNDAY) {
                prev = prev.minusDays(1)
            }
            prev
        }
    }

    fun onNextDay() {
        flushPendingDrafts()
        _selectedDate.update { current ->
            var next = current.plusDays(1)
            while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
                next = next.plusDays(1)
            }
            next
        }
    }

    fun onGoToToday() {
        flushPendingDrafts()
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

        // Debounce de guardado automático (500 ms) asegurando la fecha de origen
        val targetDateString = _selectedDate.value.toString()
        saveDebounceJobs[subjectId]?.cancel()
        saveDebounceJobs[subjectId] = viewModelScope.launch {
            delay(500)
            saveLogToDb(subjectId, targetDateString, newText)
        }
    }

    fun forceSaveCurrentLog(subjectId: Long) {
        val currentText = _draftLogs.value[subjectId] ?: return
        val targetDateString = _selectedDate.value.toString()
        saveDebounceJobs[subjectId]?.cancel()
        viewModelScope.launch {
            saveLogToDb(subjectId, targetDateString, currentText)
        }
    }

    private suspend fun saveLogToDb(subjectId: Long, dateString: String, content: String) {
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

    fun toggleSessionCompleted(subjectId: Long) {
        val dateString = _selectedDate.value.toString()
        val currentDraft = _draftLogs.value[subjectId]
        viewModelScope.launch {
            repository.toggleLogCompleted(subjectId, dateString, currentDraft)
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
