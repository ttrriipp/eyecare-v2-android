package com.eyecare.app.presentation.eyewear

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.domain.model.FulfillmentMode
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderItem
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.model.PaymentStatus
import com.eyecare.app.domain.model.PaymentSummary
import com.eyecare.app.ui.theme.EyecareTheme
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test

class OpticalOrderDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun orderDetail_showsFrameImageAndDoesNotShowClinicMessageAction() {
        composeRule.setContent {
            EyecareTheme {
                OrderDetailContent(
                    order = createOrder(),
                    onRateItem = {},
                    ratingsEnabled = false,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Everyday Frame – Tortoise image").assertIsDisplayed()
        composeRule.onNodeWithText("Message the clinic about your balance").assertDoesNotExist()
    }

    private fun createOrder() = OpticalOrder(
        id = 1,
        orderNumber = "ORD-2026-000002",
        status = OpticalOrderStatus.IN_PROGRESS,
        fulfillmentMode = FulfillmentMode.PREPARED,
        totalAmount = BigDecimal("3200.00"),
        startedAt = "2026-09-04T10:00:00Z",
        readyAt = null,
        dispensedAt = null,
        cancelledAt = null,
        createdAt = "2026-09-04T09:00:00Z",
        items = listOf(
            OpticalOrderItem(
                id = 10,
                description = "Everyday Frame – Tortoise",
                quantity = 1,
                unitPrice = BigDecimal("3200.00"),
                amount = BigDecimal("3200.00"),
                productVariantId = 42,
                isRateable = false,
                rating = null,
                imagePath = "frames/everyday-tortoise.jpg",
            ),
        ),
        paymentSummary = PaymentSummary(
            status = PaymentStatus.UNPAID,
            totalAmount = BigDecimal("3200.00"),
            amountPaid = BigDecimal.ZERO,
            balanceDue = BigDecimal("3200.00"),
            paymentDueDate = "2026-09-18",
            isOverdue = false,
        ),
    )
}
