package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object FeedbackDtos {

    @Serializable
    data class FeedbackDto(
        val id: Int,
        @SerialName("appointment_id") val appointmentId: Int? = null,
        @SerialName("order_id") val orderId: Int? = null,
        val rating: Int,
        val comment: String? = null,
    )

    @Serializable
    data class SubmitFeedbackRequest(
        @SerialName("appointment_id") val appointmentId: Int? = null,
        @SerialName("order_id") val orderId: Int? = null,
        val rating: Int,
        val comment: String? = null,
    )

    @Serializable data class FeedbackResponse(val data: FeedbackDto)
    @Serializable data class FeedbackListResponse(val data: List<FeedbackDto>)
}
