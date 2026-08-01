package com.eyecare.app.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

enum class ContactMethod { EMAIL, PHONE }

@Composable
fun ContactMethodSelector(
    selected: ContactMethod,
    onSelect: (ContactMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "How would you like to register?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = selected == ContactMethod.EMAIL,
                onClick = { onSelect(ContactMethod.EMAIL) },
                label = { Text("Email") },
            )
            FilterChip(
                selected = selected == ContactMethod.PHONE,
                onClick = { onSelect(ContactMethod.PHONE) },
                label = { Text("Phone") },
            )
        }
    }
}

@Composable
fun ContactField(
    value: String,
    onValueChange: (String) -> Unit,
    method: ContactMethod,
    error: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(if (method == ContactMethod.EMAIL) "Email address" else "Phone number") },
            placeholder = { Text(if (method == ContactMethod.EMAIL) "you@example.com" else "09171234567") },
            singleLine = true,
            enabled = enabled,
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(error)
    }
}
