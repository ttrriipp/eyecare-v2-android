package com.eyecare.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.domain.model.SessionState
import com.eyecare.app.presentation.auth.components.AuthPrimaryButton

@Composable
fun SessionGateScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is SessionState.Checking -> SessionGateCheckingContent()
        is SessionState.Unauthenticated -> {
            onNavigateToWelcome()
        }
        is SessionState.Linked -> {
            onNavigateToMain()
        }
        is SessionState.Limited -> {
            onNavigateToMain()
        }
        is SessionState.TransientFailure -> SessionGateErrorContent(
            message = s.message,
            onRetry = { viewModel.resolveSession() },
            onSignOut = { viewModel.signOut() },
        )
    }
}

@Composable
private fun SessionGateCheckingContent() {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Getting things ready…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SessionGateErrorContent(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "We couldn't verify your session",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            AuthPrimaryButton(text = "Retry", onClick = onRetry)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onSignOut) { Text("Sign out") }
        }
    }
}
