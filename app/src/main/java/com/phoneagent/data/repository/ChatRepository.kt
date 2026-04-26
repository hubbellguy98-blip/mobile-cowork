package com.phoneagent.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.phoneagent.data.local.dao.ConversationDao
import com.phoneagent.data.local.entities.ConversationEntity
import com.phoneagent.data.remote.GrokRepository
import com.phoneagent.data.remote.models.AgentAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val grokRepository: GrokRepository
) {
    private var currentSessionId: String = UUID.randomUUID().toString()

    suspend fun processUserMessage(text: String, screenshot: Bitmap?): Flow<AgentAction> = flow {
        // Save user message
        val userEntity = ConversationEntity(
            sessionId = currentSessionId,
            role = "user",
            content = text
        )
        conversationDao.insertMessage(userEntity)

        // Convert bitmap
        val base64Image = if (screenshot != null) {
            withContext(Dispatchers.Default) {
                val outputStream = ByteArrayOutputStream()
                screenshot.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            }
        } else {
            null
        }

        // Get recent history
        val history = conversationDao.getRecentMessages(10).reversed()

        // Call Grok
        val result = grokRepository.sendAgentMessage(text, base64Image, history)

        if (result.isSuccess) {
            val action = result.getOrThrow()

            // Save AI response
            val aiEntity = ConversationEntity(
                sessionId = currentSessionId,
                role = "assistant",
                content = action.thought,
                actionJson = Gson().toJson(action)
            )
            conversationDao.insertMessage(aiEntity)

            emit(action)
        } else {
            // Handle error case
            val errorAction = AgentAction(
                thought = "Error: ${result.exceptionOrNull()?.message}",
                action = com.phoneagent.data.remote.models.AgentActionType.TASK_FAILED
            )
            emit(errorAction)
        }
    }.flowOn(Dispatchers.IO)

    fun getMessages(sessionId: String = currentSessionId): Flow<List<ConversationEntity>> {
        return conversationDao.getMessagesBySession(sessionId).flowOn(Dispatchers.IO)
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            conversationDao.deleteAll()
            currentSessionId = UUID.randomUUID().toString()
        }
    }
}
