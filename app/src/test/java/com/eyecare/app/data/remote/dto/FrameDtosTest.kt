package com.eyecare.app.data.remote.dto

import com.eyecare.app.data.remote.ApiContractFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
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

    @Test
    fun `frame variant decodes the typed ready ar contract`() {
        val dto = json.decodeFromString<FrameDtos.FrameVariantDto>(
            """
            {
              "id": 42,
              "name": "Matte Black / 50mm",
              "sku": "ROUND-BLK-50",
              "price": 4500.00,
              "ar": {
                "status": "ready",
                "asset": {
                  "url": "https://cdn.example.test/ar/variants/42/v2/model.glb",
                  "format": "glb",
                  "version": 2,
                  "byte_size": 5256552,
                  "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                },
                "calibration": {
                  "frame_width_mm": 123.0,
                  "outer_frame_height_mm": 48.0,
                  "lens_width_mm": 50.0,
                  "lens_height_mm": 45.0,
                  "bridge_width_mm": 20.0,
                  "temple_length_mm": 140.0,
                  "scale": { "x": 0.123, "y": 0.144565, "z": 0.123 },
                  "anchor": { "x": 0.0, "y": 0.0, "z": 0.0 },
                  "rotation_degrees": { "x": 0.0, "y": 0.0, "z": 0.0 }
                }
              }
            }
            """.trimIndent(),
        )

        val ar = dto.ar ?: error("typed AR contract should be present")
        assertEquals(FrameDtos.ArAssetStatusDto.READY, ar.status)
        assertEquals(FrameDtos.ArAssetFormatDto.GLB, ar.asset.format)
        assertEquals("https://cdn.example.test/ar/variants/42/v2/model.glb", ar.asset.url)
        assertEquals(2, ar.asset.version)
        assertEquals(5256552L, ar.asset.byteSize)
        assertEquals(123.0, ar.calibration.frameWidthMm)
        assertEquals(0.144565, ar.calibration.scale.y)
    }

    @Test
    fun `missing typed ar remains null for legacy compatibility`() {
        val dto = json.decodeFromString<FrameDtos.FrameVariantDto>(
            """
            {
              "id": 9,
              "name": "Legacy frame",
              "sku": "LEGACY-9",
              "price": "1200.00",
              "ar_eligible": true,
              "ar_asset_reference": "legacy-frame.jpg",
              "images": []
            }
            """.trimIndent(),
        )

        assertNull(dto.ar)
        assertTrue(dto.arEligible)
        assertEquals("legacy-frame.jpg", dto.arAssetReference)
    }

    @Test
    fun `typed ar rejects invalid format and non-positive calibration`() {
        val invalidFormat = """
            {
              "id": 42,
              "name": "Invalid",
              "sku": "INVALID",
              "price": 1,
              "ar": {
                "status": "ready",
                "asset": {
                  "url": "https://cdn.example.test/model.glb",
                  "format": "usdz",
                  "version": 1,
                  "byte_size": 1,
                  "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                },
                "calibration": {
                  "frame_width_mm": 1,
                  "outer_frame_height_mm": 1,
                  "lens_width_mm": 1,
                  "lens_height_mm": 1,
                  "bridge_width_mm": 1,
                  "temple_length_mm": 1,
                  "scale": { "x": 1, "y": 1, "z": 1 },
                  "anchor": { "x": 0, "y": 0, "z": 0 },
                  "rotation_degrees": { "x": 0, "y": 0, "z": 0 }
                }
              }
            }
        """.trimIndent()
        assertThrows(Exception::class.java) {
            json.decodeFromString<FrameDtos.FrameVariantDto>(invalidFormat)
        }

        val invalidDimensions = invalidFormat.replace("\"format\": \"usdz\"", "\"format\": \"glb\"")
            .replace("\"frame_width_mm\": 1", "\"frame_width_mm\": 0")
        assertThrows(IllegalArgumentException::class.java) {
            json.decodeFromString<FrameDtos.FrameVariantDto>(invalidDimensions)
        }
    }
}
