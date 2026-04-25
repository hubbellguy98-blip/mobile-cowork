package com.phoneagent.domain.usecases

import com.phoneagent.data.local.dao.ActionLogDao
import com.phoneagent.data.local.entities.ActionLogEntity
import com.phoneagent.data.remote.models.AgentAction
import com.phoneagent.data.remote.models.AgentActionType
import com.phoneagent.service.PhoneAgentAccessibilityService
import kotlinx.coroutines.delay
import java.util.UUID
import javax.inject.Inject

data class ActionResult(
    val success: Boolean,
    val errorMessage: String? = null
)

class ExecuteActionUseCase @Inject constructor(
    private val actionLogDao: ActionLogDao
) {
    suspend fun execute(action: AgentAction, sessionId: String = UUID.randomUUID().toString()): ActionResult {
        val service = PhoneAgentAccessibilityService.instance
        
        if (service == null) {
            logAction(sessionId, action, false, "Accessibility service not enabled")
            return ActionResult(false, "Accessibility service not enabled")
        }

        var success = false
        var errorMessage: String? = null

        try {
            when (action.action) {
                AgentActionType.TAP -> {
                    val x = action.params?.x?.toFloat() ?: -1f
                    val y = action.params?.y?.toFloat() ?: -1f
                    success = service.performTap(x, y)
                }
                AgentActionType.SWIPE -> {
                    val distance = action.params?.distance?.toFloat() ?: 500f
                    val duration = action.params?.durationMs ?: 500L
                    val x = action.params?.x?.toFloat() ?: 500f
                    val y = action.params?.y?.toFloat() ?: 500f
                    
                    var endX = x
                    var endY = y
                    when (action.params?.direction?.uppercase()) {
                        "UP" -> endY -= distance
                        "DOWN" -> endY += distance
                        "LEFT" -> endX -= distance
                        "RIGHT" -> endX += distance
                    }
                    success = service.performSwipe(x, y, endX, endY, duration)
                }
                AgentActionType.TYPE -> {
                    val text = action.params?.text ?: ""
                    success = service.performTypeText(text)
                }
                AgentActionType.PRESS_BACK -> {
                    success = service.performPressBack()
                }
                AgentActionType.PRESS_HOME -> {
                    success = service.performPressHome()
                }
                AgentActionType.OPEN_APP -> {
                    val appName = action.params?.appName ?: ""
                    success = service.openApp(appName)
                }
                AgentActionType.SCROLL -> {
                    val direction = action.params?.direction ?: "DOWN"
                    val distance = action.params?.distance ?: 500
                    success = service.performScroll(direction, distance)
                }
                AgentActionType.WAIT -> {
                    val waitMs = action.params?.waitMs ?: 1000L
                    delay(waitMs)
                    success = true
                }
                AgentActionType.TASK_COMPLETE -> {
                    success = true
                }
                AgentActionType.TASK_FAILED -> {
                    success = false
                    errorMessage = action.thought
                }
            }

            // After each action: add delay of 500ms to let UI settle
            delay(500)

        } catch (e: Exception) {
            success = false
            errorMessage = e.message
        }

        logAction(sessionId, action, success, errorMessage)
        return ActionResult(success, errorMessage)
    }

    private suspend fun logAction(sessionId: String, action: AgentAction, success: Boolean, errorMessage: String?) {
        val logEntity = ActionLogEntity(
            sessionId = sessionId,
            actionType = action.action.name,
            actionParams = action.params?.toString() ?: "{}",
            success = success,
            errorMessage = errorMessage
        )
        actionLogDao.insertLog(logEntity)
    }
}
