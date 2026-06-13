package com.eyecare.app.presentation.messaging

object AttachmentValidator {
    private val ALLOWED_TYPES = setOf(
        "image/jpeg", "image/png", "image/gif",
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    )
    private const val MAX_BYTES = 10 * 1024 * 1024L // 10 MB

    fun isAllowedType(mimeType: String): Boolean = mimeType in ALLOWED_TYPES

    fun isWithinSizeLimit(bytes: Long): Boolean = bytes <= MAX_BYTES

    fun validate(mimeType: String, bytes: Long): Result<Unit> = when {
        !isAllowedType(mimeType) -> Result.failure(
            IllegalArgumentException("Unsupported file type. Allowed: images, PDF, Word documents.")
        )
        !isWithinSizeLimit(bytes) -> Result.failure(
            IllegalArgumentException("File exceeds 10 MB limit.")
        )
        else -> Result.success(Unit)
    }
}
