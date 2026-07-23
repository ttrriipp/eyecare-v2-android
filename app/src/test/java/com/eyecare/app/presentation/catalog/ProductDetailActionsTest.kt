package com.eyecare.app.presentation.catalog

import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.model.ProductVariant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProductDetailActionsTest {

    @Test
    fun `AR frame is browse only`() {
        val variant = variant(arEligible = true, asset = "ar/frame.glb")

        val actions = productDetailActions(product("frame", variant), variant)

        assertTrue(actions.showAr)
        assertFalse(actions.showOrder)
        assertNotNull(actions.browseOnlyMessage)
    }

    @Test
    fun `accessory can be ordered but never shows AR`() {
        val variant = variant(arEligible = true, asset = "ar/unexpected.glb")

        val actions = productDetailActions(product("accessory", variant), variant)

        assertFalse(actions.showAr)
        assertTrue(actions.showOrder)
        assertNull(actions.browseOnlyMessage)
    }

    private fun product(type: String, variant: ProductVariant) = Product(
        id = 1,
        name = "Product",
        slug = "product",
        description = null,
        productType = type,
        brand = "Brand",
        category = "",
        variants = listOf(variant),
        images = emptyList(),
    )

    private fun variant(arEligible: Boolean, asset: String?) = ProductVariant(
        id = 1,
        name = "Variant",
        sku = "SKU",
        price = "100.00",
        compareAtPrice = null,
        attributes = null,
        inStock = true,
        arEligible = arEligible,
        arAssetReference = asset,
        images = emptyList(),
    )
}
