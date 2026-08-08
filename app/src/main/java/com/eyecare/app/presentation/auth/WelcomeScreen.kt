package com.eyecare.app.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.auth.components.AuthOutlinedButton
import com.eyecare.app.presentation.auth.components.AuthPrimaryButton

@Composable
fun WelcomeScreen(
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // ── Logo ──────────────────────────────────────────────────────────
            Image(
                painter = painterResource(id = com.eyecare.app.R.drawable.welcome_logo),
                contentDescription = "Eyecare logo",
                modifier = Modifier.size(475.dp),
                contentScale = ContentScale.Fit,
            )

            Spacer(modifier = Modifier.weight(1.6f))

            // ── Actions — clean, spacious, confident ─────────────────────────
            // Maya pattern: generous spacing between CTAs, no dividers or
            // decorative elements. The buttons speak for themselves.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AuthPrimaryButton(
                    text = "Sign in",
                    onClick = onSignIn,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AuthOutlinedButton(
                    text = "Create account",
                    onClick = onCreateAccount,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "A simpler way to stay connected to your care.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
