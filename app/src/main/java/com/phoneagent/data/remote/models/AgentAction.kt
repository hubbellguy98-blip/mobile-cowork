package com.phoneagent.data.remote.models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

enum class AgentActionType {
    TAP, SWIPE, TYPE, PRESS_BACK, PRESS_HOME, OPEN_APP, SCROLL, WAIT, TASK_COMPLETE, TASK_FAILED
}

data class AgentParams(
    @SerializedName("x") val x: Int? = null,
    @SerializedName("y") val y: Int? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("app_name") val appName: String? = null,
    @SerializedName("direction") val direction: String? = null, // UP | DOWN | LEFT | RIGHT
    @SerializedName("distance") val distance: Int? = null,
    @SerializedName("duration_ms") val durationMs: Long? = null,
    @SerializedName("wait_ms") val waitMs: Long? = null
)

data class AgentAction(
    @SerializedName("thought") val thought: String,
    @SerializedName("action") val action: AgentActionType,
    @SerializedName("params") val params: AgentParams? = null,
    @SerializedName("step_description") val stepDescription: String? = null,
    @SerializedName("is_sensitive") val isSensitive: Boolean = false,
    @SerializedName("task_progress") val taskProgress: String? = null,
    @SerializedName("confidence") val confidence: Double? = null
) {
    companion object {
        fun fromJson(json: String): AgentAction? {
            return try {
                Gson().fromJson(json, AgentAction::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
