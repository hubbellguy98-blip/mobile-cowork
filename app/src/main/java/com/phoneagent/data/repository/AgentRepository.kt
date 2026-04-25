package com.phoneagent.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.phoneagent.data.local.dao.ActionLogDao
import com.phoneagent.data.local.entities.ActionLogEntity
import com.phoneagent.data.local.entities.ConversationEntity
import com.phoneagent.data.remote.GrokRepository
import com.phoneagent.data.remote.models.AgentAction
import com.phoneagent.data.remote.models.AgentActionType
import com.phoneagent.domain.usecases.CaptureScreenUseCase
import com.phoneagent.domain.usecases.ExecuteActionUseCase
import com.phoneagent.service.PhoneAgentAccessibilityService
import com.phoneagent.utils.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

import android.content.Context
import android.content.Intent
import com.phoneagent.ui.overlay.AgentOverlayService
import com.phoneagent.utils.SafetyChecker
import com.phoneagent.utils.ValidationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    private val grokRepository: GrokRepository,
    private val captureScreenUseCase: CaptureScreenUseCase,
    private val executeActionUseCase: ExecuteActionUseCase,
    private val actionLogDao: ActionLogDao,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    fun runAgentLoop(
        coroutineScope: CoroutineScope,
        userCommand: String,
        onStepUpdate: (stepNumber: Int, totalSteps: Int, description: String) -> Unit,
        onActionExecuted: (AgentAction) -> Unit,
        onComplete: (success: Boolean, summary: String) -> Unit,
        requestSensitiveConfirmation: suspend (AgentAction) -> Boolean
    ): Job {
        return coroutineScope.launch(Dispatchers.IO) {
            val sessionId = UUID.randomUUID().toString()
            var step = 0
            val conversationHistory = mutableListOf<ConversationEntity>()
            
            val maxStepsStr = dataStore.data.map { it[stringPreferencesKey(Constants.DATASTORE_MAX_STEPS)] ?: Constants.MAX_AGENT_STEPS.toString() }.first()
            val maxSteps = maxStepsStr.toIntOrNull() ?: Constants.MAX_AGENT_STEPS
            
            val confirmSensitive = dataStore.data.map { it[booleanPreferencesKey(Constants.DATASTORE_CONFIRM_SENSITIVE)] ?: true }.first()

            val startIntent = Intent(context, AgentOverlayService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }


            try {
            while (step < maxSteps) {
                ensureActive()
                step++
                
                // 1. Capture screen
                val screenshot = captureScreenUseCase.execute()
                
                // 3. Call Grok
                val result = grokRepository.sendAgentMessage(userCommand, screenshot, conversationHistory, step == 1)
                if (result.isFailure) {
                    withContext(Dispatchers.Main) { onComplete(false, result.exceptionOrNull()?.message ?: "Unknown error") }
                    return@launch
                }
                
                val action = result.getOrThrow()
                val stepDesc = action.stepDescription ?: action.thought

                // Detect loops
                val recentActionsList = mutableListOf<AgentAction>()
                if (recentActionsList.size > 5) recentActionsList.removeAt(0)
                recentActionsList.add(action)
                if (SafetyChecker.detectLoopCondition(recentActionsList)) {
                    withContext(Dispatchers.Main) { onComplete(false, "Agent appears to be stuck in a loop. Stopped.") }
                    return@launch
                }

                // Validate action
                val blockedApps = dataStore.data.map { it[stringPreferencesKey("blocked_apps")] ?: "" }.first().split(",").toSet()
                val validation = SafetyChecker.validateAction(action, blockedApps)
                if (validation is ValidationResult.Invalid) {
                    withContext(Dispatchers.Main) { onComplete(false, "Action blocked by safety rules: ${validation.reason}") }
                    return@launch
                }

                // Update overlay
                val intent = Intent("com.phoneagent.UPDATE_OVERLAY")
                intent.putExtra("STEP", step)
                intent.putExtra("TOTAL", maxSteps)
                intent.putExtra("DESC", stepDesc)
                context.sendBroadcast(intent)

                
                // 4. Notify UI of new step
                withContext(Dispatchers.Main) {
                    onStepUpdate(step, maxSteps, stepDesc)
                }
                
                // Double check sensitivity
                val isActuallySensitive = action.isSensitive || PhoneAgentAccessibilityService.isSensitiveAction(action)
                
                // 5. Handle sensitive action confirmation
                if (isActuallySensitive && confirmSensitive) {
                    val confirmed = requestSensitiveConfirmation(action)
                    if (!confirmed) {
                        withContext(Dispatchers.Main) { onComplete(false, "User cancelled sensitive action.") }
                        return@launch
                    }
                }
                
                // 6. Check for terminal states
                if (action.action == AgentActionType.TASK_COMPLETE) {
                    withContext(Dispatchers.Main) { onComplete(true, action.thought) }
                    break
                }
                if (action.action == AgentActionType.TASK_FAILED) {
                    withContext(Dispatchers.Main) { onComplete(false, action.thought) }
                    break
                }
                
                // 7. Execute action
                val actionResult = executeActionUseCase.execute(action, sessionId)
                
                // 8. Add to conversation history (for context in GrokRepo)
                val aiEntity = ConversationEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    content = action.thought,
                    actionJson = com.google.gson.Gson().toJson(action)
                )
                conversationHistory.add(aiEntity)
                
                withContext(Dispatchers.Main) {
                    onActionExecuted(action)
                }
                
                if (!actionResult.success) {
                    // Optional: could break or let Grok know it failed next turn
                }
                
                // 10. Wait for UI to settle
                delay(800)
            }
            
            
            context.stopService(Intent(context, AgentOverlayService::class.java))

            } finally {
                context.stopService(Intent(context, AgentOverlayService::class.java))
            }
            if (step >= maxSteps) {
                withContext(Dispatchers.Main) { onComplete(false, "Max steps reached ($maxSteps)") }
            }
        }
    }
}
