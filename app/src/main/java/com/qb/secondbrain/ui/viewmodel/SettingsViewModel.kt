package com.qb.secondbrain.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.secondbrain.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val asrEngine: String = "科大讯飞",
    val llmApiUrl: String = "",
    val llmApiKey: String = "",
    val llmModel: String = "gpt-4o-mini",
    val maxRecordingDuration: Int = 60
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.asrEngine,
                settingsDataStore.llmApiUrl,
                settingsDataStore.llmApiKey,
                settingsDataStore.llmModel,
                settingsDataStore.maxRecordingDuration
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                SettingsUiState(
                    asrEngine = values[0] as String,
                    llmApiUrl = values[1] as String,
                    llmApiKey = values[2] as String,
                    llmModel = values[3] as String,
                    maxRecordingDuration = values[4] as Int
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setAsrEngine(engine: String) {
        viewModelScope.launch { settingsDataStore.setAsrEngine(engine) }
    }

    fun setLlmApiUrl(url: String) {
        viewModelScope.launch { settingsDataStore.setLlmApiUrl(url) }
    }

    fun setLlmApiKey(key: String) {
        viewModelScope.launch { settingsDataStore.setLlmApiKey(key) }
    }

    fun setLlmModel(model: String) {
        viewModelScope.launch { settingsDataStore.setLlmModel(model) }
    }

    fun setMaxRecordingDuration(seconds: Int) {
        viewModelScope.launch { settingsDataStore.setMaxRecordingDuration(seconds) }
    }
}
