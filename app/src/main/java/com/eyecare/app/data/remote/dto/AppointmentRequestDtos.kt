package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentTypeDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("requires_referral") val requiresReferral: Boolean = false,
    @SerialName("visit_reason_presets") val visitReasonPresets: List<VisitReasonPresetDto> = emptyList(),
)

@Serializable
data class VisitReasonPresetDto(
    val id: Int,
    val label: String,
)

@Serializable
data class AppointmentTypeListResponse(
    val data: List<AppointmentTypeDto>,
)

@Serializable
data class AppointmentRequestAvailabilityResponse(
    val data: AppointmentRequestAvailabilityData,
)

@Serializable
data class AppointmentRequestAvailabilityData(
    val date: String,
    val timezone: String,
    @SerialName("interval_minutes") val intervalMinutes: Int,
    @SerialName("slot_duration_minutes") val slotDurationMinutes: Int,
    @SerialName("visit_duration_minutes") val visitDurationMinutes: Int? = null,
    @SerialName("appointment_type_id") val appointmentTypeId: Int? = null,
    @SerialName("day_status") val dayStatus: String,
    @SerialName("generated_at") val generatedAt: String,
    val slots: List<AvailabilitySlotDto>,
)

@Serializable
data class AvailabilitySlotDto(
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val available: Boolean,
    val reason: String? = null,
)

@Serializable
data class AppointmentRequestListResponse(
    val data: List<AppointmentRequestDto>,
    val links: PaginationLinks? = null,
    val meta: PaginationMeta? = null,
)

@Serializable
data class AppointmentRequestResponse(
    val data: AppointmentRequestDto,
)

@Serializable
data class AppointmentRequestTypeSummaryDto(
    val id: Int,
    val name: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
)

@Serializable
data class AppointmentRequestDto(
    val id: Int,
    @SerialName("request_number") val requestNumber: String,
    val status: String,
    @SerialName("patient_id") val patientId: Int? = null,
    @SerialName("appointment_type") val appointmentType: AppointmentRequestTypeSummaryDto? = null,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("alternative_scheduled_times") val alternativeScheduledTimes: List<String>? = null,
    @SerialName("provisional_duration_minutes") val provisionalDurationMinutes: Int? = null,
    @SerialName("reason_for_visit") val reasonForVisit: String,
    @SerialName("referring_source") val referringSource: String? = null,
    @SerialName("time_preferences_are_reserved") val timePreferencesAreReserved: Boolean = false,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String,
    val appointment: AppointmentReferenceDto? = null,
)

@Serializable
data class AppointmentReferenceDto(
    val id: Int? = null,
)

@Serializable
data class CreateAppointmentRequest(
    @SerialName("appointment_type_id") val appointmentTypeId: Int,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("alternative_scheduled_times") val alternativeScheduledTimes: List<String>? = null,
    @SerialName("reason_for_visit") val reasonForVisit: String,
    @SerialName("referring_source") val referringSource: String? = null,
    val identity: AppointmentRequestIdentityDto? = null,
)

@Serializable
data class AppointmentRequestIdentityDto(
    val phone: String? = null,
    val email: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("middle_name") val middleName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    val occupation: String? = null,
    val address: String? = null,
)
