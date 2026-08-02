package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.ApiErrorDecoder
import com.eyecare.app.domain.model.ApiDomainError
import retrofit2.HttpException

suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = runCatching {
    block()
}.recoverCatching { throwable ->
    throw mapHttpError(throwable)
}

private fun mapHttpError(throwable: Throwable): Throwable {
    if (throwable !is HttpException) return throwable
    val body = throwable.response()?.errorBody()?.string()
    val error = ApiErrorDecoder.decode(throwable.code(), body)
    return ApiDomainError(
        httpStatus = error.httpStatus,
        code = error.code,
        message = error.message,
        fieldErrors = error.fieldErrors,
    )
}
