package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object AuthDtos {

    @Serializable
    data class LoginRequest(
        val email: String,
        val password: String,
    )

    @Serializable
    data class RegisterRequest(
        val name: String,
        val email: String,
        val phone: String?,
        val password: String,
        @SerialName("password_confirmation") val passwordConfirmation: String,
    )

    @Serializable
    data class AuthResponse(
        val token: String,
        val user: UserDto,
    )

    @Serializable
    data class UserDto(
        val id: Int,
        val name: String,
        val email: String,
        val role: String,
    )

    @Serializable
    data class UserResponse(val data: UserDto)

    @Serializable
    data class ValidationErrorBody(
        val message: String,
        val errors: Map<String, List<String>> = emptyMap(),
    )
}
