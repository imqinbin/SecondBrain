package com.qb.secondbrain.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val selectedTag: String? = null,
    val allTags: List<String> = emptyList(),
    val results: List<Memo> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val memoRepository: MemoRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)

    private val allMemosFlow = memoRepository.getAllMemos()

    val uiState: StateFlow<SearchUiState> = combine(
        allMemosFlow,
        _query,
        _selectedTag
    ) { memos, query, selectedTag ->
        val activeMemos = memos.filter { !it.isDeleted }
        val allTags = activeMemos.flatMap { it.tags }.distinct().sorted()

        val filtered = when {
            selectedTag != null -> activeMemos.filter { selectedTag in it.tags }
            query.isBlank() -> activeMemos
            else -> {
                val keywords = query.split("\\s+".toRegex()).filter { it.isNotBlank() }
                activeMemos.filter { memo ->
                    keywords.any { keyword ->
                        memo.content.contains(keyword, ignoreCase = true) ||
                                memo.tags.any { tag -> tag.contains(keyword, ignoreCase = true) }
                    }
                }
            }
        }

        SearchUiState(
            query = query,
            selectedTag = selectedTag,
            allTags = allTags,
            results = filtered
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun updateQuery(query: String) {
        _query.value = query
        if (query.isNotBlank()) {
            _selectedTag.value = null
        }
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
        if (tag != null) {
            _query.value = ""
        }
    }

    fun initializeQuery(query: String?) {
        if (!query.isNullOrBlank()) {
            _query.value = query
        }
    }
}
