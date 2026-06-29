package com.eyecare.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.remote.interceptor.AuthEvent
import com.eyecare.app.data.remote.interceptor.AuthEventBus
import com.eyecare.app.domain.repository.ChatRepository
import com.eyecare.app.presentation.navigation.AuthGraph
import com.eyecare.app.presentation.navigation.EyecareNavGraph
import com.eyecare.app.presentation.navigation.MainGraph
import com.eyecare.app.ui.theme.EyecareTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var authEventBus: AuthEventBus
    @Inject lateinit var chatRepository: ChatRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent {
            EyecareTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                var logoutTrigger by remember { mutableStateOf(0) }

                // Listen for 401 → show snackbar → clear token → navigate to login
                LaunchedEffect(Unit) {
                    authEventBus.events.collect { event ->
                        if (event is AuthEvent.Logout) {
                            snackbarHostState.showSnackbar("Session expired. Please sign in again.")
                            delay(500)
                            tokenManager.clearToken()
                            navController.navigate(AuthGraph) {
                                popUpTo(MainGraph) { inclusive = true }
                            }
                            logoutTrigger++
                        }
                    }
                }

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(snackbarData = data)
                        }
                    },
                ) { _ ->
                    EyecareNavGraph(
                        tokenManager = tokenManager,
                        chatRepository = chatRepository,
                        onLogout = { logoutTrigger++ },
                        navController = navController,
                    )
                }
            }
        }
    }
}
