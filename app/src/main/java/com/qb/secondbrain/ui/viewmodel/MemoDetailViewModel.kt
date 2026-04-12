package com.qb.secondbrain.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
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

data class MemoDetailUiState(
    val memo: Memo? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isRawTextExpanded: Boolean = false
)

@HiltViewModel
class MemoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val queryMemoUseCase: QueryMemoUseCase,
    private val deleteMemoUseCase: DeleteMemoUseCase
) : ViewModel() {

    private val memoId: Long = savedStateHandle["id"] ?: 0L

    private val _uiState = MutableStateFlow(MemoDetailUiState())
    val uiState: StateFlow<MemoDetailUiState> = _uiState.asStateFlow()

    init {
        loadMemo()
    }

    fun loadMemo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val memo = queryMemoUseCase.byId(memoId)
                _uiState.value = _uiState.value.copy(
                    memo = memo,
                    isLoading = false,
                    error = if (memo == null) "备忘录不存在" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    fun deleteMemo(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                deleteMemoUseCase(memoId)
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "删除失败")
            }
        }
    }

    fun toggleRawText() {
        _uiState.value = _uiState.value.copy(isRawTextExpanded = !_uiState.value.isRawTextExpanded)
    }
}
