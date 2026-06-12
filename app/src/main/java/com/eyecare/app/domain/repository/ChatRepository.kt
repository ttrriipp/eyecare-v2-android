package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message

interface ChatRepository {
    /** Gets first conversation or creates one if none exists. */
    suspend fun getOrCreateConversation(): Result<Conversation>
    suspend fun getMessages(conversationId: Int): Result<List<Message>>
    suspend fun sendMessage(conversationId: Int, body: String): Result<Message>
}
