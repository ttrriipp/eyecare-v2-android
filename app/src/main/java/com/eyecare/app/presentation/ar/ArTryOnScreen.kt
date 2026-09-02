package com.eyecare.app.presentation.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.presentation.ar.components.ArAssetStatusBanner
import com.eyecare.app.presentation.ar.components.ArDisclosureBanner
import com.eyecare.app.presentation.ar.components.ArSavedFrameDisclaimer
import com.eyecare.app.presentation.ar.components.ArStatusOverlay
import com.eyecare.app.presentation.ar.components.VariantChipRow
import com.eyecare.app.presentation.ar.model.ArAssetSource
import com.eyecare.app.presentation.ar.model.ArAssetState
import com.eyecare.app.presentation.ar.model.ArFaceState
import com.eyecare.app.presentation.ar.model.ArTryOnUiState
import com.eyecare.app.presentation.ar.rendering.FrameModelRenderState
import com.eyecare.app.presentation.ar.rendering.FrameModelRenderer
import com.eyecare.app.presentation.ar.rendering.FrameModelSource
import com.eyecare.app.presentation.common.RefreshOnResumeEffect

@Composable
fun ArTryOnScreen(
    frameId: Int,
    initialVariantId: Int,
    onBack: () -> Unit,
    onOpenCatalog: () -> Unit = onBack,
) {
    val viewModel = hiltViewModel<ArViewModel, ArViewModel.Factory> {
        it.create(frameId, initialVariantId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val assetSource by viewModel.assetSource.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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

    // Camera access is the highest-trust ask on this screen: a returning user who already
    // granted it skips straight through (the OS shows no dialog either way), but a first-time
    // or previously-denied user sees ArStatusOverlay's rationale card and its own "Allow camera
    // access" button before the system dialog ever appears — never an unexplained OS prompt.
    val cameraAlreadyGranted = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }
    LaunchedEffect(uiState) {
        if (uiState is ArTryOnUiState.PermissionRequired && cameraAlreadyGranted) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    RefreshOnResumeEffect(onRefresh = viewModel::refreshSavedState)

    val activeContentState = uiState.toActiveTryOnContentState()
    LaunchedEffect(activeContentState?.saveError) {
        activeContentState?.saveError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveError()
        }
    }
    LaunchedEffect(activeContentState?.saveMessage) {
        activeContentState?.saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveMessage()
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
        if (activeContentState != null) {
            ActiveTryOnContent(
                state = activeContentState,
                assetSource = assetSource,
                onFaceResult = viewModel::onFaceResult,
                onAssetStateChanged = viewModel::onAssetStateChanged,
                onSelectVariant = viewModel::selectVariant,
                onToggleSaved = viewModel::toggleSaved,
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
                .statusBarsPadding()
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50)),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 112.dp),
        )
    }
}

@Composable
private fun ActiveTryOnContent(
    state: ActiveTryOnContentState,
    assetSource: ArAssetSource,
    onFaceResult: (ArFaceState) -> Unit,
    onAssetStateChanged: (ArAssetState) -> Unit,
    onSelectVariant: (FrameVariant) -> Unit,
    onToggleSaved: () -> Unit,
    onOpenCatalog: () -> Unit,
) {
    val rendererSource = assetSource.toFrameModelSource()

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

        // Catalog images are ordinary product photos and may have opaque backgrounds. Keep them
        // off the tracked face; the dedicated "View frame images" action remains available below.
        if (state.face == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                ) {
                    Text(
                        when {
                            state.phase == ActiveTryOnPhase.Loading ->
                                "Loading this frame's preview…"
                            state.hasTrackedBefore ->
                                "Lost you for a moment — hold still and center your face again"
                            else ->
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

        // Stacked in one Column, anchored once, so a wrapped line at larger font scales pushes
        // the banner below it down instead of the two independently-offset banners colliding.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.phase == ActiveTryOnPhase.Tracking && state.face != null) {
                ArDisclosureBanner()
            }
            ArAssetStatusBanner(state = state.assetState)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.4f))
                .navigationBarsPadding()
                .padding(bottom = 24.dp, top = 12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val selectedVariant = state.selectedVariant
                val canSave = selectedVariant != null
                if (canSave) {
                    Button(
                        onClick = onToggleSaved,
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.size(6.dp))
                        }
                        Text(if (selectedVariant.isSaved) "Remove from saved" else "Save this frame")
                    }
                    ArSavedFrameDisclaimer(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
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
