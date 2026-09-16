package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.dto.AppointmentRequestDto
import com.eyecare.app.data.remote.dto.AppointmentRequestTypeSummaryDto
import com.eyecare.app.data.remote.dto.AppointmentTypeDto
import com.eyecare.app.data.remote.dto.AvailabilitySlotDto
import com.eyecare.app.data.remote.dto.BookingEligibilityDto
import com.eyecare.app.data.remote.dto.AppointmentRequestAvailabilityData
import com.eyecare.app.data.remote.dto.VisitReasonPresetDto
import com.eyecare.app.domain.model.AppointmentBookingBlockingReason
import com.eyecare.app.domain.model.AppointmentBookingEligibility
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentRequestType
import com.eyecare.app.domain.model.AppointmentRequestTypeSummary
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.model.VisitReasonPreset

internal fun AppointmentTypeDto.toDomain() = AppointmentType(
    id = id,
    name = name,
    description = description,
    durationMinutes = durationMinutes,
    requiresReferral = requiresReferral,
    visitReasonPresets = visitReasonPresets.map { it.toDomain() },
)

internal fun VisitReasonPresetDto.toDomain() = VisitReasonPreset(
    id = id,
    label = label,
)

internal fun BookingEligibilityDto.toDomain() =
    AppointmentBookingEligibility(
        canSubmitNewRequest = canSubmitNewRequest,
        blockingReason = AppointmentBookingBlockingReason.fromRaw(blockingReason),
        activeRequestId = activeRequestId,
        appointmentId = appointmentId,
        canRequestRebooking = canRequestRebooking,
    )

internal fun AppointmentRequestDto.toDomain() = AppointmentRequest(
    id = id,
    requestNumber = requestNumber,
    status = AppointmentRequestStatus.fromRaw(status),
    requestType = AppointmentRequestType.fromRaw(requestType),
    patientId = patientId,
    appointmentType = appointmentType?.toDomain(),
    scheduledAt = scheduledAt,
    originalScheduledAt = originalScheduledAt,
    selectedScheduledAt = selectedScheduledAt,
    alternativeScheduledTimes = alternativeScheduledTimes ?: emptyList(),
    provisionalDurationMinutes = provisionalDurationMinutes,
    reasonForVisit = reasonForVisit,
    referringSource = referringSource,
    timePreferencesAreReserved = timePreferencesAreReserved,
    expiresAt = expiresAt,
    cancelledAt = cancelledAt,
    rejectionReason = rejectionReason,
    cancellationReason = cancellationReason,
    createdAt = createdAt,
    appointmentId = appointment?.id,
)

internal fun AppointmentRequestTypeSummaryDto.toDomain() = AppointmentRequestTypeSummary(
    id = id,
    name = name,
    durationMinutes = durationMinutes,
)

internal fun AppointmentRequestAvailabilityData.toDomain() = AppointmentRequestAvailability(
    date = date,
    timezone = timezone,
    intervalMinutes = intervalMinutes,
    slotDurationMinutes = slotDurationMinutes,
    visitDurationMinutes = visitDurationMinutes,
    appointmentTypeId = appointmentTypeId,
    dayStatus = dayStatus,
    generatedAt = generatedAt,
    slots = slots.map { it.toDomain() },
)

internal fun AvailabilitySlotDto.toDomain() = AvailabilitySlot(
    startsAt = startsAt,
    endsAt = endsAt,
    available = available,
    reason = reason,
)
