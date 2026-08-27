package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object SavedFrameDtos {

    @Serializable
    data class SavedFramePageResponse(
        val data: List<SavedFrameResource>,
        val links: PageLinks,
        val meta: PageMeta,
    )

    @Serializable
    data class SavedFrameSaveResponse(
        val data: SavedFrameResource,
    )

    @Serializable
    data class SavedFrameResource(
        @SerialName("product_variant_id") val productVariantId: Int,
        @SerialName("saved_at") val savedAt: String,
        val availability: String,
        val variant: SavedFrameVariantDto,
    )

    @Serializable
    data class SavedFrameVariantDto(
        val id: Int,
        val name: String,
        val sku: String,
        @Serializable(with = MoneyValueSerializer::class) val price: java.math.BigDecimal,
        @SerialName("compare_at_price") @Serializable(with = MoneyValueSerializer::class) val compareAtPrice: java.math.BigDecimal? = null,
        val attributes: kotlinx.serialization.json.JsonElement? = null,
        val images: List<String> = emptyList(),
        val ar: FrameDtos.ArAssetDto? = null,
        val product: SavedFrameProductDto,
    )

    @Serializable
    data class SavedFrameProductDto(
        val id: Int,
        val name: String,
        val brand: String,
        val category: String,
    )

    @Serializable
    data class PageLinks(
        val first: String? = null,
        val last: String? = null,
        val prev: String? = null,
        val next: String? = null,
    )

    @Serializable
    data class PageMeta(
        @SerialName("current_page") val currentPage: Int,
        @SerialName("last_page") val lastPage: Int,
        @SerialName("per_page") val perPage: Int,
        val total: Int,
    )
}
