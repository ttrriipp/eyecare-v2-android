package com.eyecare.app.presentation.messaging.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.messaging.PendingAttachment

@Composable
fun AttachmentPreview(
    attachment: PendingAttachment,
    error: String?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isImage = attachment.mimeType.startsWith("image/")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (error != null) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isImage) Icons.Default.Image else Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
            if (error != null) {
                Text(text = error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            } else {
                Text(
                    text = "%.1f KB".format(attachment.fileSize / 1024f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove attachment")
        }
    }
}
