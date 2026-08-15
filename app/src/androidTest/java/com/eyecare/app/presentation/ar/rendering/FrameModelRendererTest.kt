package com.eyecare.app.presentation.ar.rendering

import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.presentation.ar.model.BundledFrameAsset
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FrameModelRendererTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun missingAsset_reportsRecoverableFailure() {
        val state = AtomicReference<FrameModelRenderState>()
        val asset = BundledFrameAsset(
            assetPath = "models/missing-frame.glb",
            scale = BundledFrameAsset.RoundFrame.scale,
        )

        composeRule.setContent {
            FrameModelRenderer(asset = asset, onStateChanged = state::set)
        }

        waitForState(timeoutMillis = 5_000) {
            state.get() is FrameModelRenderState.Failed
        }

        assertTrue(state.get() is FrameModelRenderState.Failed)
        composeRule
            .onNodeWithText("Unable to load the bundled 3D frame. Try the image preview instead.")
            .assertIsDisplayed()
    }

    @Test
    fun bundledRoundFrame_reachesReadyState() {
        val state = AtomicReference<FrameModelRenderState>()

        composeRule.setContent {
            FrameModelRenderer(onStateChanged = state::set)
        }

        waitForState(timeoutMillis = 20_000) {
            state.get() == FrameModelRenderState.Ready
        }

        assertTrue(state.get() == FrameModelRenderState.Ready)
        composeRule.onNodeWithText("3D frame model ready").assertIsDisplayed()
    }

    private fun waitForState(timeoutMillis: Long, predicate: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            composeRule.waitForIdle()
            if (predicate()) return
            SystemClock.sleep(50)
        }
        assertTrue("Timed out waiting for renderer state", predicate())
    }
}
