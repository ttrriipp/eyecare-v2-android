package com.eyecare.app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private data class TabItem(val route: Any, val icon: ImageVector, val label: String)

private val tabs = listOf(
    TabItem(Home, Icons.Outlined.Home, "Home"),
    TabItem(Catalog, Icons.Outlined.RemoveRedEye, "Catalog"),
    TabItem(Appointments, Icons.Outlined.CalendarMonth, "Visits"),
    TabItem(Profile, Icons.Outlined.Person, "Profile"),
)

// Dark navy matching the reference image pill background
private val NavPillColor = Color(0xFF1A2340)

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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dark pill — 4 tabs
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(40.dp),
            shadowElevation = 12.dp,
            color = NavPillColor,
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute::class == tab.route::class
                    NavTabItem(
                        icon = tab.icon,
                        label = tab.label,
                        selected = selected,
                        onClick = { onTabSelected(tab.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Blue chat FAB
        Surface(
            onClick = onChatClick,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 12.dp,
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Chat, contentDescription = "Chat", tint = Color.White)
            }
        }
    }
}

@Composable
private fun NavTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(role = Role.Tab) { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
            )
        }
    }
}
