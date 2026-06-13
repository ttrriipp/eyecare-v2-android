package com.eyecare.app.presentation.messaging.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessageAttachment
import com.eyecare.app.BuildConfig

@Composable
fun MessageBubble(message: Message, isOwn: Boolean) {
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
                // Body text — skip if it's the placeholder for a pure attachment message
                if (message.body.isNotBlank() && message.body != "Attachment") {
                    Text(
                        message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOwn) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Attachments
                message.attachments.forEach { attachment ->
                    Spacer(Modifier.height(4.dp))
                    AttachmentContent(attachment, isOwn)
                }

                Text(
                    message.createdAt.take(16).replace("T", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOwn) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun AttachmentContent(attachment: MessageAttachment, isOwn: Boolean) {
    val context = LocalContext.current
    if (attachment.mimeType.startsWith("image/")) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("${BuildConfig.API_BASE_URL}attachments/${attachment.id}")
                .build(),
            imageLoader = SingletonImageLoader.get(context),
            contentDescription = attachment.originalName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(200.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 2.dp),
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isOwn) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                attachment.originalName,
                style = MaterialTheme.typography.bodySmall,
                color = if (isOwn) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}
