package com.eyecare.app.presentation.ar

import android.Manifest
import com.eyecare.app.presentation.common.buildImageUrl
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.eyecare.app.presentation.ar.components.VariantChipRow
import com.eyecare.app.presentation.ar.model.ArFaceState
import com.eyecare.app.presentation.ar.rendering.FrameModelRenderState
import com.eyecare.app.presentation.ar.rendering.FrameModelRenderer

@Composable
fun ArTryOnScreen(
    frameId: Int,
    initialVariantId: Int,
    onBack: () -> Unit,
) {
    val viewModel = hiltViewModel<ArViewModel, ArViewModel.Factory> {
        it.create(frameId, initialVariantId)
    }

    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()
    val faceState by viewModel.faceState.collectAsStateWithLifecycle()
    val facePose by viewModel.facePose.collectAsStateWithLifecycle()
    val variants by viewModel.variants.collectAsStateWithLifecycle()
    val selectedVariant by viewModel.selectedVariant.collectAsStateWithLifecycle()
    var modelRenderState by remember {
        mutableStateOf<FrameModelRenderState>(FrameModelRenderState.CheckingAsset)
    }
    val context = LocalContext.current
    val imageLoader = SingletonImageLoader.get(context)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val activity = context as? android.app.Activity
        val rationale = activity?.let {
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: false
        viewModel.onPermissionResult(granted = granted, shouldShowRationale = rationale)
    }

    LaunchedEffect(Unit) {
        if (permissionState is ArPermissionState.Required) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (permissionState) {
            is ArPermissionState.Granted -> {
                // Full-bleed camera
                CameraPreviewView(
                    modifier = Modifier.fillMaxSize(),
                    onFaceResult = viewModel::onFaceResult,
                )

                // Frame overlay when face detected
                val frameUrl = selectedVariant?.arAssetReference?.let { ref ->
                    buildImageUrl(ref)
                }
                when (val face = faceState) {
                    is ArFaceState.Detected -> {
                        val showThreeD = modelRenderState is FrameModelRenderState.Ready && facePose != null
                        if (!showThreeD) {
                            FrameOverlayRenderer(
                                face = face.frame,
                                frameAssetUrl = frameUrl,
                                imageLoader = imageLoader,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    is ArFaceState.NoFace -> {
                        // Guide message
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
                    else -> {}
                }

                FrameModelRenderer(
                    modifier = Modifier.fillMaxSize(),
                    pose = if (faceState is ArFaceState.Detected) facePose else null,
                    showModelWithoutPose = false,
                    transparent = true,
                    autoCenterContent = false,
                    showStatus = false,
                    onStateChanged = { modelRenderState = it },
                )

                // Bottom sheet: AR-ready frame variants
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
                            onSelectVariant = viewModel::selectVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            is ArPermissionState.Required -> {}

            is ArPermissionState.Denied -> {
                val state = permissionState as ArPermissionState.Denied
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Camera access required", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (state.shouldShowRationale) "Camera is needed to try on frames in AR. Please grant access."
                        else "Camera permission was denied. Enable it in Settings to use AR try-on.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (state.shouldShowRationale) launcher.launch(Manifest.permission.CAMERA)
                            else context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }
                    ) {
                        Text(if (state.shouldShowRationale) "Grant Permission" else "Open Settings")
                    }
                }
            }
        }

        // Close button — always top-left
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
