package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
        val images: List<String> = emptyList(),
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
