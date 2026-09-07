package com.eyecare.app.presentation.auth.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.auth.MIN_PASSWORD_LENGTH
import com.eyecare.app.presentation.auth.passwordValidation

private enum class PasswordGuidanceState {
    NOT_STARTED,
    MET,
    NOT_MET,
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Password",
    error: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    val hasError = !error.isNullOrBlank()

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
            isError = hasError,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(
                    onClick = { visible = !visible },
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (visible) "Hide password" else "Show password",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(error)
    }
}

@Composable
fun PasswordRequirements(
    password: String,
    modifier: Modifier = Modifier,
) {
    val validation = passwordValidation(password)
    val requirements = listOf(
        "At least $MIN_PASSWORD_LENGTH characters" to validation.hasMinimumLength,
        "One uppercase letter (A-Z)" to validation.hasUppercase,
        "One lowercase letter (a-z)" to validation.hasLowercase,
        "One number (0-9)" to validation.hasNumber,
        "One special character (e.g. !@#$%^&*)" to validation.hasSpecialCharacter,
    )

    Column(
        modifier = modifier.padding(start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Password requirements",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        requirements.forEach { (requirement, isMet) ->
            PasswordGuidanceRow(
                text = requirement,
                state = when {
                    password.isEmpty() -> PasswordGuidanceState.NOT_STARTED
                    isMet -> PasswordGuidanceState.MET
                    else -> PasswordGuidanceState.NOT_MET
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun PasswordMatchGuidance(
    password: String,
    confirmation: String,
    modifier: Modifier = Modifier,
) {
    if (confirmation.isEmpty()) return

    val matches = password == confirmation
    PasswordGuidanceRow(
        text = if (matches) "Passwords match" else "Passwords do not match",
        state = if (matches) PasswordGuidanceState.MET else PasswordGuidanceState.NOT_MET,
        semanticDescription = if (matches) "Passwords match" else "Passwords do not match",
        modifier = modifier.padding(start = 4.dp),
    )
}

@Composable
private fun PasswordGuidanceRow(
    text: String,
    state: PasswordGuidanceState,
    semanticDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    val (icon, tint, defaultDescription) = when (state) {
        PasswordGuidanceState.NOT_STARTED -> Triple(
            Icons.Outlined.RadioButtonUnchecked,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Password requirement: $text",
        )
        PasswordGuidanceState.MET -> Triple(
            Icons.Outlined.CheckCircle,
            MaterialTheme.colorScheme.tertiary,
            "Password requirement met: $text",
        )
        PasswordGuidanceState.NOT_MET -> Triple(
            Icons.Outlined.ErrorOutline,
            MaterialTheme.colorScheme.error,
            "Password requirement not met: $text",
        )
    }
    val description = semanticDescription ?: defaultDescription

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = tint,
        )
    }
}
