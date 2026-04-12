package com.qb.secondbrain.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.secondbrain.data.model.ImagePath
import com.qb.secondbrain.data.model.Memo
import com.qb.secondbrain.domain.usecase.AddMemoUseCase
import com.qb.secondbrain.domain.usecase.QueryMemoUseCase
import com.qb.secondbrain.domain.usecase.UpdateMemoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoEditUiState(
    val id: Long? = null,
    val isEditMode: Boolean = false,
    val content: String = "",
    val tags: List<String> = emptyList(),
    val imagePaths: List<ImagePath> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val reminderTime: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val tagInput: String = "",
    val error: String? = null
)

@HiltViewModel
class MemoEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val queryMemoUseCase: QueryMemoUseCase,
    private val addMemoUseCase: AddMemoUseCase,
    private val updateMemoUseCase: UpdateMemoUseCase
) : ViewModel() {

    private val editMemoId: Long? = savedStateHandle.get<Long>("id")?.let { if (it == -1L) null else it }

    private val _uiState = MutableStateFlow(MemoEditUiState())
    val uiState: StateFlow<MemoEditUiState> = _uiState.asStateFlow()

    init {
        if (editMemoId != null) {
            _uiState.value = _uiState.value.copy(isEditMode = true, id = editMemoId, isLoading = true)
            loadMemo(editMemoId)
        }
    }

    private fun loadMemo(id: Long) {
        viewModelScope.launch {
            try {
                val memo = queryMemoUseCase.byId(id)
                if (memo != null) {
                    _uiState.value = _uiState.value.copy(
                        id = memo.id,
                        content = memo.content,
                        tags = memo.tags,
                        imagePaths = memo.imagePaths,
                        latitude = memo.latitude,
                        longitude = memo.longitude,
                        address = memo.address,
                        reminderTime = memo.reminderTime,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "备忘录不存在")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun updateContent(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isNotBlank() && trimmed !in _uiState.value.tags) {
            _uiState.value = _uiState.value.copy(
                tags = _uiState.value.tags + trimmed,
                tagInput = ""
            )
        }
    }

    fun removeTag(tag: String) {
        _uiState.value = _uiState.value.copy(tags = _uiState.value.tags - tag)
    }

    fun updateTagInput(input: String) {
        _uiState.value = _uiState.value.copy(tagInput = input)
    }

    fun addImagePath(imagePath: ImagePath) {
        _uiState.value = _uiState.value.copy(imagePaths = _uiState.value.imagePaths + imagePath)
    }

    fun removeImagePath(imagePath: ImagePath) {
        _uiState.value = _uiState.value.copy(imagePaths = _uiState.value.imagePaths - imagePath)
    }

    fun updateLocation(latitude: Double, longitude: Double, address: String?) {
        _uiState.value = _uiState.value.copy(
            latitude = latitude,
            longitude = longitude,
            address = address
        )
    }

    fun clearLocation() {
        _uiState.value = _uiState.value.copy(latitude = null, longitude = null, address = null)
    }

    fun updateReminderTime(time: Long?) {
        _uiState.value = _uiState.value.copy(reminderTime = time)
    }

    fun saveMemo() {
        val state = _uiState.value
        if (state.content.isBlank()) {
            _uiState.value = state.copy(error = "内容不能为空")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val now = System.currentTimeMillis()
                if (state.isEditMode && state.id != null) {
                    val existing = queryMemoUseCase.byId(state.id)
                    if (existing != null) {
                        val updated = existing.copy(
                            content = state.content,
                            tags = state.tags,
                            imagePaths = state.imagePaths,
                            latitude = state.latitude,
                            longitude = state.longitude,
                            address = state.address,
                            reminderTime = state.reminderTime,
                            updatedAt = now
                        )
                        updateMemoUseCase(updated)
                    }
                } else {
                    val memo = Memo(
                        content = state.content,
                        tags = state.tags,
                        imagePaths = state.imagePaths,
                        latitude = state.latitude,
                        longitude = state.longitude,
                        address = state.address,
                        reminderTime = state.reminderTime,
                        createdAt = now,
                        updatedAt = now
                    )
                    addMemoUseCase(memo)
                }
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message ?: "保存失败")
            }
        }
    }
}
