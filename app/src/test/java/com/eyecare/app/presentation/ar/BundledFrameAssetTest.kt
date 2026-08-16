package com.eyecare.app.presentation.ar

import com.eyecare.app.presentation.ar.model.BundledFrameAsset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BundledFrameAssetTest {

    @Test
    fun `round frame applies the provisional device display scale`() {
        val scale = BundledFrameAsset.RoundFrame.scaleForPose(poseScale = 1f)

        assertEquals(0.1968f, scale.x, SCALE_EPSILON)
        assertEquals(0.231304f, scale.y, SCALE_EPSILON)
        assertEquals(0.1968f, scale.z, SCALE_EPSILON)
    }

    private companion object {
        const val SCALE_EPSILON = 0.000001f
    }
}
