package com.eyecare.app.presentation.eyewear

object PaymentProofInspector {

    private val allowedMimeTypes = setOf("image/jpeg", "image/jpg", "image/png")
    private const val maxFileSizeBytes = 5 * 1024 * 1024L // 5 MB
    private const val maxDimension = 8000

    fun validateMimeType(mimeType: String): String? {
        if (mimeType.isBlank()) return "Please select a JPG or PNG image."
        if (mimeType.lowercase() !in allowedMimeTypes) {
            return "Please select a JPG or PNG image."
        }
        return null
    }

    fun validateFileSize(sizeBytes: Long): String? {
        if (sizeBytes <= 0) return "Unable to read the selected file."
        if (sizeBytes > maxFileSizeBytes) {
            return "Image must be 5 MB or smaller."
        }
        return null
    }

    fun validateDimensions(width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return "Unable to read image dimensions."
        if (width > maxDimension || height > maxDimension) {
            return "Image dimensions must be 8,000 × 8,000 pixels or smaller."
        }
        return null
    }

    fun validateSenderName(name: String): String? {
        if (name.isBlank()) return "Sender name is required."
        if (name.length > 100) return "Sender name must be 100 characters or fewer."
        return null
    }

    fun validateReferenceNumber(ref: String): String? {
        if (ref.isBlank()) return "Reference number is required."
        if (ref.length > 100) return "Reference number must be 100 characters or fewer."
        return null
    }
}