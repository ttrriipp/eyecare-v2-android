package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.dto.AppointmentV1Dtos
import com.eyecare.app.domain.model.AppointmentCancellation
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AssignedOptometrist
import com.eyecare.app.domain.model.VisitRating

internal fun AppointmentV1Dtos.AppointmentDto.toDomain() = AppointmentV1(
    id = id,
    appointmentNumber = appointmentNumber,
    appointmentType = appointmentType,
    durationMinutes = durationMinutes,
    referringSource = referringSource,
    status = AppointmentStatus.from(status),
    scheduledAt = scheduledAt,
    contactNotes = contactNotes,
    reasonForVisit = reasonForVisit,
    lastRescheduleReason = lastRescheduleReason,
    source = source,
    assignedOptometrist = assignedOptometrist?.let { AssignedOptometrist(name = it.name) },
    isRateable = isRateable,
    visitRating = rating?.toDomain(),
    cancellation = cancellation?.let {
        AppointmentCancellation(
            reasonCategory = it.reasonCategory,
            reasonDetails = it.reasonDetails,
        )
    },
)

internal fun AppointmentV1Dtos.VisitRatingDto.toDomain() = VisitRating(
    rating = rating,
    comment = comment,
    createdAt = createdAt,
    id = id,
    revisionNumber = revisionNumber,
)

internal fun AppointmentV1Dtos.AppointmentAvailabilityDto.toDomain() = com.eyecare.app.domain.model.AppointmentAvailability(
    date = date,
    timezone = timezone,
    intervalMinutes = intervalMinutes,
    visitReasonId = appointmentTypeId,
    visitDurationMinutes = visitDurationMinutes,
    optometristId = optometristId,
    appointmentId = appointmentId,
    dayStatus = dayStatus,
    generatedAt = generatedAt,
    slots = slots.map { slot ->
        com.eyecare.app.domain.model.AppointmentSlot(
            startsAt = slot.startsAt,
            endsAt = slot.endsAt,
            available = slot.available,
            reason = slot.reason,
        )
    },
)
