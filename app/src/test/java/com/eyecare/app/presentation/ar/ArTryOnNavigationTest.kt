package com.eyecare.app.presentation.ar

import com.eyecare.app.presentation.navigation.FrameDetail
import com.eyecare.app.presentation.navigation.arImageFallbackDestination
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArTryOnNavigationTest {
    @Test
    fun `image fallback keeps the selected frame and variant`() {
        assertEquals(
            FrameDetail(frameId = 42, variantId = 7),
            arImageFallbackDestination(frameId = 42, variantId = 7),
        )
    }
}
