package com.eyecare.app.presentation.messaging.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.eyecare.app.BuildConfig
import com.eyecare.app.domain.model.MessageAttachment

/**
 * Full-screen, swipeable, pinch-zoomable viewer for the image attachments already shown as
 * thumbnails in the chat - "other attachments" here means every image sent in this
 * conversation, not just the one that was tapped, so the patient can page through the whole
 * gallery without going back to the thread.
 */
@Composable
fun AttachmentGalleryViewer(
    attachments: List<MessageAttachment>,
    initialAttachmentId: Int,
    downloadingAttachmentId: Int?,
    downloadError: String?,
    onOpenExternally: (MessageAttachment) -> Unit,
    onDownload: (MessageAttachment) -> Unit,
    onRetryDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (attachments.isEmpty()) return
    val initialPage = attachments.indexOfFirst { it.id == initialAttachmentId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { attachments.size }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    val currentAttachment = attachments[pagerState.currentPage]
    // Global in the ViewModel (only one download runs at a time), but must only read as "busy"
    // on the page it actually belongs to - otherwise swiping to a different image while a
    // download is in flight shows every page as busy, not just the one being downloaded.
    val isCurrentPageDownloading = downloadingAttachmentId == currentAttachment.id

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isCurrentPageZoomed,
                modifier = Modifier.fillMaxSize(),
                key = { attachments[it].id },
            ) { page ->
                ZoomableAttachmentImage(
                    attachment = attachments[page],
                    onZoomedChanged = { zoomed ->
                        if (pagerState.currentPage == page) isCurrentPageZoomed = zoomed
                    },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                if (attachments.size > 1) {
                    Text(
                        "${pagerState.currentPage + 1} of ${attachments.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.weight(1f))
                }
                IconButton(
                    onClick = { onDownload(currentAttachment) },
                    enabled = !isCurrentPageDownloading,
                    modifier = Modifier.size(48.dp),
                ) {
                    if (isCurrentPageDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.Download, contentDescription = "Save to Photos", tint = Color.White)
                    }
                }
                IconButton(
                    onClick = { onOpenExternally(currentAttachment) },
                    enabled = !isCurrentPageDownloading,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open in another app", tint = Color.White)
                }
            }

            // Renders inside this same Dialog window - a Text placed in the caller's own
            // Column would sit behind this full-screen Dialog and never be seen until the
            // patient closes the viewer.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                downloadError?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = error,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            TextButton(onClick = onRetryDownload) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                }
                Text(
                    text = currentAttachment.originalName,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val ZOOMED_THRESHOLD = 1.05f

@Composable
private fun ZoomableAttachmentImage(
    attachment: MessageAttachment,
    onZoomedChanged: (Boolean) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(attachment.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    scale = newScale
                    if (newScale > MIN_SCALE) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                    onZoomedChanged(newScale > ZOOMED_THRESHOLD)
                }
            }
            .pointerInput(attachment.id) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = MIN_SCALE
                        offsetX = 0f
                        offsetY = 0f
                        onZoomedChanged(false)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(buildAttachmentPreviewUrl(attachment.id, BuildConfig.API_BASE_URL))
                .build(),
            contentDescription = attachment.originalName,
            imageLoader = SingletonImageLoader.get(LocalContext.current),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                ),
        )
    }
}
