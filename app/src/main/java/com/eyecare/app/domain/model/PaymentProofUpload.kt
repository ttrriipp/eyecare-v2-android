package com.eyecare.app.domain.model

import java.io.File

data class PaymentProofUpload(
    val imageFile: File,
    val senderName: String,
    val referenceNumber: String,
)

data class PaymentProofResult(
    val id: Int,
    val status: PaymentProofStatus,
    val senderName: String,
    val referenceNumber: String,
    val createdAt: String,
)