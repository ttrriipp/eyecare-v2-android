package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object FrameRatingDtos {

    @Serializable
    data class FrameRatingRevisionDto(
        val id: Int,
        @SerialName("frame_rating_id") val frameRatingId: Int,
        @SerialName("revision_number") val revisionNumber: Int,
        val rating: Int,
        val comment: String? = null,
        @SerialName("revised_by") val revisedBy: Int,
        @SerialName("revised_at") val revisedAt: String,
    )

    @Serializable
    data class FrameRatingDto(
        val id: Int,
        @SerialName("patient_id") val patientId: Int,
        @SerialName("product_variant_id") val productVariantId: Int,
        @SerialName("dispensing_event_id") val dispensingEventId: Int? = null,
        val rating: Int,
        val comment: String? = null,
        @SerialName("current_revision_id") val currentRevisionId: Int,
        @SerialName("is_hidden") val isHidden: Boolean = false,
        @SerialName("moderation_reason") val moderationReason: String? = null,
        val revisions: List<FrameRatingRevisionDto> = emptyList(),
    )

    @Serializable
    data class FrameRatingResponse(val data: FrameRatingDto)

    @Serializable
    data class SubmitRatingRequest(
        @SerialName("product_variant_id") val productVariantId: Int,
        val rating: Int,
        val comment: String? = null,
        @SerialName("dispensing_event_id") val dispensingEventId: Int? = null,
    )
}
