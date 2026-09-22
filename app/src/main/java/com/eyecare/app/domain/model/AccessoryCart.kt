package com.eyecare.app.domain.model

import java.math.BigDecimal

data class AccessoryCartItem(
    val productVariantId: Int,
    val productName: String,
    val variantName: String,
    val imagePath: String?,
    val unitPrice: BigDecimal,
    val availability: AccessoryAvailability,
    val quantity: Int,
)

data class AccessoryCart(
    val items: List<AccessoryCartItem> = emptyList(),
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val itemCount: Int get() = items.sumOf { it.quantity }

    val estimatedTotal: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { total, item ->
            total + item.unitPrice * BigDecimal(item.quantity)
        }

    fun add(
        variant: AccessoryVariant,
        productName: String = "Product",
        variantName: String = variant.name,
        imagePath: String? = null,
        quantity: Int = 1,
    ): AccessoryCart {
        if (!variant.availability.isOrderable) return this
        if (quantity !in 1..5) return this

        val existing = items.find { it.productVariantId == variant.id }
        return if (existing != null) {
            var updated = this
            repeat(quantity) {
                updated = updated.increment(variant.id)
            }
            updated
        } else {
            if (items.size >= 20) return this
            copy(
                items = items + AccessoryCartItem(
                    productVariantId = variant.id,
                    productName = productName,
                    variantName = variantName,
                    imagePath = imagePath,
                    unitPrice = variant.price,
                    availability = variant.availability,
                    quantity = quantity,
                ),
            )
        }
    }

    fun increment(variantId: Int): AccessoryCart {
        val index = items.indexOfFirst { it.productVariantId == variantId }
        if (index < 0) return this
        val item = items[index]
        if (item.quantity >= 5) return this
        return copy(items = items.toMutableList().apply {
            set(index, item.copy(quantity = item.quantity + 1))
        })
    }

    fun decrement(variantId: Int): AccessoryCart {
        val index = items.indexOfFirst { it.productVariantId == variantId }
        if (index < 0) return this
        val item = items[index]
        return if (item.quantity <= 1) {
            remove(variantId)
        } else {
            copy(items = items.toMutableList().apply {
                set(index, item.copy(quantity = item.quantity - 1))
            })
        }
    }

    fun remove(variantId: Int): AccessoryCart {
        return copy(items = items.filter { it.productVariantId != variantId })
    }

    fun clear(): AccessoryCart = AccessoryCart()
}
