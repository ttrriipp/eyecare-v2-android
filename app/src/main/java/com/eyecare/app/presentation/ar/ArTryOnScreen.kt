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
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.eyecare.app.presentation.ar.components.VariantChipRow
import com.eyecare.app.presentation.ar.model.ArFaceState

@Composable
fun ArTryOnScreen(
    productId: Int,
    initialVariantId: Int,
    onBack: () -> Unit,
    onNavigateToOrder: (productId: Int, variantId: Int) -> Unit,
) {
    val viewModel = hiltViewModel<ArViewModel, ArViewModel.Factory> {
        it.create(productId, initialVariantId)
    }

    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()
    val faceState by viewModel.faceState.collectAsStateWithLifecycle()
    val variants by viewModel.variants.collectAsStateWithLifecycle()
    val selectedVariant by viewModel.selectedVariant.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imageLoader = SingletonImageLoader.get(context)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted = granted, shouldShowRationale = !granted)
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
                        FrameOverlayRenderer(
                            face = face.frame,
                            frameAssetUrl = frameUrl,
                            imageLoader = imageLoader,
                            modifier = Modifier.fillMaxSize(),
                        )
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

                // Bottom sheet: variant chips + Order FAB
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(bottom = 24.dp, top = 12.dp),
                ) {
                    Column {
                        if (variants.isNotEmpty()) {
                            VariantChipRow(
                                variants = variants,
                                selectedVariant = selectedVariant,
                                onSelectVariant = viewModel::selectVariant,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (selectedVariant != null) {
                            Spacer(Modifier.height(12.dp))
                            ExtendedFloatingActionButton(
                                onClick = { onNavigateToOrder(productId, selectedVariant!!.id) },
                                icon = { Icon(Icons.Outlined.ShoppingBag, contentDescription = null) },
                                text = { Text("Order this frame") },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(end = 16.dp),
                            )
                        }
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
