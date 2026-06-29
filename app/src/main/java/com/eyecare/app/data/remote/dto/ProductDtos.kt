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
        @SerialName("product_type") val productType: String,
        val brand: String,
        val category: String,
        val variants: List<VariantDto> = emptyList(),
        val images: List<String> = emptyList(),
    )

    @Serializable
    data class VariantDto(
        val id: Int,
        val name: String,
        val sku: String,
        val price: String,
        @SerialName("compare_at_price") val compareAtPrice: String? = null,
        val attributes: JsonElement? = null,
        @SerialName("in_stock") val inStock: Boolean = true,
        @SerialName("ar_eligible") val arEligible: Boolean = false,
        @SerialName("ar_asset_reference") val arAssetReference: String? = null,
        val images: List<String> = emptyList(),
    )

    @Serializable
    data class PaginationMeta(
        @SerialName("current_page") val currentPage: Int,
        @SerialName("last_page") val lastPage: Int,
        @SerialName("per_page") val perPage: Int,
        val total: Int,
    )

    @Serializable
    data class PaginatedProductResponse(
        val data: List<ProductDto>,
        val meta: PaginationMeta,
    )

    @Serializable
    data class ProductResponse(val data: ProductDto)
}
