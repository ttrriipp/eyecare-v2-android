package com.eyecare.app.di

import com.eyecare.app.BuildConfig
import com.eyecare.app.data.remote.interceptor.AuthInterceptor
import io.mockk.mockk
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NetworkModuleTest {

    @Test
    fun `debug API base URL targets version one`() {
        assertTrue(BuildConfig.API_BASE_URL.endsWith("/api/v1/"))
    }

    @Test
    fun `debug client logs request metadata without bodies`() {
        val client = NetworkModule.provideOkHttpClient(mockk<AuthInterceptor>())
        val loggingInterceptor = client.interceptors
            .filterIsInstance<HttpLoggingInterceptor>()
            .single()

        assertEquals(HttpLoggingInterceptor.Level.BASIC, loggingInterceptor.level)
    }
}
