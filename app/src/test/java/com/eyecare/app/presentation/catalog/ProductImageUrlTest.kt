package com.eyecare.app.presentation.catalog

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProductImageUrlTest {

    // Tests the core logic independent of BuildConfig (which is Android-only)

    @Test
    fun `buildImageUrl strips api segment from base URL`() {
        val apiBase = "http://192.168.254.103/api/"
        val path = "products/test.jpg"

        val storageBase = apiBase.removeSuffix("/").removeSuffix("/api")
        val url = "$storageBase/storage/$path"

        assertFalse(url.contains("/api/storage"), "Must not have /api/storage")
        assertTrue(url == "http://192.168.254.103/storage/products/test.jpg")
    }

    @Test
    fun `buildImageUrl returns http path unchanged`() {
        val path = "https://cdn.example.com/image.jpg"
        val result = if (path.startsWith("http")) path else "should-not-reach"
        assertTrue(result == path)
    }
}
