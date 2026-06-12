package com.eyecare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.remote.interceptor.AuthEvent
import com.eyecare.app.data.remote.interceptor.AuthEventBus
import com.eyecare.app.presentation.navigation.AuthGraph
import com.eyecare.app.presentation.navigation.EyecareNavGraph
import com.eyecare.app.presentation.navigation.MainGraph
import com.eyecare.app.ui.theme.EyecareTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var authEventBus: AuthEventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EyecareTheme {
                val navController = rememberNavController()
                var logoutTrigger by remember { mutableStateOf(0) }

                // Listen for 401 → clear token → navigate to login
                LaunchedEffect(Unit) {
                    authEventBus.events.collect { event ->
                        if (event is AuthEvent.Logout) {
                            tokenManager.clearToken()
                            navController.navigate(AuthGraph) {
                                popUpTo(MainGraph) { inclusive = true }
                            }
                            logoutTrigger++
                        }
                    }
                }

                EyecareNavGraph(
                    tokenManager = tokenManager,
                    onLogout = { logoutTrigger++ },
                    navController = navController,
                )
            }
        }
    }
}
