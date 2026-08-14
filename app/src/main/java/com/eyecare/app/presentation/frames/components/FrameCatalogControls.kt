package com.eyecare.app.presentation.frames.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.frames.FrameListFilters
import com.eyecare.app.ui.theme.EyecareColors

@Composable
fun FrameCatalogControls(
    filters: FrameListFilters,
    brands: List<String>,
    categories: List<String>,
    onSelectBrand: (String?) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSetArOnly: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = filters.arOnly,
                onClick = { onSetArOnly(!filters.arOnly) },
                label = { Text("AR-ready") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.FaceRetouchingNatural,
                        contentDescription = null,
                    )
                },
                colors = catalogChipColors(),
            )
            FilterDropdownChip(
                label = filters.brand ?: "Brand",
                selected = filters.brand != null,
                options = brands,
                allLabel = "All brands",
                onSelect = onSelectBrand,
            )
            FilterDropdownChip(
                label = filters.category ?: "Category",
                selected = filters.category != null,
                options = categories,
                allLabel = "All categories",
                onSelect = onSelectCategory,
            )
            if (filters.hasLocalFilters) {
                TextButton(
                    onClick = onClearFilters,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
private fun FilterDropdownChip(
    label: String,
    selected: Boolean,
    options: List<String>,
    allLabel: String,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label, maxLines = 1) },
            trailingIcon = {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Choose $label",
                )
            },
            colors = catalogChipColors(),
            enabled = options.isNotEmpty(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp),
        ) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun catalogChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    selectedLabelColor = EyecareColors.current.accentText,
    selectedLeadingIconColor = EyecareColors.current.accentText,
    selectedTrailingIconColor = EyecareColors.current.accentText,
)
