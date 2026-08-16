package com.eyecare.app.presentation.ar.model

import com.eyecare.app.presentation.ar.rendering.FrameModelSource

/**
 * Tracks the lifecycle of a typed remote asset from the repository boundary to the renderer.
 *
 * [NotLoaded] means the selected variant has no typed AR asset or the asset is not ready;
 * the 2D image overlay remains the fallback. [Ready] carries the verified local file path
 * and calibrated scale for the renderer.
 */
sealed interface ArAssetSource {
    data object NotLoaded : ArAssetSource
    data object Loading : ArAssetSource
    data class Ready(
        val filePath: String,
        val scale: FrameModelScale,
    ) : ArAssetSource
    data class Failed(val message: String) : ArAssetSource

    fun toFrameModelSource(): FrameModelSource? = when (this) {
        is NotLoaded -> null
        is Loading -> null
        is Ready -> FrameModelSource.Downloaded(filePath = filePath)
        is Failed -> null
    }
}
