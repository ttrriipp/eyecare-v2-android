package com.eyecare.app.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.User
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.ErrorContent

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToOrders: () -> Unit = {},
    onNavigateToPrescriptions: () -> Unit = {},
    onNavigateToFeedbackHistory: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    unreadMessageCount: Int = 0,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.retry()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(loggedOut) {
        if (loggedOut) onLogout()
    }

    when (val state = uiState) {
        ProfileUiState.Loading -> ProfileLoadingContent()
        is ProfileUiState.Error -> ProfileErrorContent(
            message = state.message,
            onRetry = viewModel::retry,
        )
        is ProfileUiState.Success -> ProfileContent(
            user = state.user,
            unreadMessageCount = unreadMessageCount,
            onEditProfile = onNavigateToEditProfile,
            onNavigateToMessages = onNavigateToMessages,
            onNavigateToOrders = onNavigateToOrders,
            onNavigateToPrescriptions = onNavigateToPrescriptions,
            onNavigateToFeedbackHistory = onNavigateToFeedbackHistory,
            onLogoutClick = { showLogoutDialog = true },
        )
    }

    if (showLogoutDialog) {
        AppConfirmationDialog(
            icon = Icons.AutoMirrored.Outlined.Logout,
            title = "Log out?",
            message = "You'll need to sign in again to access your account.",
            confirmLabel = "Log out",
            dismissLabel = "Stay signed in",
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismissRequest = { showLogoutDialog = false },
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
        )
    }
}

@Composable
fun ProfileContent(
    user: User,
    unreadMessageCount: Int,
    modifier: Modifier = Modifier,
    onEditProfile: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
    onNavigateToPrescriptions: () -> Unit = {},
    onNavigateToFeedbackHistory: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        ProfileHeader()
        PatientIdentityCard(user = user, onEditProfile = onEditProfile)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Care & activity",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Your conversations and clinic records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    ProfileNavRow(
                        icon = Icons.AutoMirrored.Outlined.Chat,
                        label = "Messages",
                        supportingText = "Chat with the clinic",
                        onClick = onNavigateToMessages,
                        badgeCount = unreadMessageCount,
                    )
                    ProfileDivider()
                    ProfileNavRow(
                        icon = Icons.Outlined.History,
                        label = "Order History",
                        supportingText = "Track requests and purchases",
                        onClick = onNavigateToOrders,
                    )
                    ProfileDivider()
                    ProfileNavRow(
                        icon = Icons.Outlined.LocalHospital,
                        label = "Prescriptions",
                        supportingText = "View your optical records",
                        onClick = onNavigateToPrescriptions,
                    )
                    ProfileDivider()
                    ProfileNavRow(
                        icon = Icons.Outlined.Star,
                        label = "Feedback History",
                        supportingText = "Review your clinic feedback",
                        onClick = onNavigateToFeedbackHistory,
                    )
                }
            }
        }

        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Log out",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProfileHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = "Your care, in one place",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PatientIdentityCard(user: User, onEditProfile: () -> Unit) {
    val initials = profileInitials(user.name)

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(60.dp)
                        .clearAndSetSemantics {
                            contentDescription = "Profile initials $initials"
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "PATIENT ACCOUNT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = user.name.ifBlank { "Eyecare patient" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IdentityDetail(icon = Icons.Outlined.Email, text = user.email)
                user.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                    IdentityDetail(icon = Icons.Outlined.Phone, text = phone)
                }
            }

            OutlinedButton(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(50),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Edit profile", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun IdentityDetail(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileNavRow(
    icon: ImageVector,
    label: String,
    supportingText: String,
    onClick: () -> Unit,
    badgeCount: Int = 0,
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (badgeCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .widthIn(min = 24.dp)
                        .height(24.dp)
                        .semantics {
                            contentDescription = "$badgeCount unread messages"
                        },
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp, end = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
fun ProfileLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Loading profile" }
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            LoadingBlock(width = 92.dp, height = 26.dp)
            LoadingBlock(width = 156.dp, height = 14.dp)
        }
        LoadingBlock(modifier = Modifier.fillMaxWidth(), height = 196.dp, cornerRadius = 16.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoadingBlock(width = 132.dp, height = 20.dp)
            LoadingBlock(width = 220.dp, height = 14.dp)
        }
        LoadingBlock(modifier = Modifier.fillMaxWidth(), height = 252.dp, cornerRadius = 16.dp)
    }
}

@Composable
private fun ProfileErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        ProfileHeader()
        ErrorContent(
            message = message,
            onRetry = onRetry,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LoadingBlock(
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp? = null,
    height: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

internal fun profileInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "E"
        parts.size == 1 -> parts.first().take(1).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}
