package com.eyecare.app.presentation.ar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.ar.model.ArAssetState
import com.eyecare.app.presentation.ar.model.ArTryOnUiState

private const val AR_DISCLOSURE_TEXT =
    "Visual preview only. Final fit is confirmed at the clinic."

private enum class ArPrimaryAction {
    RequestPermission,
    OpenSettings,
    Retry,
}

private data class ArStatusCopy(
    val title: String,
    val message: String,
    val primaryAction: ArPrimaryAction? = null,
    val primaryLabel: String? = null,
)

/** A non-camera surface for capability, permission, and recoverable failures. */
@Composable
fun ArStatusOverlay(
    state: ArTryOnUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenCatalog: () -> Unit,
) {
    val copy = state.toStatusCopy() ?: return

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = copy.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                copy.primaryAction?.let { action ->
                    Button(
                        onClick = {
                            when (action) {
                                ArPrimaryAction.RequestPermission -> onRequestPermission()
                                ArPrimaryAction.OpenSettings -> onOpenSettings()
                                ArPrimaryAction.Retry -> onRetry()
                            }
                        },
                    ) {
                        Text(copy.primaryLabel.orEmpty())
                    }
                }

                TextButton(onClick = onOpenCatalog) {
                    Text("View frame images")
                }
            }
        }
    }
}

/** Non-clinical disclosure that remains visible while a face is being tracked. */
@Composable
fun ArDisclosureBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.62f),
    ) {
        Text(
            text = AR_DISCLOSURE_TEXT,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

/** Keeps model loading and fallback status visible without covering the camera. */
@Composable
fun ArAssetStatusBanner(
    state: ArAssetState,
    modifier: Modifier = Modifier,
) {
    val message = when (state) {
        ArAssetState.Checking -> "Preparing 3D frame…"
        ArAssetState.Loading -> "Loading 3D frame…"
        ArAssetState.Ready -> return
        is ArAssetState.Failed -> "3D preview unavailable. Showing image preview."
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.62f),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

private fun ArTryOnUiState.toStatusCopy(): ArStatusCopy? = when (this) {
    ArTryOnUiState.CheckingCapability -> ArStatusCopy(
        title = "Preparing 3D try-on",
        message = "Checking this device before opening the camera. You can still view frame images.",
    )

    ArTryOnUiState.PermissionRequired -> ArStatusCopy(
        title = "Camera access needed",
        message = "Allow camera access to try this frame on your face. You can still view frame images.",
    )

    is ArTryOnUiState.PermissionDenied -> ArStatusCopy(
        title = "Camera access required",
        message = if (shouldShowRationale) {
            "Camera access is needed for the live preview. Grant access or view frame images instead."
        } else {
            "Camera access was denied. Enable it in Settings or view frame images instead."
        },
        primaryAction = if (shouldShowRationale) {
            ArPrimaryAction.RequestPermission
        } else {
            ArPrimaryAction.OpenSettings
        },
        primaryLabel = if (shouldShowRationale) "Grant camera access" else "Open settings",
    )

    is ArTryOnUiState.Unsupported -> ArStatusCopy(
        title = "3D try-on isn't available",
        message = buildString {
            append(failures.firstOrNull()?.message ?: "This device does not meet the 3D preview requirements.")
            append(" You can still view frame images and reserve this frame from the catalog.")
        },
    )

    is ArTryOnUiState.Error -> ArStatusCopy(
        title = "3D preview unavailable",
        message = "$message You can still view frame images and reserve this frame from the catalog.",
        primaryAction = ArPrimaryAction.Retry,
        primaryLabel = "Retry",
    )

    is ArTryOnUiState.Loading,
    is ArTryOnUiState.Searching,
    is ArTryOnUiState.Tracking,
    -> null
}
