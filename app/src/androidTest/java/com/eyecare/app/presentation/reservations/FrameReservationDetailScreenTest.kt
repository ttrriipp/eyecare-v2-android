package com.eyecare.app.presentation.reservations

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.FrameReservationItem
import com.eyecare.app.domain.model.ReservationAppointment
import com.eyecare.app.domain.repository.FrameReservationRepository
import com.eyecare.app.ui.theme.EyecareTheme
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test

class FrameReservationDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addAndRemoveFrameButtons_dispatchTheirActions() {
        val repository = RecordingReservationRepository(testReservation())
        val viewModel = FrameReservationDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("reservationId" to 1)),
        )
        var addFrameClicks = 0

        composeRule.setContent {
            EyecareTheme {
                FrameReservationDetailScreen(
                    onBack = {},
                    onViewAppointment = {},
                    onViewFrame = {},
                    onAddFrame = { addFrameClicks++ },
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("Add frame").assertIsDisplayed().performClick()
        composeRule
            .onNodeWithContentDescription("Remove Classic Rectangle")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithText("Remove frame?").assertIsDisplayed()
        composeRule.onNodeWithText("Remove frame").performClick()

        composeRule.runOnIdle {
            check(addFrameClicks == 1)
            check(repository.removedItemId == 11)
        }
    }

    @Test
    fun removingAFrame_requiresConfirmation() {
        val repository = RecordingReservationRepository(testReservation())
        val viewModel = FrameReservationDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("reservationId" to 1)),
        )

        composeRule.setContent {
            EyecareTheme {
                FrameReservationDetailScreen(
                    onBack = {},
                    onViewAppointment = {},
                    onViewFrame = {},
                    onAddFrame = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Remove Classic Rectangle")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithText("Remove frame?").assertIsDisplayed()
        composeRule.runOnIdle { check(repository.removedItemId == null) }

        composeRule.onNodeWithText("Keep frame").performClick()
        composeRule.runOnIdle { check(repository.removedItemId == null) }
    }

    @Test
    fun lastReservedFrame_scrollsAboveFixedActions() {
        val baseReservation = testReservation()
        val reservation = baseReservation.copy(
            items = baseReservation.items + baseReservation.items.single().copy(
                id = 12,
                frameId = 8,
                frameName = "Round Metal Frame",
            ),
        )
        val viewModel = FrameReservationDetailViewModel(
            repository = RecordingReservationRepository(reservation),
            savedStateHandle = SavedStateHandle(mapOf("reservationId" to 1)),
        )

        composeRule.setContent {
            EyecareTheme {
                FrameReservationDetailScreen(
                    onBack = {},
                    onViewAppointment = {},
                    onViewFrame = {},
                    onAddFrame = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithTag("reservation-bottom-spacer").performScrollTo()
        composeRule.runOnIdle {
            val frameBounds = composeRule
                .onNodeWithTag("reservation-frame-card-12")
                .fetchSemanticsNode()
                .boundsInRoot
            val actionBarBounds = composeRule
                .onNodeWithTag("reservation-action-bar")
                .fetchSemanticsNode()
                .boundsInRoot

            check(frameBounds.bottom <= actionBarBounds.top) {
                "Last frame bottom ${frameBounds.bottom} is behind action bar top ${actionBarBounds.top}"
            }
        }
    }

    private class RecordingReservationRepository(
        private val reservation: FrameReservation,
    ) : FrameReservationRepository {
        var removedItemId: Int? = null

        override suspend fun getReservations(): Result<List<FrameReservation>> =
            Result.success(listOf(reservation))

        override suspend fun createReservation(
            variantIds: List<Int>,
            appointmentId: Int,
        ): Result<FrameReservation> = Result.failure(UnsupportedOperationException())

        override suspend fun deleteReservation(reservationId: Int): Result<Unit> =
            Result.failure(UnsupportedOperationException())

        override suspend fun addItem(
            reservationId: Int,
            variantId: Int,
        ): Result<FrameReservation> = Result.failure(UnsupportedOperationException())

        override suspend fun removeItem(
            reservationId: Int,
            itemId: Int,
        ): Result<FrameReservation?> {
            removedItemId = itemId
            return Result.success(reservation.copy(items = emptyList()))
        }
    }

    private fun testReservation() = FrameReservation(
        id = 1,
        appointment = ReservationAppointment(
            id = 42,
            appointmentNumber = "APT-2026-000042",
            status = AppointmentStatus.SCHEDULED,
            scheduledAt = "2026-08-30T09:00:00+08:00",
            durationMinutes = 30,
        ),
        isHeld = false,
        expiresAt = null,
        createdAt = "2026-07-27T10:00:00+08:00",
        items = listOf(
            FrameReservationItem(
                id = 11,
                productVariantId = 42,
                variantName = "Black / 52mm",
                variantSku = "RB-CR-BLK-52",
                price = BigDecimal("4500.00"),
                compareAtPrice = null,
                frameId = 7,
                frameName = "Classic Rectangle",
                frameBrand = "Ray-Ban",
                frameCategory = "Full Rim",
                frameDescription = "Timeless frame design",
                attributes = mapOf("color" to "black"),
                images = emptyList(),
            ),
        ),
    )
}
