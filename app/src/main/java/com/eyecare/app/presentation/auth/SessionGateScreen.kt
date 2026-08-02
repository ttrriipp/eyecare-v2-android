package com.eyecare.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
            Text("Checking your account…", style = MaterialTheme.typography.bodyLarge)
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "We couldn't verify your session",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Retry") }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onSignOut) { Text("Sign out") }
        }
    }
}
