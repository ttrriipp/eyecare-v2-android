package com.eyecare.app.data.repository

import com.eyecare.app.domain.model.ApiDomainError
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

class RepositoryApiCallTest {

    @Test
    fun `success returns value`() = runTest {
        val result = safeApiCall { "ok" }
        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun `non-HTTP exception passes through`() = runTest {
        val result = safeApiCall { throw IllegalStateException("local") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `HTTP 422 with V13 error envelope maps to ApiDomainError`() = runTest {
        val body = """{"error":{"code":"SLOT_UNAVAILABLE","message":"Slot taken."}}""".toResponseBody("application/json".toMediaType())
        val httpException = HttpException(Response.error<Any>(422, body))
        val result = safeApiCall { throw httpException }
        val error = result.exceptionOrNull() as ApiDomainError
        assertEquals(422, error.httpStatus)
        assertEquals("SLOT_UNAVAILABLE", error.code)
        assertEquals("Slot taken.", error.message)
    }

    @Test
    fun `HTTP 422 with legacy validation envelope maps as fallback`() = runTest {
        val body = """{"message":"Invalid.","errors":{"email":["required"]}}""".toResponseBody("application/json".toMediaType())
        val httpException = HttpException(Response.error<Any>(422, body))
        val result = safeApiCall { throw httpException }
        val error = result.exceptionOrNull() as ApiDomainError
        assertEquals(422, error.httpStatus)
        assertEquals(listOf("required"), error.fieldErrors["email"])
    }

    @Test
    fun `empty body maps to unknown error`() = runTest {
        val body = "".toResponseBody("application/json".toMediaType())
        val httpException = HttpException(Response.error<Any>(500, body))
        val result = safeApiCall { throw httpException }
        val error = result.exceptionOrNull() as ApiDomainError
        assertEquals(500, error.httpStatus)
        assertEquals("UNKNOWN_ERROR", error.code)
    }
}
