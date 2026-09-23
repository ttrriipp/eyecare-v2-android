package com.eyecare.app.presentation.accessories

import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.AccessoryOrderRequestItem
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.DiscountProofResult
import com.eyecare.app.domain.model.DiscountProofStatus
import com.eyecare.app.domain.model.DiscountType
import com.eyecare.app.domain.model.DiscountProofUpload
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.domain.repository.AccessoryOrderRequestRepository
import com.eyecare.app.domain.repository.PaginatedResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AccessoryCheckoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AccessoryOrderRequestRepository

    private fun orderRequest(
        id: Int = 10,
        status: String = "pending",
        discountType: DiscountType = DiscountType.NONE,
    ) = AccessoryOrderRequest(
        id = id,
        requestNumber = "ORQ-$id",
        status = OrderRequestStatus.from(status),
        subtotalAmount = BigDecimal("100.00"),
        requestedDiscountType = discountType,
        resolvedBy = null,
        resolvedAt = null,
        items = emptyList(),
        rejectionReason = null,
        cancelledAt = null,
        createdAt = "2026-09-20T10:00:00+08:00",
        order = null,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is idle`() = runTest {
        val viewModel = AccessoryCheckoutViewModel(repository)
        assertTrue(viewModel.uiState.value is CheckoutUiState.Idle)
    }

    @Test
    fun `submit success clears state and emits requestId`() = runTest {
        coEvery { repository.submitRequest(any(), any()) } returns Result.success(orderRequest(10))
        val viewModel = AccessoryCheckoutViewModel(repository)

        viewModel.submit("none", listOf(42 to 2))
        advanceUntilIdle()

        val state = viewModel.uiState.value as CheckoutUiState.Success
        assertEquals(10, state.requestId)
        assertTrue(!state.requiresDiscountProof)
    }

    @Test
    fun `discounted request without proof is blocked before creation`() = runTest {
        val viewModel = AccessoryCheckoutViewModel(repository)

        viewModel.submit("senior_citizen", listOf(42 to 2))

        val state = viewModel.uiState.value as CheckoutUiState.Error
        assertTrue(state.message.contains("proof image"))
        coVerify(exactly = 0) { repository.submitRequest(any(), any()) }
    }

    @Test
    fun `discounted request uploads proof before succeeding`() = runTest {
        coEvery {
            repository.submitRequest(any(), any())
        } returns Result.success(orderRequest(10, discountType = DiscountType.SENIOR_CITIZEN))
        coEvery {
            repository.uploadDiscountProof(10, any())
        } returns Result.success(
            DiscountProofResult(
                id = 20,
                status = DiscountProofStatus.PENDING,
                createdAt = "2026-09-20T10:00:00+08:00",
            ),
        )
        val viewModel = AccessoryCheckoutViewModel(repository)

        viewModel.submit(
            discountType = "senior_citizen",
            items = listOf(42 to 2),
            proof = DiscountProofUpload(imageFile = File("proof.jpg")),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as CheckoutUiState.Success
        assertTrue(state.proofSubmitted)
        coVerify(exactly = 1) { repository.uploadDiscountProof(10, any()) }
    }

    @Test
    fun `submit preserves cart on failure`() = runTest {
        coEvery { repository.submitRequest(any(), any()) } returns Result.failure(RuntimeException("fail"))
        val viewModel = AccessoryCheckoutViewModel(repository)

        viewModel.submit("none", listOf(42 to 2))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CheckoutUiState.Error)
    }

    @Test
    fun `rapid taps produce single submission`() = runTest {
        coEvery { repository.submitRequest(any(), any()) } returns Result.success(orderRequest(10))
        val viewModel = AccessoryCheckoutViewModel(repository)

        viewModel.submit("none", listOf(42 to 2))
        viewModel.submit("none", listOf(42 to 2)) // second call ignored
        advanceUntilIdle()

        val state = viewModel.uiState.value as CheckoutUiState.Success
        assertEquals(10, state.requestId)
    }

    @Test
    fun `active order request conflict navigates to requests`() = runTest {
        coEvery { repository.submitRequest(any(), any()) } returns Result.failure(
            ApiDomainError(422, "ACTIVE_ORDER_REQUEST_EXISTS", "Active request exists"),
        )
        val viewModel = AccessoryCheckoutViewModel(repository)

        viewModel.submit("none", listOf(42 to 2))
        advanceUntilIdle()

        val state = viewModel.uiState.value as CheckoutUiState.Error
        assertTrue(state.isConflict)
    }

    @Test
    fun `discount selection updates`() = runTest {
        val viewModel = AccessoryCheckoutViewModel(repository)
        viewModel.selectDiscount("senior_citizen")
        assertEquals("senior_citizen", viewModel.selectedDiscount.value)
    }
}
