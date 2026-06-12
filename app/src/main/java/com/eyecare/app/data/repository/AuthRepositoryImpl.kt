package com.eyecare.app.data.repository

import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.remote.api.AuthApiService
import com.eyecare.app.data.remote.dto.AuthDtos
import com.eyecare.app.domain.model.AuthError
import com.eyecare.app.domain.model.User
import com.eyecare.app.domain.repository.AuthRepository
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val tokenManager: TokenManager,
    private val json: Json,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> =
        safeCall { api.login(AuthDtos.LoginRequest(email, password)) }

    override suspend fun register(request: AuthDtos.RegisterRequest): Result<User> =
        safeCall { api.register(request) }

    override suspend fun logout(): Result<Unit> = runCatching {
        api.logout()
        tokenManager.clearToken()
    }

    override suspend fun getUser(): Result<User> = runCatching {
        api.getUser().data.toDomain()
    }

    private suspend fun safeCall(block: suspend () -> AuthDtos.AuthResponse): Result<User> =
        runCatching {
            val response = block()
            tokenManager.saveToken(response.data.token)
            response.data.user.toDomain()
        }.recoverCatching { throwable ->
            when {
                throwable is HttpException && throwable.code() == 422 -> {
                    val body = throwable.response()?.errorBody()?.string() ?: ""
                    val parsed = json.decodeFromString<AuthDtos.ValidationErrorBody>(body)
                    throw AuthError.ValidationError(parsed.errors)
                }
                throwable is HttpException && throwable.code() == 429 ->
                    throw AuthError.RateLimitError
                else -> throw throwable
            }
        }

    private fun AuthDtos.UserDto.toDomain() = User(id, name, email, role)
}
