package com.phoneagent.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneagent.data.local.entities.SessionEntity
import com.phoneagent.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val sessions: StateFlow<List<SessionEntity>> = MutableStateFlow(emptyList())

    private val _currentSessionId = MutableStateFlow("")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.getAllSessions().collect { list ->
                (sessions as MutableStateFlow).value = list
            }
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val id = sessionRepository.createNewSession()
            _currentSessionId.value = id
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
        }
    }

    fun archiveSession(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.archiveSession(sessionId)
        }
    }
}
