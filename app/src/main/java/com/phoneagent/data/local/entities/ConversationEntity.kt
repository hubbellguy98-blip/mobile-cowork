package com.phoneagent.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: String, // "user", "assistant", "action", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null,
    val actionSuccess: Boolean? = null,
    val stepNumber: Int? = null,
    val confidence: Float? = null,
    val screenshotPath: String? = null,
    val actionJson: String? = null
)
