package com.eyecare.app.presentation.eyewear

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.common.components.StarRatingRow

@Composable
fun FrameRatingDialog(
    currentRating: Int? = null,
    currentComment: String? = null,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (Int, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedRating by remember { mutableIntStateOf(currentRating ?: 0) }
    var comment by remember { mutableStateOf(currentComment ?: "") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(if (currentRating != null) "Update your rating" else "Rate this frame") },
        text = {
            Column {
                StarRatingRow(
                    rating = selectedRating,
                    onRatingChange = { selectedRating = it },
                    enabled = !isSubmitting,
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 1000) comment = it },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    enabled = !isSubmitting,
                )

                errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedRating, comment.takeIf { it.isNotBlank() }) },
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
