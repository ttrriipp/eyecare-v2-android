package com.eyecare.app.presentation.appointments

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.VisitRating
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppointmentHistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rateableHistoryRow_showsRatingAction() {
        var ratingClicked = false
        composeRule.setContent {
            EyecareTheme {
                AppointmentHistoryScreen(
                    uiState = AppointmentHistoryUiState.Content(
                        appointments = listOf(
                            AppointmentV1(
                                id = 1,
                                appointmentNumber = "APT-1",
                                appointmentType = "Eye examination",
                                durationMinutes = 30,
                                referringSource = null,
                                status = AppointmentStatus.FULFILLED,
                                scheduledAt = "2026-09-10T09:00:00+08:00",
                                contactNotes = null,
                                reasonForVisit = "Annual checkup",
                                lastRescheduleReason = null,
                                source = "mobile",
                                assignedOptometrist = null,
                                isRateable = true,
                            ),
                        ),
                        hasMorePages = false,
                    ),
                    onRetry = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onBack = {},
                    onNavigateToDetail = {},
                    onShowRating = { ratingClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Rate this visit").assertIsDisplayed().performClick()
        assertTrue(ratingClicked)
    }

    @Test
    fun ratedHistoryRow_keepsRatingUpdateAction() {
        var ratingClicked = false
        composeRule.setContent {
            EyecareTheme {
                AppointmentHistoryScreen(
                    uiState = AppointmentHistoryUiState.Content(
                        appointments = listOf(
                            AppointmentV1(
                                id = 1,
                                appointmentNumber = "APT-1",
                                appointmentType = "Eye examination",
                                durationMinutes = 30,
                                referringSource = null,
                                status = AppointmentStatus.FULFILLED,
                                scheduledAt = "2026-09-10T09:00:00+08:00",
                                contactNotes = null,
                                reasonForVisit = "Annual checkup",
                                lastRescheduleReason = null,
                                source = "mobile",
                                assignedOptometrist = null,
                                isRateable = true,
                                visitRating = VisitRating(
                                    rating = 4,
                                    comment = "Helpful",
                                    createdAt = "2026-09-11T09:00:00+08:00",
                                ),
                            ),
                        ),
                        hasMorePages = false,
                    ),
                    onRetry = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onBack = {},
                    onNavigateToDetail = {},
                    onShowRating = { ratingClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Update rating").assertIsDisplayed().performClick()
        assertTrue(ratingClicked)
    }
}
