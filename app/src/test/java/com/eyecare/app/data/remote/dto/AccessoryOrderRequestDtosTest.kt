package com.eyecare.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AccessoryOrderRequestDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes complete order request response`() {
        val fixture = """
        {
            "data": {
                "id": 10,
                "request_number": "ORQ-2026-000001",
                "status": "pending",
                "subtotal_amount": "700.00",
                "requested_discount_type": "none",
                "resolved_by": null,
                "resolved_at": null,
                "items": [
                    {
                        "id": 501,
                        "product_variant_id": 101,
                        "description": "Daily Care Kit — 90 mL",
                        "quantity": 2,
                        "unit_price": "350.00",
                        "amount": "700.00",
                        "item_kind": "accessory",
                        "item_snapshot": {
                            "product_name": "Daily Care Kit",
                            "variant_name": "90 mL",
                            "attributes": {"volume_ml": 90, "package_size": "90 mL"}
                        }
                    }
                ],
                "rejection_reason": null,
                "cancelled_at": null,
                "created_at": "2026-09-20T10:00:00+08:00",
                "order": null
            }
        }
        """.trimIndent()

        val response = json.decodeFromString<AccessoryOrderRequestDtos.OrderRequestResponse>(fixture)
        val request = response.data
        assertEquals(10, request.id)
        assertEquals("ORQ-2026-000001", request.requestNumber)
        assertEquals("pending", request.status)
        assertEquals(BigDecimal("700.00"), request.subtotalAmount)
        assertEquals("none", request.requestedDiscountType)
        assertNull(request.resolvedBy)
        assertNull(request.resolvedAt)
        assertEquals(1, request.items.size)
        val item = request.items[0]
        assertEquals(501, item.id)
        assertEquals(101, item.productVariantId)
        assertEquals("Daily Care Kit — 90 mL", item.description)
        assertEquals(2, item.quantity)
        assertEquals(BigDecimal("350.00"), item.unitPrice)
        assertEquals(BigDecimal("700.00"), item.amount)
        assertEquals("accessory", item.itemKind)
        assertEquals("Daily Care Kit", item.itemSnapshot?.productName)
        assertEquals("90 mL", item.itemSnapshot?.variantName)
        assertNull(request.rejectionReason)
        assertNull(request.cancelledAt)
        assertEquals("2026-09-20T10:00:00+08:00", request.createdAt)
        assertNull(request.order)
    }

    @Test
    fun `decodes accepted request with order summary`() {
        val fixture = """
        {
            "data": {
                "id": 10,
                "request_number": "ORQ-2026-000001",
                "status": "accepted",
                "subtotal_amount": "700.00",
                "requested_discount_type": "none",
                "resolved_by": 5,
                "resolved_at": "2026-09-20T10:15:00+08:00",
                "items": [],
                "rejection_reason": null,
                "cancelled_at": null,
                "created_at": "2026-09-20T10:00:00+08:00",
                "order": {
                    "id": 55,
                    "order_number": "ORD-2026-000055",
                    "status": "pending_payment",
                    "discount_amount": "0.00",
                    "total_amount": "700.00",
                    "payment_expires_at": "2026-09-20T10:30:00+08:00"
                }
            }
        }
        """.trimIndent()

        val response = json.decodeFromString<AccessoryOrderRequestDtos.OrderRequestResponse>(fixture)
        val request = response.data
        assertEquals("accepted", request.status)
        assertEquals(5, request.resolvedBy)
        assertNotNull(request.order)
        assertEquals(55, request.order!!.id)
        assertEquals("ORD-2026-000055", request.order!!.orderNumber)
        assertEquals("pending_payment", request.order!!.status)
        assertEquals(BigDecimal("700.00"), request.order!!.totalAmount)
    }

    @Test
    fun `decodes rejected request with reason`() {
        val fixture = """
        {
            "data": {
                "id": 10,
                "request_number": "ORQ-2026-000001",
                "status": "rejected",
                "subtotal_amount": "700.00",
                "requested_discount_type": "senior_citizen",
                "resolved_by": 5,
                "resolved_at": "2026-09-20T10:15:00+08:00",
                "items": [],
                "rejection_reason": "Item no longer available",
                "cancelled_at": null,
                "created_at": "2026-09-20T10:00:00+08:00",
                "order": null
            }
        }
        """.trimIndent()

        val response = json.decodeFromString<AccessoryOrderRequestDtos.OrderRequestResponse>(fixture)
        val request = response.data
        assertEquals("rejected", request.status)
        assertEquals("senior_citizen", request.requestedDiscountType)
        assertEquals("Item no longer available", request.rejectionReason)
    }

    @Test
    fun `decodes cancelled request`() {
        val fixture = """
        {
            "data": {
                "id": 10,
                "request_number": "ORQ-2026-000001",
                "status": "cancelled",
                "subtotal_amount": "700.00",
                "requested_discount_type": "none",
                "resolved_by": null,
                "resolved_at": null,
                "items": [],
                "rejection_reason": null,
                "cancelled_at": "2026-09-20T10:05:00+08:00",
                "created_at": "2026-09-20T10:00:00+08:00",
                "order": null
            }
        }
        """.trimIndent()

        val response = json.decodeFromString<AccessoryOrderRequestDtos.OrderRequestResponse>(fixture)
        assertEquals("cancelled", response.data.status)
        assertEquals("2026-09-20T10:05:00+08:00", response.data.cancelledAt)
    }

    @Test
    fun `decodes paginated list response`() {
        val fixture = """
        {
            "data": [
                {"id":1,"request_number":"ORQ-001","status":"pending","subtotal_amount":"100.00","requested_discount_type":"none","items":[],"created_at":"2026-09-20T10:00:00+08:00"},
                {"id":2,"request_number":"ORQ-002","status":"accepted","subtotal_amount":"200.00","requested_discount_type":"none","items":[],"created_at":"2026-09-20T09:00:00+08:00"}
            ],
            "meta": {"current_page":1,"last_page":1,"per_page":15,"total":2}
        }
        """.trimIndent()

        val response = json.decodeFromString<AccessoryOrderRequestDtos.OrderRequestListResponse>(fixture)
        assertEquals(2, response.data.size)
        assertEquals(1, response.data[0].id)
        assertEquals(2, response.data[1].id)
        assertEquals(1, response.meta?.currentPage)
    }

    @Test
    fun `decodes money with exact precision`() {
        val fixture = """
        {"data":{"id":1,"request_number":"ORQ-001","status":"pending","subtotal_amount":"9999.99","requested_discount_type":"none","items":[
            {"id":1,"product_variant_id":1,"description":"X","quantity":1,"unit_price":"9999.99","amount":"9999.99","item_kind":"accessory","item_snapshot":{"product_name":"X","variant_name":"V","attributes":{}}}
        ],"created_at":"2026-09-20T10:00:00+08:00"}}
        """.trimIndent()

        val response = json.decodeFromString<AccessoryOrderRequestDtos.OrderRequestResponse>(fixture)
        assertEquals(BigDecimal("9999.99"), response.data.subtotalAmount)
        assertEquals(BigDecimal("9999.99"), response.data.items[0].unitPrice)
    }

    @Test
    fun `decodes all status values`() {
        listOf("pending", "accepted", "rejected", "cancelled").forEach { status ->
            val fixture = """
            {"data":{"id":1,"request_number":"ORQ-001","status":"$status","subtotal_amount":"0.00","requested_discount_type":"none","items":[],"created_at":"2026-09-20T10:00:00+08:00"}}
            """.trimIndent()
            val response = json.decodeFromString<AccessoryOrderRequestDtos.OrderRequestResponse>(fixture)
            assertEquals(status, response.data.status)
        }
    }

    @Test
    fun `decodes all discount types`() {
        listOf("none", "senior_citizen", "pwd").forEach { discount ->
            val fixture = """
            {"data":{"id":1,"request_number":"ORQ-001","status":"pending","subtotal_amount":"0.00","requested_discount_type":"$discount","items":[],"created_at":"2026-09-20T10:00:00+08:00"}}
            """.trimIndent()
            val response = json.decodeFromString<AccessoryOrderRequestDtos.OrderRequestResponse>(fixture)
            assertEquals(discount, response.data.requestedDiscountType)
        }
    }

    private fun assertNotNull(value: Any?) {
        org.junit.jupiter.api.Assertions.assertNotNull(value)
    }
}