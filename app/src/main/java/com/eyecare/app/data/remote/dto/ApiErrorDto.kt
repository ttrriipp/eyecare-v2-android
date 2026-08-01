package com.eyecare.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val error: ApiErrorDetail? = null,
)

@Serializable
data class ApiErrorDetail(
    val code: String? = null,
    val message: String? = null,
    val details: Map<String, List<String>>? = null,
)
