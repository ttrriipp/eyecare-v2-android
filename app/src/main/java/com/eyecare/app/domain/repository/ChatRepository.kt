package com.eyecare.app.domain.repository

import android.net.Uri
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessagePage

interface ChatRepository {
    suspend fun getConversation(): Result<Conversation>
    suspend fun getMessages(cursor: String? = null): Result<MessagePage>
    suspend fun searchMessages(query: String, cursor: String? = null): Result<MessagePage>
    suspend fun markMessagesRead(): Result<Int>
    suspend fun sendMessage(body: String): Result<Message>
    suspend fun sendFileMessage(uri: Uri, mimeType: String, fileName: String): Result<Message>
    suspend fun downloadAttachment(attachmentId: Int): Result<AttachmentDownload>
}

data class AttachmentDownload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)
