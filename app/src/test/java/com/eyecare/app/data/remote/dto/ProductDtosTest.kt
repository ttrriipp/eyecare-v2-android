package com.eyecare.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProductDtosTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `product list accepts a null category`() {
        val response = json.decodeFromString<ProductDtos.PaginatedProductResponse>(
            """
            {
              "data": [{
                "id": 5,
                "name": "Plastic Frame",
                "slug": "plastic-frame",
                "description": null,
                "product_type": "frame",
                "brand": "VisionCraft",
                "category": null,
                "variants": [],
                "images": []
              }],
              "meta": {
                "current_page": 1,
                "last_page": 1,
                "per_page": 15,
                "total": 1
              }
            }
            """.trimIndent(),
        )

        assertNull(response.data.single().category)
    }
}
