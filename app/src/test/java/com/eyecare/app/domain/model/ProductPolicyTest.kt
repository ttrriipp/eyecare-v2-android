package com.eyecare.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProductPolicyTest {

    @Test
    fun `accessories are visible and orderable`() {
        val product = product(type = "ACCESSORY")

        assertEquals(ProductType.ACCESSORY, product.type)
        assertTrue(product.isMobileCatalogVisible)
        assertTrue(product.isMobileOrderable)
        assertEquals(product, product.forMobileCatalog())
    }

    @Test
    fun `frames are visible only with AR ready variants and are never orderable`() {
        val ready = variant(id = 1, arEligible = true, asset = "ar/frame.glb")
        val missingAsset = variant(id = 2, arEligible = true, asset = null)
        val ineligible = variant(id = 3, arEligible = false, asset = "ar/other.glb")
        val frame = product(type = "frame", variants = listOf(ready, missingAsset, ineligible))

        val sanitized = frame.forMobileCatalog()

        assertEquals(ProductType.FRAME, frame.type)
        assertTrue(frame.isMobileCatalogVisible)
        assertFalse(frame.isMobileOrderable)
        assertEquals(listOf(ready), sanitized?.variants)
    }

    @Test
    fun `non AR frames and non mobile product types fail closed`() {
        val nonArFrame = product(
            type = "frame",
            variants = listOf(variant(id = 1, arEligible = true, asset = " ")),
        )

        assertFalse(nonArFrame.isMobileCatalogVisible)
        assertNull(nonArFrame.forMobileCatalog())

        listOf("contact_lens", "lens", "general", "service", "future_type").forEach { type ->
            val product = product(type = type)
            assertFalse(product.isMobileCatalogVisible, type)
            assertFalse(product.isMobileOrderable, type)
            assertNull(product.forMobileCatalog(), type)
        }
    }

    private fun product(
        type: String,
        variants: List<ProductVariant> = listOf(variant()),
    ) = Product(
        id = 1,
        name = "Product",
        slug = "product",
        description = null,
        productType = type,
        brand = "Brand",
        category = "",
        variants = variants,
        images = emptyList(),
    )

    private fun variant(
        id: Int = 1,
        arEligible: Boolean = false,
        asset: String? = null,
    ) = ProductVariant(
        id = id,
        name = "Variant",
        sku = "SKU-$id",
        price = "100.00",
        compareAtPrice = null,
        attributes = null,
        inStock = true,
        arEligible = arEligible,
        arAssetReference = asset,
        images = emptyList(),
    )
}
