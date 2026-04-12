package com.qb.secondbrain.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.data.repository.MemoRepository
import com.qb.secondbrain.domain.usecase.DeleteMemoUseCase
import com.qb.secondbrain.domain.usecase.QueryMemoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoListUiState(
    val memos: List<Memo> = emptyList(),
    val isLoading: Boolean = true,
    val recentlyDeleted: Memo? = null
)

@HiltViewModel
class MemoListViewModel @Inject constructor(
    private val queryMemoUseCase: QueryMemoUseCase,
    private val deleteMemoUseCase: DeleteMemoUseCase,
    private val memoRepository: MemoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoListUiState())
    val uiState: StateFlow<MemoListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            queryMemoUseCase.getAll().collect { memos ->
                _uiState.value = _uiState.value.copy(
                    memos = memos.filter { !it.isDeleted },
                    isLoading = false
                )
            }
        }
    }

    fun deleteMemo(memo: Memo) {
        viewModelScope.launch {
            val deletedMemo = memo.copy(isDeleted = true, updatedAt = System.currentTimeMillis())
            memoRepository.updateMemo(deletedMemo)
            _uiState.value = _uiState.value.copy(recentlyDeleted = memo)
        }
    }

    fun undoDelete() {
        val recentlyDeleted = _uiState.value.recentlyDeleted ?: return
        viewModelScope.launch {
            val restored = recentlyDeleted.copy(isDeleted = false, updatedAt = System.currentTimeMillis())
            memoRepository.updateMemo(restored)
            _uiState.value = _uiState.value.copy(recentlyDeleted = null)
        }
    }

    fun clearRecentlyDeleted() {
        _uiState.value = _uiState.value.copy(recentlyDeleted = null)
    }
}
