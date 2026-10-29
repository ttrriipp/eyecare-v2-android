package com.eyecare.app.presentation.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ImageUrlHelperTest {

    @Test
    fun `variant image is preferred over product image`() {
        assertEquals(
            "variants/frame-front.jpg",
            preferredCatalogImage(
                productImages = listOf("products/frame-front.jpg"),
                variantImages = listOf("variants/frame-front.jpg"),
            ),
        )
    }

    @Test
    fun `blank variant images fall back to the first product image`() {
        assertEquals(
            "products/frame-front.jpg",
            preferredCatalogImage(
                productImages = listOf(" ", "products/frame-front.jpg"),
                variantImages = listOf("", "  "),
            ),
        )
    }

    @Test
    fun `no usable catalog images returns null`() {
        assertNull(
            preferredCatalogImage(
                productImages = listOf(" "),
                variantImages = listOf(""),
            ),
        )
    }

    @Test
    fun `storage paths are resolved against the api origin`() {
        assertEquals(
            "https://api.example.test/storage/variants/frame-front.jpg",
            buildImageUrl(
                path = "/storage/variants/frame-front.jpg",
                catalogBase = "",
                apiBase = "https://api.example.test/api/v1/",
            ),
        )
    }

    @Test
    fun `catalog paths use the configured public image base`() {
        assertEquals(
            "https://cdn.example.test/catalog/variants/frame-front.jpg",
            buildImageUrl(
                path = "variants/frame-front.jpg",
                catalogBase = "https://cdn.example.test/catalog/",
                apiBase = "https://api.example.test/api/v1/",
            ),
        )
    }
}
