package com.eyecare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eyecare.app.presentation.auth.LoginScreen
import com.eyecare.app.presentation.auth.RegisterScreen
import com.eyecare.app.ui.theme.EyecareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EyecareTheme {
                // Temporary routing until Task 7 wires the full NavGraph
                var showRegister by remember { mutableStateOf(false) }
                if (showRegister) {
                    RegisterScreen(
                        onNavigateToLogin = { showRegister = false },
                        onRegisterSuccess = { showRegister = false },
                    )
                } else {
                    LoginScreen(
                        onNavigateToRegister = { showRegister = true },
                        onLoginSuccess = { /* TODO Task 7: navigate to main graph */ },
                    )
                }
            }
        }
    }
}
