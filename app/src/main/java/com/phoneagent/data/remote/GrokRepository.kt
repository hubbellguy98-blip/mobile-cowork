package com.phoneagent.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.phoneagent.data.remote.models.*
import com.phoneagent.utils.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GrokRepository @Inject constructor(
    private val apiService: GrokApiService,
    private val dataStore: DataStore<Preferences>
) {

    private val systemPrompt = """
        You are a highly capable AI agent that controls an Android phone to accomplish tasks for the user.
        You will receive a user command and a screenshot of the current phone screen.
        The screen dimensions are 1080x2340 pixels. Identify UI elements by their visual position (x, y coordinates).
        
        You MUST respond ONLY with a valid JSON object matching this exact schema, without any markdown formatting or extra text:
        {
          "thought": "Brief explanation of what I see on screen and what I'm planning to do",
          "action": "TAP | SWIPE | TYPE | PRESS_BACK | PRESS_HOME | OPEN_APP | SCROLL | WAIT | TASK_COMPLETE | TASK_FAILED",
          "params": {
            "x": 540,
            "y": 1200,
            "text": "text to type if action is TYPE",
            "app_name": "Instagram (if action is OPEN_APP)",
            "direction": "UP | DOWN | LEFT | RIGHT (if action is SCROLL or SWIPE)",
            "distance": 500,
            "duration_ms": 300,
            "wait_ms": 1000
          },
          "step_description": "Human readable: Opening Instagram app",
          "is_sensitive": false,
          "task_progress": "3/10 steps completed",
          "confidence": 0.92
        }
        
        RULES:
        1. If action is TASK_COMPLETE: set thought to explain what was accomplished.
        2. If action is TASK_FAILED: set thought to explain why it failed and what was tried.
        3. Describe your reasoning in "thought" BEFORE every action.
        4. Provide NO output other than the JSON object.
    """.trimIndent()

    suspend fun sendAgentMessage(
        userCommand: String,
        screenshotBase64: String?,
        conversationHistory: List<com.phoneagent.data.local.entities.ConversationEntity>,
        isFirstTurn: Boolean = true
    ): Result<AgentAction> {
        return try {
            val modelKey = stringPreferencesKey(Constants.DATASTORE_MODEL)
            val model = dataStore.data.map { it[modelKey] ?: Constants.GROK_MODEL }.first()

            val messages = mutableListOf<Message>()
            messages.add(Message(role = "system", content = systemPrompt))

            // Add history - only keep last 6 turns to save tokens
            val recentHistory = conversationHistory.takeLast(6)
            recentHistory.forEach { entity ->
                val role = if (entity.role == "user") "user" else "assistant"
                messages.add(Message(role = role, content = entity.content))
            }

            // Current turn logic
            val contentList = mutableListOf<ContentPart>()
            
            if (isFirstTurn) {
                contentList.add(TextPart(text = "COMMAND: $userCommand\n\nCurrent screen:"))
            } else {
                contentList.add(TextPart(text = "Remember: You are helping with task: '$userCommand'.\nRespond ONLY with valid JSON matching the required schema.\n\nTask is still in progress. What do you see now? Continue."))
            }
            
            if (screenshotBase64 != null) {
                contentList.add(ImagePart(imageUrl = ImageUrl("data:image/jpeg;base64,$screenshotBase64")))
            }

            messages.add(Message(role = "user", content = contentList))

            val request = GrokRequest(
                model = model,
                messages = messages
            )

            val response = apiService.sendRequest(request)
            val responseContent = response.choices.firstOrNull()?.message?.content

            if (responseContent.isNullOrBlank()) {
                return Result.failure(Exception("Empty response from Grok API"))
            }

            val jsonString = responseContent.replace("```json", "").replace("```", "").trim()
            val action = AgentAction.fromJson(jsonString)

            if (action != null) {
                Result.success(action)
            } else {
                Result.success(
                    AgentAction(
                        thought = "Failed to parse API response: $responseContent",
                        action = AgentActionType.TASK_FAILED
                    )
                )
            }
        } catch (e: HttpException) {
            val errorMsg = when (e.code()) {
                401 -> "Invalid API Key"
                429 -> "Rate limit exceeded"
                500 -> "Server error"
                else -> "HTTP Error: ${e.code()}"
            }
            Result.success(
                AgentAction(
                    thought = errorMsg,
                    action = AgentActionType.TASK_FAILED
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
