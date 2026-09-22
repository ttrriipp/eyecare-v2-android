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
                "items": [{
                    "id": 10,
                    "description": "Progressive lens",
                    "quantity": 1,
                    "unit_price": "4500.00",
                    "amount": "4500.00",
                    "product_variant_id": 5,
                    "is_rateable": false,
                    "rating": null,
                    "image_url": "frames/progressive-lens.jpg"
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
        assertEquals(1, o.items.size)
        assertEquals(5, o.items[0].productVariantId)
        assertFalse(o.items[0].isRateable)
        assertEquals("frames/progressive-lens.jpg", o.items[0].imageUrl)
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
            "created_at": "2026-08-05T10:00:00+08:00"
        }
        """.trimIndent()

        val result = json.decodeFromString<OpticalOrderDtos.RatingResultDto>(fixture)
        assertEquals(1, result.id)
        assertEquals(5, result.itemId)
        assertEquals(5, result.rating)
        assertEquals("Excellent frame quality", result.comment)
        assertEquals("2026-08-05T10:00:00+08:00", result.createdAt)
    }

    @Test
    fun `decodes rating result with null item_id`() {
        // Regression: FrameRatingResource returns item_id: null, which must not crash
        val fixture = """
        {
            "id": 1,
            "item_id": null,
            "rating": 5,
            "comment": "Excellent frame quality",
            "created_at": "2026-08-05T10:00:00+08:00"
        }
        """.trimIndent()

        val result = json.decodeFromString<OpticalOrderDtos.RatingResultDto>(fixture)
        assertEquals(1, result.id)
        assertNull(result.itemId)
        assertEquals(5, result.rating)
    }

    @Test
    fun `decodes rating result with product_variant_id`() {
        val fixture = """
        {
            "id": 2,
            "item_id": 10,
            "product_variant_id": 42,
            "rating": 4,
            "comment": null,
            "created_at": "2026-08-05T10:00:00+08:00"
        }
        """.trimIndent()

        val result = json.decodeFromString<OpticalOrderDtos.RatingResultDto>(fixture)
        assertEquals(2, result.id)
        assertEquals(10, result.itemId)
        assertEquals(42, result.productVariantId)
        assertNull(result.comment)
    }

    @Test
    fun `decodes rating result without product_variant_id defaults to null`() {
        val fixture = """
        {
            "id": 3,
            "item_id": null,
            "rating": 3,
            "comment": "OK",
            "created_at": "2026-08-05T10:00:00+08:00"
        }
        """.trimIndent()

        val result = json.decodeFromString<OpticalOrderDtos.RatingResultDto>(fixture)
        assertNull(result.productVariantId)
    }

    @Test
    fun `decodes pending_payment order with payment instructions`() {
        val fixture = """
        {
            "id": 55,
            "order_number": "ORD-2026-000055",
            "status": "pending_payment",
            "fulfillment_mode": "prepared",
            "total_amount": "700.00",
            "payment_expires_at": "2026-09-20T10:30:00+08:00",
            "created_at": "2026-09-20T10:00:00+08:00",
            "items": [],
            "payment_instructions": {
                "method": "gcash",
                "clinic_account_name": "Padilla Optical Clinic",
                "clinic_account_number": "09XXXXXXXXX",
                "amount": "700.00",
                "order_reference": "ORD-2026-000055",
                "payment_expires_at": "2026-09-20T10:30:00+08:00"
            },
            "payment_proof": null,
            "payment_proof_status": "not_submitted",
            "payment_proof_rejection_reason": null
        }
        """.trimIndent()

        val dto = json.decodeFromString<OpticalOrderDtos.OpticalOrderDto>(fixture)
        assertEquals("pending_payment", dto.status)
        assertEquals("2026-09-20T10:30:00+08:00", dto.paymentExpiresAt)
        assertNotNull(dto.paymentInstructions)
        assertEquals("gcash", dto.paymentInstructions!!.method)
        assertEquals("Padilla Optical Clinic", dto.paymentInstructions!!.clinicAccountName)
        assertEquals("09XXXXXXXXX", dto.paymentInstructions!!.clinicAccountNumber)
        assertEquals(BigDecimal("700.00"), dto.paymentInstructions!!.amount)
        assertEquals("ORD-2026-000055", dto.paymentInstructions!!.orderReference)
        assertEquals("2026-09-20T10:30:00+08:00", dto.paymentInstructions!!.paymentExpiresAt)
        assertNull(dto.paymentProof)
        assertEquals("not_submitted", dto.paymentProofStatus)
        assertNull(dto.paymentProofRejectionReason)
    }

    @Test
    fun `decodes top-level payment proof status and rejection reason`() {
        val fixture = """
        {
            "id": 55,
            "order_number": "ORD-2026-000055",
            "status": "payment_review",
            "fulfillment_mode": "prepared",
            "total_amount": "700.00",
            "created_at": "2026-09-20T10:00:00+08:00",
            "items": [],
            "payment_proof_status": "rejected",
            "payment_proof_rejection_reason": "Screenshot unclear"
        }
        """.trimIndent()

        val dto = json.decodeFromString<OpticalOrderDto>(fixture)

        assertEquals("rejected", dto.paymentProofStatus)
        assertEquals("Screenshot unclear", dto.paymentProofRejectionReason)
    }

    @Test
    fun `decodes payment_review order with proof`() {
        val fixture = """
        {
            "id": 55,
            "order_number": "ORD-2026-000055",
            "status": "payment_review",
            "fulfillment_mode": "prepared",
            "total_amount": "700.00",
            "created_at": "2026-09-20T10:00:00+08:00",
            "items": [],
            "payment_instructions": null,
            "payment_proof": {
                "id": 900,
                "status": "pending",
                "sender_name": "Ana Reyes",
                "reference_number": "GCASH-12345",
                "created_at": "2026-09-20T10:12:00+08:00"
            }
        }
        """.trimIndent()

        val dto = json.decodeFromString<OpticalOrderDtos.OpticalOrderDto>(fixture)
        assertEquals("payment_review", dto.status)
        assertNull(dto.paymentInstructions)
        assertNotNull(dto.paymentProof)
        assertEquals(900, dto.paymentProof!!.id)
        assertEquals("pending", dto.paymentProof!!.status)
        assertEquals("Ana Reyes", dto.paymentProof!!.senderName)
        assertEquals("GCASH-12345", dto.paymentProof!!.referenceNumber)
    }

    @Test
    fun `decodes all payment proof statuses`() {
        listOf("pending", "accepted", "rejected").forEach { status ->
            val fixture = """
            {"id":1,"order_number":"OO-1","status":"payment_review","fulfillment_mode":"prepared","total_amount":"100.00","created_at":"2026-09-20T10:00:00+08:00","items":[],"payment_proof":{"id":1,"status":"$status","sender_name":"X","reference_number":"Y","created_at":"2026-09-20T10:00:00+08:00"}}
            """.trimIndent()
            val dto = json.decodeFromString<OpticalOrderDtos.OpticalOrderDto>(fixture)
            assertEquals(status, dto.paymentProof!!.status)
        }
    }

    @Test
    fun `decodes accepted proof with rejection reason`() {
        val fixture = """
        {
            "id": 55,
            "order_number": "ORD-2026-000055",
            "status": "pending_payment",
            "fulfillment_mode": "prepared",
            "total_amount": "700.00",
            "created_at": "2026-09-20T10:00:00+08:00",
            "items": [],
            "payment_proof": {
                "id": 901,
                "status": "rejected",
                "sender_name": "Ana Reyes",
                "reference_number": "GCASH-12345",
                "rejection_reason": "Screenshot unclear",
                "created_at": "2026-09-20T10:12:00+08:00"
            }
        }
        """.trimIndent()

        val dto = json.decodeFromString<OpticalOrderDtos.OpticalOrderDto>(fixture)
        assertEquals("rejected", dto.paymentProof!!.status)
        assertEquals("Screenshot unclear", dto.paymentProof!!.rejectionReason)
    }

    @Test
    fun `existing order fixtures still decode without new fields`() {
        val fixture = """
        {"id":1,"order_number":"OO-001","status":"in_progress","fulfillment_mode":"prepared","total_amount":"5000.00","created_at":"2026-08-01T10:00:00Z","items":[]}
        """.trimIndent()

        val dto = json.decodeFromString<OpticalOrderDtos.OpticalOrderDto>(fixture)
        assertEquals("in_progress", dto.status)
        assertNull(dto.paymentExpiresAt)
        assertNull(dto.paymentInstructions)
        assertNull(dto.paymentProof)
    }

    private fun assertNotNull(value: Any?) {
        org.junit.jupiter.api.Assertions.assertNotNull(value)
    }
}
