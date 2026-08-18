package com.eyecare.app.presentation.messaging.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.eyecare.app.BuildConfig
import com.eyecare.app.domain.model.ConversationAccessLevel
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessageAttachment
import com.eyecare.app.domain.model.SenderType
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun shouldRenderImagePreview(
    attachment: MessageAttachment,
    accessLevel: ConversationAccessLevel,
): Boolean = accessLevel == ConversationAccessLevel.LINKED_PATIENT && isImageAttachment(attachment)

private fun isImageAttachment(attachment: MessageAttachment): Boolean {
    val mimeType = attachment.mimeType.trim().lowercase(Locale.ROOT)
    if (mimeType.startsWith("image/")) return true

    // Some staff-uploaded files arrive as application/octet-stream even though the
    // validated filename is an image. Keep the fallback limited to known image extensions.
    val extension = attachment.originalName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return extension in setOf("jpg", "jpeg", "png", "webp")
}

internal fun shouldShowMessageBody(body: String, hasAttachments: Boolean): Boolean =
    body.isNotBlank() && (!hasAttachments || !body.trim().equals("attachment", ignoreCase = true))

internal fun buildAttachmentPreviewUrl(attachmentId: Int, apiBaseUrl: String): String =
    "${apiBaseUrl.trimEnd('/')}/conversation/attachments/$attachmentId"

private val messageTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val messageDateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)
private val messageDateTimeWithYearFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.US)

/**
 * "2:32 PM" for today, "Yesterday, 2:32 PM", then a date once it's further back - shown in the
 * device's own time zone (this is when the patient sent/received it, not a clinic-time value)
 * rather than the raw ISO instant that used to render verbatim in the bubble.
 */
internal fun formatMessageTimestamp(
    iso: String,
    now: OffsetDateTime = OffsetDateTime.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val parsed = runCatching { OffsetDateTime.parse(iso) }.getOrNull()
        ?: return iso.take(16).replace("T", " ")

    val local = parsed.atZoneSameInstant(zone)
    val messageDate = local.toLocalDate()
    val today = now.atZoneSameInstant(zone).toLocalDate()

    return when {
        messageDate == today -> local.format(messageTimeFormatter)
        messageDate == today.minusDays(1) -> "Yesterday, ${local.format(messageTimeFormatter)}"
        messageDate.year == today.year -> local.format(messageDateTimeFormatter)
        else -> local.format(messageDateTimeWithYearFormatter)
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    conversationAccessLevel: ConversationAccessLevel = ConversationAccessLevel.UNKNOWN,
    onImageClick: (MessageAttachment) -> Unit = {},
    onFileClick: (MessageAttachment) -> Unit = {},
    onDownloadImageClick: (MessageAttachment) -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isOwn) 16.dp else 4.dp,
                bottomEnd = if (isOwn) 4.dp else 16.dp,
            ),
            color = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Hide the server's attachment placeholder, including lowercase variants.
                val showBody = shouldShowMessageBody(message.body, message.attachments.isNotEmpty())
                if (showBody) {
                    Text(
                        message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Attachments - only fetch protected image previews for linked_patient conversations.
                message.attachments.forEach { attachment ->
                    Spacer(Modifier.height(4.dp))
                    AttachmentContent(
                        attachment,
                        isOwn,
                        conversationAccessLevel,
                        onImageClick,
                        onFileClick,
                        onDownloadImageClick,
                    )
                }

                Text(
                    formatMessageTimestamp(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOwn) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun AttachmentContent(
    attachment: MessageAttachment,
    isOwn: Boolean,
    accessLevel: ConversationAccessLevel,
    onImageClick: (MessageAttachment) -> Unit,
    onFileClick: (MessageAttachment) -> Unit,
    onDownloadImageClick: (MessageAttachment) -> Unit,
) {
    // Only attempt image rendering for linked_patient conversations. The attachment ID is
    // sufficient to build the protected route; some upload responses omit download_url.
    val canLoadImage = shouldRenderImagePreview(attachment, accessLevel)
    val contentColor = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    if (canLoadImage) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Fit, not Crop, and no fixed height - the whole image is visible at its own
            // aspect ratio right in the bubble, not a cropped square peek that only reveals
            // its full extent once the patient taps into the full-screen viewer.
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(buildAttachmentPreviewUrl(attachment.id, BuildConfig.API_BASE_URL))
                    .build(),
                contentDescription = attachment.originalName,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onImageClick(attachment) },
                contentScale = ContentScale.Fit,
                imageLoader = SingletonImageLoader.get(LocalContext.current),
            )
            // Outer Surface owns the 48dp touch target (DESIGN.md's floor); the visible black
            // circle inside stays small so it doesn't dominate the photo underneath it.
            Surface(
                onClick = { onDownloadImageClick(attachment) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(48.dp),
                shape = CircleShape,
                color = Color.Transparent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download image",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Same access gate as the image preview - only offer to open the file when this
        // conversation is actually allowed to see attachment content.
        val canOpen = accessLevel == ConversationAccessLevel.LINKED_PATIENT
        Row(
            modifier = if (canOpen) Modifier.clickable { onFileClick(attachment) } else Modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                attachment.originalName.ifBlank { "Attachment" },
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (canOpen) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = "Open in another app",
                    modifier = Modifier.size(16.dp),
                    tint = contentColor,
                )
            }
        }
    }
}
