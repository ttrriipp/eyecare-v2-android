package com.eyecare.app.presentation.catalog

import com.eyecare.app.presentation.common.buildImageUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProductImageUrlTest {

    @Test
    fun `buildImageUrl strips versioned API path from base URL`() {
        val apiBase = "http://192.168.254.103/api/v1/"
        val path = "products/test.jpg"

        assertEquals(
            "http://192.168.254.103/storage/products/test.jpg",
            buildImageUrl(path, apiBase),
        )
    }

    @Test
    fun `buildImageUrl returns http path unchanged`() {
        val path = "https://cdn.example.com/image.jpg"

        assertEquals(path, buildImageUrl(path, "http://192.168.254.103/api/v1/"))
    }
}
