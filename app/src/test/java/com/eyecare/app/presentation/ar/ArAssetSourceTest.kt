package com.eyecare.app.presentation.ar

import com.eyecare.app.presentation.ar.model.ArAssetSource
import com.eyecare.app.presentation.ar.model.FrameModelScale
import com.eyecare.app.presentation.ar.rendering.FrameModelSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class ArAssetSourceTest {

    @Test
    fun `remote source preserves calibration scale for the renderer`() {
        val calibrationScale = FrameModelScale(x = 0.12f, y = 0.23f, z = 0.34f)

        val source = ArAssetSource.Ready(
            filePath = "/cache/round-frame.glb",
            scale = calibrationScale,
        ).toFrameModelSource()

        val downloaded = assertInstanceOf(FrameModelSource.Downloaded::class.java, source)
        assertEquals(calibrationScale, downloaded.modelScale)
        assertEquals(
            FrameModelScale(x = 0.24f, y = 0.46f, z = 0.68f),
            downloaded.scaleForPose(poseScale = 2f),
        )
    }
}
