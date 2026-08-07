package com.eyecare.app.data.remote.dto

import com.eyecare.app.data.remote.dto.FrameDtos.FrameDto
import com.eyecare.app.data.remote.dto.FrameDtos.PaginatedFrameResponse
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FrameDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes frame with rating aggregates`() {
        val fixture = """
        {
            "id": 1,
            "name": "Classic Frame",
            "slug": "classic-frame",
            "description": "A nice frame",
            "product_type": "frame",
            "brand": "Ray-Ban",
            "category": "Eyeglasses",
            "variants": [],
            "images": [],
            "average_rating": 4.5,
            "rating_count": 12
        }
        """.trimIndent()

        val dto = json.decodeFromString<FrameDto>(fixture)
        assertEquals(4.5, dto.averageRating!!, 0.001)
        assertEquals(12, dto.ratingCount)
    }

    @Test
    fun `decodes frame with null average_rating`() {
        val fixture = """
        {
            "id": 2,
            "name": "New Frame",
            "slug": "new-frame",
            "brand": "Oakley",
            "average_rating": null,
            "rating_count": 0
        }
        """.trimIndent()

        val dto = json.decodeFromString<FrameDto>(fixture)
        assertNull(dto.averageRating)
        assertEquals(0, dto.ratingCount)
    }

    @Test
    fun `decodes frame without rating fields defaults safely`() {
        // Simulates a response that doesn't include the fields at all
        val fixture = """
        {
            "id": 3,
            "name": "Old Frame",
            "slug": "old-frame",
            "brand": "Generic"
        }
        """.trimIndent()

        val dto = json.decodeFromString<FrameDto>(fixture)
        assertNull(dto.averageRating)
        assertEquals(0, dto.ratingCount)
    }

    @Test
    fun `null average_rating stays null through list response`() {
        val fixture = """
        {
            "data": [
                {
                    "id": 4,
                    "name": "Unrated Frame",
                    "slug": "unrated-frame",
                    "brand": "Test",
                    "average_rating": null,
                    "rating_count": 0
                }
            ],
            "meta": {"current_page": 1, "last_page": 1, "per_page": 15, "total": 1}
        }
        """.trimIndent()

        val response = json.decodeFromString<PaginatedFrameResponse>(fixture)
        assertNull(response.data[0].averageRating)
        assertEquals(0, response.data[0].ratingCount)
    }
}
