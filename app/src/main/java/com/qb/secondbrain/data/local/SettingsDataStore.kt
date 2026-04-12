package com.qb.secondbrain.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ASR_ENGINE = stringPreferencesKey("asr_engine")
        val LLM_API_URL = stringPreferencesKey("llm_api_url")
        val LLM_API_KEY = stringPreferencesKey("llm_api_key")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val MAX_RECORDING_DURATION = intPreferencesKey("max_recording_duration")
    }

    val asrEngine: Flow<String> = context.dataStore.data.map { it[Keys.ASR_ENGINE] ?: "科大讯飞" }
    val llmApiUrl: Flow<String> = context.dataStore.data.map { it[Keys.LLM_API_URL] ?: "" }
    val llmApiKey: Flow<String> = context.dataStore.data.map { it[Keys.LLM_API_KEY] ?: "" }
    val llmModel: Flow<String> = context.dataStore.data.map { it[Keys.LLM_MODEL] ?: "gpt-4o-mini" }
    val maxRecordingDuration: Flow<Int> = context.dataStore.data.map { it[Keys.MAX_RECORDING_DURATION] ?: 60 }

    suspend fun setAsrEngine(engine: String) { context.dataStore.edit { it[Keys.ASR_ENGINE] = engine } }
    suspend fun setLlmApiUrl(url: String) { context.dataStore.edit { it[Keys.LLM_API_URL] = url } }
    suspend fun setLlmApiKey(key: String) { context.dataStore.edit { it[Keys.LLM_API_KEY] = key } }
    suspend fun setLlmModel(model: String) { context.dataStore.edit { it[Keys.LLM_MODEL] = model } }
    suspend fun setMaxRecordingDuration(seconds: Int) { context.dataStore.edit { it[Keys.MAX_RECORDING_DURATION] = seconds } }
}
