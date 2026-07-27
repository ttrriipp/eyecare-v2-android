package com.eyecare.app.data.remote.dto

import com.eyecare.app.data.remote.ApiContractFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FrameDtosTest {

    private val json = ApiContractFixtures.json

    @Test
    fun `paginated frame list decodes variants money images and AR fields`() {
        val response = json.decodeFromString<FrameDtos.PaginatedFrameResponse>(
            """
            {
              "data": [
                {
                  "id": 1,
                  "name": "Classic Rectangle",
                  "slug": "classic-rectangle",
                  "description": "Timeless frame design",
                  "product_type": "frame",
                  "brand": "Ray-Ban",
                  "category": "Full Rim",
                  "variants": [
                    {
                      "id": 1,
                      "name": "Black / 52mm",
                      "sku": "RB-CR-BLK-52",
                      "price": 4500.00,
                      "compare_at_price": null,
                      "attributes": { "color": "black", "size": "52mm" },
                      "ar_eligible": true,
                      "ar_asset_reference": "rb-cr-blk-52.usdz",
                      "images": []
                    }
                  ],
                  "images": []
                }
              ],
              "links": {
                "first": "https://example.test/api/v1/frames?page=1",
                "last": "https://example.test/api/v1/frames?page=1",
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
            """.trimIndent(),
        )

        val frame = response.data.single()
        assertEquals(1, frame.id)
        assertEquals("Classic Rectangle", frame.name)
        assertEquals("classic-rectangle", frame.slug)
        assertEquals("Timeless frame design", frame.description)
        assertEquals("frame", frame.productType)
        assertEquals("Ray-Ban", frame.brand)
        assertEquals("Full Rim", frame.category)

        val variant = frame.variants.single()
        assertEquals(1, variant.id)
        assertEquals("Black / 52mm", variant.name)
        assertEquals("RB-CR-BLK-52", variant.sku)
        assertEquals(BigDecimal("4500.00"), variant.price)
        assertNull(variant.compareAtPrice)
        assertTrue(variant.arEligible)
        assertEquals("rb-cr-blk-52.usdz", variant.arAssetReference)

        assertEquals(1, response.meta?.currentPage)
        assertEquals(15, response.meta?.perPage)
        assertEquals(1, response.meta?.total)
    }

    @Test
    fun `frame variant decodes string price`() {
        val dto = json.decodeFromString<FrameDtos.FrameVariantDto>(
            """
            {
              "id": 2,
              "name": "Gold / 54mm",
              "sku": "RB-CR-GLD-54",
              "price": "5200.50",
              "compare_at_price": "6000.00",
              "attributes": { "color": "gold", "size": "54mm" },
              "ar_eligible": false,
              "ar_asset_reference": null,
              "images": ["https://example.test/img1.jpg"]
            }
            """.trimIndent(),
        )

        assertEquals(BigDecimal("5200.50"), dto.price)
        assertEquals(BigDecimal("6000.00"), dto.compareAtPrice)
        assertFalse(dto.arEligible)
        assertNull(dto.arAssetReference)
        assertEquals(1, dto.images.size)
    }

    @Test
    fun `frame detail response decodes single frame`() {
        val response = json.decodeFromString<FrameDtos.FrameResponse>(
            """
            {
              "data": {
                "id": 1,
                "name": "Classic Rectangle",
                "slug": "classic-rectangle",
                "description": null,
                "product_type": "frame",
                "brand": "Ray-Ban",
                "category": null,
                "variants": [],
                "images": []
              }
            }
            """.trimIndent(),
        )

        assertEquals(1, response.data.id)
        assertNull(response.data.description)
        assertNull(response.data.category)
        assertTrue(response.data.variants.isEmpty())
    }

    @Test
    fun `frame with null category decodes safely`() {
        val dto = json.decodeFromString<FrameDtos.FrameDto>(
            """
            {
              "id": 3,
              "name": "Aviator",
              "slug": "aviator",
              "description": "Classic aviator style",
              "product_type": "frame",
              "brand": "Ray-Ban",
              "category": null,
              "variants": [],
              "images": []
            }
            """.trimIndent(),
        )

        assertEquals(3, dto.id)
        assertNull(dto.category)
    }

    @Test
    fun `frame variant attributes decode as JsonElement`() {
        val dto = json.decodeFromString<FrameDtos.FrameVariantDto>(
            """
            {
              "id": 1,
              "name": "Black / 52mm",
              "sku": "RB-CR-BLK-52",
              "price": 4500.00,
              "compare_at_price": null,
              "attributes": { "color": "black", "size": "52mm", "material": "titanium" },
              "ar_eligible": true,
              "ar_asset_reference": "rb-cr-blk-52.usdz",
              "images": []
            }
            """.trimIndent(),
        )

        assertNotNull(dto.attributes)
    }
}
