package com.eyecare.app.domain.model

data class Product(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String?,
    val productType: String,
    val brand: String,
    val category: String,
    val variants: List<ProductVariant>,
    val images: List<String>,
)

data class ProductVariant(
    val id: Int,
    val name: String,
    val sku: String,
    val price: String,
    val compareAtPrice: String?,
    val attributes: Map<String, String>?,
    val inStock: Boolean,
    val arEligible: Boolean,
    val arAssetReference: String?,
    val images: List<String>,
)
