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
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.presentation.common.RefreshOnResumeEffect
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.ui.theme.EyecareColors

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToPrescriptions: () -> Unit = {},
    onNavigateToSavedFrames: () -> Unit = {},
    onNavigateToEyewear: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToPatientProfile: () -> Unit = {},
    onNavigateToAccountSecurity: () -> Unit = {},
    onNavigateToInviteCode: () -> Unit = {},
    unreadMessageCount: Int = 0,
    account: PatientAccount? = null,
    onLinkedAccountResolved: (PatientAccount) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    RefreshOnResumeEffect(onRefresh = viewModel::retry)

    LaunchedEffect(account) {
        account?.let(viewModel::adoptAccount)
    }

    val resolvedAccount = (uiState as? ProfileUiState.Success)?.account
    LaunchedEffect(resolvedAccount) {
        if (resolvedAccount?.linkStatus == PatientLinkStatus.LINKED) {
            onLinkedAccountResolved(resolvedAccount)
        }
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
            account = state.account,
            unreadMessageCount = unreadMessageCount,
            onNavigateToMessages = onNavigateToMessages,
            onNavigateToPrescriptions = onNavigateToPrescriptions,
            onNavigateToSavedFrames = onNavigateToSavedFrames,
            onNavigateToEyewear = onNavigateToEyewear,
            onNavigateToPatientProfile = onNavigateToPatientProfile,
            onNavigateToAccountSecurity = onNavigateToAccountSecurity,
            onNavigateToInviteCode = onNavigateToInviteCode,
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
    account: PatientAccount,
    unreadMessageCount: Int,
    modifier: Modifier = Modifier,
    onNavigateToMessages: () -> Unit = {},
    onNavigateToPrescriptions: () -> Unit = {},
    onNavigateToSavedFrames: () -> Unit = {},
    onNavigateToEyewear: () -> Unit = {},
    onNavigateToPatientProfile: () -> Unit = {},
    onNavigateToAccountSecurity: () -> Unit = {},
    onNavigateToInviteCode: () -> Unit = {},
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
        ProfileHeader(account = account)

        AccountSection(
            account = account,
            onNavigateToPatientProfile = onNavigateToPatientProfile,
            onNavigateToAccountSecurity = onNavigateToAccountSecurity,
            onNavigateToInviteCode = onNavigateToInviteCode,
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Care & activity",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )

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
                        onClick = onNavigateToMessages,
                        badgeCount = unreadMessageCount,
                    )
                    ProfileDivider()
                    ProfileNavRow(
                        icon = Icons.Outlined.LocalHospital,
                        label = "Prescriptions",
                        onClick = onNavigateToPrescriptions,
                    )
                    ProfileDivider()
                    ProfileNavRow(
                        icon = Icons.Outlined.Bookmark,
                        label = "Saved Frames",
                        onClick = onNavigateToSavedFrames,
                    )
                    ProfileDivider()
                    ProfileNavRow(
                        icon = Icons.Outlined.Receipt,
                        label = "My Orders",
                        onClick = onNavigateToEyewear,
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
private fun AccountSection(
    account: PatientAccount,
    onNavigateToPatientProfile: () -> Unit,
    onNavigateToAccountSecurity: () -> Unit,
    onNavigateToInviteCode: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                ProfileNavRow(
                    icon = Icons.Outlined.Person,
                    label = "Account details",
                    onClick = onNavigateToAccountSecurity,
                )
                if (account.linkedPatient != null) {
                    ProfileDivider()
                    ProfileNavRow(
                        icon = Icons.Outlined.LocalHospital,
                        label = "Patient profile",
                        onClick = onNavigateToPatientProfile,
                    )
                }
                if (account.linkStatus != PatientLinkStatus.LINKED) {
                    ProfileDivider()
                    ProfileNavRow(
                        icon = Icons.Outlined.Bookmark,
                        label = "Enter Invitation Code",
                        onClick = onNavigateToInviteCode,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(account: PatientAccount? = null) {
    if (account == null) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.displayLarge,
        )
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = profileInitials(account),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EyecareColors.current.accentText,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = account.name.ifBlank { "Account" },
                style = MaterialTheme.typography.displayLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun profileInitials(account: PatientAccount): String {
    val first = account.firstName?.trim()?.firstOrNull()
    val last = account.lastName?.trim()?.firstOrNull()
    if (first != null && last != null) return "$first$last".uppercase()

    val nameParts = account.name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        nameParts.size >= 2 -> "${nameParts.first().first()}${nameParts.last().first()}".uppercase()
        nameParts.size == 1 -> nameParts.first().take(1).uppercase()
        else -> "?"
    }
}
@Composable
private fun ProfileNavRow(
    icon: ImageVector,
    label: String,
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
                        tint = EyecareColors.current.accentText,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }

            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

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
        }
        LoadingBlock(modifier = Modifier.fillMaxWidth(), height = 196.dp, cornerRadius = 16.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoadingBlock(width = 132.dp, height = 20.dp)
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
