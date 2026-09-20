package com.eyecare.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AccessoryDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes complete accessory list response`() {
        val fixture = """
        {
            "data": [{
                "id": 42,
                "name": "Daily Care Kit",
                "slug": "daily-care-kit",
                "description": "Everyday lens-care accessory.",
                "brand": "Acme",
                "category": "Lens Care",
                "images": ["catalog/daily-care-kit.jpg"],
                "average_rating": 4.5,
                "rating_count": 12,
                "variants": [{
                    "id": 101,
                    "name": "90 mL",
                    "price": "350.00",
                    "compare_at_price": null,
                    "attributes": {"volume_ml": 90, "package_size": "90 mL"},
                    "images": ["variants/daily-care-90ml.jpg"],
                    "availability": "available"
                }]
            }],
            "links": {"first": "...", "last": "...", "prev": null, "next": null},
            "meta": {"current_page": 1, "last_page": 1, "per_page": 15, "total": 1}
        }
        """.trimIndent()

        val response = json.decodeFromString<AccessoryDtos.AccessoryListResponse>(fixture)
        assertEquals(1, response.data.size)
        val product = response.data[0]
        assertEquals(42, product.id)
        assertEquals("Daily Care Kit", product.name)
        assertEquals("daily-care-kit", product.slug)
        assertEquals("Everyday lens-care accessory.", product.description)
        assertEquals("Acme", product.brand)
        assertEquals("Lens Care", product.category)
        assertEquals(1, product.images.size)
        assertEquals("catalog/daily-care-kit.jpg", product.images[0])
        assertEquals(4.5, product.averageRating!!, 0.01)
        assertEquals(12, product.ratingCount)
        assertEquals(1, product.variants.size)
        val variant = product.variants[0]
        assertEquals(101, variant.id)
        assertEquals("90 mL", variant.name)
        assertEquals(BigDecimal("350.00"), variant.price)
        assertNull(variant.compareAtPrice)
        assertEquals("available", variant.availability)
        assertEquals(1, response.meta?.currentPage)
        assertEquals(1, response.meta?.lastPage)
    }

    @Test
    fun `decodes accessory detail response`() {
        val fixture = """
        {
            "data": {
                "id": 42,
                "name": "Daily Care Kit",
                "slug": "daily-care-kit",
                "description": null,
                "brand": null,
                "category": null,
                "images": [],
                "average_rating": null,
                "rating_count": 0,
                "variants": []
            }
        }
        """.trimIndent()

        val response = json.decodeFromString<AccessoryDtos.AccessoryResponse>(fixture)
        val product = response.data
        assertEquals(42, product.id)
        assertNull(product.description)
        assertNull(product.brand)
        assertNull(product.category)
        assertTrue(product.images.isEmpty())
        assertNull(product.averageRating)
        assertEquals(0, product.ratingCount)
        assertTrue(product.variants.isEmpty())
    }

    @Test
    fun `decodes all availability values`() {
        listOf("available", "low_stock", "unavailable").forEach { availability ->
            val fixture = """
            {"id":1,"name":"X","slug":"x","images":[],"rating_count":0,"variants":[
                {"id":1,"name":"V","price":"100.00","attributes":{},"images":[],"availability":"$availability"}
            ]}
            """.trimIndent()
            val dto = json.decodeFromString<AccessoryDtos.AccessoryDto>(fixture)
            assertEquals(availability, dto.variants[0].availability)
        }
    }

    @Test
    fun `decodes money with exact precision`() {
        val fixture = """
        {"id":1,"name":"X","slug":"x","images":[],"rating_count":0,"variants":[
            {"id":1,"name":"V","price":"9999.99","compare_at_price":"12000.00","attributes":{},"images":[],"availability":"available"}
        ]}
        """.trimIndent()

        val dto = json.decodeFromString<AccessoryDtos.AccessoryDto>(fixture)
        assertEquals(BigDecimal("9999.99"), dto.variants[0].price)
        assertEquals(BigDecimal("12000.00"), dto.variants[0].compareAtPrice)
    }

    @Test
    fun `decodes heterogeneous attributes as JsonObject`() {
        val fixture = """
        {"id":1,"name":"X","slug":"x","images":[],"rating_count":0,"variants":[
            {"id":1,"name":"V","price":"100.00","attributes":{"volume_ml":90,"package_size":"90 mL","is_organic":true},"images":[],"availability":"available"}
        ]}
        """.trimIndent()

        val dto = json.decodeFromString<AccessoryDtos.AccessoryDto>(fixture)
        val attrs = dto.variants[0].attributes
        assertTrue(attrs.containsKey("volume_ml"))
        assertTrue(attrs.containsKey("package_size"))
        assertTrue(attrs.containsKey("is_organic"))
    }

    @Test
    fun `ignores extra paginator metadata`() {
        val fixture = """
        {
            "data": [],
            "links": {"first": "...", "last": "...", "prev": null, "next": null},
            "meta": {"current_page": 1, "last_page": 1, "per_page": 15, "total": 0, "extra_field": "ignored"}
        }
        """.trimIndent()

        val response = json.decodeFromString<AccessoryDtos.AccessoryListResponse>(fixture)
        assertTrue(response.data.isEmpty())
        assertEquals(0, response.meta?.total)
    }

    @Test
    fun `decodes empty images and attributes`() {
        val fixture = """
        {"id":1,"name":"X","slug":"x","images":[],"rating_count":0,"variants":[
            {"id":1,"name":"V","price":"100.00","attributes":{},"images":[],"availability":"available"}
        ]}
        """.trimIndent()

        val dto = json.decodeFromString<AccessoryDtos.AccessoryDto>(fixture)
        assertTrue(dto.images.isEmpty())
        assertTrue(dto.variants[0].images.isEmpty())
    }
}