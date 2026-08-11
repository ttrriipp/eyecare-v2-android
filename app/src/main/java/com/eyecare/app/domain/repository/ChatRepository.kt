package com.eyecare.app.domain.repository

import android.net.Uri
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message

interface ChatRepository {
    suspend fun getConversation(): Result<Conversation>
    suspend fun getMessages(): Result<List<Message>>
    suspend fun sendMessage(body: String): Result<Message>
    suspend fun sendFileMessage(uri: Uri, mimeType: String, fileName: String): Result<Message>
    suspend fun downloadAttachment(attachmentId: Int): Result<AttachmentDownload>
}

data class AttachmentDownload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)
