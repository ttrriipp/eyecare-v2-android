package com.eyecare.app.presentation.frames.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eyecare.app.ui.theme.EyecareColors
import java.util.Locale

/**
 * Displays a star rating badge: ★ 4.5 (12)
 *
 * Renders nothing when [averageRating] is null — unrated products
 * should not show a placeholder in the catalog list.
 *
 * Accessibility: reads "rated 4.5 out of 5 from 12 ratings"
 */
@Composable
fun RatingBadge(
    averageRating: Double?,
    ratingCount: Int,
    modifier: Modifier = Modifier,
) {
    if (averageRating == null) return

    val formatted = String.format(Locale.US, "%.1f", averageRating)
    val displayText = "★ $formatted ($ratingCount)"
    val a11yLabel = "rated $formatted out of 5 from $ratingCount ratings"

    Row(
        modifier = modifier.semantics { contentDescription = a11yLabel },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = EyecareColors.current.accentText,
        )
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Detail-screen variant: shows "No ratings yet" when unrated.
 */
@Composable
fun RatingSummary(
    averageRating: Double?,
    ratingCount: Int,
    modifier: Modifier = Modifier,
) {
    if (averageRating == null) {
        Text(
            text = "No ratings yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(top = 4.dp),
        )
        return
    }

    val formatted = String.format(Locale.US, "%.1f", averageRating)
    val a11yLabel = "rated $formatted out of 5 from $ratingCount ratings"

    Row(
        modifier = modifier.semantics { contentDescription = a11yLabel },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = EyecareColors.current.accentText,
        )
        Text(
            text = "$formatted ($ratingCount ratings)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
