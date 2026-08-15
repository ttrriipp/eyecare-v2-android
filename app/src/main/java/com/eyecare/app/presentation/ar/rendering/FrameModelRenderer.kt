package com.eyecare.app.presentation.ar.rendering

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.ar.model.BundledFrameAsset
import com.eyecare.app.presentation.ar.model.FacePose
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

private const val MODEL_LOAD_TIMEOUT_MILLIS = 15_000L
const val FRAME_MODEL_RENDERER_TAG = "frame-model-renderer"

/** State exposed by the isolated renderer so its host can choose a fallback without guessing. */
sealed interface FrameModelRenderState {
    data object CheckingAsset : FrameModelRenderState
    data object Loading : FrameModelRenderState
    data object Ready : FrameModelRenderState
    data class Failed(val message: String) : FrameModelRenderState
}

private sealed interface AssetCheck {
    data object Checking : AssetCheck
    data object Valid : AssetCheck
    data class Invalid(val message: String) : AssetCheck
}

/**
 * Renders one known GLB in a standalone SceneView viewport.
 *
 * SceneView remembers and disposes Filament resources with the Compose lifecycle; the caller only
 * observes the explicit loading/ready/failure state and can keep a non-3D fallback visible when
 * needed.
 */
@Composable
fun FrameModelRenderer(
    modifier: Modifier = Modifier,
    asset: BundledFrameAsset = BundledFrameAsset.RoundFrame,
    pose: FacePose? = null,
    showModelWithoutPose: Boolean = true,
    transparent: Boolean = false,
    autoCenterContent: Boolean = true,
    showStatus: Boolean = true,
    onStateChanged: (FrameModelRenderState) -> Unit = {},
) {
    val context = LocalContext.current
    var assetCheck by remember(asset.assetPath) {
        mutableStateOf<AssetCheck>(AssetCheck.Checking)
    }
    var renderState by remember(asset.assetPath) {
        mutableStateOf<FrameModelRenderState>(FrameModelRenderState.CheckingAsset)
    }

    LaunchedEffect(asset.assetPath) {
        assetCheck = AssetCheck.Checking
        renderState = FrameModelRenderState.CheckingAsset

        val validation = asset.validate(context.assets)
        assetCheck = validation.fold(
            onSuccess = {
                renderState = FrameModelRenderState.Loading
                AssetCheck.Valid
            },
            onFailure = {
                val message = "Unable to load the bundled 3D frame. Try the image preview instead."
                renderState = FrameModelRenderState.Failed(message = message)
                AssetCheck.Invalid(message)
            },
        )
    }

    LaunchedEffect(renderState) {
        onStateChanged(renderState)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(FRAME_MODEL_RENDERER_TAG),
    ) {
        when (val check = assetCheck) {
            AssetCheck.Checking -> if (showStatus) {
                RendererStatus(
                    message = "Checking frame model",
                    showProgress = true,
                )
            }

            is AssetCheck.Invalid -> if (showStatus) {
                RendererStatus(message = check.message)
            }

            AssetCheck.Valid -> {
                val engine = rememberEngine()
                val modelLoader = rememberModelLoader(engine)
                val materialLoader = rememberMaterialLoader(engine)
                val environmentLoader = rememberEnvironmentLoader(engine)
                val cameraNode = rememberCameraNode(engine) {
                    position = Position(z = 0.45f)
                    lookAt(Position(x = 0f, y = 0f, z = 0f))
                }
                val mainLightNode = rememberMainLightNode(engine)
                val modelInstance = rememberModelInstance(modelLoader, asset.assetPath)

                LaunchedEffect(modelInstance, asset.assetPath) {
                    if (modelInstance != null) {
                        renderState = FrameModelRenderState.Ready
                    } else {
                        renderState = FrameModelRenderState.Loading
                        kotlinx.coroutines.delay(MODEL_LOAD_TIMEOUT_MILLIS)
                        renderState = FrameModelRenderState.Failed(
                            message = "The 3D frame could not be initialized. Try the image preview instead.",
                        )
                    }
                }

                SceneView(
                    modifier = Modifier.fillMaxSize(),
                    surfaceType = SurfaceType.TextureSurface,
                    isOpaque = !transparent,
                    autoCenterContent = autoCenterContent,
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    environmentLoader = environmentLoader,
                    cameraNode = cameraNode,
                    mainLightNode = mainLightNode,
                    fillLightNode = null,
                ) {
                    modelInstance?.let { instance ->
                        ModelNode(
                            modelInstance = instance,
                            autoAnimate = false,
                            position = pose?.let { currentPose ->
                                Position(
                                    x = currentPose.translationX,
                                    y = currentPose.translationY,
                                    z = currentPose.translationZ,
                                )
                            } ?: Position(),
                            rotation = pose?.let { currentPose ->
                                Rotation(
                                    x = currentPose.pitchDeg,
                                    y = currentPose.yawDeg,
                                    z = currentPose.rollDeg,
                                )
                            } ?: Rotation(0f),
                            scale = pose?.let { currentPose ->
                                Scale(
                                    asset.scale.x * currentPose.scale,
                                    asset.scale.y * currentPose.scale,
                                    asset.scale.z * currentPose.scale,
                                )
                            } ?: Scale(asset.scale.x, asset.scale.y, asset.scale.z),
                            isVisible = showModelWithoutPose || pose != null,
                        )
                    }
                }

                if (showStatus) {
                    when (val state = renderState) {
                        FrameModelRenderState.CheckingAsset,
                        FrameModelRenderState.Loading,
                        -> RendererStatus(
                            message = "Loading 3D frame",
                            showProgress = true,
                        )

                        FrameModelRenderState.Ready -> RendererStatus(
                            message = "3D frame model ready",
                            alignment = Alignment.BottomCenter,
                        )

                        is FrameModelRenderState.Failed -> RendererStatus(
                            message = state.message,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RendererStatus(
    message: String,
    showProgress: Boolean = false,
    alignment: Alignment = Alignment.Center,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.72f), shape = MaterialTheme.shapes.medium)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showProgress) {
                CircularProgressIndicator()
            }
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
