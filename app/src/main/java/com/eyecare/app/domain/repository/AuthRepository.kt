package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(
        name: String,
        email: String,
        phone: String?,
        password: String,
        passwordConfirmation: String,
    ): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun getMe(): Result<User>
    suspend fun updateMe(request: UpdateProfileRequest): Result<User>
}

data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val fullName: String? = null,
    val dateOfBirth: String? = null,
    val occupation: String? = null,
    val gender: String? = null,
    val contactEmail: String? = null,
)
