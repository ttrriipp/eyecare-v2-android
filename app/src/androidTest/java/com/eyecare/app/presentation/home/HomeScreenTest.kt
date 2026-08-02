package com.eyecare.app.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AssignedOptometrist
import com.eyecare.app.domain.model.EyeMeasurement
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.model.PrescriptionMeasurementGroup
import com.eyecare.app.domain.model.PrescriptionMeasurements
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun upcomingAppointment_isDisplayed() {
        val state = successState(
            nextAppointment = AppointmentV1(
                id = 1,
                appointmentNumber = "APT-001",
                appointmentType = "New Patient",
                durationMinutes = 30,
                referringSource = null,
                status = AppointmentStatus.SCHEDULED,
                scheduledAt = "2030-07-16T10:00:00+08:00",
                contactNotes = null,
                reasonForVisit = null,
                lastRescheduleReason = null,
                source = "mobile",
                assignedOptometrist = AssignedOptometrist("Dr. Santos"),
            ),
        )

        composeRule.setContent {
            EyecareTheme {
                HomeContent(state = state)
            }
        }

        composeRule.onNodeWithText("YOUR NEXT VISIT").assertIsDisplayed()
        composeRule.onNodeWithText("Scheduled").assertIsDisplayed()
    }

    @Test
    fun noAppointment_offersBooking() {
        composeRule.setContent {
            EyecareTheme {
                HomeContent(state = successState())
            }
        }

        composeRule.onNodeWithText("Book an appointment").assertIsDisplayed()
    }

    @Test
    fun currentPrescription_isDisplayed() {
        val state = successState().copy(
            currentPrescription = Prescription(
                id = 1,
                appointmentId = 1,
                previousPrescriptionId = null,
                isCurrent = true,
                date = "2026-07-27",
                measurements = PrescriptionMeasurements(
                    main = PrescriptionMeasurementGroup(
                        od = EyeMeasurement(null, "-2.00", "-0.50"),
                        os = EyeMeasurement(null, "-1.75", "-0.25"),
                    ),
                    add = PrescriptionMeasurementGroup(
                        od = EyeMeasurement(null, null, null),
                        os = EyeMeasurement(null, null, null),
                    ),
                ),
                remarks = null,
            ),
        )

        composeRule.setContent {
            EyecareTheme {
                HomeContent(state = state)
            }
        }

        composeRule.onNodeWithText("Current prescription").assertIsDisplayed()
        composeRule.onNodeWithText("View details").assertIsDisplayed()
    }

    @Test
    fun loadingState_hasAccessibleContentShape() {
        composeRule.setContent {
            EyecareTheme {
                HomeLoadingContent()
            }
        }

        composeRule.onNodeWithContentDescription("Loading home").assertIsDisplayed()
    }

    private fun successState(
        nextAppointment: AppointmentV1? = null,
        currentPrescription: Prescription? = null,
    ) = HomeUiState.Success(
        nextAppointment = nextAppointment,
        currentPrescription = currentPrescription,
        featuredFrames = emptyList(),
    )
}
