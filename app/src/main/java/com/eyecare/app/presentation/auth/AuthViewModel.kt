package com.eyecare.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.AuthError
import com.eyecare.app.domain.model.User
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val user: User) : AuthUiState
    data class ValidationError(val fieldErrors: Map<String, List<String>>) : AuthUiState
    data object RateLimit : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        val localError = validateLogin(email, password)
        if (localError != null) { _uiState.value = localError; return }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = authRepository.login(email, password).fold(
                onSuccess = { AuthUiState.Success(it) },
                onFailure = { it.toUiState() },
            )
        }
    }

    fun register(name: String, email: String, phone: String?, password: String, confirm: String) {
        val localError = validateRegister(name, email, password, confirm)
        if (localError != null) { _uiState.value = localError; return }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = authRepository.register(
                name, email, phone?.takeIf { it.isNotBlank() }, password, confirm
            ).fold(
                onSuccess = { AuthUiState.Success(it) },
                onFailure = { it.toUiState() },
            )
        }
    }

    fun resetState() { _uiState.value = AuthUiState.Idle }

    private fun validateLogin(email: String, password: String): AuthUiState.ValidationError? {
        val errors = mutableMapOf<String, List<String>>()
        if (email.isBlank()) errors["email"] = listOf("Email is required")
        else if (!EMAIL_REGEX.matches(email)) errors["email"] = listOf("Enter a valid email")
        if (password.length < 8) errors["password"] = listOf("Password must be at least 8 characters")
        return if (errors.isEmpty()) null else AuthUiState.ValidationError(errors)
    }

    private fun validateRegister(
        name: String, email: String, password: String, confirm: String,
    ): AuthUiState.ValidationError? {
        val errors = mutableMapOf<String, List<String>>()
        if (name.isBlank()) errors["name"] = listOf("Name is required")
        if (email.isBlank()) errors["email"] = listOf("Email is required")
        else if (!EMAIL_REGEX.matches(email)) errors["email"] = listOf("Enter a valid email")
        if (password.length < 8) errors["password"] = listOf("Password must be at least 8 characters")
        if (password != confirm) errors["password_confirmation"] = listOf("Passwords do not match")
        return if (errors.isEmpty()) null else AuthUiState.ValidationError(errors)
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }

    private fun Throwable.toUiState(): AuthUiState = when (this) {
        is AuthError.ValidationError -> AuthUiState.ValidationError(fieldErrors)
        is AuthError.RateLimitError -> AuthUiState.RateLimit
        else -> AuthUiState.Error(message ?: "An unexpected error occurred")
    }
}
