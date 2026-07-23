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

enum class ProductType {
    FRAME,
    ACCESSORY,
    CONTACT_LENS,
    LENS,
    UNKNOWN;

    companion object {
        fun fromApi(value: String): ProductType = entries.firstOrNull {
            it != UNKNOWN && it.name.equals(value, ignoreCase = true)
        } ?: UNKNOWN
    }
}

val Product.type: ProductType
    get() = ProductType.fromApi(productType)

val ProductVariant.isArReady: Boolean
    get() = arEligible && !arAssetReference.isNullOrBlank()

val Product.isMobileCatalogVisible: Boolean
    get() = when (type) {
        ProductType.ACCESSORY -> true
        ProductType.FRAME -> variants.any(ProductVariant::isArReady)
        else -> false
    }

val Product.isMobileOrderable: Boolean
    get() = type == ProductType.ACCESSORY

fun Product.forMobileCatalog(): Product? = when (type) {
    ProductType.ACCESSORY -> this
    ProductType.FRAME -> copy(variants = variants.filter(ProductVariant::isArReady))
        .takeIf { it.variants.isNotEmpty() }
    else -> null
}
