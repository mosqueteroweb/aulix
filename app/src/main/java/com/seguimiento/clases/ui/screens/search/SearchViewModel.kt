package com.seguimiento.clases.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seguimiento.clases.data.repository.ClassRepository
import com.seguimiento.clases.util.DateUtils
import com.seguimiento.clases.util.SearchUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: ClassRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(SearchFilter.ALL)
    private val _viewingItem = MutableStateFlow<SearchResultItem?>(null)

    // Debounce sobre el término de búsqueda para evitar cómputos excesivos
    private val _debouncedQuery = _query
        .debounce(250)
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private data class SearchDbData(
        val subjects: List<com.seguimiento.clases.data.local.entity.SubjectEntity>,
        val logs: List<com.seguimiento.clases.data.local.entity.ClassLogEntity>,
        val ideas: List<com.seguimiento.clases.data.local.entity.IdeaEntity>,
        val pinnedNote: String
    )

    private val _dbData = combine(
        repository.getAllSubjects(),
        repository.getAllLogsWithContent(),
        repository.getAllIdeasWithContent(),
        repository.getPinnedNote()
    ) { subjects, logs, ideas, pinnedNote ->
        SearchDbData(subjects, logs, ideas, pinnedNote)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _debouncedQuery,
        _filter,
        _viewingItem,
        _dbData
    ) { rawQuery, debouncedQuery, filter, viewingItem, dbData ->
        val trimmedQuery = debouncedQuery.trim()

        if (trimmedQuery.length < 2) {
            return@combine SearchUiState(
                query = rawQuery,
                filter = filter,
                results = emptyList(),
                totalCount = 0,
                isSearching = false,
                viewingItem = viewingItem
            )
        }

        val subjectMap = dbData.subjects.associateBy { it.id }
        val results = mutableListOf<SearchResultItem>()

        // 1. Buscar en anotaciones de clase (Lo visto en clase)
        if (filter == SearchFilter.ALL || filter == SearchFilter.LOGS) {
            for (log in dbData.logs) {
                val matches = SearchUtils.findMatchRanges(log.content, trimmedQuery)
                val dateMatches = SearchUtils.findMatchRanges(DateUtils.formatShortDate(log.date), trimmedQuery)
                if (matches.isNotEmpty() || dateMatches.isNotEmpty()) {
                    results.add(
                        SearchResultItem.ClassLogResult(
                            log = log,
                            subject = subjectMap[log.subjectId],
                            formattedDate = DateUtils.formatShortDate(log.date),
                            matchRanges = matches
                        )
                    )
                }
            }
        }

        // 2. Buscar en ideas
        if (filter == SearchFilter.ALL || filter == SearchFilter.IDEAS) {
            for (idea in dbData.ideas) {
                val matches = SearchUtils.findMatchRanges(idea.text, trimmedQuery)
                if (matches.isNotEmpty()) {
                    results.add(
                        SearchResultItem.IdeaResult(
                            idea = idea,
                            subject = subjectMap[idea.subjectId],
                            matchRanges = matches
                        )
                    )
                }
            }
        }

        // 3. Buscar en la nota fija
        if (filter == SearchFilter.ALL && dbData.pinnedNote.isNotBlank()) {
            val matches = SearchUtils.findMatchRanges(dbData.pinnedNote, trimmedQuery)
            if (matches.isNotEmpty()) {
                results.add(
                    SearchResultItem.PinnedNoteResult(
                        content = dbData.pinnedNote,
                        matchRanges = matches
                    )
                )
            }
        }

        SearchUiState(
            query = rawQuery,
            filter = filter,
            results = results,
            totalCount = results.size,
            isSearching = false,
            viewingItem = viewingItem
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onFilterChange(newFilter: SearchFilter) {
        _filter.value = newFilter
    }

    fun onOpenItem(item: SearchResultItem) {
        _viewingItem.value = item
    }

    fun onCloseItem() {
        _viewingItem.value = null
    }

    companion object {
        fun provideFactory(repository: ClassRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(repository) as T
                }
            }
    }
}
