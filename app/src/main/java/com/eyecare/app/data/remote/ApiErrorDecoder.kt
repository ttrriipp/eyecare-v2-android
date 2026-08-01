package com.eyecare.app.data.remote

import com.eyecare.app.data.remote.dto.ApiErrorResponse
import com.eyecare.app.domain.model.ApiDomainError
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody

object ApiErrorDecoder {

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(httpStatus: Int, body: String?): ApiDomainError {
        if (body.isNullOrBlank()) return ApiDomainError.unknown(httpStatus)

        return try {
            val envelope = json.decodeFromString<ApiErrorResponse>(body)
            val error = envelope.error
            if (error == null || (error.code == null && error.message == null)) {
                return ApiDomainError.unknown(httpStatus)
            }
            ApiDomainError(
                httpStatus = httpStatus,
                code = error.code ?: "UNKNOWN_ERROR",
                message = error.message ?: "Something went wrong. Please try again.",
                fieldErrors = error.details ?: emptyMap(),
            )
        } catch (_: Exception) {
            ApiDomainError.unknown(httpStatus)
        }
    }

    fun decodeFromResponse(httpStatus: Int, body: ResponseBody?): ApiDomainError =
        decode(httpStatus, body?.string())
}
