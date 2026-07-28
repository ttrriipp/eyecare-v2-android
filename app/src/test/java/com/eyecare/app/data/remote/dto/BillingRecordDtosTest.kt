package com.eyecare.app.data.remote.dto

import com.eyecare.app.di.NetworkModule
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingRecordDtosTest {

    private val json: Json = NetworkModule.provideJson()

    @Test
    fun `billing record list decodes paginated response with money`() {
        val responseJson = """
        {
          "data": [
            {
              "id": 1,
              "billing_record_number": "BR-2026-000001",
              "patient_id": 1,
              "job_order_id": 1,
              "encounter_id": 1,
              "status": "partially_paid",
              "total_amount": 8000.00,
              "amount_paid": 5000.00,
              "balance_due": 3000.00,
              "recorded_by": 1,
              "recorded_at": "2026-07-27T16:00:00+08:00",
              "created_at": "2026-07-27T10:00:00+08:00",
              "updated_at": "2026-07-27T16:00:00+08:00",
              "deleted_at": null,
              "payments": [
                {
                  "id": 1,
                  "billing_record_id": 1,
                  "amount": 5000.00,
                  "payment_method": "gcash",
                  "reference_number": "GC-12345",
                  "recorded_by": 1,
                  "recorded_at": "2026-07-27T16:00:00+08:00",
                  "notes": null,
                  "status": "posted",
                  "created_at": "2026-07-27T16:00:00+08:00",
                  "updated_at": "2026-07-27T16:00:00+08:00"
                }
              ]
            }
          ],
          "links": { "first": "...", "last": "...", "prev": null, "next": null },
          "meta": { "current_page": 1, "last_page": 1, "per_page": 15, "total": 1 }
        }
        """.trimIndent()

        val response = json.decodeFromString<BillingRecordDtos.BillingRecordListResponse>(responseJson)
        assertEquals(1, response.data.size)
        assertNotNull(response.meta)
        assertEquals(1, response.meta?.total)

        val record = response.data[0]
        assertEquals(1, record.id)
        assertEquals("BR-2026-000001", record.billingRecordNumber)
        assertEquals(1, record.jobOrderId)
        assertEquals("partially_paid", record.status)
        assertEquals(BigDecimal("8000.00"), record.totalAmount)
        assertEquals(BigDecimal("5000.00"), record.amountPaid)
        assertEquals(BigDecimal("3000.00"), record.balanceDue)
        assertNotNull(record.recordedAt)
        assertEquals(1, record.payments.size)
    }

    @Test
    fun `billing record detail decodes with payments`() {
        val responseJson = """
        {
          "data": {
            "id": 1,
            "billing_record_number": "BR-2026-000001",
            "patient_id": 1,
            "job_order_id": 1,
            "encounter_id": null,
            "status": "paid",
            "total_amount": 8000,
            "amount_paid": 8000,
            "balance_due": 0,
            "recorded_by": 1,
            "recorded_at": "2026-07-27T16:00:00+08:00",
            "created_at": "2026-07-27T10:00:00+08:00",
            "updated_at": "2026-07-27T16:00:00+08:00",
            "deleted_at": null,
            "payments": []
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<BillingRecordDtos.BillingRecordResponse>(responseJson)
        val record = response.data

        assertEquals("paid", record.status)
        assertEquals(BigDecimal("8000"), record.totalAmount)
        assertEquals(BigDecimal("0"), record.balanceDue)
        assertNull(record.encounterId)
        assertEquals(0, record.payments.size)
    }

    @Test
    fun `billing record with decimal string money decodes correctly`() {
        val responseJson = """
        {
          "data": {
            "id": 2,
            "billing_record_number": "BR-2026-000002",
            "job_order_id": 2,
            "status": "unpaid",
            "total_amount": "12345.67",
            "amount_paid": "0.00",
            "balance_due": "12345.67",
            "recorded_at": null,
            "payments": []
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<BillingRecordDtos.BillingRecordResponse>(responseJson)
        val record = response.data

        assertEquals(BigDecimal("12345.67"), record.totalAmount)
        assertEquals(BigDecimal("0.00"), record.amountPaid)
        assertEquals(BigDecimal("12345.67"), record.balanceDue)
        assertNull(record.recordedAt)
    }

    @Test
    fun `unknown status maps safely`() {
        val responseJson = """
        {
          "data": {
            "id": 3,
            "billing_record_number": "BR-2026-000003",
            "job_order_id": 3,
            "status": "some_future_status",
            "total_amount": 100,
            "amount_paid": 0,
            "balance_due": 100,
            "payments": []
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<BillingRecordDtos.BillingRecordResponse>(responseJson)
        assertEquals("some_future_status", response.data.status)
    }
}
