package com.eyecare.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.repository.AppointmentRepository
import com.eyecare.app.domain.repository.OrderRepository
import com.eyecare.app.domain.repository.PrescriptionRepository
import com.eyecare.app.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val nextAppointment: Appointment?,
        val activeOrder: Order?,
        val expiringPrescription: Prescription?,
        val newArrivals: List<Product>,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val prescriptionRepository: PrescriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() { load() }

    private fun load() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            val appointmentsDeferred = async { appointmentRepository.getAppointments() }
            val ordersDeferred = async { orderRepository.getOrders() }
            val productsDeferred = async { productRepository.getProducts() }
            val prescriptionsDeferred = async { prescriptionRepository.getPrescriptions() }

            val appointments = appointmentsDeferred.await().getOrElse { emptyList() }
            val orders = ordersDeferred.await().getOrElse { emptyList() }
            val products = productsDeferred.await().getOrElse { emptyList() }
            val prescriptions = prescriptionsDeferred.await().getOrElse { emptyList() }

            val today = LocalDate.now()

            val nextAppointment = appointments
                .filter {
                    it.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.PENDING) &&
                        runCatching { !LocalDate.parse(it.scheduledAt.take(10)).isBefore(today) }.getOrElse { false }
                }
                .minByOrNull { it.scheduledAt }

            val activeOrder = orders
                .filter { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }
                .maxByOrNull { it.createdAt }

            val expiringPrescription = prescriptions
                .filter { p ->
                    p.expiresAt != null && runCatching {
                        val exp = LocalDate.parse(p.expiresAt.take(10))
                        !exp.isBefore(today) && !exp.isAfter(today.plusDays(30))
                    }.getOrElse { false }
                }
                .minByOrNull { it.expiresAt ?: "" }
                ?: prescriptions.firstOrNull { p ->
                    p.expiresAt != null && runCatching {
                        LocalDate.parse(p.expiresAt.take(10)).isBefore(today)
                    }.getOrElse { false }
                }

            _uiState.value = HomeUiState.Success(
                nextAppointment = nextAppointment,
                activeOrder = activeOrder,
                expiringPrescription = expiringPrescription,
                newArrivals = products.take(6),
            )
        }
    }
}
