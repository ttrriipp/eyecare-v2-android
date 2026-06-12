package com.eyecare.app.data.remote.interceptor

import com.eyecare.app.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val authEventBus: AuthEventBus,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = tokenManager.getToken()?.let { token ->
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } ?: chain.request()

        val response = chain.proceed(request)

        if (response.code == 401) {
            authEventBus.send(AuthEvent.Logout)
        }

        return response
    }
}
