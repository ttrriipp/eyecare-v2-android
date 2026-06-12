package com.eyecare.app.presentation.billing

import com.eyecare.app.data.remote.api.BillingApiService
import com.eyecare.app.data.remote.dto.BillingDtos
import com.eyecare.app.domain.model.BillingStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var api: BillingApiService
    private val json = Json { ignoreUnknownKeys = true }

    private val fakeBillingDto = BillingDtos.BillingDto(
        id = 1, orderId = 1, status = "issued", totalAmount = "165.00",
        amountPaid = "0.00", balanceDue = "165.00",
        issuedAt = "2026-10-25T10:00:00Z", createdAt = "2026-10-24T10:00:00Z",
        payments = emptyList(),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        api = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads billing and maps correctly`() = runTest {
        coEvery { api.getBilling(1) } returns BillingDtos.BillingResponse(fakeBillingDto)
        val vm = BillingDetailViewModel(api, json, billingId = 1)

        val state = vm.uiState.value as BillingDetailUiState.Success
        assertEquals("165.00", state.billing.totalAmount)
        assertEquals("0.00", state.billing.amountPaid)
        assertEquals(BillingStatus.ISSUED, state.billing.status)
    }

    @Test
    fun `error state on failure`() = runTest {
        coEvery { api.getBilling(99) } throws RuntimeException("not found")
        val vm = BillingDetailViewModel(api, json, billingId = 99)
        assertInstanceOf(BillingDetailUiState.Error::class.java, vm.uiState.value)
    }
}
