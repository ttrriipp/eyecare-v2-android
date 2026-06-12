package com.eyecare.app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class TabItem(val route: Any, val icon: ImageVector, val label: String)

private val tabs = listOf(
    TabItem(Home, Icons.Outlined.Home, "Home"),
    TabItem(Catalog, Icons.Outlined.RemoveRedEye, "Catalog"),
    TabItem(Appointments, Icons.Outlined.CalendarMonth, "Visits"),
    TabItem(Profile, Icons.Outlined.Person, "Profile"),
)

@Composable
fun SplitBottomNavBar(
    currentRoute: Any,
    onTabSelected: (Any) -> Unit,
    onChatClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // White pill — 4 tabs
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 8.dp,
            color = Color.White,
        ) {
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                tabs.forEach { tab ->
                    val selected = currentRoute::class == tab.route::class
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onTabSelected(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(22.dp)) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        }

        // Blue chat FAB (rounded square)
        Surface(
            onClick = onChatClick,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp,
            modifier = Modifier.size(56.dp),
        ) {
            IconButton(onClick = onChatClick) {
                Icon(Icons.Outlined.Chat, contentDescription = "Chat", tint = Color.White)
            }
        }
    }
}
