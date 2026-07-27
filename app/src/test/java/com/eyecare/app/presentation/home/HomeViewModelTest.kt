package com.eyecare.app.presentation.home

import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.PaginatedResult
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
    private lateinit var appointmentRepo: AppointmentV1Repository
    private lateinit var orderRepo: OrderRepository
    private lateinit var productRepo: ProductRepository
    private lateinit var prescriptionRepo: PrescriptionRepository

    private val futureAppt = AppointmentV1(1, "APT-001", "New Patient", 30, null, AppointmentStatus.CONFIRMED,
        "${LocalDate.now().plusDays(3)}T10:00:00+08:00", null, null, "mobile", null)
    private val pastAppt = AppointmentV1(2, "APT-002", "Follow-up", 15, null, AppointmentStatus.COMPLETED,
        "${LocalDate.now().minusDays(5)}T10:00:00+08:00", null, null, "mobile", null)
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
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(PaginatedResult(emptyList(), 1, 1, 0))
        coEvery { orderRepo.getOrders(any()) } returns Result.success(emptyList())
        coEvery { orderRepo.hasMorePages(any()) } returns false
        coEvery { productRepo.getProducts(any()) } returns Result.success(emptyList())
        coEvery { productRepo.hasMorePages(any()) } returns false
        coEvery { prescriptionRepo.getPrescriptions() } returns Result.success(emptyList())
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = HomeViewModel(appointmentRepo, orderRepo, productRepo, prescriptionRepo)

    @Test
    fun `nextAppointment is the soonest future confirmed appointment`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(PaginatedResult(listOf(pastAppt, futureAppt), 1, 1, 2))
        coEvery { orderRepo.getOrders(any()) } returns Result.success(emptyList())
                coEvery { orderRepo.hasMorePages(any()) } returns false
        coEvery { prescriptionRepo.getPrescriptions() } returns Result.success(emptyList())
        val state = vm().uiState.value as HomeUiState.Success
        assertEquals(futureAppt, state.nextAppointment)
    }

    @Test
    fun `activeOrder is the most recent non-completed order`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(PaginatedResult(emptyList(), 1, 1, 0))
        coEvery { orderRepo.getOrders(any()) } returns Result.success(listOf(activeOrder))
                coEvery { orderRepo.hasMorePages(any()) } returns false
        coEvery { prescriptionRepo.getPrescriptions() } returns Result.success(emptyList())
        val state = vm().uiState.value as HomeUiState.Success
        assertEquals(activeOrder, state.activeOrder)
    }

    @Test
    fun `expiringPrescription is set when prescription expires within 30 days`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(PaginatedResult(emptyList(), 1, 1, 0))
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
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(PaginatedResult(emptyList(), 1, 1, 0))
        coEvery { orderRepo.getOrders(any()) } returns Result.success(emptyList())
                coEvery { orderRepo.hasMorePages(any()) } returns false
        coEvery { prescriptionRepo.getPrescriptions() } returns Result.success(listOf(healthyPrescription))
        val state = vm().uiState.value as HomeUiState.Success
        assertNull(state.expiringPrescription)
    }

    @Test
    fun `products are curated into capped home groups in source order`() = runTest {
        val products = listOf(
            product(1, productType = "frame", category = "Eyeglasses"),
            product(2, productType = "accessory", category = "Contact Lens Accessories"),
            product(3, productType = "accessory", category = "Cleaning Kit"),
            product(4, productType = "accessory", category = "Contact Solutions"),
            product(5, productType = "service", category = "Eye Exam"),
            product(6, productType = "frame", category = "Sunglasses"),
            product(7, productType = "frame", category = "Eyeglasses"),
            product(8, productType = "frame", category = "Eyeglasses"),
            product(9, productType = "frame", category = "Eyeglasses"),
        )
        coEvery { productRepo.getProducts(any()) } returns Result.success(products)

        val state = vm().uiState.value as HomeUiState.Success

        assertEquals(listOf(1, 6, 7, 8), state.featuredFrames.map(Product::id))
        assertEquals(listOf(2, 3), state.accessories.map(Product::id))
        assertEquals(listOf(4), state.eyeCareEssentials.map(Product::id))
    }

    @Test
    fun `accessory category matching ignores case and separator differences`() = runTest {
        val products = listOf(
            product(1, productType = "accessory", category = "ACCESSORY"),
            product(2, productType = "accessory", category = "Protective_Cases"),
            product(3, productType = "accessory", category = "cleaning-kits"),
        )
        coEvery { productRepo.getProducts(any()) } returns Result.success(products)

        val state = vm().uiState.value as HomeUiState.Success

        assertEquals(listOf(1, 2, 3), state.accessories.map(Product::id))
        assertEquals(emptyList<Product>(), state.eyeCareEssentials)
    }

    @Test
    fun `non mobile product types do not appear in home product groups`() = runTest {
        val products = listOf(
            product(1, productType = "accessory", category = "Accessories"),
            product(2, productType = "contact_lens", category = "Contact Lenses"),
            product(3, productType = "lens", category = "Prescription Lenses"),
            product(4, productType = "service", category = "Eye Exam"),
        )
        coEvery { productRepo.getProducts(any()) } returns Result.success(products)

        val state = vm().uiState.value as HomeUiState.Success

        assertEquals(listOf(1), state.accessories.map(Product::id))
        assertEquals(emptyList<Product>(), state.eyeCareEssentials)
    }

    private fun product(
        id: Int,
        productType: String,
        category: String,
    ) = Product(
        id = id,
        name = "Product $id",
        slug = "product-$id",
        description = null,
        productType = productType,
        brand = "Padilla Optical",
        category = category,
        variants = emptyList(),
        images = emptyList(),
    )
}
