package com.eyecare.app.data.remote.dto

import com.eyecare.app.data.remote.ApiContractFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SavedFrameDtosTest {

    private val json = ApiContractFixtures.json

    @Test
    fun `decodes available saved frame with string price and typed AR`() {
        val page = json.decodeFromString<SavedFrameDtos.SavedFramePageResponse>(
            ApiContractFixtures.savedFramesPageAvailable,
        )

        assertEquals(1, page.data.size)
        assertEquals(1, page.meta.currentPage)
        assertEquals(1, page.meta.lastPage)
        assertEquals(15, page.meta.perPage)
        assertEquals(1, page.meta.total)

        val item = page.data[0]
        assertEquals(42, item.productVariantId)
        assertEquals("2026-08-27T10:00:00+08:00", item.savedAt)
        assertEquals("available", item.availability)

        val variant = item.variant
        assertEquals(42, variant.id)
        assertEquals("Black / 52mm", variant.name)
        assertEquals("RB-CR-BLK-52", variant.sku)
        assertEquals("4500.00", variant.price.toPlainString())
        assertNull(variant.compareAtPrice)
        assertEquals(1, variant.images.size)

        assertNotNull(variant.ar)
        assertEquals(FrameDtos.ArAssetStatusDto.READY, variant.ar!!.status)

        val product = variant.product
        assertEquals(7, product.id)
        assertEquals("Classic Rectangle", product.name)
        assertEquals("Ray-Ban", product.brand)
    }

    @Test
    fun `decodes unavailable saved frame with number price and null AR`() {
        val page = json.decodeFromString<SavedFrameDtos.SavedFramePageResponse>(
            ApiContractFixtures.savedFramesPageUnavailable,
        )

        val item = page.data[0]
        assertEquals(99, item.productVariantId)
        assertEquals("unavailable", item.availability)

        val variant = item.variant
        assertEquals("5200", variant.price.toPlainString())
        assertNotNull(variant.compareAtPrice)
        assertEquals("6000.00", variant.compareAtPrice!!.toPlainString())
        assertNull(variant.ar)
        assertTrue(variant.images.isEmpty())
    }

    @Test
    fun `decodes save response as single resource`() {
        val response = json.decodeFromString<SavedFrameDtos.SavedFrameSaveResponse>(
            ApiContractFixtures.savedFrameSaveResponse,
        )

        assertEquals(42, response.data.productVariantId)
        assertEquals("available", response.data.availability)
    }

    @Test
    fun `availability maps fail closed for unknown values`() {
        val page = json.decodeFromString<SavedFrameDtos.SavedFramePageResponse>(
            ApiContractFixtures.savedFramesPageAvailable.replace("\"available\"", "\"future_status\""),
        )

        assertEquals("future_status", page.data[0].availability)
    }
}
