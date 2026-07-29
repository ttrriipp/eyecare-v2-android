package com.eyecare.app.data.remote.dto

import com.eyecare.app.di.NetworkModule
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EyewearDtosTest {

    private val json: Json = NetworkModule.provideJson()

    @Test
    fun `list decodes summary with all fields`() {
        val responseJson = """
        {
          "data": [{
            "key": "eyw_01K1D7H4R1V87GJ7D2GCB9QT4X",
            "description": "Classic Rectangle Frame + 1 more",
            "consultation_at": "2026-07-27T09:00:00+08:00",
            "created_at": "2026-07-27T10:00:00+08:00",
            "progress": "in_preparation",
            "payment_status": null,
            "total_amount": "8000.00",
            "balance_due": null,
            "activity_at": "2026-07-27T11:00:00+08:00"
          }],
          "meta": {"current_page": 1, "last_page": 1, "per_page": 15, "total": 1}
        }
        """.trimIndent()

        val response = json.decodeFromString<EyewearDtos.EyewearListResponse>(responseJson)
        assertEquals(1, response.data.size)
        val item = response.data[0]
        assertEquals("eyw_01K1D7H4R1V87GJ7D2GCB9QT4X", item.key)
        assertEquals("Classic Rectangle Frame + 1 more", item.description)
        assertEquals("in_preparation", item.progress)
        assertNull(item.paymentStatus)
        assertEquals(BigDecimal("8000.00"), item.totalAmount)
        assertNull(item.balanceDue)
    }

    @Test
    fun `list decodes balance_due payment status`() {
        val responseJson = """
        {
          "data": [{
            "key": "eyw_test",
            "description": "Test",
            "created_at": "2026-07-27T10:00:00+08:00",
            "progress": "dispensed",
            "payment_status": "balance_due",
            "total_amount": 8000,
            "balance_due": 3000,
            "activity_at": "2026-07-29T10:05:00+08:00"
          }],
          "meta": {"current_page": 1, "last_page": 1, "per_page": 15, "total": 1}
        }
        """.trimIndent()

        val response = json.decodeFromString<EyewearDtos.EyewearListResponse>(responseJson)
        val item = response.data[0]
        assertEquals("balance_due", item.paymentStatus)
        assertEquals(BigDecimal("3000"), item.balanceDue)
    }

    @Test
    fun `detail decodes complete linked resource`() {
        val responseJson = """
        {
          "data": {
            "key": "eyw_01K1D7H4R1V87GJ7D2GCB9QT4X",
            "description": "Classic Rectangle Frame + 1 more",
            "consultation_at": "2026-07-27T09:00:00+08:00",
            "created_at": "2026-07-27T10:00:00+08:00",
            "progress": "dispensed",
            "payment_status": "balance_due",
            "total_amount": "8000.00",
            "balance_due": "3000.00",
            "activity_at": "2026-07-29T10:05:00+08:00",
            "estimate": {
              "quotation_number": "QUO-01K1D7",
              "status": "accepted",
              "valid_until": "2026-08-03",
              "subtotal": "8500.00",
              "discount_amount": "500.00",
              "total": "8000.00",
              "items": [{"description": "Frame", "quantity": 1, "unit_price": "4500.00", "amount": "4500.00"}]
            },
            "preparation": {
              "job_order_number": "JO-2026-000017",
              "status": "dispensed",
              "total_amount": "8000.00",
              "started_at": "2026-07-27T11:00:00+08:00",
              "ready_at": "2026-07-28T15:00:00+08:00",
              "items": [{"id": 31, "description": "Frame", "quantity": 1, "unit_price": "4500.00", "amount": "4500.00", "product_variant_id": 42}]
            },
            "dispensing": {
              "status": "dispensed",
              "ready_at": "2026-07-28T15:00:00+08:00",
              "dispensed_at": "2026-07-29T10:00:00+08:00"
            },
            "payment_summary": {
              "billing_record_number": "BR-2026-000017",
              "status": "partially_paid",
              "total_amount": "8000.00",
              "amount_paid": "5000.00",
              "balance_due": "3000.00",
              "payments": [{"id": 44, "amount": "5000.00", "payment_method": "cash", "reference_number": null, "recorded_at": "2026-07-29T10:05:00+08:00"}]
            }
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<EyewearDtos.EyewearDetailResponse>(responseJson)
        val detail = response.data
        assertEquals("eyw_01K1D7H4R1V87GJ7D2GCB9QT4X", detail.key)
        assertEquals("dispensed", detail.progress)
        assertNotNull(detail.estimate)
        assertNotNull(detail.preparation)
        assertNotNull(detail.dispensing)
        assertNotNull(detail.paymentSummary)
        assertEquals(1, detail.estimate!!.items.size)
        assertEquals(BigDecimal("4500.00"), detail.estimate!!.items[0].unitPrice)
        assertEquals(42, detail.preparation!!.items[0].productVariantId)
        assertEquals(1, detail.paymentSummary!!.payments.size)
    }

    @Test
    fun `detail decodes estimate-only partial response`() {
        val responseJson = """
        {
          "data": {
            "key": "eyw_estimate_only",
            "description": "Estimate only",
            "created_at": "2026-07-27T10:00:00+08:00",
            "progress": "estimate_available",
            "total_amount": "5000.00",
            "activity_at": "2026-07-27T10:00:00+08:00",
            "estimate": {
              "subtotal": "5000.00",
              "discount_amount": "0.00",
              "total": "5000.00",
              "items": [{"description": "Frame", "quantity": 1, "unit_price": "5000.00", "amount": "5000.00"}]
            }
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<EyewearDtos.EyewearDetailResponse>(responseJson)
        val detail = response.data
        assertNotNull(detail.estimate)
        assertNull(detail.preparation)
        assertNull(detail.dispensing)
        assertNull(detail.paymentSummary)
    }

    @Test
    fun `null consultation_at and balance_due decode safely`() {
        val responseJson = """
        {
          "data": {
            "key": "eyw_test",
            "description": "Test",
            "created_at": "2026-07-27T10:00:00+08:00",
            "progress": "estimate_available",
            "total_amount": "1000.00",
            "activity_at": "2026-07-27T10:00:00+08:00"
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<EyewearDtos.EyewearDetailResponse>(responseJson)
        assertNull(response.data.consultationAt)
        assertNull(response.data.balanceDue)
        assertNull(response.data.paymentStatus)
    }

    @Test
    fun `unknown progress string maps safely`() {
        val responseJson = """
        {
          "data": [{
            "key": "eyw_unknown",
            "description": "Unknown",
            "created_at": "2026-07-27T10:00:00+08:00",
            "progress": "some_future_status",
            "total_amount": "0.00",
            "activity_at": "2026-07-27T10:00:00+08:00"
          }],
          "meta": {"current_page": 1, "last_page": 1, "per_page": 15, "total": 1}
        }
        """.trimIndent()

        val response = json.decodeFromString<EyewearDtos.EyewearListResponse>(responseJson)
        assertEquals("some_future_status", response.data[0].progress)
    }
}
