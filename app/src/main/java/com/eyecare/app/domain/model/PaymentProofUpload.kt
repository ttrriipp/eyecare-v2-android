package com.eyecare.app.domain.model

import java.io.File

data class PaymentProofUpload(
    val imageFile: File,
    val senderName: String,
    val referenceNumber: String,
    val mimeType: String = "image/jpeg",
    val width: Int = 1,
    val height: Int = 1,
    val deleteAfterUpload: Boolean = false,
)

data class PaymentProofResult(
    val id: Int,
    val status: PaymentProofStatus,
    val senderName: String,
    val referenceNumber: String,
    val createdAt: String,
)
