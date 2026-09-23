package com.eyecare.app.presentation.accessories

import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.DiscountType
import com.eyecare.app.domain.model.DiscountProofResult
import com.eyecare.app.domain.model.DiscountProofStatus
import com.eyecare.app.domain.model.DiscountProofUpload
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.domain.repository.AccessoryOrderRequestRepository
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AccessoryOrderRequestDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AccessoryOrderRequestRepository

    private fun request(
        id: Int = 10,
        status: OrderRequestStatus = OrderRequestStatus.PENDING,
        discountType: DiscountType = DiscountType.NONE,
        proofStatus: DiscountProofStatus = if (discountType == DiscountType.NONE) DiscountProofStatus.NOT_REQUIRED else DiscountProofStatus.NOT_SUBMITTED,
        rejectionReason: String? = null,
    ) = AccessoryOrderRequest(
        id = id,
        requestNumber = "ORQ-$id",
        status = status,
        subtotalAmount = BigDecimal("700.00"),
        requestedDiscountType = discountType,
        resolvedBy = null,
        resolvedAt = null,
        items = emptyList(),
        rejectionReason = null,
        cancelledAt = null,
        createdAt = "2026-09-20T10:00:00+08:00",
        order = null,
        discountProofStatus = proofStatus,
        discountProofRejectionReason = rejectionReason,
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

    private fun createViewModel(requestId: Int = 10) = AccessoryOrderRequestDetailViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("requestId" to requestId)),
    )

    @Test
    fun `initial load shows loading then success`() = runTest {
        coEvery { repository.getRequest(10) } returns Result.success(request(10))
        val viewModel = createViewModel(10)

        assertTrue(viewModel.uiState.value is RequestDetailUiState.Loading)
        advanceUntilIdle()
        val state = viewModel.uiState.value as RequestDetailUiState.Success
        assertEquals(10, state.request.id)
    }

    @Test
    fun `pending shows cancel action`() = runTest {
        coEvery { repository.getRequest(10) } returns Result.success(request(10, OrderRequestStatus.PENDING))
        val viewModel = createViewModel(10)
        advanceUntilIdle()

        val state = viewModel.uiState.value as RequestDetailUiState.Success
        assertTrue(state.canCancel)
    }

    @Test
    fun `accepted shows order navigation`() = runTest {
        coEvery { repository.getRequest(10) } returns Result.success(request(10, OrderRequestStatus.ACCEPTED))
        val viewModel = createViewModel(10)
        advanceUntilIdle()

        val state = viewModel.uiState.value as RequestDetailUiState.Success
        assertFalse(state.canCancel)
    }

    @Test
    fun `cancel uses returned resource`() = runTest {
        coEvery { repository.getRequest(10) } returns Result.success(request(10, OrderRequestStatus.PENDING))
        coEvery { repository.cancelRequest(10, any()) } returns Result.success(request(10, OrderRequestStatus.CANCELLED))
        val viewModel = createViewModel(10)
        advanceUntilIdle()

        viewModel.cancel("Changed my mind")
        advanceUntilIdle()

        val state = viewModel.uiState.value as RequestDetailUiState.Success
        assertEquals(OrderRequestStatus.CANCELLED, state.request.status)
    }

    @Test
    fun `cancel handles idempotent success`() = runTest {
        coEvery { repository.getRequest(10) } returns Result.success(request(10, OrderRequestStatus.PENDING))
        coEvery { repository.cancelRequest(10, any()) } returns Result.success(request(10, OrderRequestStatus.CANCELLED))
        val viewModel = createViewModel(10)
        advanceUntilIdle()

        viewModel.cancel("Changed my mind")
        advanceUntilIdle()
        // Second cancel on already cancelled
        viewModel.cancel("Changed my mind")
        advanceUntilIdle()

        val state = viewModel.uiState.value as RequestDetailUiState.Success
        assertEquals(OrderRequestStatus.CANCELLED, state.request.status)
    }

    @Test
    fun `not actionable refreshes detail`() = runTest {
        coEvery { repository.getRequest(10) } returns Result.success(request(10, OrderRequestStatus.PENDING))
        coEvery { repository.cancelRequest(10, any()) } returns Result.failure(
            ApiDomainError(422, "ORDER_REQUEST_NOT_ACTIONABLE", "Not actionable")
        )
        coEvery { repository.getRequest(10) } returns Result.success(request(10, OrderRequestStatus.CANCELLED))
        val viewModel = createViewModel(10)
        advanceUntilIdle()

        viewModel.cancel("Changed my mind")
        advanceUntilIdle()

        val state = viewModel.uiState.value as RequestDetailUiState.Success
        assertEquals(OrderRequestStatus.CANCELLED, state.request.status)
    }

    @Test
    fun `retry reloads after error`() = runTest {
        coEvery { repository.getRequest(10) } returns Result.failure(RuntimeException("fail"))
        val viewModel = createViewModel(10)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is RequestDetailUiState.Error)

        coEvery { repository.getRequest(10) } returns Result.success(request(10))
        viewModel.retry()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is RequestDetailUiState.Success)
    }

    @Test
    fun `uploads discount proof for a pending requested discount and refreshes`() = runTest {
        val first = request(discountType = DiscountType.SENIOR_CITIZEN)
        val refreshed = request(
            discountType = DiscountType.SENIOR_CITIZEN,
            proofStatus = DiscountProofStatus.PENDING,
        )
        coEvery { repository.getRequest(10) } returnsMany listOf(Result.success(first), Result.success(refreshed))
        coEvery { repository.uploadDiscountProof(10, any()) } returns Result.success(
            DiscountProofResult(id = 17, status = DiscountProofStatus.PENDING, createdAt = "2026-09-22T12:00:00+08:00"),
        )
        val viewModel = createViewModel(10)
        advanceUntilIdle()
        val proofFile = File.createTempFile("discount-proof-test-", ".jpg").apply { writeBytes(byteArrayOf(1)) }
        try {
            viewModel.uploadDiscountProof(
                DiscountProofUpload(
                    imageFile = proofFile,
                    mimeType = "image/jpeg",
                    width = 100,
                    height = 100,
                ),
            )
            advanceUntilIdle()
            val state = viewModel.uiState.value as RequestDetailUiState.Success
            assertEquals(DiscountProofStatus.PENDING, state.request.discountProofStatus)
            coVerify(exactly = 1) { repository.uploadDiscountProof(10, any()) }
            coVerify(exactly = 2) { repository.getRequest(10) }
        } finally {
            proofFile.delete()
        }
    }

    @Test
    fun `does not upload while proof is pending`() = runTest {
        coEvery { repository.getRequest(10) } returns Result.success(
            request(
                discountType = DiscountType.PWD,
                proofStatus = DiscountProofStatus.PENDING,
            ),
        )
        val viewModel = createViewModel(10)
        advanceUntilIdle()
        val proofFile = File.createTempFile("discount-proof-test-", ".jpg").apply { writeBytes(byteArrayOf(1)) }
        try {
            viewModel.uploadDiscountProof(DiscountProofUpload(proofFile, "image/jpeg", 100, 100))
            assertTrue((viewModel.uiState.value as RequestDetailUiState.Success).uploadState is DiscountProofUploadState.Error)
            coVerify(exactly = 0) { repository.uploadDiscountProof(any(), any()) }
        } finally {
            proofFile.delete()
        }
    }
}
