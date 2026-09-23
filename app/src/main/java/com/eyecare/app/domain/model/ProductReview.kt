package com.eyecare.app.domain.model

import java.io.File

data class ProductReview(
    val rating: Int,
    val comment: String,
    val createdAt: String,
    val attachmentUrl: String? = null,
)

data class ProductRatingAttachment(
    val imageFile: File,
    val mimeType: String,
    val width: Int,
    val height: Int,
)
