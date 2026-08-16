package com.eyecare.app.presentation.ar

import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.presentation.ar.model.ArAssetState
import com.eyecare.app.presentation.ar.model.ArTryOnUiState
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FacePose

/**
 * The stable content contract shared by loading, face-searching, and tracking states.
 *
 * Keeping these states in one model lets the camera preview and SceneView remain in the same
 * Compose subtree while only their inputs change.
 */
internal data class ActiveTryOnContentState(
    val phase: ActiveTryOnPhase,
    val variants: List<FrameVariant>,
    val selectedVariant: FrameVariant?,
    val face: FaceFrame?,
    val pose: FacePose?,
    val assetState: ArAssetState,
)

internal enum class ActiveTryOnPhase {
    Loading,
    Searching,
    Tracking,
}

internal fun ArTryOnUiState.toActiveTryOnContentState(): ActiveTryOnContentState? = when (this) {
    is ArTryOnUiState.Loading -> ActiveTryOnContentState(
        phase = ActiveTryOnPhase.Loading,
        variants = variants,
        selectedVariant = selectedVariant,
        face = null,
        pose = null,
        assetState = assetState,
    )

    is ArTryOnUiState.Searching -> ActiveTryOnContentState(
        phase = ActiveTryOnPhase.Searching,
        variants = variants,
        selectedVariant = selectedVariant,
        face = null,
        pose = null,
        assetState = assetState,
    )

    is ArTryOnUiState.Tracking -> ActiveTryOnContentState(
        phase = ActiveTryOnPhase.Tracking,
        variants = variants,
        selectedVariant = selectedVariant,
        face = face,
        pose = pose,
        assetState = assetState,
    )

    else -> null
}
