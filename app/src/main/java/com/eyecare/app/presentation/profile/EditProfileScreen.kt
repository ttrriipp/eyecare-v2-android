package com.eyecare.app.presentation.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.ErrorContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is ProfileUiState.Success && !state.isEditing && !state.saveSuccess) {
            viewModel.startEditing()
        }
    }

    val successState = uiState as? ProfileUiState.Success
    LaunchedEffect(successState?.saveSuccess) {
        if (successState?.saveSuccess == true && !successState.isEditing) onBack()
    }

    val requestExit = {
        val state = uiState as? ProfileUiState.Success
        if (state != null && hasProfileChanges(state)) {
            showDiscardDialog = true
        } else {
            viewModel.cancelEditing()
            onBack()
        }
    }

    BackHandler {
        if (successState?.isSaving != true) requestExit()
    }

    when (val state = uiState) {
        ProfileUiState.Loading -> EditProfileLoadingContent(onBackRequest = requestExit)
        is ProfileUiState.Error -> EditProfileErrorContent(
            message = state.message,
            onRetry = viewModel::retry,
            onBackRequest = requestExit,
        )
        is ProfileUiState.Success -> EditProfileContent(
            state = state,
            onBackRequest = requestExit,
            onCancel = requestExit,
            onFirstNameChange = viewModel::updateFirstName,
            onLastNameChange = viewModel::updateLastName,
            onSave = viewModel::saveProfile,
        )
    }

    if (showDiscardDialog) {
        AppConfirmationDialog(
            icon = Icons.Outlined.WarningAmber,
            title = "Discard changes?",
            message = "Your profile changes haven't been saved.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            onConfirm = {
                showDiscardDialog = false
                viewModel.cancelEditing()
                onBack()
            },
            onDismissRequest = { showDiscardDialog = false },
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileContent(
    state: ProfileUiState.Success,
    modifier: Modifier = Modifier,
    onBackRequest: () -> Unit = {},
    onCancel: () -> Unit = {},
    onFirstNameChange: (String) -> Unit = {},
    onLastNameChange: (String) -> Unit = {},
    onSave: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        EditProfileTopBar(onBackRequest = onBackRequest, enabled = !state.isSaving)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Edit profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Only first and last name can be edited.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = state.editFirstName,
                onValueChange = onFirstNameChange,
                label = { Text("First name") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )

            OutlinedTextField(
                value = state.editLastName,
                onValueChange = onLastNameChange,
                label = { Text("Last name") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
            )

            state.saveError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(50),
                ) { Text("Cancel") }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(50),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp).semantics { contentDescription = "Saving" },
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileTopBar(onBackRequest: () -> Unit, enabled: Boolean) {
    TopAppBar(
        windowInsets = WindowInsets(0),
        title = { Text("Edit Profile") },
        navigationIcon = {
            IconButton(onClick = onBackRequest, enabled = enabled) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileLoadingContent(onBackRequest: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        EditProfileTopBar(onBackRequest = onBackRequest, enabled = true)
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            ProfileLoadingContent(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileErrorContent(
    message: String,
    onRetry: () -> Unit,
    onBackRequest: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        EditProfileTopBar(onBackRequest = onBackRequest, enabled = true)
        ErrorContent(message = message, onRetry = onRetry, modifier = Modifier.weight(1f))
    }
}

private fun hasProfileChanges(state: ProfileUiState.Success): Boolean {
    val a = state.account
    return state.editFirstName != (a.firstName ?: "") || state.editLastName != (a.lastName ?: "")
}
