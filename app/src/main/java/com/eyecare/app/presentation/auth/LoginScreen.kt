package com.eyecare.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.ui.theme.EyecareTheme

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    LoginContent(
        uiState = uiState,
        onLogin = viewModel::login,
        onNavigateToRegister = onNavigateToRegister,
    )
}

@Composable
private fun LoginContent(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading = uiState is AuthUiState.Loading
    val fieldErrors = (uiState as? AuthUiState.ValidationError)?.fieldErrors ?: emptyMap()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome back", style = MaterialTheme.typography.displayLarge)
        Text("Sign in to continue", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            isError = fieldErrors.containsKey("email"),
            supportingText = fieldErrors["email"]?.firstOrNull()?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            isError = fieldErrors.containsKey("password"),
            supportingText = fieldErrors["password"]?.firstOrNull()?.let { { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState is AuthUiState.RateLimit) {
            Spacer(Modifier.height(8.dp))
            Text("Too many attempts. Please wait and try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (uiState is AuthUiState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(uiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onLogin(email, password) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
            else Text("Login")
        }
        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Create one")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    EyecareTheme {
        LoginContent(uiState = AuthUiState.Idle, onLogin = { _, _ -> }, onNavigateToRegister = {})
    }
}
