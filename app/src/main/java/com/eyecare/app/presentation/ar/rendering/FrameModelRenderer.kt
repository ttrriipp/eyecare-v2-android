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
import com.eyecare.app.presentation.ar.model.FrameModelScale
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

private const val MODEL_LOAD_TIMEOUT_MILLIS = 15_000L
const val FRAME_MODEL_RENDERER_TAG = "frame-model-renderer"

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

@Composable
fun FrameModelRenderer(
    modifier: Modifier = Modifier,
    source: FrameModelSource = FrameModelSource.Bundled(BundledFrameAsset.RoundFrame),
    pose: FacePose? = null,
    showModelWithoutPose: Boolean = true,
    transparent: Boolean = false,
    autoCenterContent: Boolean = true,
    showStatus: Boolean = true,
    onStateChanged: (FrameModelRenderState) -> Unit = {},
) {
    val context = LocalContext.current
    var assetCheck by remember(source.assetPath) {
        mutableStateOf<AssetCheck>(AssetCheck.Checking)
    }
    var renderState by remember(source.assetPath) {
        mutableStateOf<FrameModelRenderState>(FrameModelRenderState.CheckingAsset)
    }

    LaunchedEffect(source.assetPath) {
        assetCheck = AssetCheck.Checking
        renderState = FrameModelRenderState.CheckingAsset

        val validation = when (source) {
            is FrameModelSource.Bundled -> source.descriptor.validate(context.assets)
            is FrameModelSource.Downloaded -> validateFileAsset(source.assetPath)
        }
        assetCheck = validation.fold(
            onSuccess = {
                renderState = FrameModelRenderState.Loading
                AssetCheck.Valid
            },
            onFailure = {
                val message = when (source) {
                    is FrameModelSource.Bundled ->
                        "Unable to load the bundled 3D frame. Try the image preview instead."
                    is FrameModelSource.Downloaded ->
                        "Unable to load the 3D frame. Try the image preview instead."
                }
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
                ModelScene(
                    source = source,
                    pose = pose,
                    showModelWithoutPose = showModelWithoutPose,
                    transparent = transparent,
                    autoCenterContent = autoCenterContent,
                    onRenderStateChange = { renderState = it },
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

@Composable
private fun ModelScene(
    source: FrameModelSource,
    pose: FacePose?,
    showModelWithoutPose: Boolean,
    transparent: Boolean,
    autoCenterContent: Boolean,
    onRenderStateChange: (FrameModelRenderState) -> Unit,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(z = 0.45f)
        lookAt(Position(x = 0f, y = 0f, z = 0f))
    }
    val mainLightNode = rememberMainLightNode(engine)

    when (source) {
        is FrameModelSource.Bundled -> {
            val modelInstance = rememberModelInstance(modelLoader, source.assetPath)
            val scale = source.descriptor.scaleForPose(pose?.scale ?: 1f)

            LaunchedEffect(modelInstance) {
                if (modelInstance != null) {
                    onRenderStateChange(FrameModelRenderState.Ready)
                } else {
                    kotlinx.coroutines.delay(MODEL_LOAD_TIMEOUT_MILLIS)
                    onRenderStateChange(
                        FrameModelRenderState.Failed(
                            "The 3D frame could not be initialized. Try the image preview instead.",
                        ),
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
                        scale = Scale(scale.x, scale.y, scale.z),
                        isVisible = showModelWithoutPose || pose != null,
                    )
                }
            }
        }

        is FrameModelSource.Downloaded -> {
            var downloadedInstance by remember(source.assetPath) {
                mutableStateOf<com.google.android.filament.gltfio.FilamentInstance?>(null)
            }
            val nodeScale = remember(pose?.scale) {
                val s = pose?.scale ?: 1f
                FrameModelScale(s, s, s)
            }

            LaunchedEffect(source.assetPath) {
                try {
                    val file = java.io.File(source.assetPath)
                    val instance = modelLoader.createModelInstance(file)
                    downloadedInstance = instance
                    onRenderStateChange(FrameModelRenderState.Ready)
                } catch (e: Exception) {
                    onRenderStateChange(
                        FrameModelRenderState.Failed(
                            "The 3D frame could not be initialized. Try the image preview instead.",
                        ),
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
                downloadedInstance?.let { instance ->
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
                        scale = Scale(nodeScale.x, nodeScale.y, nodeScale.z),
                        isVisible = showModelWithoutPose || pose != null,
                    )
                }
            }
        }
    }
}

private fun validateFileAsset(filePath: String): Result<Unit> = runCatching {
    val file = java.io.File(filePath)
    require(file.exists()) {
        "Downloaded asset file does not exist"
    }
    require(file.length() > 0) {
        "Downloaded asset file is empty"
    }
    file.inputStream().use { input ->
        val header = ByteArray(GLB_HEADER_SIZE)
        var bytesRead = 0
        while (bytesRead < header.size) {
            val read = input.read(header, bytesRead, header.size - bytesRead)
            if (read < 0) break
            bytesRead += read
        }
        require(bytesRead == GLB_HEADER_SIZE) { "GLB header is truncated" }
        require(header.copyOfRange(0, 4).contentEquals(GLB_MAGIC)) {
            "Asset is not a binary glTF file"
        }
        val version = readIntLittleEndian(header, offset = 4)
        require(version == GLB_VERSION) { "Unsupported binary glTF version: $version" }
        val declaredLength = readIntLittleEndian(header, offset = 8)
        require(declaredLength >= GLB_HEADER_SIZE) { "GLB length is invalid" }
    }
}

private const val GLB_HEADER_SIZE = 12
private const val GLB_VERSION = 2
private val GLB_MAGIC = byteArrayOf(0x67, 0x6c, 0x54, 0x46)

private fun readIntLittleEndian(bytes: ByteArray, offset: Int): Int =
    java.nio.ByteBuffer.wrap(bytes, offset, Int.SIZE_BYTES)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        .int

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
