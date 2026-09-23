package com.eyecare.app.presentation.accessories

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import com.eyecare.app.domain.model.AcceptedOrderSummary
import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.DiscountType
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.ui.theme.EyecareTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccessoryOrderRequestDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingIndicatorIsCenteredInTheContentArea() {
        composeRule.setContent {
            EyecareTheme {
                AccessoryOrderRequestDetailScreen(
                    uiState = RequestDetailUiState.Loading,
                    showCancelDialog = false,
                    onShowCancelDialog = {},
                    onDismissCancelDialog = {},
                    onCancel = {},
                    onNavigateToOrder = {},
                    onRetry = {},
                    onBack = {},
                )
            }
        }

        val screenBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val titleBounds = composeRule.onNodeWithText("Request details").fetchSemanticsNode().boundsInRoot
        val indicatorBounds = composeRule.onNode(
            hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate),
        ).fetchSemanticsNode().boundsInRoot

        assertEquals(screenBounds.center.x, indicatorBounds.center.x, 1f)
        assertTrue(
            "Loading indicator should be below the header, centered in the content area",
            indicatorBounds.center.y > titleBounds.bottom + screenBounds.height * 0.2f,
        )
    }

    @Test
    fun pendingRequestExplainsThatNoActionIsNeeded() {
        var refreshed = false

        composeRule.setContent {
            EyecareTheme {
                AccessoryOrderRequestDetailScreen(
                    uiState = RequestDetailUiState.Success(request(status = OrderRequestStatus.PENDING)),
                    showCancelDialog = false,
                    onShowCancelDialog = {},
                    onDismissCancelDialog = {},
                    onCancel = {},
                    onNavigateToOrder = {},
                    onRetry = { refreshed = true },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Estimated subtotal").assertIsDisplayed()
        composeRule.onNodeWithText("No action needed yet.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Refresh status").performClick()

        assertTrue(refreshed)
    }

    @Test
    fun acceptedRequestShowsOrderPaymentSummary() {
        composeRule.setContent {
            EyecareTheme {
                AccessoryOrderRequestDetailScreen(
                    uiState = RequestDetailUiState.Success(
                        request(
                            status = OrderRequestStatus.ACCEPTED,
                            order = AcceptedOrderSummary(
                                id = 42,
                                orderNumber = "ORD-2026-000001",
                                status = "pending_payment",
                                discountAmount = BigDecimal("100.00"),
                                totalAmount = BigDecimal("1200.00"),
                                paymentExpiresAt = "2026-09-23T13:30:00+08:00",
                            ),
                        ),
                    ),
                    showCancelDialog = false,
                    onShowCancelDialog = {},
                    onDismissCancelDialog = {},
                    onCancel = {},
                    onNavigateToOrder = {},
                    onRetry = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Order created").assertIsDisplayed()
        composeRule.onNodeWithText("Awaiting payment").assertIsDisplayed()
        composeRule.onNodeWithText("Payment deadline").assertIsDisplayed()
        composeRule.onNodeWithText("View order and payment").assertIsDisplayed()
    }

    private fun request(
        status: OrderRequestStatus,
        order: AcceptedOrderSummary? = null,
    ) = AccessoryOrderRequest(
        id = 1,
        requestNumber = "ORQ-2026-000001",
        status = status,
        subtotalAmount = BigDecimal("1300.00"),
        requestedDiscountType = DiscountType.NONE,
        resolvedBy = null,
        resolvedAt = if (status == OrderRequestStatus.ACCEPTED) "2026-09-23T12:30:00+08:00" else null,
        items = emptyList(),
        rejectionReason = null,
        cancelledAt = null,
        createdAt = "2026-09-23T10:00:00+08:00",
        order = order,
    )
}
