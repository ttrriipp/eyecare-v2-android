package com.eyecare.app.presentation.accessories

import androidx.lifecycle.ViewModel
import com.eyecare.app.domain.model.AccessoryCart
import com.eyecare.app.domain.model.AccessoryVariant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AccessoryCartViewModel @Inject constructor() : ViewModel() {

    private val _cart = MutableStateFlow(AccessoryCart())
    val cart: StateFlow<AccessoryCart> = _cart.asStateFlow()

    fun addToCart(
        variant: AccessoryVariant,
        productName: String = "Product",
        variantName: String = variant.name,
        imagePath: String? = null,
    ): Boolean {
        val current = _cart.value
        val updated = current.add(variant, productName, variantName, imagePath)
        _cart.value = updated
        return updated != current
    }

    fun increment(variantId: Int) {
        _cart.value = _cart.value.increment(variantId)
    }

    fun decrement(variantId: Int) {
        _cart.value = _cart.value.decrement(variantId)
    }

    fun remove(variantId: Int) {
        _cart.value = _cart.value.remove(variantId)
    }

    fun clear() {
        _cart.value = _cart.value.clear()
    }
}
