package com.eyecare.app.presentation.accessories.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
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
import com.eyecare.app.ui.theme.EyecareColors

@Composable
fun AccessoryCatalogControls(
    currentSort: String?,
    onSortChange: (String?) -> Unit,
    minimumRating: Int?,
    onMinimumRatingChange: (Int?) -> Unit,
    rated: String?,
    onRatedChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortOptions = listOf(
        null to "Default",
        "name" to "Name",
        "newest" to "Newest",
        "rating" to "Rating",
        "most_rated" to "Most rated",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccessoryFilterDropdown(
            label = "Sort",
            selected = currentSort != null,
            selectedLabel = sortOptions.firstOrNull { it.first == currentSort }?.second ?: "Default",
            options = sortOptions,
            onSelect = onSortChange,
        )
        AccessoryFilterDropdown(
            label = "Rating",
            selected = minimumRating != null,
            selectedLabel = minimumRating?.let { "$it+ stars" } ?: "Any rating",
            options = listOf(null to "Any rating") + (1..5).map { it to "$it+ stars" },
            onSelect = onMinimumRatingChange,
        )
        AccessoryFilterDropdown(
            label = "Rated",
            selected = rated != null,
            selectedLabel = when (rated) {
                "rated" -> "Rated"
                "unrated" -> "Unrated"
                else -> "All ratings"
            },
            options = listOf(null to "All ratings", "rated" to "Rated", "unrated" to "Unrated"),
            onSelect = onRatedChange,
        )
        if (currentSort != null || minimumRating != null || rated != null) {
            TextButton(
                onClick = onClearFilters,
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun <T> AccessoryFilterDropdown(
    label: String,
    selected: Boolean,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text("$label: $selectedLabel", maxLines = 1) },
            trailingIcon = {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Choose $label",
                )
            },
            colors = accessoryFilterChipColors(),
            modifier = Modifier.heightIn(min = 48.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun accessoryFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    selectedLabelColor = EyecareColors.current.accentText,
    selectedTrailingIconColor = EyecareColors.current.accentText,
)
