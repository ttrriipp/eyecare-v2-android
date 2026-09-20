package com.eyecare.app.presentation.accessories.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessoryCatalogControls(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    currentSort: String?,
    onSortChange: (String?) -> Unit,
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search accessories...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )

        var sortExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = sortExpanded,
            onExpandedChange = { sortExpanded = it },
        ) {
            TextButton(
                onClick = { sortExpanded = true },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            ) {
                Text(sortOptions.firstOrNull { it.first == currentSort }?.second ?: "Sort")
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded)
            }
            ExposedDropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = { sortExpanded = false },
            ) {
                sortOptions.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSortChange(value)
                            sortExpanded = false
                        },
                    )
                }
            }
        }
    }
}