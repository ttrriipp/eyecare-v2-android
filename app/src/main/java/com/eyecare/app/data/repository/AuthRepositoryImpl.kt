package com.eyecare.app.data.repository

import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.local.dao.UserDao
import com.eyecare.app.data.local.entity.UserEntity
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
    private val userDao: UserDao,
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

    override suspend fun getUser(): Result<User> {
        return try {
            val user = api.getUser().data.toDomain()
            userDao.insert(user.toEntity())
            Result.success(user)
        } catch (e: Exception) {
            val cached = userDao.get()
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateUser(name: String, email: String, phone: String?): Result<User> =
        runCatching {
            api.updateUser(AuthDtos.UpdateUserRequest(name, email, phone)).data.toDomain()
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
            val user = response.data.user.toDomain()
            userDao.insert(user.toEntity())
            user
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

    // ── DTO → Domain ──

    private fun AuthDtos.UserDto.toDomain() = User(id, name, email, phone, role)

    // ── Domain → Entity ──

    private fun User.toEntity() = UserEntity(
        id = id,
        name = name,
        email = email,
        phone = phone,
        role = role,
    )

    // ── Entity → Domain ──

    private fun UserEntity.toDomain() = User(
        id = id,
        name = name,
        email = email,
        phone = phone,
        role = role,
    )
}
