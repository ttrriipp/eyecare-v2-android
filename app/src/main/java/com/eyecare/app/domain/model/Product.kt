package com.eyecare.app.domain.model

data class Product(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String?,
    val price: String,
    val dimensions: String?,
    val brand: String,
    val category: String,
    val variants: List<ProductVariant>,
    val images: List<ProductImage>,
)

data class ProductVariant(
    val id: Int,
    val name: String,
    val sku: String,
    val price: String,
    val dimensions: String?,
    val arEligible: Boolean,
    val arAssetReference: String?,
)

data class ProductImage(
    val id: Int,
    val path: String,
    val isPrimary: Boolean,
    val sortOrder: Int,
)
