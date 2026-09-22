package com.eyecare.app.presentation.accessories

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.domain.model.AccessoryCart
import com.eyecare.app.domain.model.AccessoryCartItem
import com.eyecare.app.ui.theme.EyecareTheme
import java.math.BigDecimal
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccessoryCheckoutScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyCartOffersActionToBrowseAccessories() {
        var browseClicked = false

        composeRule.setContent {
            EyecareTheme {
                AccessoryCheckoutScreen(
                    cart = AccessoryCart(),
                    selectedDiscount = "none",
                    checkoutState = CheckoutUiState.Idle,
                    onDiscountSelect = {},
                    onSubmit = {},
                    onViewRequest = {},
                    onViewRequests = {},
                    onBack = {},
                    onBrowseAccessories = { browseClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Your cart is empty", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Back to catalog").performClick()

        assertTrue(browseClicked)
    }

    @Test
    fun reviewCopyExplainsEstimateAndRequestBoundary() {
        composeRule.setContent {
            EyecareTheme {
                AccessoryCheckoutScreen(
                    cart = AccessoryCart(
                        items = listOf(
                            AccessoryCartItem(
                                productVariantId = 42,
                                productName = "New Look Multi-Purpose All-In-One Solution",
                                variantName = "New Era Comfort 90 ml",
                                imagePath = "products/new-look.jpg",
                                unitPrice = BigDecimal("350.00"),
                                availability = AccessoryAvailability.AVAILABLE,
                                quantity = 1,
                            ),
                        ),
                    ),
                    selectedDiscount = "none",
                    checkoutState = CheckoutUiState.Idle,
                    onDiscountSelect = {},
                    onSubmit = {},
                    onViewRequest = {},
                    onViewRequests = {},
                    onBack = {},
                    onBrowseAccessories = {},
                )
            }
        }

        composeRule.onNodeWithText("Estimated total before approved discount").assertIsDisplayed()
        composeRule.onNodeWithText("No payment or stock hold occurs yet.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "New Look Multi-Purpose All-In-One Solution product image",
        ).assertIsDisplayed()
    }
}
