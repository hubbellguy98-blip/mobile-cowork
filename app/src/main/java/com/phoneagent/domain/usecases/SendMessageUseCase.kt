package com.phoneagent.domain.usecases

import com.phoneagent.data.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(message: String) {
        chatRepository.saveMessage(message, isFromUser = true)
        // Here we will eventually trigger the Agent logic
    }
}
