package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object AppointmentV1Dtos {

    @Serializable
    data class AppointmentTypeDto(
        val id: Int,
        val name: String,
        @SerialName("duration_minutes") val durationMinutes: Int,
        @SerialName("requires_referral") val requiresReferral: Boolean,
    )

    @Serializable
    data class AppointmentTypeListResponse(val data: List<AppointmentTypeDto>)

    @Serializable
    data class AssignedOptometristDto(
        val name: String,
    )

    @Serializable
    data class AppointmentDto(
        val id: Int,
        @SerialName("appointment_number") val appointmentNumber: String? = null,
        @SerialName("appointment_type") val appointmentType: String,
        @SerialName("duration_minutes") val durationMinutes: Int,
        @SerialName("referring_source") val referringSource: String? = null,
        val status: String,
        @SerialName("scheduled_at") val scheduledAt: String,
        @SerialName("contact_notes") val contactNotes: String? = null,
        @SerialName("last_reschedule_reason") val lastRescheduleReason: String? = null,
        val source: String? = null,
        @SerialName("assigned_optometrist") val assignedOptometrist: AssignedOptometristDto? = null,
    )

    @Serializable
    data class AppointmentListResponse(
        val data: List<AppointmentDto>,
        val links: PaginationLinks? = null,
        val meta: PaginationMeta? = null,
    )

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
        @SerialName("appointment_type_id") val appointmentTypeId: Int,
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
        @SerialName("appointment_type_id") val appointmentTypeId: Int,
        @SerialName("scheduled_at") val scheduledAt: String,
        @SerialName("contact_notes") val contactNotes: String? = null,
        @SerialName("referring_source") val referringSource: String? = null,
    )

    @Serializable
    data class RescheduleRequest(
        @SerialName("scheduled_at") val scheduledAt: String,
    )
}
