package com.eyecare.app.presentation.frames.components

import com.eyecare.app.domain.model.FrameVariant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FrameCardTest {

    private fun variant(
        price: BigDecimal = BigDecimal("159.99"),
        compareAtPrice: BigDecimal? = null,
    ) = FrameVariant(
        id = 1,
        name = "Standard",
        sku = "SKU-1",
        price = price,
        compareAtPrice = compareAtPrice,
        attributes = null,
        arEligible = false,
        arAssetReference = null,
        images = emptyList(),
    )

    @Test
    fun `formattedPrice renders price as peso currency`() {
        assertEquals("₱159.99", variant(price = BigDecimal("159.99")).formattedPrice())
    }

    @Test
    fun `formattedCompareAtPrice is null when there is no discount`() {
        assertNull(variant(compareAtPrice = null).formattedCompareAtPrice())
    }

    @Test
    fun `formattedCompareAtPrice renders the original price as peso currency`() {
        val discounted = variant(price = BigDecimal("159.99"), compareAtPrice = BigDecimal("199.99"))

        assertEquals("₱199.99", discounted.formattedCompareAtPrice())
    }
}
