package com.eyecare.app.presentation.eyewear

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eyecare.app.domain.model.ProductRatingAttachment
import com.eyecare.app.presentation.common.components.StarRatingRow

@Composable
fun FrameRatingDialog(
    currentRating: Int? = null,
    currentComment: String? = null,
    currentAttachmentUrl: String? = null,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (Int, String?, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSubmitWithAttachment: ((Int, String?, Boolean, ProductRatingAttachment?, Boolean) -> Unit)? = null,
) {
    var selectedRating by remember { mutableIntStateOf(currentRating ?: 0) }
    var comment by remember { mutableStateOf(currentComment ?: "") }
    var publicDisplayConsent by remember { mutableStateOf(false) }
    var publicAttachmentConsent by remember { mutableStateOf(false) }
    var selectedAttachment by remember { mutableStateOf<ProductRatingAttachment?>(null) }
    var attachmentErrorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val selection = prepareProductRatingAttachment(context, uri)
            attachmentErrorMessage = selection.errorMessage
            selection.attachment?.let { attachment ->
                selectedAttachment = attachment
                publicAttachmentConsent = false
            }
        }
    }

    DisposableEffect(selectedAttachment) {
        val temporaryFile = selectedAttachment?.imageFile
        onDispose { temporaryFile?.delete() }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(if (currentRating != null) "Update your rating" else "Rate this product") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                StarRatingRow(
                    rating = selectedRating,
                    onRatingChange = { selectedRating = it },
                    enabled = !isSubmitting,
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = {
                        if (it.length <= 1000) {
                            if (it != comment) publicDisplayConsent = false
                            comment = it
                        }
                    },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    enabled = !isSubmitting,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = publicDisplayConsent,
                            enabled = comment.isNotBlank() && !isSubmitting,
                            role = Role.Checkbox,
                            onValueChange = { publicDisplayConsent = it },
                        )
                        .testTag("public-review-consent"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = publicDisplayConsent,
                        onCheckedChange = null,
                        enabled = comment.isNotBlank() && !isSubmitting,
                    )
                    Column {
                        Text("Share this comment publicly", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Your name won't be shown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        attachmentErrorMessage = null
                        attachmentPicker.launch(arrayOf("image/jpeg", "image/png"))
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (selectedAttachment == null && currentAttachmentUrl.isNullOrBlank()) {
                            "Add a review photo"
                        } else {
                            "Replace review photo"
                        },
                    )
                }

                selectedAttachment?.let { attachment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = attachment.imageFile,
                            contentDescription = "Selected review photo preview",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Photo attached", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${attachment.width} × ${attachment.height} pixels",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = {
                                selectedAttachment = null
                                publicAttachmentConsent = false
                                attachmentErrorMessage = null
                            },
                            enabled = !isSubmitting,
                        ) {
                            Text("Remove")
                        }
                    }
                }

                if (selectedAttachment == null) {
                    currentAttachmentUrl?.takeIf(String::isNotBlank)?.let { attachmentUrl ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OwnerRatingPhoto(
                                url = attachmentUrl,
                                contentDescription = "Current review photo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Photo attached", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Only you can see this photo unless you choose to share it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (selectedAttachment != null || currentRating != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = publicAttachmentConsent,
                                enabled = !isSubmitting,
                                role = Role.Checkbox,
                                onValueChange = { publicAttachmentConsent = it },
                            )
                            .testTag("public-review-attachment-consent"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = publicAttachmentConsent,
                            onCheckedChange = null,
                            enabled = !isSubmitting,
                        )
                        Column {
                            Text("Show the photo publicly", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Photos appear only with public comments and may reveal identifying details. This can also share a photo already attached to this rating.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                attachmentErrorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nonBlankComment = comment.takeIf { it.isNotBlank() }
                    val publicComment = publicDisplayConsent && nonBlankComment != null
                    if (onSubmitWithAttachment != null) {
                        onSubmitWithAttachment(
                            selectedRating,
                            nonBlankComment,
                            publicComment,
                            selectedAttachment,
                            publicAttachmentConsent,
                        )
                    } else {
                        onSubmit(selectedRating, nonBlankComment, publicComment)
                    }
                },
                enabled = selectedRating in 1..5 && !isSubmitting,
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                else Text(if (currentRating != null) "Update" else "Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        },
    )
}
