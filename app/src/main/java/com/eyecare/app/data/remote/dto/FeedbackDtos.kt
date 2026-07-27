package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object FeedbackDtos {

    @Serializable
    data class FeedbackDto(
        val id: Int,
        @SerialName("appointment_id") val appointmentId: Int,
        val rating: Int,
        val comment: String? = null,
    )

    @Serializable
    data class SubmitFeedbackRequest(
        @SerialName("appointment_id") val appointmentId: Int,
        val rating: Int,
        val comment: String? = null,
    )

    @Serializable data class FeedbackResponse(val data: FeedbackDto)
}
