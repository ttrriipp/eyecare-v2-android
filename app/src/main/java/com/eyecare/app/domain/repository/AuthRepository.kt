package com.eyecare.app.domain.repository

import com.eyecare.app.data.remote.dto.AuthDtos
import com.eyecare.app.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(request: AuthDtos.RegisterRequest): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun getUser(): Result<User>
}
