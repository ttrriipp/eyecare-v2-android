package com.eyecare.app.presentation.frames

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.presentation.common.components.SAVED_FRAME_DISCLAIMER
import com.eyecare.app.ui.theme.EyecareTheme
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test

class FrameDetailSavedStateScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unsavedVariantShowsSaveActionAndDisclaimer() {
        composeRule.setContent {
            EyecareTheme {
                FrameDetailSaveControls(
                    selected = variant(isSaved = false),
                    isSaving = false,
                    onTryOn = null,
                    onToggleSaved = {},
                )
            }
        }

        composeRule.onNodeWithText("Save frame").assertIsDisplayed()
        composeRule.onNodeWithText(SAVED_FRAME_DISCLAIMER).assertIsDisplayed()
    }

    @Test
    fun savedVariantDisablesRemoveActionWhileSaving() {
        composeRule.setContent {
            EyecareTheme {
                FrameDetailSaveControls(
                    selected = variant(isSaved = true),
                    isSaving = true,
                    onTryOn = null,
                    onToggleSaved = {},
                )
            }
        }

        composeRule.onNodeWithText("Remove from saved").assertIsNotEnabled()
    }

    private fun variant(isSaved: Boolean) = FrameVariant(
        id = 71,
        name = "Black",
        sku = "SKU-71",
        price = BigDecimal("4500.00"),
        compareAtPrice = null,
        attributes = null,
        arEligible = false,
        arAssetReference = null,
        images = emptyList(),
        isSaved = isSaved,
    )
}
