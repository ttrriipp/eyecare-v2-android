package com.eyecare.app.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eyecare.app.ui.theme.EyecareColors

/**
 * Shared 1-5 star picker used by both the visit-feedback and frame-rating dialogs, so the two
 * features read as one consistent rating control rather than two separately-built widgets.
 */
@Composable
fun StarRatingRow(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    starCount: Int = 5,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        (1..starCount).forEach { star ->
            val isSelected = star <= rating
            val label = if (isSelected) {
                "Star $star of $starCount, selected"
            } else {
                "Star $star of $starCount, not selected"
            }
            IconButton(
                onClick = { onRatingChange(star) },
                enabled = enabled,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = label,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected) EyecareColors.current.accentText
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
