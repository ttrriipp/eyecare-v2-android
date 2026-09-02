package com.eyecare.app.presentation.ar.components

import com.eyecare.app.presentation.ar.model.ArAssetState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArAssetStatusBannerMessageTest {

    @Test
    fun `failed 3D asset directs the patient to frame images without claiming an inline preview`() {
        assertEquals(
            "3D preview unavailable. View frame images instead.",
            ArAssetState.Failed("bad model").statusBannerMessage(),
        )
    }
}
