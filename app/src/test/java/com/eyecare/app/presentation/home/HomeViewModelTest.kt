package com.eyecare.app.presentation.home

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
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var appointmentRepo: AppointmentRepository
    private lateinit var orderRepo: OrderRepository
    private lateinit var productRepo: ProductRepository
    private lateinit var prescriptionRepo: PrescriptionRepository

    private val futureAppt = Appointment(1, "eye_exam", AppointmentStatus.CONFIRMED,
        "${LocalDate.now().plusDays(3)}T10:00:00Z", null, null)
    private val pastAppt = Appointment(2, "follow_up", AppointmentStatus.COMPLETED,
        "${LocalDate.now().minusDays(5)}T10:00:00Z", null, null)
    private val activeOrder = Order(1, "ORD-001", null, null, false, OrderStatus.PROCESSING,
        "165.00", "165.00", emptyList(), "${LocalDate.now().minusDays(1)}T10:00:00Z")
    private val expiredPrescription = Prescription(1, 1, null, null, null, null,
        null, null, null, null, null,
        prescribedAt = "${LocalDate.now().minusYears(1)}",
        expiresAt = "${LocalDate.now().minusDays(5)}", notes = null)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        appointmentRepo = mockk()
        orderRepo = mockk()
        productRepo = mockk()
        prescriptionRepo = mockk()
        coEvery { productRepo.getProducts(any()) } returns Result.success(emptyList())
        coEvery { productRepo.hasMorePages(any()) } returns false
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = HomeViewModel(appointmentRepo, orderRepo, productRepo, prescriptionRepo)

    @Test
    fun `nextAppointment is the soonest future confirmed appointment`() = runTest {
        coEvery { appointmentRepo.getAppointments() } returns Result.success(listOf(pastAppt, futureAppt))
        coEvery { orderRepo.getOrders(any()) } returns Result.success(emptyList())
                coEvery { orderRepo.hasMorePages(any()) } returns false
        coEvery { prescriptionRepo.getPrescriptions() } returns Result.success(emptyList())
        val state = vm().uiState.value as HomeUiState.Success
        assertEquals(futureAppt, state.nextAppointment)
    }

    @Test
    fun `activeOrder is the most recent non-completed order`() = runTest {
        coEvery { appointmentRepo.getAppointments() } returns Result.success(emptyList())
        coEvery { orderRepo.getOrders(any()) } returns Result.success(listOf(activeOrder))
                coEvery { orderRepo.hasMorePages(any()) } returns false
        coEvery { prescriptionRepo.getPrescriptions() } returns Result.success(emptyList())
        val state = vm().uiState.value as HomeUiState.Success
        assertEquals(activeOrder, state.activeOrder)
    }

    @Test
    fun `expiringPrescription is set when prescription expires within 30 days`() = runTest {
        coEvery { appointmentRepo.getAppointments() } returns Result.success(emptyList())
        coEvery { orderRepo.getOrders(any()) } returns Result.success(emptyList())
                coEvery { orderRepo.hasMorePages(any()) } returns false
        coEvery { prescriptionRepo.getPrescriptions() } returns Result.success(listOf(expiredPrescription))
        val state = vm().uiState.value as HomeUiState.Success
        assertNotNull(state.expiringPrescription)
    }

    @Test
    fun `expiringPrescription is null when no expiry within 30 days`() = runTest {
        val healthyPrescription = expiredPrescription.copy(
            expiresAt = "${LocalDate.now().plusMonths(6)}"
        )
        coEvery { appointmentRepo.getAppointments() } returns Result.success(emptyList())
        coEvery { orderRepo.getOrders(any()) } returns Result.success(emptyList())
                coEvery { orderRepo.hasMorePages(any()) } returns false
        coEvery { prescriptionRepo.getPrescriptions() } returns Result.success(listOf(healthyPrescription))
        val state = vm().uiState.value as HomeUiState.Success
        assertNull(state.expiringPrescription)
    }
}
