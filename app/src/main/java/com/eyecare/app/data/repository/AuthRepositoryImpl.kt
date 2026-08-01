package com.eyecare.app.data.repository

import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.remote.api.AuthApiService
import com.eyecare.app.data.remote.dto.LoginRequest
import com.eyecare.app.data.remote.dto.MeResponse
import com.eyecare.app.data.remote.dto.PatientAccountDto
import com.eyecare.app.data.remote.dto.UpdateMeRequest
import com.eyecare.app.domain.model.User
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.UpdateProfileRequest
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val tokenManager: TokenManager,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val response = api.login(LoginRequest(contactValue = email, password = password))
        val data = response.data
        if (data.token != null && data.user != null) {
            tokenManager.saveToken(data.token)
            data.user.toLegacyUser()
        } else {
            throw IllegalStateException("OTP required — use new auth flow")
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        phone: String?,
        password: String,
        passwordConfirmation: String,
    ): Result<User> = runCatching {
        throw IllegalStateException("Use new two-stage registration flow")
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        api.logout()
        tokenManager.clearToken()
    }

    override suspend fun getMe(): Result<User> = runCatching {
        api.getMe().data.toLegacyUser()
    }

    override suspend fun updateMe(request: UpdateProfileRequest): Result<User> = runCatching {
        api.updateMe(
            UpdateMeRequest(
                firstName = request.name,
                lastName = null,
            ),
        ).data.toLegacyUser()
    }

    private fun PatientAccountDto.toLegacyUser() = User(
        id = id,
        name = name,
        email = email ?: "",
        phone = phone,
        role = role,
        patientNumber = linkedPatient?.patientNumber,
        fullName = linkedPatient?.fullName,
        dateOfBirth = dateOfBirth,
        occupation = linkedPatient?.occupation,
        address = linkedPatient?.address,
        gender = linkedPatient?.gender,
        contactEmail = linkedPatient?.contactEmail,
    )
}
