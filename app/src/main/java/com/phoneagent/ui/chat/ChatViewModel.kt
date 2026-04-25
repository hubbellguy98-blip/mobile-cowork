package com.phoneagent.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneagent.data.repository.ChatRepository
import com.phoneagent.domain.usecases.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Intent

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.phoneagent.data.remote.models.AgentAction
import com.phoneagent.data.repository.AgentRepository
import com.phoneagent.service.PhoneAgentAccessibilityService
import com.phoneagent.utils.Constants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

import com.phoneagent.service.ScreenCaptureService
import javax.inject.Inject

enum class Role {
    USER, AI, ACTION
}

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val role: Role,
    val timestamp: Long = System.currentTimeMillis(),
    val actionSteps: List<String> = emptyList()
)

data class AgentStatus(
    val step: Int = 0,
    val totalSteps: Int = 0,
    val description: String = "",
    val isIdle: Boolean = false
) {
    companion object {
        fun idle() = AgentStatus(isIdle = true)
    }
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val dataStore: DataStore<Preferences>,
    private val chatRepository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _messagesList = MutableStateFlow<List<Message>>(emptyList())
    val messagesList: StateFlow<List<Message>> = _messagesList.asStateFlow()

    private val _isAgentRunning = MutableStateFlow(false)
    val isAgentRunning: StateFlow<Boolean> = _isAgentRunning.asStateFlow()

    private val _agentStatus = MutableStateFlow(AgentStatus())
    val agentStatus: StateFlow<AgentStatus> = _agentStatus.asStateFlow()

    
    private val _needsScreenCapturePermission = MutableStateFlow(false)
    val needsScreenCapturePermission: StateFlow<Boolean> = _needsScreenCapturePermission.asStateFlow()

    private var agentJob: Job? = null

    init {
        // Here we would normally observe chatRepository
        // For Phase 2 UI testing, we will just have an empty list initially
    }

    private var agentJob: Job? = null
    
    private var sensitiveConfirmation: CompletableDeferred<Boolean>? = null
    private val _sensitiveActionPending = MutableStateFlow<AgentAction?>(null)
    val sensitiveActionPending: StateFlow<AgentAction?> = _sensitiveActionPending.asStateFlow()

    fun confirmSensitiveAction(allow: Boolean) {
        sensitiveConfirmation?.complete(allow)
    }

    private var pendingMessage: String? = null

    fun onScreenCapturePermissionResult(resultCode: Int, data: Intent?) {
        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            _needsScreenCapturePermission.value = false
            pendingMessage?.let {
                sendMessage(it)
                pendingMessage = null
            }
        } else {
            addMessage(Message(role = Role.AI, content = "Screen capture permission denied."))
        }
    }
    
    private fun addMessage(msg: Message) {
        _messagesList.value = _messagesList.value + msg
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        addMessage(Message(role = Role.USER, content = text))
        
        if (!ScreenCaptureService.isRunning()) {
            pendingMessage = text
            _needsScreenCapturePermission.value = true
            return
        }
        
        if (!PhoneAgentAccessibilityService.isRunning()) {
            addMessage(Message(role = Role.AI, content = "⚠️ Please enable Accessibility permission first in Settings > Accessibility > PhoneAgent"))
            return
        }
        
        _isAgentRunning.value = true
        _agentStatus.value = AgentStatus(step = 1, totalSteps = 10, description = "Starting agent...")
        
        agentJob = agentRepository.runAgentLoop(
            coroutineScope = viewModelScope,
            userCommand = text,
            onStepUpdate = { step, total, desc ->
                _agentStatus.value = AgentStatus(step, total, desc)
                viewModelScope.launch {
                    val showSteps = dataStore.data.map { it[booleanPreferencesKey(Constants.DATASTORE_SHOW_STEPS)] ?: true }.first()
                    if (showSteps) {
                        addMessage(Message(role = Role.ACTION, content = "Step $step: $desc"))
                    }
                }
            },
            onActionExecuted = { action ->
                // Update action in UI if needed
            },
            onComplete = { success, summary ->
                val icon = if (success) "✅" else "❌"
                addMessage(Message(role = Role.AI, content = "$icon $summary"))
                _isAgentRunning.value = false
                _agentStatus.value = AgentStatus.idle()
            },
            requestSensitiveConfirmation = { action ->
                sensitiveConfirmation = CompletableDeferred()
                _sensitiveActionPending.value = action
                val result = sensitiveConfirmation!!.await()
                _sensitiveActionPending.value = null
                result
            }
        )
    }

    fun stopAgent() {
        agentJob?.cancel()
        agentJob = null
        _isAgentRunning.value = false
        _agentStatus.value = AgentStatus.idle()
        addMessage(Message(role = Role.AI, content = "🛑 Agent stopped by user."))
    }

    fun clearMessages() {
        _messagesList.value = emptyList()
    }
}
