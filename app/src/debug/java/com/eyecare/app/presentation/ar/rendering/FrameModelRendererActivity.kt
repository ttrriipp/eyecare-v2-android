package com.eyecare.app.presentation.ar.rendering

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.eyecare.app.ui.theme.EyecareTheme

/**
 * Debug-only visual harness for the static GLB checkpoint.
 *
 * It is intentionally outside the production navigation graph so the existing CameraX/MediaPipe
 * AR flow remains unchanged while the asset is reviewed on a physical device.
 */
class FrameModelRendererActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EyecareTheme {
                FrameModelRenderer(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
