package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.AuthDtos
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApiService {
    @POST("login")
    suspend fun login(@Body request: AuthDtos.LoginRequest): AuthDtos.AuthResponse

    @POST("register")
    suspend fun register(@Body request: AuthDtos.RegisterRequest): AuthDtos.AuthResponse

    @POST("logout")
    suspend fun logout()

    @GET("user")
    suspend fun getUser(): AuthDtos.UserResponse

    @PATCH("user")
    suspend fun updateUser(@Body request: AuthDtos.UpdateUserRequest): AuthDtos.UserResponse
}
