package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.net.URI

private const val SHA256_HEX_LENGTH = 64

private fun requireHttpsAssetUrl(url: String) {
    val uri = runCatching { URI(url) }.getOrNull()
    require(
        uri != null && uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() && uri.userInfo == null,
    ) {
        "AR asset URL must be an HTTPS URL without embedded credentials"
    }
}

private fun validatePositiveFinite(value: Double, field: String) {
    require(value.isFinite() && value > 0.0) {
        "$field must be finite and positive"
    }
}

private fun validateFinite(value: Double, field: String) {
    require(value.isFinite()) {
        "$field must be finite"
    }
}

private fun validatePositiveFinite(value: FrameDtos.ArVectorDto, field: String) {
    validatePositiveFinite(value.x, "$field.x")
    validatePositiveFinite(value.y, "$field.y")
    validatePositiveFinite(value.z, "$field.z")
}

private fun validateFinite(value: FrameDtos.ArVectorDto, field: String) {
    validateFinite(value.x, "$field.x")
    validateFinite(value.y, "$field.y")
    validateFinite(value.z, "$field.z")
}

object FrameDtos {

    @Serializable
    data class FrameVariantDto(
        val id: Int,
        val name: String,
        val sku: String,
        @Serializable(with = MoneyValueSerializer::class) val price: java.math.BigDecimal,
        @SerialName("compare_at_price") @Serializable(with = MoneyValueSerializer::class) val compareAtPrice: java.math.BigDecimal? = null,
        val attributes: JsonElement? = null,
        @SerialName("ar_eligible") val arEligible: Boolean = false,
        @SerialName("ar_asset_reference") val arAssetReference: String? = null,
        @SerialName("is_saved") val isSaved: Boolean = false,
        val ar: ArAssetDto? = null,
        val images: List<String> = emptyList(),
    )

    @Serializable
    data class ArAssetDto(
        val status: ArAssetStatusDto,
        val asset: ArAssetFileDto,
        val calibration: ArCalibrationDto,
    )

    @Serializable
    enum class ArAssetStatusDto {
        @SerialName("ready") READY,
    }

    @Serializable
    data class ArAssetFileDto(
        val url: String,
        val format: ArAssetFormatDto,
        val version: Int,
        @SerialName("byte_size") val byteSize: Long,
        val sha256: String,
    ) {
        init {
            requireHttpsAssetUrl(url)
            require(version > 0) { "AR asset version must be positive" }
            require(byteSize > 0) { "AR asset byte_size must be positive" }
            require(
                sha256.length == SHA256_HEX_LENGTH && sha256.all { it in '0'..'9' || it in 'a'..'f' },
            ) {
                "AR asset sha256 must be 64 lowercase hexadecimal characters"
            }
        }
    }

    @Serializable
    enum class ArAssetFormatDto {
        @SerialName("glb") GLB,
    }

    @Serializable
    data class ArCalibrationDto(
        @SerialName("frame_width_mm") val frameWidthMm: Double,
        @SerialName("outer_frame_height_mm") val outerFrameHeightMm: Double,
        @SerialName("lens_width_mm") val lensWidthMm: Double,
        @SerialName("lens_height_mm") val lensHeightMm: Double,
        @SerialName("bridge_width_mm") val bridgeWidthMm: Double,
        @SerialName("temple_length_mm") val templeLengthMm: Double,
        val scale: ArVectorDto,
        val anchor: ArVectorDto,
        @SerialName("rotation_degrees") val rotationDegrees: ArVectorDto,
    ) {
        init {
            validatePositiveFinite(frameWidthMm, "frame_width_mm")
            validatePositiveFinite(outerFrameHeightMm, "outer_frame_height_mm")
            validatePositiveFinite(lensWidthMm, "lens_width_mm")
            validatePositiveFinite(lensHeightMm, "lens_height_mm")
            validatePositiveFinite(bridgeWidthMm, "bridge_width_mm")
            validatePositiveFinite(templeLengthMm, "temple_length_mm")
            validatePositiveFinite(scale, "scale")
            validateFinite(anchor, "anchor")
            validateFinite(rotationDegrees, "rotation_degrees")
        }
    }

    @Serializable
    data class ArVectorDto(
        val x: Double,
        val y: Double,
        val z: Double,
    )

    @Serializable
    data class FrameDto(
        val id: Int,
        val name: String,
        val slug: String,
        val description: String? = null,
        @SerialName("product_type") val productType: String = "frame",
        val brand: String,
        val category: String? = null,
        val variants: List<FrameVariantDto> = emptyList(),
        val images: List<String> = emptyList(),
        @SerialName("average_rating") val averageRating: Double? = null,
        @SerialName("rating_count") val ratingCount: Int = 0,
    )

    @Serializable
    data class PaginatedFrameResponse(
        val data: List<FrameDto>,
        val links: JsonElement? = null,
        val meta: PaginationMeta? = null,
    )

    @Serializable
    data class FrameResponse(val data: FrameDto)
}
