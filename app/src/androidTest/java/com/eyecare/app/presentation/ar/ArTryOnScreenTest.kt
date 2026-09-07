package com.eyecare.app.presentation.ar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material3.SnackbarHostState
import com.eyecare.app.presentation.ar.capability.ArCapabilityFailure
import com.eyecare.app.presentation.ar.components.ArAssetStatusBanner
import com.eyecare.app.presentation.ar.components.ArDisclosureBanner
import com.eyecare.app.presentation.ar.components.ArPrivacyStatusBanner
import com.eyecare.app.presentation.ar.components.ArStatusOverlay
import com.eyecare.app.presentation.ar.model.ArAssetState
import com.eyecare.app.presentation.ar.model.ArTryOnUiState
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.presentation.common.components.SAVED_FRAME_DISCLAIMER
import com.eyecare.app.ui.theme.EyecareTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArTryOnScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unsupportedState_keepsFrameImagePathAvailable() {
        var openedCatalog = false

        composeRule.setContent {
            EyecareTheme {
                ArStatusOverlay(
                    state = ArTryOnUiState.Unsupported(
                        failures = listOf(ArCapabilityFailure.OPENGL_ES),
                    ),
                    onOpenCatalog = { openedCatalog = true },
                )
            }
        }

        composeRule.onNodeWithText("3D try-on isn't available").assertIsDisplayed()
        composeRule.onNodeWithText("View frame images").performClick()
        composeRule.runOnIdle { assertEquals(true, openedCatalog) }
    }

    @Test
    fun recoverableError_offersRetryAndCatalogImagePath() {
        var retryCount = 0
        var openedCatalog = false

        composeRule.setContent {
            EyecareTheme {
                ArStatusOverlay(
                    state = ArTryOnUiState.Error("The model could not be initialized."),
                    onRetry = { retryCount++ },
                    onOpenCatalog = { openedCatalog = true },
                )
            }
        }

        composeRule.onNodeWithText("3D preview unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.onNodeWithText("View frame images").performClick()
        composeRule.runOnIdle {
            assertEquals(1, retryCount)
            assertEquals(true, openedCatalog)
        }
    }

    @Test
    fun trackingDisclosure_usesApprovedNonClinicalCopy() {
        composeRule.setContent {
            EyecareTheme {
                ArDisclosureBanner()
            }
        }

        composeRule
            .onNodeWithText("Visual preview only. Final fit is confirmed at the clinic.")
            .assertIsDisplayed()
    }

    @Test
    fun activeCameraPrivacyStatus_isVisible() {
        composeRule.setContent {
            EyecareTheme {
                ArPrivacyStatusBanner()
            }
        }

        composeRule
            .onNodeWithText("Camera on · processed on this device")
            .assertIsDisplayed()
    }

    @Test
    fun activeSaveControls_matchFrameDetailWithoutStaticDisclaimer() {
        composeRule.setContent {
            EyecareTheme {
                ArTryOnBottomControls(
                    variants = emptyList(),
                    selectedVariant = testVariant(isSaved = false),
                    isSaving = false,
                    onSelectVariant = {},
                    onToggleSaved = {},
                    onOpenCatalog = {},
                    snackbarHostState = SnackbarHostState(),
                )
            }
        }

        composeRule.onNodeWithText("Save frame").assertIsDisplayed()
        composeRule.onNodeWithText(SAVED_FRAME_DISCLAIMER).assertDoesNotExist()
    }

    @Test
    fun capabilityState_hasDistinctGuidance() {
        composeRule.setContent {
            EyecareTheme {
                ArStatusOverlay(
                    state = ArTryOnUiState.CheckingCapability,
                    onOpenCatalog = {},
                )
            }
        }

        composeRule.onNodeWithText("Preparing 3D try-on").assertIsDisplayed()
    }

    @Test
    fun permissionState_hasDistinctGuidance() {
        composeRule.setContent {
            EyecareTheme {
                ArStatusOverlay(
                    state = ArTryOnUiState.PermissionRequired,
                    onOpenCatalog = {},
                )
            }
        }

        composeRule.onNodeWithText("Camera access needed").assertIsDisplayed()
    }

    @Test
    fun assetFailure_directsToFrameImagesWithoutClaimingInlinePreview() {
        composeRule.setContent {
            EyecareTheme {
                ArAssetStatusBanner(state = ArAssetState.Failed("bad model"))
            }
        }

        composeRule.onNodeWithText("3D preview unavailable. View frame images instead.").assertIsDisplayed()
    }

    private fun testVariant(isSaved: Boolean) = FrameVariant(
        id = 71,
        name = "Black",
        sku = "SKU-71",
        price = BigDecimal("4500.00"),
        compareAtPrice = null,
        attributes = null,
        arEligible = true,
        arAssetReference = "asset-71",
        images = emptyList(),
        isSaved = isSaved,
    )
}
