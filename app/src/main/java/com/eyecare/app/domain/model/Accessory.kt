package com.eyecare.app.domain.model

import java.math.BigDecimal

data class Accessory(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String?,
    val brand: String?,
    val category: String?,
    val images: List<String>,
    val averageRating: Double?,
    val ratingCount: Int,
    val variants: List<AccessoryVariant>,
)

data class AccessoryVariant(
    val id: Int,
    val name: String,
    val price: BigDecimal,
    val compareAtPrice: BigDecimal?,
    val attributes: Map<String, String>,
    val images: List<String>,
    val availability: AccessoryAvailability,
)

enum class AccessoryAvailability {
    AVAILABLE,
    LOW_STOCK,
    UNAVAILABLE,
    UNKNOWN;

    val isOrderable: Boolean
        get() = this == AVAILABLE || this == LOW_STOCK

    companion object {
        fun from(value: String): AccessoryAvailability = when (value.lowercase()) {
            "available" -> AVAILABLE
            "low_stock" -> LOW_STOCK
            "unavailable" -> UNAVAILABLE
            else -> UNKNOWN
        }
    }
}