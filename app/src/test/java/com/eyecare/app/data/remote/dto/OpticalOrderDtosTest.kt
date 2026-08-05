package com.eyecare.app.data.remote.dto

import com.eyecare.app.data.remote.dto.OpticalOrderDtos.OpticalOrderDto
import com.eyecare.app.data.remote.dto.OpticalOrderDtos.OpticalOrderListResponse
import com.eyecare.app.data.remote.dto.OpticalOrderDtos.OpticalOrderResponse
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OpticalOrderDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes complete order list response`() {
        val fixture = """
        {
            "data": [{
                "id": 1,
                "order_number": "OO-2026-001",
                "status": "in_progress",
                "fulfillment_mode": "prepared",
                "total_amount": "5000.00",
                "started_at": "2026-08-02T09:00:00Z",
                "ready_at": null,
                "dispensed_at": null,
                "cancelled_at": null,
                "created_at": "2026-08-01T10:00:00Z",
                "source_quotation": {"id": 1, "quotation_number": "Q-001"},
                "items": [{
                    "id": 10,
                    "description": "Progressive lens",
                    "quantity": 1,
                    "unit_price": "4500.00",
                    "amount": "4500.00",
                    "product_variant_id": 5,
                    "is_rateable": false,
                    "rating": null
                }],
                "payment_summary": {
                    "status": "partially_paid",
                    "total_amount": "5000.00",
                    "amount_paid": "2000.00",
                    "balance_due": "3000.00",
                    "payment_due_date": "2026-09-01",
                    "is_overdue": false
                }
            }],
            "meta": {"current_page": 1, "last_page": 1, "per_page": 15, "total": 1}
        }
        """.trimIndent()

        val response = json.decodeFromString<OpticalOrderListResponse>(fixture)
        assertEquals(1, response.data.size)
        val o = response.data[0]
        assertEquals(1, o.id)
        assertEquals("OO-2026-001", o.orderNumber)
        assertEquals("in_progress", o.status)
        assertEquals("prepared", o.fulfillmentMode)
        assertEquals(BigDecimal("5000.00"), o.totalAmount)
        assertEquals("2026-08-02T09:00:00Z", o.startedAt)
        assertNull(o.readyAt)
        assertNull(o.dispensedAt)
        assertNull(o.cancelledAt)
        assertEquals("2026-08-01T10:00:00Z", o.createdAt)
        assertEquals(1, o.sourceQuotation?.id)
        assertEquals(1, o.items.size)
        assertEquals(5, o.items[0].productVariantId)
        assertFalse(o.items[0].isRateable)
        val ps = o.paymentSummary!!
        assertEquals("partially_paid", ps.status)
        assertEquals(BigDecimal("3000.00"), ps.balanceDue)
        assertFalse(ps.isOverdue)
    }

    @Test
    fun `decodes dispensed order with rating`() {
        val fixture = """
        {
            "data": [{
                "id": 2,
                "order_number": "OO-002",
                "status": "dispensed",
                "fulfillment_mode": "prepared",
                "total_amount": "3000.00",
                "started_at": "2026-08-01T09:00:00Z",
                "ready_at": "2026-08-03T14:00:00Z",
                "dispensed_at": "2026-08-04T10:00:00Z",
                "cancelled_at": null,
                "created_at": "2026-07-30T10:00:00Z",
                "source_quotation": null,
                "items": [{
                    "id": 20,
                    "description": "Frame",
                    "quantity": 1,
                    "unit_price": "3000.00",
                    "amount": "3000.00",
                    "product_variant_id": 10,
                    "is_rateable": true,
                    "rating": {"rating": 5, "comment": "Great!", "created_at": "2026-08-05T10:00:00Z"}
                }],
                "payment_summary": null
            }],
            "meta": {"current_page": 1, "last_page": 1, "per_page": 15, "total": 1}
        }
        """.trimIndent()

        val o = json.decodeFromString<OpticalOrderListResponse>(fixture).data[0]
        assertEquals("dispensed", o.status)
        assertEquals("2026-08-04T10:00:00Z", o.dispensedAt)
        assertNull(o.sourceQuotation)
        assertTrue(o.items[0].isRateable)
        assertEquals(5, o.items[0].rating?.rating)
        assertEquals("Great!", o.items[0].rating?.comment)
        assertNull(o.paymentSummary)
    }

    @Test
    fun `decodes all order status values`() {
        listOf("queued", "in_progress", "ready_for_dispensing", "dispensed", "cancelled").forEach { status ->
            val fixture = """
            {"id":1,"order_number":"OO-001","status":"$status","fulfillment_mode":"prepared","total_amount":"0.00","created_at":"2026-08-01T00:00:00Z","items":[]}
            """.trimIndent()
            val dto = json.decodeFromString<OpticalOrderDto>(fixture)
            assertEquals(status, dto.status)
        }
    }

    @Test
    fun `decodes money with exact precision`() {
        val fixture = """
        {"id":1,"order_number":"OO-001","status":"queued","fulfillment_mode":"prepared","total_amount":"9999.99","created_at":"2026-08-01T00:00:00Z","items":[{"id":1,"description":"X","quantity":1,"unit_price":"9999.99","amount":"9999.99","is_rateable":false}]}
        """.trimIndent()

        val dto = json.decodeFromString<OpticalOrderDto>(fixture)
        assertEquals(BigDecimal("9999.99"), dto.totalAmount)
        assertEquals(BigDecimal("9999.99"), dto.items[0].unitPrice)
    }

    @Test
    fun `decodes cancelled order`() {
        val fixture = """
        {"id":3,"order_number":"OO-003","status":"cancelled","fulfillment_mode":"prepared","total_amount":"1000.00","cancelled_at":"2026-08-05T12:00:00Z","created_at":"2026-08-01T00:00:00Z","items":[]}
        """.trimIndent()

        val dto = json.decodeFromString<OpticalOrderDto>(fixture)
        assertEquals("cancelled", dto.status)
        assertEquals("2026-08-05T12:00:00Z", dto.cancelledAt)
    }

    @Test
    fun `decodes immediate fulfillment mode`() {
        val fixture = """
        {"id":4,"order_number":"OO-004","status":"dispensed","fulfillment_mode":"immediate","total_amount":"500.00","created_at":"2026-08-01T00:00:00Z","items":[]}
        """.trimIndent()

        val dto = json.decodeFromString<OpticalOrderDto>(fixture)
        assertEquals("immediate", dto.fulfillmentMode)
    }

    @Test
    fun `decodes overdue payment`() {
        val fixture = """
        {"id":5,"order_number":"OO-005","status":"in_progress","fulfillment_mode":"prepared","total_amount":"1000.00","created_at":"2026-08-01T00:00:00Z","items":[],"payment_summary":{"status":"unpaid","total_amount":"1000.00","amount_paid":"0.00","balance_due":"1000.00","payment_due_date":"2026-08-01","is_overdue":true}}
        """.trimIndent()

        val dto = json.decodeFromString<OpticalOrderDto>(fixture)
        assertTrue(dto.paymentSummary!!.isOverdue)
        assertEquals("unpaid", dto.paymentSummary!!.status)
    }

    @Test
    fun `decodes rating result with id and item_id`() {
        val fixture = """
        {
            "id": 1,
            "item_id": 5,
            "rating": 5,
            "comment": "Excellent frame quality",
            "revision_number": 1,
            "created_at": "2026-08-05T10:00:00+08:00"
        }
        """.trimIndent()

        val result = json.decodeFromString<OpticalOrderDtos.RatingResultDto>(fixture)
        assertEquals(1, result.id)
        assertEquals(5, result.itemId)
        assertEquals(5, result.rating)
        assertEquals("Excellent frame quality", result.comment)
        assertEquals(1, result.revisionNumber)
        assertEquals("2026-08-05T10:00:00+08:00", result.createdAt)
    }
}
