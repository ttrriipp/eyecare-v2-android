package com.eyecare.app.data.repository

import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.remote.api.AuthApiService
import com.eyecare.app.data.remote.dto.AuthDtos
import com.eyecare.app.domain.model.AuthError
import com.eyecare.app.domain.model.User
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.UpdateProfileRequest
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

    override suspend fun register(
        name: String,
        email: String,
        phone: String?,
        password: String,
        passwordConfirmation: String,
    ): Result<User> =
        safeCall { api.register(AuthDtos.RegisterRequest(name, email, phone, password, passwordConfirmation)) }

    override suspend fun logout(): Result<Unit> = runCatching {
        api.logout()
        tokenManager.clearToken()
    }

    override suspend fun getMe(): Result<User> = runCatching {
        api.getMe().data.toDomain()
    }

    override suspend fun updateMe(request: UpdateProfileRequest): Result<User> =
        runCatching {
            api.updateMe(
                AuthDtos.UpdateUserRequest(
                    name = request.name,
                    email = request.email,
                    phone = request.phone,
                    address = request.address,
                    fullName = request.fullName,
                    dateOfBirth = request.dateOfBirth,
                    occupation = request.occupation,
                    gender = request.gender,
                    contactEmail = request.contactEmail,
                ),
            ).data.toDomain()
        }.recoverCatching { throwable ->
            when {
                throwable is HttpException && throwable.code() == 422 -> {
                    val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
                    val parsed = json.decodeFromString<AuthDtos.ValidationErrorBody>(body)
                    throw AuthError.ValidationError(parsed.errors)
                }
                else -> throw throwable
            }
        }

    private suspend fun safeCall(block: suspend () -> AuthDtos.AuthResponse): Result<User> =
        runCatching {
            val response = block()
            tokenManager.saveToken(response.data.token)
            response.data.user.toDomain()
        }.recoverCatching { throwable ->
            when {
                throwable is HttpException && throwable.code() == 422 -> {
                    val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
                    val parsed = json.decodeFromString<AuthDtos.ValidationErrorBody>(body)
                    throw AuthError.ValidationError(parsed.errors)
                }
                throwable is HttpException && throwable.code() == 429 ->
                    throw AuthError.RateLimitError
                else -> throw throwable
            }
        }

    private fun AuthDtos.UserDto.toDomain() = User(
        id = id,
        name = name,
        email = email,
        phone = phone,
        role = role,
        patientNumber = patientNumber,
        fullName = fullName,
        dateOfBirth = dateOfBirth,
        occupation = occupation,
        address = address,
        gender = gender,
        contactEmail = contactEmail,
    )
}
