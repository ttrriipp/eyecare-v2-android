package com.eyecare.app.presentation.eyewear

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.FulfillmentMode
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderItem
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.ui.theme.EyecareTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OpticalOrderListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun currentFilterIsDefaultAndEmptyStateHasNoAction() {
        var selectedFilter: OrderListFilter? = null
        composeRule.setContent {
            EyecareTheme {
                OpticalOrderListContent(
                    uiState = OrderListUiState.Empty(OrderListFilter.CURRENT),
                    onRefresh = {},
                    onRetry = {},
                    onLoadMore = {},
                    onSelectFilter = { selectedFilter = it },
                    onNavigateToOrder = {},
                )
            }
        }

        composeRule.onNodeWithText("Current").assertIsDisplayed()
        composeRule.onNodeWithText("History").assertIsDisplayed()
        composeRule.onNodeWithText("No current orders", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("View frames").assertDoesNotExist()

        composeRule.onNodeWithText("History").performClick()
        assertEquals(OrderListFilter.HISTORY, selectedFilter)
    }

    @Test
    fun orderCardLeadsWithOrderNumberAndKeepsProductSummary() {
        composeRule.setContent {
            EyecareTheme {
                OpticalOrderListContent(
                    uiState = OrderListUiState.Success(
                        items = listOf(createOrder()),
                        selectedFilter = OrderListFilter.CURRENT,
                    ),
                    onRefresh = {},
                    onRetry = {},
                    onLoadMore = {},
                    onSelectFilter = {},
                    onNavigateToOrder = {},
                )
            }
        }

        composeRule.onNodeWithText("Order #OO-42").assertIsDisplayed()
        composeRule.onNodeWithText("Single vision lenses").assertIsDisplayed()
        composeRule.onNodeWithText("In preparation").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("View order details").assertIsDisplayed()
    }

    private fun createOrder() = OpticalOrder(
        id = 42,
        orderNumber = "OO-42",
        status = OpticalOrderStatus.IN_PROGRESS,
        fulfillmentMode = FulfillmentMode.PREPARED,
        totalAmount = BigDecimal("5000.00"),
        startedAt = null,
        readyAt = null,
        dispensedAt = null,
        cancelledAt = null,
        createdAt = "2026-08-01T10:00:00Z",
        items = listOf(
            OpticalOrderItem(
                id = 10,
                description = "Single vision lenses",
                quantity = 1,
                unitPrice = BigDecimal("5000.00"),
                amount = BigDecimal("5000.00"),
                productVariantId = null,
                isRateable = false,
                rating = null,
            ),
        ),
        paymentSummary = null,
    )
}
