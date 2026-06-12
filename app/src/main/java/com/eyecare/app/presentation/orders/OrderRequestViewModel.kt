package com.eyecare.app.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.remote.dto.OrderDtos
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.model.ProductVariant
import com.eyecare.app.domain.repository.AppointmentRepository
import com.eyecare.app.domain.repository.OrderRepository
import com.eyecare.app.domain.repository.ProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LensType(val id: Int, val label: String) {
    SINGLE_VISION(1, "Single Vision"),
    BIFOCAL(2, "Bifocal"),
    PROGRESSIVE(3, "Progressive"),
}

sealed interface OrderRequestUiState {
    data object Loading : OrderRequestUiState
    data class Ready(
        val product: Product,
        val selectedVariant: ProductVariant,
        val appointments: List<Appointment> = emptyList(),
        val selectedLensType: LensType? = null,
        val quantity: Int = 1,
        val isNonPrescription: Boolean = false,
        val linkedAppointmentId: Int? = null,
        val isSubmitting: Boolean = false,
        val error: String? = null,
    ) : OrderRequestUiState
    data class Submitted(val order: Order) : OrderRequestUiState
    data class Error(val message: String) : OrderRequestUiState
}

@HiltViewModel(assistedFactory = OrderRequestViewModel.Factory::class)
class OrderRequestViewModel @AssistedInject constructor(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val appointmentRepository: AppointmentRepository,
    @Assisted("productId") private val productId: Int,
    @Assisted("variantId") private val variantId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("productId") productId: Int,
            @Assisted("variantId") variantId: Int,
        ): OrderRequestViewModel
    }

    private val _uiState = MutableStateFlow<OrderRequestUiState>(OrderRequestUiState.Loading)
    val uiState: StateFlow<OrderRequestUiState> = _uiState.asStateFlow()

    init { load() }

    fun selectLensType(lensType: LensType) = updateReady { copy(selectedLensType = lensType, error = null) }
    fun setQuantity(qty: Int) = updateReady { copy(quantity = qty.coerceIn(1, 4)) }
    fun toggleNonPrescription(value: Boolean) = updateReady { copy(isNonPrescription = value) }
    fun linkAppointment(id: Int?) = updateReady { copy(linkedAppointmentId = id) }

    fun submit() {
        val state = _uiState.value as? OrderRequestUiState.Ready ?: return
        if (!state.isNonPrescription && state.selectedLensType == null) {
            updateReady { copy(error = "Please select a lens type") }
            return
        }
        viewModelScope.launch {
            updateReady { copy(isSubmitting = true, error = null) }
            orderRepository.createOrder(
                appointmentId = state.linkedAppointmentId,
                isNonPrescription = state.isNonPrescription,
                items = listOf(
                    OrderDtos.OrderItemRequest(
                        productVariantId = state.selectedVariant.id,
                        lensTypeId = state.selectedLensType?.id ?: 1,
                        quantity = state.quantity,
                    )
                ),
            ).fold(
                onSuccess = { _uiState.value = OrderRequestUiState.Submitted(it) },
                onFailure = { updateReady { copy(isSubmitting = false, error = it.message) } },
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            val productDeferred = async { productRepository.getProduct(productId) }
            val appointmentsDeferred = async { appointmentRepository.getAppointments() }

            val productResult = productDeferred.await()
            val appointmentsResult = appointmentsDeferred.await()

            productResult.fold(
                onSuccess = { product ->
                    val variant = product.variants.firstOrNull { it.id == variantId }
                        ?: product.variants.firstOrNull()
                        ?: return@fold run { _uiState.value = OrderRequestUiState.Error("Variant not found") }
                    _uiState.value = OrderRequestUiState.Ready(
                        product = product,
                        selectedVariant = variant,
                        appointments = appointmentsResult.getOrElse { emptyList() },
                    )
                },
                onFailure = { _uiState.value = OrderRequestUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun updateReady(transform: OrderRequestUiState.Ready.() -> OrderRequestUiState.Ready) {
        val current = _uiState.value as? OrderRequestUiState.Ready ?: return
        _uiState.value = current.transform()
    }
}
