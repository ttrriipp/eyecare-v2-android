package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.AuthSessionResponse
import com.eyecare.app.data.remote.dto.LoginRequest
import com.eyecare.app.data.remote.dto.LoginResponse
import com.eyecare.app.data.remote.dto.LoginVerifyRequest
import com.eyecare.app.data.remote.dto.MeResponse
import com.eyecare.app.data.remote.dto.PasswordRecoveryOtpRequest
import com.eyecare.app.data.remote.dto.PasswordRecoveryVerifyRequest
import com.eyecare.app.data.remote.dto.PoliciesResponse
import com.eyecare.app.data.remote.dto.RegisterRequest
import com.eyecare.app.data.remote.dto.RegistrationOtpRequest
import com.eyecare.app.data.remote.dto.RegistrationOtpResponse
import com.eyecare.app.data.remote.dto.RegistrationVerifyRequest
import com.eyecare.app.data.remote.dto.RegistrationVerifyResponse
import com.eyecare.app.data.remote.dto.UpdateMeRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/registration/otp")
    suspend fun requestRegistrationOtp(@Body request: RegistrationOtpRequest): RegistrationOtpResponse

    @POST("auth/registration/verify")
    suspend fun verifyRegistrationOtp(@Body request: RegistrationVerifyRequest): RegistrationVerifyResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthSessionResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/login/verify")
    suspend fun verifyLogin(@Body request: LoginVerifyRequest): AuthSessionResponse

    @POST("auth/password-recovery/otp")
    suspend fun requestPasswordRecoveryOtp(@Body request: PasswordRecoveryOtpRequest): RegistrationOtpResponse

    @POST("auth/password-recovery/verify")
    suspend fun verifyPasswordRecovery(@Body request: PasswordRecoveryVerifyRequest): AuthSessionResponse

    @GET("auth/policies")
    suspend fun getPolicies(): PoliciesResponse

    @POST("logout")
    suspend fun logout()

    @POST("logout-all")
    suspend fun logoutAll()

    @GET("me")
    suspend fun getMe(): MeResponse

    @PATCH("me")
    suspend fun updateMe(@Body request: UpdateMeRequest): MeResponse
}
