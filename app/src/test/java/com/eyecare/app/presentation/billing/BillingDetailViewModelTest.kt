package com.eyecare.app.presentation.billing

import com.eyecare.app.domain.model.Billing
import com.eyecare.app.domain.model.BillingStatus
import com.eyecare.app.domain.repository.BillingRepository
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
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: BillingRepository

    private val fakeBilling = Billing(
        id = 1, billingNumber = "BIL-2026-000001", status = BillingStatus.ISSUED,
        subtotal = "165.00", discountAmount = "0.00",
        totalAmount = "165.00", amountPaid = "0.00", balanceDue = "165.00",
        issuedAt = "2026-10-25T10:00:00Z", createdAt = "2026-10-24T10:00:00Z",
        items = emptyList(), payments = emptyList(),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads billing and maps correctly`() = runTest {
        coEvery { repo.getBilling(1) } returns Result.success(fakeBilling)
        val vm = BillingDetailViewModel(repo, billingId = 1)

        val state = vm.uiState.value as BillingDetailUiState.Success
        assertEquals("165.00", state.billing.totalAmount)
        assertEquals("0.00", state.billing.amountPaid)
        assertEquals(BillingStatus.ISSUED, state.billing.status)
    }

    @Test
    fun `error state on failure`() = runTest {
        coEvery { repo.getBilling(99) } returns Result.failure(RuntimeException("not found"))
        val vm = BillingDetailViewModel(repo, billingId = 99)
        assertInstanceOf(BillingDetailUiState.Error::class.java, vm.uiState.value)
    }
}
