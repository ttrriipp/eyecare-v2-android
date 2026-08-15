package com.eyecare.app.presentation.ar

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.presentation.ar.components.VariantChipRow
import com.eyecare.app.presentation.ar.model.ArAssetState
import com.eyecare.app.presentation.ar.model.ArFaceState
import com.eyecare.app.presentation.ar.model.ArTryOnUiState
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FacePose
import com.eyecare.app.presentation.ar.rendering.FrameModelRenderState
import com.eyecare.app.presentation.ar.rendering.FrameModelRenderer
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent

@Composable
fun ArTryOnScreen(
    frameId: Int,
    initialVariantId: Int,
    onBack: () -> Unit,
) {
    val viewModel = hiltViewModel<ArViewModel, ArViewModel.Factory> {
        it.create(frameId, initialVariantId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imageLoader = SingletonImageLoader.get(context)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val activity = context as? android.app.Activity
        val rationale = activity?.let {
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                it,
                Manifest.permission.CAMERA,
            )
        } ?: false
        viewModel.onPermissionResult(granted = granted, shouldShowRationale = rationale)
    }

    LaunchedEffect(uiState) {
        if (uiState is ArTryOnUiState.PermissionRequired) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (val state = uiState) {
            ArTryOnUiState.CheckingCapability,
            ArTryOnUiState.PermissionRequired,
            -> LoadingContent()

            is ArTryOnUiState.PermissionDenied -> PermissionDeniedContent(
                state = state,
                context = context,
                launcher = { launcher.launch(Manifest.permission.CAMERA) },
            )

            is ArTryOnUiState.Unsupported -> ErrorContent(
                message = state.failures.firstOrNull()?.message
                    ?: "3D try-on is not supported on this device.",
            )

            is ArTryOnUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = viewModel::retry,
            )

            is ArTryOnUiState.Loading -> ActiveTryOnContent(
                variants = state.variants,
                selectedVariant = state.selectedVariant,
                face = null,
                pose = null,
                assetState = state.assetState,
                imageLoader = imageLoader,
                onFaceResult = viewModel::onFaceResult,
                onAssetStateChanged = viewModel::onAssetStateChanged,
                onSelectVariant = viewModel::selectVariant,
            )

            is ArTryOnUiState.Searching -> ActiveTryOnContent(
                variants = state.variants,
                selectedVariant = state.selectedVariant,
                face = null,
                pose = null,
                assetState = state.assetState,
                imageLoader = imageLoader,
                onFaceResult = viewModel::onFaceResult,
                onAssetStateChanged = viewModel::onAssetStateChanged,
                onSelectVariant = viewModel::selectVariant,
            )

            is ArTryOnUiState.Tracking -> ActiveTryOnContent(
                variants = state.variants,
                selectedVariant = state.selectedVariant,
                face = state.face,
                pose = state.pose,
                assetState = state.assetState,
                imageLoader = imageLoader,
                onFaceResult = viewModel::onFaceResult,
                onAssetStateChanged = viewModel::onAssetStateChanged,
                onSelectVariant = viewModel::selectVariant,
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50)),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
        }
    }
}

@Composable
private fun ActiveTryOnContent(
    variants: List<FrameVariant>,
    selectedVariant: FrameVariant?,
    face: FaceFrame?,
    pose: FacePose?,
    assetState: ArAssetState,
    imageLoader: ImageLoader,
    onFaceResult: (ArFaceState) -> Unit,
    onAssetStateChanged: (ArAssetState) -> Unit,
    onSelectVariant: (FrameVariant) -> Unit,
) {
    val frameUrl = selectedVariant?.arAssetReference?.let(::buildImageUrl)
    val showThreeD = assetState is ArAssetState.Ready && face != null && pose != null

    Box(Modifier.fillMaxSize()) {
        CameraPreviewView(
            modifier = Modifier.fillMaxSize(),
            onFaceResult = onFaceResult,
        )

        if (face != null && !showThreeD) {
            FrameOverlayRenderer(
                face = face,
                frameAssetUrl = frameUrl,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (face == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                ) {
                    Text(
                        "Position your face in the center",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        FrameModelRenderer(
            modifier = Modifier.fillMaxSize(),
            pose = if (face != null) pose else null,
            showModelWithoutPose = false,
            transparent = true,
            autoCenterContent = false,
            showStatus = false,
            onStateChanged = { onAssetStateChanged(it.toArAssetState()) },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(bottom = 24.dp, top = 12.dp),
        ) {
            if (variants.isNotEmpty()) {
                VariantChipRow(
                    variants = variants,
                    selectedVariant = selectedVariant,
                    onSelectVariant = onSelectVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    state: ArTryOnUiState.PermissionDenied,
    context: android.content.Context,
    launcher: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Camera access required",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (state.shouldShowRationale) {
                "Camera is needed to try on frames in AR. Please grant access."
            } else {
                "Camera permission was denied. Enable it in Settings to use AR try-on."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (state.shouldShowRationale) {
                    launcher()
                } else {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                }
            },
        ) {
            Text(if (state.shouldShowRationale) "Grant Permission" else "Open Settings")
        }
    }
}

private fun FrameModelRenderState.toArAssetState(): ArAssetState = when (this) {
    FrameModelRenderState.CheckingAsset -> ArAssetState.Checking
    FrameModelRenderState.Loading -> ArAssetState.Loading
    FrameModelRenderState.Ready -> ArAssetState.Ready
    is FrameModelRenderState.Failed -> ArAssetState.Failed(message)
}
