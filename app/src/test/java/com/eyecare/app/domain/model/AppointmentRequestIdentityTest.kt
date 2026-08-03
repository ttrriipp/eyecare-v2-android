package com.eyecare.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AppointmentRequestIdentityTest {

    @Test
    fun `unlinked account exposes structured identity for appointment request`() {
        val identity = account(linkStatus = PatientLinkStatus.UNLINKED)
            .toAppointmentRequestIdentityOrNull()

        assertEquals(
            AppointmentRequestIdentity(
                phone = "+639171234567",
                email = "alex@example.com",
                firstName = "Alex",
                middleName = "M",
                lastName = "Rivera",
                dateOfBirth = "1990-05-15",
            ),
            identity,
        )
    }

    @Test
    fun `linked account never sends appointment request identity`() {
        assertNull(
            account(linkStatus = PatientLinkStatus.LINKED)
                .toAppointmentRequestIdentityOrNull(),
        )
    }

    @Test
    fun `account without structured identity omits optional payload`() {
        assertNull(
            account(
                firstName = null,
                middleName = null,
                lastName = null,
                dateOfBirth = null,
                email = null,
                phone = null,
            ).toAppointmentRequestIdentityOrNull(),
        )
    }

    private fun account(
        firstName: String? = "Alex",
        middleName: String? = "M",
        lastName: String? = "Rivera",
        dateOfBirth: String? = "1990-05-15",
        email: String? = "alex@example.com",
        phone: String? = "+639171234567",
        linkStatus: PatientLinkStatus = PatientLinkStatus.UNLINKED,
    ) = PatientAccount(
        id = 1,
        name = "Alex Rivera",
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        email = email,
        phone = phone,
        role = "patient",
        dateOfBirth = dateOfBirth,
        linkStatus = linkStatus,
        privacyPolicyVersion = null,
        privacyAcceptedAt = null,
        linkedPatient = null,
    )
}
