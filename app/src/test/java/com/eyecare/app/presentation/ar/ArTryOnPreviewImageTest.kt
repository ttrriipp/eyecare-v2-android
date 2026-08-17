package com.eyecare.app.presentation.ar

import com.eyecare.app.domain.model.FrameVariant
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ArTryOnPreviewImageTest {

    @Test
    fun `try-on preview uses the selected variant image`() {
        val variant = variant(
            images = listOf("frames/round-front.jpg"),
            legacyAssetReference = "legacy-frame.glb",
        )

        assertEquals("frames/round-front.jpg", variant.tryOnPreviewImageReference())
    }

    @Test
    fun `try-on preview does not treat the legacy asset reference as an image`() {
        val variant = variant(
            images = emptyList(),
            legacyAssetReference = "legacy-frame.glb",
        )

        assertNull(variant.tryOnPreviewImageReference())
    }

    private fun variant(
        images: List<String>,
        legacyAssetReference: String?,
    ) = FrameVariant(
        id = 1,
        name = "Matte Black",
        sku = "SKU-1",
        price = BigDecimal("4500.00"),
        compareAtPrice = null,
        attributes = null,
        arEligible = true,
        arAssetReference = legacyAssetReference,
        images = images,
    )
}
