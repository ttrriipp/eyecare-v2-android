package com.eyecare.app.presentation.ar

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.eyecare.app.presentation.ar.components.ArAssetStatusBanner
import com.eyecare.app.presentation.ar.components.ArDisclosureBanner
import com.eyecare.app.presentation.ar.components.ArStatusOverlay
import com.eyecare.app.presentation.ar.components.VariantChipRow
import com.eyecare.app.presentation.ar.model.ArAssetSource
import com.eyecare.app.presentation.ar.model.ArAssetState
import com.eyecare.app.presentation.ar.model.ArFaceState
import com.eyecare.app.presentation.ar.model.ArTryOnUiState
import com.eyecare.app.presentation.ar.rendering.FrameModelRenderState
import com.eyecare.app.presentation.ar.rendering.FrameModelRenderer
import com.eyecare.app.presentation.ar.rendering.FrameModelSource
import com.eyecare.app.presentation.common.buildImageUrl

@Composable
fun ArTryOnScreen(
    frameId: Int,
    initialVariantId: Int,
    onBack: () -> Unit,
    onReserveFrame: (frameId: Int, variantId: Int) -> Unit,
    onOpenCatalog: () -> Unit = onBack,
) {
    val viewModel = hiltViewModel<ArViewModel, ArViewModel.Factory> {
        it.create(frameId, initialVariantId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val assetSource by viewModel.assetSource.collectAsStateWithLifecycle()
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

    val openSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        val activeContentState = uiState.toActiveTryOnContentState()
        if (activeContentState != null) {
            ActiveTryOnContent(
                state = activeContentState,
                assetSource = assetSource,
                imageLoader = imageLoader,
                onFaceResult = viewModel::onFaceResult,
                onAssetStateChanged = viewModel::onAssetStateChanged,
                onSelectVariant = viewModel::selectVariant,
                onReserveFrame = { variantId -> onReserveFrame(frameId, variantId) },
                onOpenCatalog = onOpenCatalog,
            )
        } else {
            ArStatusOverlay(
                state = uiState,
                onRetry = viewModel::retry,
                onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = openSettings,
                onOpenCatalog = onOpenCatalog,
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
    state: ActiveTryOnContentState,
    assetSource: ArAssetSource,
    imageLoader: ImageLoader,
    onFaceResult: (ArFaceState) -> Unit,
    onAssetStateChanged: (ArAssetState) -> Unit,
    onSelectVariant: (FrameVariant) -> Unit,
    onReserveFrame: (variantId: Int) -> Unit,
    onOpenCatalog: () -> Unit,
) {
    val frameUrl = state.selectedVariant
        ?.tryOnPreviewImageReference()
        ?.let(::buildImageUrl)
    val rendererSource = assetSource.toFrameModelSource()
    val assetReady = rendererSource != null && state.assetState is ArAssetState.Ready
    val hasPose = state.face != null && state.pose != null
    val showThreeD = assetReady && hasPose

    Box(Modifier.fillMaxSize()) {
        CameraPreviewView(
            modifier = Modifier.fillMaxSize(),
            onFaceResult = onFaceResult,
        )

        if (rendererSource != null) {
            FrameModelRenderer(
                modifier = Modifier.fillMaxSize(),
                source = rendererSource,
                face = state.face,
                pose = if (state.face != null) state.pose else null,
                showModelWithoutPose = false,
                transparent = true,
                autoCenterContent = false,
                showStatus = false,
                onStateChanged = { onAssetStateChanged(it.toArAssetState()) },
            )
        }

        if (state.face != null && !showThreeD) {
            FrameOverlayRenderer(
                face = state.face,
                frameAssetUrl = frameUrl,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (state.face == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                ) {
                    Text(
                        if (state.phase == ActiveTryOnPhase.Loading) {
                            "Loading this frame's preview…"
                        } else {
                            "Position your face in the center"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (state.phase == ActiveTryOnPhase.Tracking && state.face != null) {
            ArDisclosureBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
            )
        }

        ArAssetStatusBanner(
            state = state.assetState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (state.face != null) 112.dp else 64.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(bottom = 24.dp, top = 12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val selectedVariant = state.selectedVariant
                val canReserve = selectedVariant != null &&
                    state.assetState !is ArAssetState.Loading &&
                    state.assetState !is ArAssetState.Checking
                if (canReserve) {
                    Button(
                        onClick = { onReserveFrame(selectedVariant.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Reserve this frame")
                    }
                }
                OutlinedButton(
                    onClick = onOpenCatalog,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.7f),
                    ),
                ) {
                    Text("View frame images")
                }
                if (state.variants.isNotEmpty()) {
                    VariantChipRow(
                        variants = state.variants,
                        selectedVariant = state.selectedVariant,
                        onSelectVariant = onSelectVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun FrameModelRenderState.toArAssetState(): ArAssetState = when (this) {
    FrameModelRenderState.CheckingAsset -> ArAssetState.Checking
    FrameModelRenderState.Loading -> ArAssetState.Loading
    FrameModelRenderState.Ready -> ArAssetState.Ready
    is FrameModelRenderState.Failed -> ArAssetState.Failed(message)
}

internal fun FrameVariant.tryOnPreviewImageReference(): String? = images.firstOrNull()
