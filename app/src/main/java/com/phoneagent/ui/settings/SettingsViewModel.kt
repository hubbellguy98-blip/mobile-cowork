package com.phoneagent.ui.settings

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phoneagent.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.dataStore by preferencesDataStore(name = Constants.PREFS_NAME)

data class SettingsState(
    val apiKey: String = "",
    val model: String = Constants.GROK_MODEL,
    val maxSteps: String = Constants.MAX_AGENT_STEPS.toString(),
    val showSteps: Boolean = true,
    val confirmSensitive: Boolean = true,
    val autoStop: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.map { preferences ->
                SettingsState(
                    apiKey = preferences[stringPreferencesKey(Constants.DATASTORE_API_KEY)] ?: "",
                    model = preferences[stringPreferencesKey(Constants.DATASTORE_MODEL)] ?: Constants.GROK_MODEL,
                    maxSteps = preferences[stringPreferencesKey(Constants.DATASTORE_MAX_STEPS)] ?: Constants.MAX_AGENT_STEPS.toString(),
                    showSteps = preferences[booleanPreferencesKey(Constants.DATASTORE_SHOW_STEPS)] ?: true,
                    confirmSensitive = preferences[booleanPreferencesKey(Constants.DATASTORE_CONFIRM_SENSITIVE)] ?: true,
                    autoStop = preferences[booleanPreferencesKey(Constants.DATASTORE_AUTO_STOP)] ?: true
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun saveSettings(state: SettingsState) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(Constants.DATASTORE_API_KEY)] = state.apiKey
                preferences[stringPreferencesKey(Constants.DATASTORE_MODEL)] = state.model
                preferences[stringPreferencesKey(Constants.DATASTORE_MAX_STEPS)] = state.maxSteps
                preferences[booleanPreferencesKey(Constants.DATASTORE_SHOW_STEPS)] = state.showSteps
                preferences[booleanPreferencesKey(Constants.DATASTORE_CONFIRM_SENSITIVE)] = state.confirmSensitive
                preferences[booleanPreferencesKey(Constants.DATASTORE_AUTO_STOP)] = state.autoStop
            }
        }
    }

    fun clearHistory() {
        // Here we would call chatRepository.clearMessages()
    }
    
    fun exportLogs() {
        // Implementation for exporting logs
    }
}
