package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object AppointmentDtos {

    @Serializable
    data class AssignedProviderDto(
        val id: Int,
        val name: String,
    )

    @Serializable
    data class AppointmentDto(
        val id: Int,
        @SerialName("visit_reason") val visitReason: String,
        val status: String,
        @SerialName("scheduled_at") val scheduledAt: String,
        @SerialName("contact_notes") val contactNotes: String? = null,
        @SerialName("staff_notes") val staffNotes: String? = null,
        @SerialName("appointment_number") val appointmentNumber: String? = null,
        val source: String? = null,
        @SerialName("assigned_optometrist") val assignedOptometrist: AssignedProviderDto? = null,
        @SerialName("assigned_staff") val legacyAssignedStaff: AssignedProviderDto? = null,
    )

    @Serializable
    data class AppointmentListResponse(val data: List<AppointmentDto>)

    @Serializable
    data class AppointmentResponse(val data: AppointmentDto)

    @Serializable
    data class AvailabilitySlotDto(
        @SerialName("starts_at") val startsAt: String,
        @SerialName("ends_at") val endsAt: String,
        val available: Boolean,
        val reason: String? = null,
    )

    @Serializable
    data class AppointmentAvailabilityDto(
        val date: String,
        val timezone: String,
        @SerialName("interval_minutes") val intervalMinutes: Int,
        @SerialName("visit_reason_id") val visitReasonId: Int,
        @SerialName("visit_duration_minutes") val visitDurationMinutes: Int,
        @SerialName("optometrist_id") val optometristId: Int? = null,
        @SerialName("appointment_id") val appointmentId: Int? = null,
        @SerialName("day_status") val dayStatus: String,
        @SerialName("generated_at") val generatedAt: String,
        val slots: List<AvailabilitySlotDto>,
    )

    @Serializable
    data class AppointmentAvailabilityResponse(val data: AppointmentAvailabilityDto)

    @Serializable
    data class CreateAppointmentRequest(
        @SerialName("visit_reason_id") val visitReasonId: Int,
        @SerialName("scheduled_at") val scheduledAt: String,
        @SerialName("contact_notes") val contactNotes: String? = null,
    )

    @Serializable
    data class RescheduleRequest(
        @SerialName("scheduled_at") val scheduledAt: String,
    )

    @Serializable
    data class VisitReasonDto(
        val id: Int,
        val name: String,
        @SerialName("duration_minutes") val durationMinutes: Int,
    )

    @Serializable
    data class VisitReasonListResponse(val data: List<VisitReasonDto>)

    @Serializable
    data class ValidationErrorBody(
        val message: String,
        val code: String? = null,
        val errors: Map<String, List<String>> = emptyMap(),
    )
}
