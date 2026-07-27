package com.eyecare.app.domain.model

data class FrameRating(
    val id: Int,
    val patientId: Int,
    val productVariantId: Int,
    val dispensingEventId: Int?,
    val rating: Int,
    val comment: String?,
    val currentRevisionId: Int,
    val isHidden: Boolean,
    val moderationReason: String?,
    val revisions: List<FrameRatingRevision>,
)

data class FrameRatingRevision(
    val id: Int,
    val frameRatingId: Int,
    val revisionNumber: Int,
    val rating: Int,
    val comment: String?,
    val revisedBy: Int,
    val revisedAt: String,
)
