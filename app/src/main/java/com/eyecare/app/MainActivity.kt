package com.eyecare.app

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.remote.interceptor.AuthEvent
import com.eyecare.app.data.remote.interceptor.AuthEventBus
import com.eyecare.app.domain.repository.ChatRepository
import com.eyecare.app.presentation.auth.SessionViewModel
import com.eyecare.app.presentation.navigation.EyecareNavGraph
import com.eyecare.app.presentation.navigation.Welcome
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
        // Status/nav bar icon style is independent of the Compose theme below and
        // normally follows system dark mode automatically; pinned to light (dark icons)
        // to match EyecareTheme being pinned to light mode for now.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent {
            EyecareTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val sessionViewModel: SessionViewModel = hiltViewModel()
                var logoutTrigger by remember { mutableStateOf(0) }

                // Listen for 401 → show snackbar → clear session → navigate to Welcome
                LaunchedEffect(Unit) {
                    authEventBus.events.collect { event ->
                        if (event is AuthEvent.Logout) {
                            snackbarHostState.showSnackbar("Session expired. Please sign in again.")
                            delay(500)
                            // Must clear SessionViewModel's cached state, not just the token —
                            // otherwise SessionGate reads the stale Linked/Limited state and
                            // bounces straight back into MainGraph instead of redirecting.
                            sessionViewModel.signOut()
                            navController.navigate(Welcome) {
                                popUpTo(0) { inclusive = true }
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
