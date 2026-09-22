package com.eyecare.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AccessoryCartTest {

    private fun variant(id: Int, price: BigDecimal = BigDecimal("100.00"), availability: AccessoryAvailability = AccessoryAvailability.AVAILABLE) =
        AccessoryVariant(
            id = id,
            name = "Variant $id",
            price = price,
            compareAtPrice = null,
            attributes = emptyMap(),
            images = emptyList(),
            availability = availability,
        )

    private fun cartItem(variantId: Int, quantity: Int = 1, price: BigDecimal = BigDecimal("100.00")) =
        AccessoryCartItem(
            productVariantId = variantId,
            productName = "Product",
            variantName = "Variant",
            imagePath = null,
            unitPrice = price,
            availability = AccessoryAvailability.AVAILABLE,
            quantity = quantity,
        )

    @Test
    fun `empty cart has no items`() {
        val cart = AccessoryCart()
        assertTrue(cart.items.isEmpty())
        assertEquals(BigDecimal.ZERO, cart.estimatedTotal)
    }

    @Test
    fun `add creates new item with quantity 1`() {
        val cart = AccessoryCart().add(variant(1))
        assertEquals(1, cart.items.size)
        assertEquals(1, cart.items[0].quantity)
        assertEquals(1, cart.items[0].productVariantId)
    }

    @Test
    fun `add accepts a requested quantity for a new item`() {
        val cart = AccessoryCart().add(variant(1), quantity = 3)
        assertEquals(3, cart.items[0].quantity)
    }

    @Test
    fun `add existing variant increments quantity`() {
        val cart = AccessoryCart()
            .add(variant(1))
            .add(variant(1))
        assertEquals(1, cart.items.size)
        assertEquals(2, cart.items[0].quantity)
    }

    @Test
    fun `add caps quantity at 5`() {
        var cart = AccessoryCart()
        repeat(10) { cart = cart.add(variant(1)) }
        assertEquals(5, cart.items[0].quantity)
    }

    @Test
    fun `add rejects more than 20 distinct variants`() {
        var cart = AccessoryCart()
        for (i in 1..21) {
            cart = cart.add(variant(i))
        }
        assertEquals(20, cart.items.size)
    }

    @Test
    fun `increment increases quantity up to 5`() {
        val cart = AccessoryCart()
            .add(variant(1))
            .increment(1)
            .increment(1)
        assertEquals(3, cart.items[0].quantity)
    }

    @Test
    fun `increment caps at 5`() {
        var cart = AccessoryCart().add(variant(1))
        repeat(10) { cart = cart.increment(1) }
        assertEquals(5, cart.items[0].quantity)
    }

    @Test
    fun `decrement reduces quantity`() {
        val cart = AccessoryCart()
            .add(variant(1))
            .add(variant(1))
            .decrement(1)
        assertEquals(1, cart.items[0].quantity)
    }

    @Test
    fun `decrement from 1 removes item`() {
        val cart = AccessoryCart()
            .add(variant(1))
            .decrement(1)
        assertTrue(cart.items.isEmpty())
    }

    @Test
    fun `remove removes item entirely`() {
        val cart = AccessoryCart()
            .add(variant(1))
            .remove(1)
        assertTrue(cart.items.isEmpty())
    }

    @Test
    fun `clear empties cart`() {
        val cart = AccessoryCart()
            .add(variant(1))
            .add(variant(2))
            .clear()
        assertTrue(cart.items.isEmpty())
    }

    @Test
    fun `estimated total sums correctly`() {
        val cart = AccessoryCart()
            .add(variant(1, price = BigDecimal("100.00")))
            .add(variant(1, price = BigDecimal("100.00")))
            .add(variant(2, price = BigDecimal("250.00")))
        assertEquals(BigDecimal("450.00"), cart.estimatedTotal)
    }

    @Test
    fun `cannot add unavailable variant`() {
        val cart = AccessoryCart()
            .add(variant(1, availability = AccessoryAvailability.UNAVAILABLE))
        assertTrue(cart.items.isEmpty())
    }

    @Test
    fun `can add low_stock variant`() {
        val cart = AccessoryCart()
            .add(variant(1, availability = AccessoryAvailability.LOW_STOCK))
        assertEquals(1, cart.items.size)
    }

    @Test
    fun `cannot add unknown availability variant`() {
        val cart = AccessoryCart()
            .add(variant(1, availability = AccessoryAvailability.UNKNOWN))
        assertTrue(cart.items.isEmpty())
    }

    @Test
    fun `isEmpty returns true for empty cart`() {
        assertTrue(AccessoryCart().isEmpty)
    }

    @Test
    fun `itemCount returns total quantity`() {
        val cart = AccessoryCart()
            .add(variant(1))
            .add(variant(1))
            .add(variant(2))
        assertEquals(3, cart.itemCount)
    }
}
