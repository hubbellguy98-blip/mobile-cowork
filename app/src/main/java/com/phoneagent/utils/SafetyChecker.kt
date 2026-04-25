package com.phoneagent.utils

import android.content.Context
import com.phoneagent.data.remote.models.AgentAction
import com.phoneagent.data.remote.models.AgentActionType

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

object SafetyChecker {

    fun isAppBlocked(appName: String, blockedList: Set<String>): Boolean {
        if (appName.isBlank()) return false
        return blockedList.any { blocked ->
            appName.contains(blocked, ignoreCase = true) || blocked.contains(appName, ignoreCase = true)
        }
    }

    fun detectLoopCondition(recentActions: List<AgentAction>): Boolean {
        if (recentActions.size < 3) return false
        val last3 = recentActions.takeLast(3)
        
        val first = last3[0]
        val allSame = last3.all { 
            it.action == first.action &&
            it.params?.x == first.params?.x &&
            it.params?.y == first.params?.y &&
            it.params?.text == first.params?.text
        }
        return allSame
    }

    fun validateAction(action: AgentAction, blockedList: Set<String>): ValidationResult {
        if ((action.confidence ?: 1.0) < 0.5) {
            return ValidationResult.Invalid("Confidence too low")
        }

        when (action.action) {
            AgentActionType.TAP, AgentActionType.LONG_PRESS, AgentActionType.SWIPE -> {
                val x = action.params?.x ?: 0
                val y = action.params?.y ?: 0
                if (x < 0 || x > 1080 || y < 0 || y > 2340) {
                    return ValidationResult.Invalid("Coordinates out of bounds ($x, $y)")
                }
            }
            AgentActionType.TYPE -> {
                if (action.params?.text.isNullOrBlank()) {
                    return ValidationResult.Invalid("Text to type is empty")
                }
            }
            AgentActionType.OPEN_APP -> {
                val appName = action.params?.appName
                if (appName.isNullOrBlank()) {
                    return ValidationResult.Invalid("App name is empty")
                }
                if (isAppBlocked(appName, blockedList)) {
                    return ValidationResult.Invalid("App '$appName' is blocked in Safety Settings")
                }
            }
            else -> {}
        }
        return ValidationResult.Valid
    }

    fun isCurrentAppBlocked(context: Context, blockedList: Set<String>): Boolean {
        // Mock implementation since getting active app reliably requires UsageStats
        return false 
    }
}
