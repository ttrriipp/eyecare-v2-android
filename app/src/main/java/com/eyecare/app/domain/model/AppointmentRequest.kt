package com.eyecare.app.domain.model

enum class AppointmentRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    EXPIRED,
    UNKNOWN;

    companion object {
        fun fromRaw(value: String): AppointmentRequestStatus = when (value.lowercase()) {
            "pending" -> PENDING
            "accepted" -> ACCEPTED
            "rejected" -> REJECTED
            "cancelled" -> CANCELLED
            "expired" -> EXPIRED
            else -> UNKNOWN
        }
    }

    val isTerminal: Boolean
        get() = this in setOf(ACCEPTED, REJECTED, CANCELLED, EXPIRED)

    val isCancellable: Boolean
        get() = this == PENDING
}

data class AppointmentRequestTypeSummary(
    val id: Int,
    val name: String,
    val durationMinutes: Int,
)

data class AppointmentRequest(
    val id: Int,
    val requestNumber: String,
    val status: AppointmentRequestStatus,
    val patientId: Int?,
    val appointmentType: AppointmentRequestTypeSummary?,
    val scheduledAt: String,
    val alternativeScheduledTimes: List<String>,
    val provisionalDurationMinutes: Int?,
    val reasonForVisit: String,
    val referringSource: String?,
    val timePreferencesAreReserved: Boolean,
    val expiresAt: String?,
    val cancelledAt: String?,
    val rejectionReason: String?,
    val createdAt: String,
    val appointmentId: Int?,
)

enum class AppointmentRequestGender(
    val apiValue: String,
    val label: String,
) {
    MALE("male", "Male"),
    FEMALE("female", "Female"),
    OTHER("other", "Other")
}

/**
 * Identity supplied when an account has no linked clinic Patient record yet.
 * The verified phone is copied from the account and cannot be edited by the user.
 * The backend rejects this object for linked accounts.
 */
data class AppointmentRequestIdentity(
    val phone: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val middleName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: String? = null,
    val gender: AppointmentRequestGender? = null,
    val occupation: String? = null,
    val address: String? = null,
)

fun PatientAccount.toAppointmentRequestIdentityOrNull(): AppointmentRequestIdentity? {
    if (linkStatus == PatientLinkStatus.LINKED) return null

    val identity = AppointmentRequestIdentity(
        phone = phone?.trim()?.takeIf(String::isNotEmpty),
        email = email?.trim()?.takeIf(String::isNotEmpty),
        firstName = firstName?.trim()?.takeIf(String::isNotEmpty),
        middleName = middleName?.trim()?.takeIf(String::isNotEmpty),
        lastName = lastName?.trim()?.takeIf(String::isNotEmpty),
        dateOfBirth = dateOfBirth?.trim()?.takeIf(String::isNotEmpty),
    )
    return identity.takeIf {
        it.phone != null ||
            it.email != null ||
            it.firstName != null ||
            it.middleName != null ||
            it.lastName != null ||
            it.dateOfBirth != null
    }
}

/**
 * The session variant is an access-policy detail; the account's link status is the source of
 * truth for whether the appointment-request identity step is needed.
 */
fun PatientAccount.requiresAppointmentRequestIdentity(): Boolean =
    linkStatus != PatientLinkStatus.LINKED

data class AppointmentRequestAvailability(
    val date: String,
    val timezone: String,
    val intervalMinutes: Int,
    val slotDurationMinutes: Int,
    val visitDurationMinutes: Int?,
    val appointmentTypeId: Int?,
    val dayStatus: String,
    val generatedAt: String,
    val slots: List<AvailabilitySlot>,
)

data class AvailabilitySlot(
    val startsAt: String,
    val endsAt: String,
    val available: Boolean,
    val reason: String?,
)
