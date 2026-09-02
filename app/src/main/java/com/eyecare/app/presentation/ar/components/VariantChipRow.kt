package com.eyecare.app.presentation.ar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.FrameVariant

@Composable
fun VariantChipRow(
    variants: List<FrameVariant>,
    selectedVariant: FrameVariant?,
    onSelectVariant: (FrameVariant) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(variants, key = { it.id }) { variant ->
            val isSelected = variant.id == selectedVariant?.id
            FilterChip(
                selected = isSelected,
                onClick = { onSelectVariant(variant) },
                label = { Text(variant.name) },
                shape = RoundedCornerShape(32.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.3f),
                    labelColor = Color.White,
                    // Selection uses the same solid Lens Cyan fill + Charcoal-on-Cyan text as the
                    // app's other selected-state chrome (segmented tabs, the date selector's
                    // active day) — the one moment on this screen where brand identity should
                    // show through, rather than a hand-rolled white/charcoal pairing.
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color.White.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}
