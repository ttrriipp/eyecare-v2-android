package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object AppointmentDtos {

    @Serializable
    data class AppointmentDto(
        val id: Int,
        @SerialName("visit_reason") val visitReason: String,
        val status: String,
        @SerialName("scheduled_at") val scheduledAt: String,
        @SerialName("contact_notes") val contactNotes: String? = null,
        @SerialName("staff_notes") val staffNotes: String? = null,
    )

    @Serializable
    data class AppointmentListResponse(val data: List<AppointmentDto>)

    @Serializable
    data class AppointmentResponse(val data: AppointmentDto)

    @Serializable
    data class CreateAppointmentRequest(
        @SerialName("visit_reason_id") val visitReasonId: Int,
        @SerialName("scheduled_at") val scheduledAt: String,
        @SerialName("contact_notes") val contactNotes: String? = null,
    )

    @Serializable
    data class ValidationErrorBody(
        val message: String,
        val errors: Map<String, List<String>> = emptyMap(),
    )
}
