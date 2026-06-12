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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.ProductVariant

@Composable
fun VariantChipRow(
    variants: List<ProductVariant>,
    selectedVariant: ProductVariant?,
    onSelectVariant: (ProductVariant) -> Unit,
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
                    selectedContainerColor = Color.White,
                    selectedLabelColor = Color(0xFF2D3748),
                    containerColor = Color.White.copy(alpha = 0.3f),
                    labelColor = Color.White,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color.White.copy(alpha = 0.5f),
                    selectedBorderColor = Color.White,
                ),
            )
        }
    }
}
