package com.eyecare.app.presentation.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
        if (
            state != null &&
            hasProfileChanges(
                user = state.user,
                name = state.editName,
                email = state.editEmail,
                phone = state.editPhone,
                fullName = state.editFullName,
                dateOfBirth = state.editDateOfBirth,
                occupation = state.editOccupation,
                address = state.editAddress,
                gender = state.editGender,
                contactEmail = state.editContactEmail,
            )
        ) {
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
            onNameChange = viewModel::updateName,
            onEmailChange = viewModel::updateEmail,
            onPhoneChange = viewModel::updatePhone,
            onFullNameChange = viewModel::updateFullName,
            onDateOfBirthChange = viewModel::updateDateOfBirth,
            onOccupationChange = viewModel::updateOccupation,
            onAddressChange = viewModel::updateAddress,
            onGenderChange = viewModel::updateGender,
            onContactEmailChange = viewModel::updateContactEmail,
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
    onNameChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onPhoneChange: (String) -> Unit = {},
    onFullNameChange: (String) -> Unit = {},
    onDateOfBirthChange: (String) -> Unit = {},
    onOccupationChange: (String) -> Unit = {},
    onAddressChange: (String) -> Unit = {},
    onGenderChange: (String) -> Unit = {},
    onContactEmailChange: (String) -> Unit = {},
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
                    text = "Personal details",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Keep your contact information up to date for clinic communication.",
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = state.editName,
                        onValueChange = onNameChange,
                        label = { Text("Name") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.fieldErrors.containsKey("name"),
                        supportingText = fieldSupportingText(state, "name"),
                        singleLine = true,
                        enabled = !state.isSaving,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.editEmail,
                        onValueChange = onEmailChange,
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.fieldErrors.containsKey("email"),
                        supportingText = fieldSupportingText(state, "email"),
                        singleLine = true,
                        enabled = !state.isSaving,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.editPhone,
                        onValueChange = onPhoneChange,
                        label = { Text("Phone") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Phone, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.fieldErrors.containsKey("phone"),
                        supportingText = fieldSupportingText(state, "phone"),
                        singleLine = true,
                        enabled = !state.isSaving,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.editFullName,
                        onValueChange = onFullNameChange,
                        label = { Text("Full name") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.fieldErrors.containsKey("full_name"),
                        supportingText = fieldSupportingText(state, "full_name"),
                        singleLine = true,
                        enabled = !state.isSaving,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.editDateOfBirth,
                        onValueChange = onDateOfBirthChange,
                        label = { Text("Date of birth") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Cake, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.fieldErrors.containsKey("date_of_birth"),
                        supportingText = fieldSupportingText(state, "date_of_birth"),
                        singleLine = true,
                        enabled = !state.isSaving,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.editOccupation,
                        onValueChange = onOccupationChange,
                        label = { Text("Occupation") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Work, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.fieldErrors.containsKey("occupation"),
                        supportingText = fieldSupportingText(state, "occupation"),
                        singleLine = true,
                        enabled = !state.isSaving,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.editAddress,
                        onValueChange = onAddressChange,
                        label = { Text("Address") },
                        leadingIcon = {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.fieldErrors.containsKey("address"),
                        supportingText = fieldSupportingText(state, "address"),
                        singleLine = true,
                        enabled = !state.isSaving,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.editGender,
                        onValueChange = onGenderChange,
                        label = { Text("Gender") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Wc, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.fieldErrors.containsKey("gender"),
                        supportingText = fieldSupportingText(state, "gender"),
                        singleLine = true,
                        enabled = !state.isSaving,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.editContactEmail,
                        onValueChange = onContactEmailChange,
                        label = { Text("Contact email") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.fieldErrors.containsKey("contact_email"),
                        supportingText = fieldSupportingText(state, "contact_email"),
                        singleLine = true,
                        enabled = !state.isSaving,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                        ),
                    )
                }
            }

            state.saveError?.let { message ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp),
                    )
                }
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
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(50),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .semantics { contentDescription = "Saving profile" },
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Save changes", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldSupportingText(
    state: ProfileUiState.Success,
    field: String,
): (@Composable () -> Unit)? = state.fieldErrors[field]?.firstOrNull()?.let { message ->
    { Text(message) }
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
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Loading profile editor" }
                .padding(horizontal = 24.dp),
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
        ErrorContent(
            message = message,
            onRetry = onRetry,
            modifier = Modifier.weight(1f),
        )
    }
}
