package com.eyecare.app.domain.model

import java.math.BigDecimal

data class SavedFramePage(
    val items: List<SavedFrame>,
    val currentPage: Int,
    val lastPage: Int,
)

data class SavedFrame(
    val productVariantId: Int,
    val savedAt: String,
    val availability: SavedFrameAvailability,
    val variant: SavedFrameVariant,
)

data class SavedFrameVariant(
    val id: Int,
    val name: String,
    val sku: String,
    val price: BigDecimal,
    val compareAtPrice: BigDecimal?,
    val attributes: Map<String, String>?,
    val images: List<String>,
    val ar: ArAsset?,
    val product: SavedFrameProduct,
)

data class SavedFrameProduct(
    val id: Int,
    val name: String,
    val brand: String,
    val category: String,
)

enum class SavedFrameAvailability {
    AVAILABLE,
    UNAVAILABLE,
    UNKNOWN;

    companion object {
        fun from(value: String): SavedFrameAvailability = when (value) {
            "available" -> AVAILABLE
            "unavailable" -> UNAVAILABLE
            else -> UNKNOWN
        }
    }
}
