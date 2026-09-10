package com.eyecare.app.presentation.ar

import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.presentation.ar.model.ArAssetState
import com.eyecare.app.presentation.ar.model.ArTrackingQuality
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
    val trackingQuality: ArTrackingQuality = ArTrackingQuality.Stabilizing,
    val hasTrackedBefore: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveMessage: String? = null,
    val frameName: String? = null,
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
        isSaving = isSaving,
        saveError = saveError,
        saveMessage = saveMessage,
        frameName = frameName,
    )

    is ArTryOnUiState.Searching -> ActiveTryOnContentState(
        phase = ActiveTryOnPhase.Searching,
        variants = variants,
        selectedVariant = selectedVariant,
        face = null,
        pose = null,
        assetState = assetState,
        hasTrackedBefore = hasTrackedBefore,
        isSaving = isSaving,
        saveError = saveError,
        saveMessage = saveMessage,
        frameName = frameName,
    )

    is ArTryOnUiState.Tracking -> ActiveTryOnContentState(
        phase = ActiveTryOnPhase.Tracking,
        variants = variants,
        selectedVariant = selectedVariant,
        face = face,
        pose = pose,
        assetState = assetState,
        trackingQuality = trackingQuality,
        isSaving = isSaving,
        saveError = saveError,
        saveMessage = saveMessage,
        frameName = frameName,
    )

    else -> null
}

internal fun arFaceGuidanceMessage(
    phase: ActiveTryOnPhase,
    hasTrackedBefore: Boolean,
    assetState: ArAssetState,
    trackingQuality: ArTrackingQuality = ArTrackingQuality.Stabilizing,
): String? = when {
    assetState is ArAssetState.Failed -> null
    phase == ActiveTryOnPhase.Loading -> "Preparing your try-on…"
    trackingQuality == ArTrackingQuality.Stable -> null
    trackingQuality == ArTrackingQuality.Reacquiring ->
        "Preview paused — hold still while we reacquire your face"
    trackingQuality == ArTrackingQuality.CenterFace -> "Center your face inside the guide"
    trackingQuality == ArTrackingQuality.LookStraight -> "Look straight at the camera"
    trackingQuality == ArTrackingQuality.LevelHead -> "Keep your head level"
    trackingQuality == ArTrackingQuality.Stabilizing && phase == ActiveTryOnPhase.Tracking ->
        "Center your face and look straight at the camera"
    hasTrackedBefore -> "We lost you — center your face inside the guide"
    else -> "Center your face inside the guide"
}
