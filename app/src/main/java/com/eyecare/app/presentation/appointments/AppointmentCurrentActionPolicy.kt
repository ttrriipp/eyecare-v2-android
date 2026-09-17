package com.eyecare.app.presentation.appointments

import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AuthApiCodes

/**
 * Patient-facing affordances for an appointment. The backend remains authoritative; these
 * values only keep the current journey and deep appointment detail from drifting apart.
 */
internal data class AppointmentCurrentActionPolicy(
    val canCancel: Boolean,
    val canRequestDifferentTime: Boolean,
    val sameDayCancellationBlocked: Boolean,
)

internal fun appointmentCurrentActionPolicy(
    appointment: AppointmentV1,
    hasPendingReschedule: Boolean,
): AppointmentCurrentActionPolicy {
    val sameDayCancellationBlocked = appointment.status.canCancel &&
        isSameDayInClinic(appointment.scheduledAt)
    return AppointmentCurrentActionPolicy(
        canCancel = appointment.status.canCancel && !sameDayCancellationBlocked,
        canRequestDifferentTime = appointment.status.canReschedule && !hasPendingReschedule,
        sameDayCancellationBlocked = sameDayCancellationBlocked,
    )
}

internal fun appointmentDayAvailabilityVerdict(
    availability: AppointmentAvailability,
): DayAvailability = when {
    !availability.dayStatus.equals("open", ignoreCase = true) -> DayAvailability.CLOSED
    availability.slots.none { it.available } -> DayAvailability.FULL
    else -> DayAvailability.OPEN
}

internal fun isAppointmentSlotUnavailableError(error: Throwable): Boolean {
    val apiError = error as? ApiDomainError
    return apiError?.code == "SLOT_UNAVAILABLE" ||
        apiError?.fieldErrors?.keys?.any {
            it == "scheduled_at" || it.endsWith(".scheduled_at")
        } == true ||
        error is AppointmentError.ValidationError
}

internal fun patientSafeRescheduleError(error: Throwable): String = when {
    isAppointmentSlotUnavailableError(error) ->
        "That time is no longer available. Choose another time."
    (error as? ApiDomainError)?.code == AuthApiCodes.ACTIVE_REQUEST_LIMIT_REACHED ->
        "You already have an active appointment request. Cancel it or wait for the clinic to respond."
    (error as? ApiDomainError)?.fieldErrors?.keys?.any {
        it == "appointment_id" || it.endsWith(".appointment_id")
    } == true ->
        "This appointment can no longer be rescheduled. Refresh and try again."
    else -> "We couldn't submit this reschedule request. Try again."
}
