package com.eyecare.app.presentation.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PatientFeatureIntentTest {

    @Test
    fun `route mapping preserves feature arguments`() {
        val intents = listOf(
            PatientFeatureIntent.AppointmentsTab to Appointments,
            PatientFeatureIntent.FramesTab to Frames,
            PatientFeatureIntent.RequestAppointment to RequestAppointment,
            PatientFeatureIntent.AppointmentDetail(42) to AppointmentDetail(42),
            PatientFeatureIntent.FrameDetail(7) to FrameDetail(7),
            PatientFeatureIntent.ArTryOn(7, 3) to ArTryOn(7, 3),
            PatientFeatureIntent.PrescriptionDetail(9) to PrescriptionDetail(9),
        )

        intents.forEach { (intent, route) ->
            assertEquals(intent, patientFeatureIntentFrom(route))
            assertEquals(route, intent.toRoute())
        }
    }

    @Test
    fun `unrecognized route does not create a pending feature intent`() {
        assertNull(patientFeatureIntentFrom(Profile))
    }

    @Test
    fun `feature intent exposes a user-facing label`() {
        assertEquals("appointments", PatientFeatureIntent.AppointmentDetail(42).label)
        assertEquals("frames", PatientFeatureIntent.FramesTab.label)
        assertEquals("appointments", PatientFeatureIntent.RequestAppointment.label)
    }
}
