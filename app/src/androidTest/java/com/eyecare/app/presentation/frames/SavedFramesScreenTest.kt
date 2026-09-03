package com.eyecare.app.presentation.frames

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.domain.model.SavedFrame
import com.eyecare.app.domain.model.SavedFrameAvailability
import com.eyecare.app.domain.model.SavedFrameProduct
import com.eyecare.app.domain.model.SavedFrameVariant
import com.eyecare.app.presentation.common.components.SAVED_FRAME_DISCLAIMER
import com.eyecare.app.ui.theme.EyecareTheme
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test

class SavedFramesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedRowShowsSavedTimeWithoutPersistentDisclaimer() {
        composeRule.setContent {
            EyecareTheme {
                SavedFramesScreen(
                    uiState = SavedFramesUiState.Success(
                        items = listOf(savedFrame()),
                        currentPage = 1,
                        canLoadMore = false,
                    ),
                    onBack = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onRemoveFrame = {},
                    onOpenFrame = { _, _ -> },
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithText(SAVED_FRAME_DISCLAIMER).assertDoesNotExist()
        composeRule.onNodeWithText("Saved Aug 27, 2026", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Unavailable").assertIsDisplayed()
    }

    @Test
    fun emptyStateDoesNotShowPersistentDisclaimer() {
        composeRule.setContent {
            EyecareTheme {
                SavedFramesScreen(
                    uiState = SavedFramesUiState.Success(
                        items = emptyList(),
                        currentPage = 1,
                        canLoadMore = false,
                    ),
                    onBack = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onRemoveFrame = {},
                    onOpenFrame = { _, _ -> },
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithText("No saved frames yet").assertIsDisplayed()
        composeRule.onNodeWithText(SAVED_FRAME_DISCLAIMER).assertDoesNotExist()
    }

    private fun savedFrame() = SavedFrame(
        productVariantId = 71,
        savedAt = "2026-08-27T10:00:00+08:00",
        availability = SavedFrameAvailability.UNKNOWN,
        variant = SavedFrameVariant(
            id = 71,
            name = "Black",
            sku = "SKU-71",
            price = BigDecimal("4500.00"),
            compareAtPrice = null,
            attributes = null,
            images = emptyList(),
            ar = null,
            product = SavedFrameProduct(
                id = 7,
                name = "Classic Rectangle",
                brand = "Eyecare",
                category = "Full Rim",
            ),
        ),
    )
}
