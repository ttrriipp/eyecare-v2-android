package com.eyecare.app.presentation.ar.model

import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.presentation.ar.capability.ArCapabilityFailure

/** Renderer status exposed to the screen without leaking SceneView types into orchestration. */
sealed interface ArAssetState {
    data object Checking : ArAssetState
    data object Loading : ArAssetState
    data object Ready : ArAssetState
    data class Failed(val message: String) : ArAssetState
}

/** Asset failures stay inside the active state so the 2D overlay can remain usable. */

/** One exhaustive state stream for the try-on screen. */
sealed interface ArTryOnUiState {
    data object CheckingCapability : ArTryOnUiState

    data object PermissionRequired : ArTryOnUiState

    data class PermissionDenied(
        val shouldShowRationale: Boolean,
    ) : ArTryOnUiState

    data class Loading(
        val variants: List<FrameVariant>,
        val selectedVariant: FrameVariant?,
        val assetState: ArAssetState,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val saveMessage: String? = null,
    ) : ArTryOnUiState

    data class Searching(
        val variants: List<FrameVariant>,
        val selectedVariant: FrameVariant?,
        val assetState: ArAssetState,
        /** True once a face has been tracked this session, so the copy can say "lost" rather than "never found". */
        val hasTrackedBefore: Boolean = false,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val saveMessage: String? = null,
    ) : ArTryOnUiState

    data class Tracking(
        val variants: List<FrameVariant>,
        val selectedVariant: FrameVariant?,
        val face: FaceFrame,
        val pose: FacePose?,
        val assetState: ArAssetState,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val saveMessage: String? = null,
    ) : ArTryOnUiState

    data class Unsupported(
        val failures: List<ArCapabilityFailure>,
    ) : ArTryOnUiState

    data class Error(
        val message: String,
    ) : ArTryOnUiState
}
