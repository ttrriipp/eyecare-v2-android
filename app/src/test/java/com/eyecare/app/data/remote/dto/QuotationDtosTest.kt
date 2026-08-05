package com.eyecare.app.data.remote.dto

import com.eyecare.app.data.remote.dto.QuotationDtos.OpticalOrderReferenceDto
import com.eyecare.app.data.remote.dto.QuotationDtos.QuotationDto
import com.eyecare.app.data.remote.dto.QuotationDtos.QuotationItemDto
import com.eyecare.app.data.remote.dto.QuotationDtos.QuotationListResponse
import com.eyecare.app.data.remote.dto.QuotationDtos.QuotationResponse
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class QuotationDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes complete quotation list response`() {
        val fixture = """
        {
            "data": [
                {
                    "id": 1,
                    "quotation_number": "Q-2026-001",
                    "status": "presented",
                    "valid_until": "2026-09-01T00:00:00.000000Z",
                    "subtotal": "1500.00",
                    "discount_amount": "100.00",
                    "total": "1400.00",
                    "notes": "Please confirm within 30 days",
                    "created_at": "2026-08-01T10:00:00.000000Z",
                    "presented_at": "2026-08-01T10:05:00.000000Z",
                    "confirmed_at": null,
                    "optical_order": null,
                    "items": [
                        {
                            "id": 10,
                            "item_type": "product",
                            "description": "Progressive lens - Essilor",
                            "quantity": 1,
                            "unit_price": "1200.00",
                            "amount": "1200.00"
                        },
                        {
                            "id": 11,
                            "item_type": "service",
                            "description": "Fitting fee",
                            "quantity": 1,
                            "unit_price": "300.00",
                            "amount": "300.00"
                        }
                    ]
                }
            ],
            "links": {
                "first": "/api/v1/quotations?page=1",
                "last": "/api/v1/quotations?page=1",
                "prev": null,
                "next": null
            },
            "meta": {
                "current_page": 1,
                "last_page": 1,
                "per_page": 15,
                "total": 1
            }
        }
        """.trimIndent()

        val response = json.decodeFromString<QuotationListResponse>(fixture)
        assertEquals(1, response.data.size)
        assertEquals(1, response.meta?.currentPage)

        val q = response.data[0]
        assertEquals(1, q.id)
        assertEquals("Q-2026-001", q.quotationNumber)
        assertEquals("presented", q.status)
        assertEquals("2026-09-01T00:00:00.000000Z", q.validUntil)
        assertEquals(BigDecimal("1500.00"), q.subtotal)
        assertEquals(BigDecimal("100.00"), q.discountAmount)
        assertEquals(BigDecimal("1400.00"), q.total)
        assertEquals("Please confirm within 30 days", q.notes)
        assertEquals("2026-08-01T10:00:00.000000Z", q.createdAt)
        assertEquals("2026-08-01T10:05:00.000000Z", q.presentedAt)
        assertNull(q.confirmedAt)
        assertNull(q.opticalOrder)
        assertEquals(2, q.items.size)

        val productItem = q.items[0]
        assertEquals(10, productItem.id)
        assertEquals("product", productItem.itemType)
        assertEquals("Progressive lens - Essilor", productItem.description)
        assertEquals(1, productItem.quantity)
        assertEquals(BigDecimal("1200.00"), productItem.unitPrice)
        assertEquals(BigDecimal("1200.00"), productItem.amount)

        val serviceItem = q.items[1]
        assertEquals(11, serviceItem.id)
        assertEquals("service", serviceItem.itemType)
    }

    @Test
    fun `decodes quotation with optical_order cross-link`() {
        val fixture = """
        {
            "id": 2,
            "quotation_number": "Q-2026-002",
            "status": "accepted",
            "valid_until": "2026-08-15T00:00:00.000000Z",
            "subtotal": "800.00",
            "discount_amount": "0.00",
            "total": "800.00",
            "notes": null,
            "created_at": "2026-07-20T09:00:00.000000Z",
            "presented_at": "2026-07-20T09:00:00.000000Z",
            "confirmed_at": "2026-07-22T14:00:00.000000Z",
            "optical_order": {
                "id": 5,
                "order_number": "OO-2026-005"
            },
            "items": [
                {
                    "id": 20,
                    "item_type": "product",
                    "description": "Single vision lens",
                    "quantity": 2,
                    "unit_price": "400.00",
                    "amount": "800.00"
                }
            ]
        }
        """.trimIndent()

        val q = json.decodeFromString<QuotationDto>(fixture)
        assertEquals(2, q.id)
        assertEquals("accepted", q.status)
        assertEquals("2026-07-22T14:00:00.000000Z", q.confirmedAt)

        val orderRef = q.opticalOrder!!
        assertEquals(5, orderRef.id)
        assertEquals("OO-2026-005", orderRef.orderNumber)
    }

    @Test
    fun `decodes all quotation status values`() {
        val statuses = listOf("presented", "accepted", "declined", "expired")
        statuses.forEach { status ->
            val fixture = """
            {
                "id": 1,
                "quotation_number": "Q-001",
                "status": "$status",
                "subtotal": "0.00",
                "discount_amount": "0.00",
                "total": "0.00",
                "created_at": "2026-08-01T00:00:00Z",
                "items": []
            }
            """.trimIndent()
            val dto = json.decodeFromString<QuotationDto>(fixture)
            assertEquals(status, dto.status)
        }
    }

    @Test
    fun `decodes money with exact decimal precision`() {
        val fixture = """
        {
            "id": 3,
            "quotation_number": "Q-2026-003",
            "status": "presented",
            "subtotal": "9999.99",
            "discount_amount": "0.01",
            "total": "9999.98",
            "created_at": "2026-08-01T00:00:00Z",
            "items": [
                {
                    "id": 30,
                    "item_type": "product",
                    "description": "Expensive lens",
                    "quantity": 1,
                    "unit_price": "9999.99",
                    "amount": "9999.99"
                }
            ]
        }
        """.trimIndent()

        val dto = json.decodeFromString<QuotationDto>(fixture)
        assertEquals(BigDecimal("9999.99"), dto.subtotal)
        assertEquals(BigDecimal("0.01"), dto.discountAmount)
        assertEquals(BigDecimal("9999.98"), dto.total)
        assertEquals(BigDecimal("9999.99"), dto.items[0].unitPrice)
        assertEquals(BigDecimal("9999.99"), dto.items[0].amount)
    }

    @Test
    fun `decodes quotation with zero discount`() {
        val fixture = """
        {
            "id": 4,
            "quotation_number": "Q-2026-004",
            "status": "declined",
            "subtotal": "500.00",
            "discount_amount": "0.00",
            "total": "500.00",
            "created_at": "2026-08-01T00:00:00Z",
            "items": []
        }
        """.trimIndent()

        val dto = json.decodeFromString<QuotationDto>(fixture)
        assertEquals(BigDecimal("0.00"), dto.discountAmount)
        assertEquals("declined", dto.status)
    }

    @Test
    fun `decodes item catalog references`() {
        val fixture = """
        {
            "id": 6,
            "quotation_number": "Q-2026-006",
            "status": "presented",
            "subtotal": "5500.00",
            "discount_amount": "0.00",
            "total": "5500.00",
            "created_at": "2026-08-01T00:00:00Z",
            "items": [
                {
                    "id": 40,
                    "item_type": "product",
                    "description": "Classic Rectangle Frame",
                    "quantity": 1,
                    "unit_price": "4500.00",
                    "amount": "4500.00",
                    "product_variant_id": 42,
                    "lens_category_id": null,
                    "service_id": null
                },
                {
                    "id": 41,
                    "item_type": "product",
                    "description": "Progressive Lens",
                    "quantity": 1,
                    "unit_price": "0.00",
                    "amount": "0.00",
                    "product_variant_id": null,
                    "lens_category_id": 7,
                    "service_id": null
                },
                {
                    "id": 42,
                    "item_type": "service",
                    "description": "Eye Examination",
                    "quantity": 1,
                    "unit_price": "1000.00",
                    "amount": "1000.00",
                    "product_variant_id": null,
                    "lens_category_id": null,
                    "service_id": 3
                }
            ]
        }
        """.trimIndent()

        val dto = json.decodeFromString<QuotationDto>(fixture)
        assertEquals(3, dto.items.size)

        val frameItem = dto.items[0]
        assertEquals(42, frameItem.productVariantId)
        assertNull(frameItem.lensCategoryId)
        assertNull(frameItem.serviceId)

        val lensItem = dto.items[1]
        assertNull(lensItem.productVariantId)
        assertEquals(7, lensItem.lensCategoryId)
        assertNull(lensItem.serviceId)

        val serviceItem = dto.items[2]
        assertNull(serviceItem.productVariantId)
        assertNull(serviceItem.lensCategoryId)
        assertEquals(3, serviceItem.serviceId)
    }

    @Test
    fun `decodes item catalog references as null when absent`() {
        val fixture = """
        {
            "id": 7,
            "quotation_number": "Q-2026-007",
            "status": "presented",
            "subtotal": "100.00",
            "discount_amount": "0.00",
            "total": "100.00",
            "created_at": "2026-08-01T00:00:00Z",
            "items": [
                {
                    "id": 50,
                    "item_type": "product",
                    "description": "Legacy free-text item",
                    "quantity": 1,
                    "unit_price": "100.00",
                    "amount": "100.00"
                }
            ]
        }
        """.trimIndent()

        val dto = json.decodeFromString<QuotationDto>(fixture)
        assertNull(dto.items[0].productVariantId)
        assertNull(dto.items[0].lensCategoryId)
        assertNull(dto.items[0].serviceId)
    }

    @Test
    fun `decodes nullable fields as null when absent`() {
        val fixture = """
        {
            "id": 5,
            "quotation_number": "Q-2026-005",
            "status": "expired",
            "subtotal": "100.00",
            "discount_amount": "0.00",
            "total": "100.00",
            "created_at": "2026-08-01T00:00:00Z",
            "items": []
        }
        """.trimIndent()

        val dto = json.decodeFromString<QuotationDto>(fixture)
        assertNull(dto.validUntil)
        assertNull(dto.notes)
        assertNull(dto.presentedAt)
        assertNull(dto.confirmedAt)
        assertNull(dto.opticalOrder)
    }
}
