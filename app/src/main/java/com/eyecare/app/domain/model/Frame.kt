package com.eyecare.app.domain.model

import java.math.BigDecimal

data class Frame(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String?,
    val brand: String,
    val category: String,
    val variants: List<FrameVariant>,
    val images: List<String>,
    val averageRating: Double? = null,
    val ratingCount: Int = 0,
)

data class FrameVariant(
    val id: Int,
    val name: String,
    val sku: String,
    val price: BigDecimal,
    val compareAtPrice: BigDecimal?,
    val attributes: Map<String, String>?,
    val arEligible: Boolean,
    val arAssetReference: String?,
    val images: List<String>,
    val ar: ArAsset? = null,
    val isSaved: Boolean = false,
)

val FrameVariant.isArReady: Boolean
    get() = arEligible && !arAssetReference.isNullOrBlank()

/** True only when the additive typed remote asset contract is present and ready. */
val FrameVariant.isTypedArReady: Boolean
    get() = ar?.status == ArAssetStatus.READY
