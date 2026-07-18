package com.eyecare.app.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.model.Product
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun upcomingAppointment_isTheFirstCareAction() {
        val state = successState(
            nextAppointment = Appointment(
                id = 1,
                visitReason = "comprehensive_eye_exam",
                status = AppointmentStatus.CONFIRMED,
                scheduledAt = "2030-07-16T10:00:00+08:00",
                contactNotes = null,
                staffNotes = null,
            ),
        )

        composeRule.setContent {
            EyecareTheme {
                HomeContent(state = state)
            }
        }

        composeRule.onNodeWithText("YOUR NEXT VISIT").assertIsDisplayed()
        composeRule.onNodeWithText("Comprehensive Eye Exam").assertIsDisplayed()
        composeRule.onNodeWithText("Confirmed").assertIsDisplayed()
        composeRule.onNodeWithText("Jul 16, 2030").assertIsDisplayed()
        composeRule.onNodeWithText("10:00 AM").assertIsDisplayed()
        composeRule.onNodeWithText("View appointments").assertIsDisplayed()
    }

    @Test
    fun noAppointment_offersBookingInTheSameDominantPosition() {
        composeRule.setContent {
            EyecareTheme {
                HomeContent(state = successState())
            }
        }

        composeRule.onNodeWithText("PLAN YOUR NEXT VISIT").assertIsDisplayed()
        composeRule.onNodeWithText("Book an appointment").assertIsDisplayed()
    }

    @Test
    fun careUpdates_renderOnlyWhenRelevant() {
        val state = successState().copy(
            activeOrder = Order(
                id = 8,
                orderNumber = "ORD-008",
                appointmentId = null,
                billingId = null,
                isNonPrescription = false,
                status = OrderStatus.PROCESSING,
                subtotal = "165.00",
                totalAmount = "165.00",
                items = emptyList(),
                createdAt = "2030-07-01T10:00:00+08:00",
            ),
            expiringPrescription = Prescription(
                id = 3,
                appointmentId = 1,
                odSphere = null,
                odCylinder = null,
                odAxis = null,
                odAdd = null,
                osSphere = null,
                osCylinder = null,
                osAxis = null,
                osAdd = null,
                pd = null,
                prescribedAt = "2029-07-30",
                expiresAt = "2030-07-30",
                notes = null,
            ),
        )

        composeRule.setContent {
            EyecareTheme {
                HomeContent(state = state)
            }
        }

        composeRule.onNodeWithText("Order Status").assertIsDisplayed()
        composeRule.onNodeWithText("Prescription Expiring Soon").assertIsDisplayed()
        composeRule.onNodeWithText("ORD-008").assertIsDisplayed()
    }

    @Test
    fun productShelves_omitEmptyGroupsAndKeepExistingNavigationCallbacks() {
        var selectedProductId: Int? = null
        var catalogOpened = false
        val state = successState().copy(
            featuredFrames = listOf(product(1, "Avery", "frame", "Eyeglasses")),
            eyeCareEssentials = listOf(product(2, "Comfort Drops", "general", "Eye Care")),
        )

        composeRule.setContent {
            EyecareTheme {
                HomeContent(
                    state = state,
                    onNavigateToCatalog = { catalogOpened = true },
                    onNavigateToProductDetail = { selectedProductId = it },
                )
            }
        }

        composeRule.onNodeWithText("Featured frames").assertIsDisplayed()
        composeRule.onNodeWithText("Accessories").assertDoesNotExist()
        composeRule.onNodeWithText("Eye-care essentials").assertIsDisplayed()
        composeRule.onNodeWithText("Avery").performClick()
        composeRule.runOnIdle { check(selectedProductId == 1) }
        composeRule.onAllNodesWithText("See all")[0].performClick()
        composeRule.runOnIdle { check(catalogOpened) }
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

    private fun successState(nextAppointment: Appointment? = null) = HomeUiState.Success(
        nextAppointment = nextAppointment,
        activeOrder = null,
        expiringPrescription = null,
        featuredFrames = emptyList(),
        accessories = emptyList(),
        eyeCareEssentials = emptyList(),
    )

    private fun product(
        id: Int,
        name: String,
        productType: String,
        category: String,
    ) = Product(
        id = id,
        name = name,
        slug = name.lowercase().replace(" ", "-"),
        description = null,
        productType = productType,
        brand = "Padilla Optical",
        category = category,
        variants = emptyList(),
        images = emptyList(),
    )
}
