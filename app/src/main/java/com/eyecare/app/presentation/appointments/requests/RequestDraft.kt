package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import java.io.Serializable

/**
 * Everything the patient typed during a request, in a form `SavedStateHandle` can put in a
 * bundle. Only primitives live here on purpose: the domain models stay free of serialization
 * concerns, and a restored draft can never smuggle a stale [com.eyecare.app.domain.model.AppointmentType]
 * or slot back into the flow — those are always re-fetched from the clinic on restore.
 */
data class RequestDraft(
    val appointmentTypeId: Int? = null,
    val reason: String = "",
    val referringSource: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val middleName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: String? = null,
    val genderApiValue: String? = null,
    val occupation: String? = null,
    val address: String? = null,
) : Serializable {

    /** Null when nothing identity-shaped has been entered yet, so callers can fall back to a seed. */
    fun toIdentityOrNull(): AppointmentRequestIdentity? {
        val anyPresent = listOfNotNull(
            phone, email, firstName, middleName, lastName, dateOfBirth, occupation, address,
        ).any { it.isNotBlank() } || genderApiValue != null
        if (!anyPresent) return null
        return AppointmentRequestIdentity(
            phone = phone?.ifBlank { null },
            email = email?.ifBlank { null },
            firstName = firstName?.ifBlank { null },
            middleName = middleName?.ifBlank { null },
            lastName = lastName?.ifBlank { null },
            dateOfBirth = dateOfBirth?.ifBlank { null },
            gender = genderApiValue?.let { value ->
                AppointmentRequestGender.entries.firstOrNull { it.apiValue == value }
            },
            occupation = occupation?.ifBlank { null },
            address = address?.ifBlank { null },
        )
    }

    fun withIdentity(identity: AppointmentRequestIdentity?): RequestDraft = copy(
        phone = identity?.phone,
        email = identity?.email,
        firstName = identity?.firstName,
        middleName = identity?.middleName,
        lastName = identity?.lastName,
        dateOfBirth = identity?.dateOfBirth,
        genderApiValue = identity?.gender?.apiValue,
        occupation = identity?.occupation,
        address = identity?.address,
    )

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
