package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

object ProductDtos {

    @Serializable
    data class ProductDto(
        val id: Int,
        val name: String,
        val slug: String,
        val description: String? = null,
        val price: String,
        val dimensions: JsonElement? = null,
        val brand: String,
        val category: String,
        val variants: List<VariantDto> = emptyList(),
        val images: List<ImageDto> = emptyList(),
    )

    @Serializable
    data class VariantDto(
        val id: Int,
        val name: String,
        val sku: String,
        val price: String,
        val dimensions: JsonElement? = null,
        @SerialName("ar_eligible") val arEligible: Boolean = false,
        @SerialName("ar_asset_reference") val arAssetReference: String? = null,
    )

    @Serializable
    data class ImageDto(
        val id: Int,
        val path: String,
        @SerialName("is_primary") val isPrimary: Boolean = false,
        @SerialName("sort_order") val sortOrder: Int = 0,
    )

    @Serializable
    data class ProductListResponse(val data: List<ProductDto>)

    @Serializable
    data class ProductResponse(val data: ProductDto)
}
