package com.eyecare.app.presentation.auth.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.eyecare.app.domain.model.toPhilippineE164
import com.eyecare.app.domain.model.toPhilippineLocalDigits

enum class ContactMethod { EMAIL, PHONE }

@Composable
fun ContactField(
    value: String,
    onValueChange: (String) -> Unit,
    method: ContactMethod,
    error: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val isPhone = method == ContactMethod.PHONE

    Column(modifier = modifier) {
        OutlinedTextField(
            value = if (isPhone) toPhilippineLocalDigits(value) else value,
            onValueChange = { enteredValue ->
                onValueChange(if (isPhone) toPhilippineE164(enteredValue) else enteredValue)
            },
            label = { Text(if (method == ContactMethod.EMAIL) "Email address" else "Phone number") },
            placeholder = { Text(if (method == ContactMethod.EMAIL) "you@example.com" else "9XXXXXXXXX") },
            prefix = if (isPhone) {
                { Text("+63") }
            } else null,
            singleLine = true,
            enabled = enabled,
            isError = error != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (method == ContactMethod.EMAIL) KeyboardType.Email else KeyboardType.Phone,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(error)
    }
}
