package com.eyecare.app.presentation.accessories

import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.domain.model.AccessoryCart
import com.eyecare.app.domain.model.AccessoryVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AccessoryCartViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

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

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        val viewModel = AccessoryCartViewModel()
        assertTrue(viewModel.cart.value.isEmpty)
        assertEquals(0, viewModel.cart.value.itemCount)
    }

    @Test
    fun `addToCart adds variant`() = runTest {
        val viewModel = AccessoryCartViewModel()
        viewModel.addToCart(variant(1))
        assertEquals(1, viewModel.cart.value.items.size)
        assertEquals(1, viewModel.cart.value.items[0].quantity)
    }

    @Test
    fun `addToCart accepts a requested quantity`() = runTest {
        val viewModel = AccessoryCartViewModel()
        viewModel.addToCart(variant(1), quantity = 3)
        assertEquals(3, viewModel.cart.value.items[0].quantity)
    }

    @Test
    fun `addToCart same variant increments`() = runTest {
        val viewModel = AccessoryCartViewModel()
        viewModel.addToCart(variant(1))
        viewModel.addToCart(variant(1))
        assertEquals(2, viewModel.cart.value.items[0].quantity)
    }

    @Test
    fun `increment increases quantity`() = runTest {
        val viewModel = AccessoryCartViewModel()
        viewModel.addToCart(variant(1))
        viewModel.increment(1)
        assertEquals(2, viewModel.cart.value.items[0].quantity)
    }

    @Test
    fun `decrement reduces quantity`() = runTest {
        val viewModel = AccessoryCartViewModel()
        viewModel.addToCart(variant(1))
        viewModel.addToCart(variant(1))
        viewModel.decrement(1)
        assertEquals(1, viewModel.cart.value.items[0].quantity)
    }

    @Test
    fun `decrement from 1 keeps item for explicit removal`() = runTest {
        val viewModel = AccessoryCartViewModel()
        viewModel.addToCart(variant(1))
        viewModel.decrement(1)
        assertEquals(1, viewModel.cart.value.items.single().quantity)
    }

    @Test
    fun `restore puts a removed item back`() = runTest {
        val viewModel = AccessoryCartViewModel()
        viewModel.addToCart(variant(1), quantity = 2)
        val removed = viewModel.cart.value.items.single()
        viewModel.remove(1)

        viewModel.restore(removed)

        assertEquals(listOf(removed), viewModel.cart.value.items)
    }

    @Test
    fun `remove removes item`() = runTest {
        val viewModel = AccessoryCartViewModel()
        viewModel.addToCart(variant(1))
        viewModel.remove(1)
        assertTrue(viewModel.cart.value.isEmpty)
    }

    @Test
    fun `clear empties cart`() = runTest {
        val viewModel = AccessoryCartViewModel()
        viewModel.addToCart(variant(1))
        viewModel.addToCart(variant(2))
        viewModel.clear()
        assertTrue(viewModel.cart.value.isEmpty)
    }

    @Test
    fun `estimated total updates correctly`() = runTest {
        val viewModel = AccessoryCartViewModel()
        viewModel.addToCart(variant(1, price = BigDecimal("100.00")))
        viewModel.addToCart(variant(2, price = BigDecimal("250.00")))
        assertEquals(BigDecimal("350.00"), viewModel.cart.value.estimatedTotal)
    }

    @Test
    fun `cannot add unavailable variant`() = runTest {
        val viewModel = AccessoryCartViewModel()
        viewModel.addToCart(variant(1, availability = AccessoryAvailability.UNAVAILABLE))
        assertTrue(viewModel.cart.value.isEmpty)
    }

    @Test
    fun `addToCart reports whether cart changed`() = runTest {
        val viewModel = AccessoryCartViewModel()

        assertTrue(viewModel.addToCart(variant(1)))

        viewModel.addToCart(variant(1))
        repeat(3) { viewModel.addToCart(variant(1)) }
        assertTrue(!viewModel.addToCart(variant(1)))
    }
}
