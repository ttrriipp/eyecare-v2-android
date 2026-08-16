package com.eyecare.app.presentation.ar.model

import android.content.res.AssetManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Non-rendering scale metadata for a frame model.
 *
 * The values are the provisional physical calibration for the first round frame. Keeping this
 * metadata outside SceneView makes it possible to test and revise calibration without coupling
 * the catalog/domain models to Filament types.
 */
data class FrameModelScale(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    init {
        require(x.isFinite() && y.isFinite() && z.isFinite()) {
            "Frame model scale must contain finite values"
        }
        require(x > 0f && y > 0f && z > 0f) {
            "Frame model scale must be positive"
        }
    }

    fun multiplied(multiplier: Float): FrameModelScale {
        require(multiplier.isFinite() && multiplier > 0f) {
            "Frame model scale multiplier must be positive and finite"
        }
        return FrameModelScale(x * multiplier, y * multiplier, z * multiplier)
    }
}

/**
 * A locally bundled GLB that can be rendered by the feasibility harness.
 *
 * This descriptor deliberately contains no network URL or user-provided file path. Remote,
 * versioned assets are introduced only after the static renderer passes its device gate.
 */
data class BundledFrameAsset(
    val assetPath: String,
    val scale: FrameModelScale,
    /** Temporary device-smoke calibration kept separate from the measured source dimensions. */
    val displayScaleMultiplier: Float = 1f,
) {
    init {
        require(displayScaleMultiplier.isFinite() && displayScaleMultiplier > 0f) {
            "Frame display scale multiplier must be positive and finite"
        }
    }

    fun scaleForPose(poseScale: Float): FrameModelScale {
        require(poseScale.isFinite() && poseScale > 0f) {
            "Pose scale must be positive and finite"
        }
        return scale.multiplied(displayScaleMultiplier * poseScale)
    }

    /**
     * Performs a cheap, IO-bound GLB header check before initializing Filament.
     *
     * SceneView performs the actual model parse. This check gives missing, truncated, and
     * non-GLB files a deterministic recoverable error instead of allowing a failed load to look
     * like an indefinitely loading renderer.
     */
    suspend fun validate(assetManager: AssetManager): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            assetManager.open(assetPath).use { input ->
                val header = ByteArray(GLB_HEADER_SIZE)
                var bytesRead = 0
                while (bytesRead < header.size) {
                    val read = input.read(header, bytesRead, header.size - bytesRead)
                    if (read < 0) break
                    bytesRead += read
                }

                require(bytesRead == GLB_HEADER_SIZE) {
                    "GLB header is truncated"
                }
                require(header.copyOfRange(0, 4).contentEquals(GLB_MAGIC)) {
                    "Asset is not a binary glTF file"
                }

                val version = readIntLittleEndian(header, offset = 4)
                require(version == GLB_VERSION) {
                    "Unsupported binary glTF version: $version"
                }

                val declaredLength = readIntLittleEndian(header, offset = 8)
                require(declaredLength >= GLB_HEADER_SIZE) {
                    "GLB length is invalid"
                }
            }
        }
    }

    companion object {
        private const val GLB_HEADER_SIZE = 12
        private const val GLB_VERSION = 2
        private val GLB_MAGIC = byteArrayOf(0x67, 0x6c, 0x54, 0x46)

        val RoundFrame = BundledFrameAsset(
            assetPath = "models/round_frame_textured.glb",
            scale = FrameModelScale(
                x = 0.123f,
                y = 0.144565f,
                z = 0.123f,
            ),
            // A third POCO smoke check still showed the measured mesh undersized in the live
            // view. Keep this provisional until the physical side-by-side calibration checkpoint.
            displayScaleMultiplier = 1.75f,
        )

        private fun readIntLittleEndian(bytes: ByteArray, offset: Int): Int =
            ByteBuffer.wrap(bytes, offset, Int.SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .int
    }
}
