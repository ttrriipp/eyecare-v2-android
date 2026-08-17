package com.eyecare.app.presentation.ar.rendering

import com.eyecare.app.presentation.ar.model.BundledFrameAsset
import com.eyecare.app.presentation.ar.model.FrameModelScale

/**
 * Identifies which GLB the renderer should load.
 *
 * [Bundled] loads from Android assets. [Downloaded] loads from a verified file in the app cache
 * directory. Neither type leaks SceneView, OkHttp, or file-system details into the caller.
 */
sealed interface FrameModelSource {

    /** The renderer-local asset path used by [io.github.sceneview.rememberModelInstance]. */
    val assetPath: String

    data class Bundled(
        val descriptor: BundledFrameAsset,
    ) : FrameModelSource {
        override val assetPath: String get() = descriptor.assetPath
    }

    /**
     * A verified GLB file downloaded from the remote asset repository.
     *
     * @param filePath absolute path to the cached file (e.g. `/data/.../ar-assets/variant-42-v2-sha256-abc.glb`)
     * @param modelScale calibrated scale from the published remote asset metadata
     */
    data class Downloaded(
        val filePath: String,
        val modelScale: FrameModelScale,
    ) : FrameModelSource {
        override val assetPath: String get() = filePath

        fun scaleForPose(poseScale: Float): FrameModelScale = modelScale.multiplied(poseScale)
    }
}
