package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.BillingRecordApiService
import com.eyecare.app.data.remote.dto.BillingRecordDtos
import com.eyecare.app.data.remote.dto.PaginationMeta
import com.eyecare.app.domain.model.BillingRecordStatus
import com.eyecare.app.domain.model.BillingPaymentStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingRecordRepositoryImplTest {

    private val api: BillingRecordApiService = mockk()
    private val repository = BillingRecordRepositoryImpl(api)

    private fun createPaymentDto(
        id: Int = 1,
        amount: Double = 5000.00,
        method: String = "gcash",
        ref: String? = "GC-12345",
        status: String = "posted",
    ) = BillingRecordDtos.BillingPaymentDto(
        id = id,
        amount = BigDecimal.valueOf(amount),
        paymentMethod = method,
        referenceNumber = ref,
        status = status,
    )

    private fun createRecordDto(
        id: Int = 1,
        number: String = "BR-2026-000001",
        jobOrderId: Int = 1,
        status: String = "partially_paid",
        total: Double = 8000.00,
        paid: Double = 5000.00,
        balance: Double = 3000.00,
        recordedAt: String? = "2026-07-27T16:00:00+08:00",
        payments: List<BillingRecordDtos.BillingPaymentDto> = listOf(createPaymentDto()),
    ) = BillingRecordDtos.BillingRecordDto(
        id = id,
        billingRecordNumber = number,
        jobOrderId = jobOrderId,
        status = status,
        totalAmount = BigDecimal.valueOf(total),
        amountPaid = BigDecimal.valueOf(paid),
        balanceDue = BigDecimal.valueOf(balance),
        recordedAt = recordedAt,
        payments = payments,
    )

    @Test
    fun `getBillingRecords maps paginated list`() = runTest {
        val dto = createRecordDto()
        val response = BillingRecordDtos.BillingRecordListResponse(
            data = listOf(dto),
            meta = PaginationMeta(currentPage = 1, lastPage = 2, perPage = 15, total = 20),
        )
        coEvery { api.getBillingRecords(any()) } returns response

        val result = repository.getBillingRecords(1).getOrThrow()

        assertEquals(1, result.data.size)
        assertEquals(1, result.currentPage)
        assertEquals(2, result.lastPage)
        assertEquals(20, result.total)
        assertTrue(result.hasMorePages)
    }

    @Test
    fun `getBillingRecord maps detail with payments`() = runTest {
        val dto = createRecordDto(status = "paid", paid = 8000.00, balance = 0.00)
        val response = BillingRecordDtos.BillingRecordResponse(data = dto)
        coEvery { api.getBillingRecord(1) } returns response

        val record = repository.getBillingRecord(1).getOrThrow()

        assertEquals(1, record.id)
        assertEquals("BR-2026-000001", record.billingRecordNumber)
        assertEquals(BillingRecordStatus.PAID, record.status)
        assertEquals(BigDecimal.valueOf(8000.00), record.totalAmount)
        assertEquals(BigDecimal.valueOf(8000.00), record.amountPaid)
        assertEquals(BigDecimal.valueOf(0.00), record.balanceDue)
        assertEquals(1, record.payments.size)
        assertEquals(BillingPaymentStatus.POSTED, record.payments[0].status)
        assertEquals("gcash", record.payments[0].paymentMethod)
    }

    @Test
    fun `unknown status maps to UNKNOWN`() = runTest {
        val dto = createRecordDto(status = "future_status")
        val response = BillingRecordDtos.BillingRecordResponse(data = dto)
        coEvery { api.getBillingRecord(1) } returns response

        val record = repository.getBillingRecord(1).getOrThrow()
        assertEquals(BillingRecordStatus.UNKNOWN, record.status)
    }

    @Test
    fun `nullable recordedAt maps correctly`() = runTest {
        val dto = createRecordDto(recordedAt = null, payments = emptyList())
        val response = BillingRecordDtos.BillingRecordResponse(data = dto)
        coEvery { api.getBillingRecord(1) } returns response

        val record = repository.getBillingRecord(1).getOrThrow()
        assertNull(record.recordedAt)
        assertEquals(0, record.payments.size)
    }
}
