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
    suspend fun getUser(): Result<User>
    suspend fun updateUser(name: String, email: String, phone: String?): Result<User>
}
