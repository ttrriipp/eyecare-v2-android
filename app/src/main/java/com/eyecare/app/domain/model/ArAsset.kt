package com.eyecare.app.domain.model

/** A patient-visible, immutable AR asset publication. */
data class ArAsset(
    val status: ArAssetStatus,
    val asset: ArAssetFile,
    val calibration: ArCalibration,
)

enum class ArAssetStatus {
    READY,
}

enum class ArAssetFormat {
    GLB,
}

data class ArAssetFile(
    val url: String,
    val format: ArAssetFormat,
    val version: Int,
    val byteSize: Long,
    val sha256: String,
)

data class ArCalibration(
    val frameWidthMm: Double,
    val outerFrameHeightMm: Double,
    val lensWidthMm: Double,
    val lensHeightMm: Double,
    val bridgeWidthMm: Double,
    val templeLengthMm: Double,
    val scale: ArVector,
    val anchor: ArVector,
    val rotationDegrees: ArVector,
)

data class ArVector(
    val x: Double,
    val y: Double,
    val z: Double,
)
