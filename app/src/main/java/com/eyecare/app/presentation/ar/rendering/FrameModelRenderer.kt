package com.eyecare.app.presentation.ar.rendering

import android.os.SystemClock
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.ar.model.BundledFrameAsset
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FacePose
import com.eyecare.app.presentation.ar.tracking.FaceOccluderViewport
import com.eyecare.app.presentation.ar.tracking.mapFaceOccluder
import com.eyecare.app.presentation.ar.tracking.loadMediaPipeFaceMeshTopology
import io.github.sceneview.SceneScope
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.CameraNode
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
    face: FaceFrame? = null,
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
                    face = face,
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
    face: FaceFrame?,
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
    val templeVisibilityPolicy = remember(source.assetPath) { TempleVisibilityPolicy() }
    val templeVisibility = templeVisibilityPolicy.update(pose?.yawDeg)
    val currentTempleVisibility = rememberUpdatedState(templeVisibility)
    val faceOcclusionPolicy = remember { FaceOcclusionPolicy() }
    val faceOcclusionTopology = remember { loadMediaPipeFaceMeshTopology() }
    val faceOccluderNode = remember(engine, source.assetPath, faceOcclusionTopology) {
        faceOcclusionTopology?.let { topology ->
            FaceOccluderNode.create(
                engine = engine,
                materialLoader = materialLoader,
                topology = topology,
            )
        }
    }
    val faceOcclusionActive = remember(source.assetPath) { mutableStateOf(false) }

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
                FaceOcclusionNodeContent(
                    node = faceOccluderNode,
                    face = face,
                    pose = pose,
                    cameraNode = cameraNode,
                    policy = faceOcclusionPolicy,
                    activeState = faceOcclusionActive,
                )
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
                        apply = {
                            installTempleVisibilityUpdater {
                                if (faceOcclusionActive.value) {
                                    TempleVisibility.Both
                                } else {
                                    currentTempleVisibility.value
                                }
                            }
                        },
                    )
                }
            }
        }

        is FrameModelSource.Downloaded -> {
            var downloadedInstance by remember(source.assetPath) {
                mutableStateOf<com.google.android.filament.gltfio.FilamentInstance?>(null)
            }
            val nodeScale = remember(source.assetPath, source.modelScale, pose?.scale) {
                source.scaleForPose(pose?.scale ?: 1f)
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
                FaceOcclusionNodeContent(
                    node = faceOccluderNode,
                    face = face,
                    pose = pose,
                    cameraNode = cameraNode,
                    policy = faceOcclusionPolicy,
                    activeState = faceOcclusionActive,
                )
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
                        apply = {
                            installTempleVisibilityUpdater {
                                if (faceOcclusionActive.value) {
                                    TempleVisibility.Both
                                } else {
                                    currentTempleVisibility.value
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("RestrictedApi")
private fun SceneScope.FaceOcclusionNodeContent(
    node: FaceOccluderNode?,
    face: FaceFrame?,
    pose: FacePose?,
    cameraNode: CameraNode,
    policy: FaceOcclusionPolicy,
    activeState: MutableState<Boolean>,
) {
    node ?: return

    // SceneView exposes NodeLifecycle as the extension point for custom nodes, although
    // the pinned artifact marks it library-group restricted. Keep that suppression scoped here.
    NodeLifecycle(node = node, content = null)
    SideEffect {
        val view = cameraNode.view
        val geometry = if (face != null && pose != null && view != null) {
            val viewport = view.viewport
            if (viewport.width > 0 && viewport.height > 0) {
                face.faceMesh?.let { mesh ->
                    mapFaceOccluder(
                        landmarks = mesh,
                        imageWidth = face.imageWidth,
                        imageHeight = face.imageHeight,
                        faceWidthNorm = face.faceWidthNorm,
                        viewport = FaceOccluderViewport(
                            widthPx = viewport.width.toFloat(),
                            heightPx = viewport.height.toFloat(),
                        ),
                    )
                }
            } else {
                null
            }
        } else {
            null
        }

        val mode = policy.select(
            faceTimestampMs = face?.timestampMs,
            nowTimestampMs = SystemClock.uptimeMillis(),
            geometry = geometry,
        )
        val active = if (mode == FaceOcclusionMode.Depth && pose != null) {
            node.update(
                geometry = geometry,
                view = view,
                referencePlaneZ = pose.translationZ,
            )
        } else {
            node.hide()
            false
        }
        activeState.value = active
    }
}

private fun ModelNode.installTempleVisibilityUpdater(
    currentVisibility: () -> TempleVisibility,
) {
    val frontFrame = renderableNodes.firstOrNull { it.name == FRONT_FRAME_NODE }
    val leftTemple = renderableNodes.firstOrNull { it.name == LEFT_TEMPLE_NODE }
    val rightTemple = renderableNodes.firstOrNull { it.name == RIGHT_TEMPLE_NODE }

    // A combined or partially named asset is intentionally left unchanged. This preserves the
    // existing renderer behavior while allowing the separated asset to opt into the polish.
    if (frontFrame == null || leftTemple == null || rightTemple == null) return

    var appliedVisibility: TempleVisibility? = null
    onFrame = {
        val visibility = currentVisibility()
        if (visibility != appliedVisibility) {
            leftTemple.isVisible = visibility != TempleVisibility.RightOnly
            rightTemple.isVisible = visibility != TempleVisibility.LeftOnly
            appliedVisibility = visibility
        }
    }
}

private const val FRONT_FRAME_NODE = "frame_front"
private const val LEFT_TEMPLE_NODE = "temple_left"
private const val RIGHT_TEMPLE_NODE = "temple_right"

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
