package com.seguimiento.clases.ui.screens.search

import com.seguimiento.clases.data.local.entity.ClassLogEntity
import com.seguimiento.clases.data.local.entity.IdeaEntity
import com.seguimiento.clases.data.local.entity.SubjectEntity

enum class SearchFilter(val label: String) {
    ALL("Todos"),
    LOGS("Clases"),
    IDEAS("Ideas")
}

sealed class SearchResultItem {
    abstract val key: String
    abstract val displayText: String
    abstract val matchRanges: List<IntRange>

    data class ClassLogResult(
        val log: ClassLogEntity,
        val subject: SubjectEntity?,
        val formattedDate: String,
        override val matchRanges: List<IntRange>
    ) : SearchResultItem() {
        override val key: String = "log_${log.id}"
        override val displayText: String = log.content
    }

    data class IdeaResult(
        val idea: IdeaEntity,
        val subject: SubjectEntity?,
        override val matchRanges: List<IntRange>
    ) : SearchResultItem() {
        override val key: String = "idea_${idea.id}"
        override val displayText: String = idea.text
    }

    data class PinnedNoteResult(
        val content: String,
        override val matchRanges: List<IntRange>
    ) : SearchResultItem() {
        override val key: String = "pinned_note"
        override val displayText: String = content
    }
}

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val results: List<SearchResultItem> = emptyList(),
    val totalCount: Int = 0,
    val isSearching: Boolean = false,
    val viewingItem: SearchResultItem? = null
)
