package com.eyecare.app.presentation.ar

import com.eyecare.app.presentation.ar.model.ArAssetState
import com.eyecare.app.presentation.ar.model.ArTryOnUiState
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FacePose
import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ArTryOnContentStateTest {

    @Test
    fun `all active face states map to one stable content contract`() {
        val states = listOf(
            ArTryOnUiState.Loading(emptyList(), null, ArAssetState.Loading),
            ArTryOnUiState.Searching(emptyList(), null, ArAssetState.Ready),
            ArTryOnUiState.Tracking(
                variants = emptyList(),
                selectedVariant = null,
                face = faceFrame(),
                pose = FacePose(0f, 0f, 0f, 0f, 0f, 0f, 1f),
                assetState = ArAssetState.Ready,
            ),
        )

        states.forEachIndexed { index, state ->
            val content = state.toActiveTryOnContentState()

            assertNotNull(content)
            assertEquals(EXPECTED_PHASES[index], content?.phase)
        }
    }

    @Test
    fun `save feedback is preserved when active state is mapped`() {
        val content = ArTryOnUiState.Searching(
            variants = emptyList(),
            selectedVariant = null,
            assetState = ArAssetState.Ready,
            saveError = "Couldn't update saved state. Try again.",
            saveMessage = "Saved as a preference. Availability is not guaranteed.",
        ).toActiveTryOnContentState()

        assertEquals("Couldn't update saved state. Try again.", content?.saveError)
        assertEquals("Saved as a preference. Availability is not guaranteed.", content?.saveMessage)
    }

    private fun faceFrame() = FaceFrame(
        noseBridgeX = 0.5f,
        noseBridgeY = 0.5f,
        leftTempleX = 0.3f,
        rightTempleX = 0.7f,
        faceWidthNorm = 0.4f,
        rotationDeg = 0f,
        imageWidth = 640,
        imageHeight = 480,
        transformationMatrix = IDENTITY_MATRIX,
        timestampMs = 0L,
    )

    private companion object {
        val EXPECTED_PHASES = listOf(
            ActiveTryOnPhase.Loading,
            ActiveTryOnPhase.Searching,
            ActiveTryOnPhase.Tracking,
        )
        val IDENTITY_MATRIX = FaceTransformationMatrix.from(
            floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            ),
        )!!
    }
}
