package com.phoneagent.data.remote.models

import com.google.gson.annotations.SerializedName

data class GrokRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    @SerializedName("temperature") val temperature: Double = 0.0,
    @SerializedName("stream") val stream: Boolean = false
)

data class Message(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: Any // Can be String or List<ContentPart>
)

sealed class ContentPart(
    @SerializedName("type") val type: String
)

data class TextPart(
    @SerializedName("text") val text: String
) : ContentPart("text")

data class ImagePart(
    @SerializedName("image_url") val imageUrl: ImageUrl
) : ContentPart("image_url")

data class ImageUrl(
    @SerializedName("url") val url: String
)
