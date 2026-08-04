package com.eyecare.app.data.remote

import com.eyecare.app.domain.model.ApiDomainError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.ResponseBody

object ApiErrorDecoder {

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(httpStatus: Int, body: String?): ApiDomainError {
        if (body.isNullOrBlank()) return ApiDomainError.unknown(httpStatus)

        return runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            val customError = root["error"]?.jsonObject
            if (customError != null) {
                val fieldErrors = parseFieldErrors(customError["details"])
                val message = customError["message"].asTextOrNull()
                val code = customError["code"].asTextOrNull()
                if (code == null && message == null && fieldErrors.isEmpty()) {
                    return@runCatching ApiDomainError.unknown(httpStatus)
                }
                return@runCatching ApiDomainError(
                    httpStatus = httpStatus,
                    code = code ?: "UNKNOWN_ERROR",
                    message = message ?: fieldErrors.firstMessage() ?: UNKNOWN_MESSAGE,
                    fieldErrors = fieldErrors,
                )
            }

            val fieldErrors = parseFieldErrors(root["errors"])
            val message = root["message"].asTextOrNull()
            if (message == null && fieldErrors.isEmpty()) {
                ApiDomainError.unknown(httpStatus)
            } else {
                ApiDomainError(
                    httpStatus = httpStatus,
                    code = if (fieldErrors.isNotEmpty()) "VALIDATION" else "UNKNOWN_ERROR",
                    // Laravel's default validation-failure wrapper always sets a generic top-level
                    // message ("The given data was invalid.") alongside the real per-field reason in
                    // `errors` — prefer the specific field message so users see the actual problem.
                    message = fieldErrors.firstMessage() ?: message ?: UNKNOWN_MESSAGE,
                    fieldErrors = fieldErrors,
                )
            }
        }.getOrElse { ApiDomainError.unknown(httpStatus) }
    }

    fun decodeFromResponse(httpStatus: Int, body: ResponseBody?): ApiDomainError =
        decode(httpStatus, body?.string())

    private fun parseFieldErrors(element: JsonElement?): Map<String, List<String>> {
        val objectElement = element as? JsonObject ?: return emptyMap()
        return objectElement.mapNotNull { (field, value) ->
            val messages = when (value) {
                is JsonArray -> value.mapNotNull { it.asTextOrNull() }
                is JsonPrimitive -> listOfNotNull(value.contentOrNull)
                else -> emptyList()
            }.filter(String::isNotBlank)
            if (messages.isEmpty()) null else field to messages
        }.toMap()
    }

    private fun JsonElement?.asTextOrNull(): String? =
        (this as? JsonPrimitive)?.contentOrNull

    private fun Map<String, List<String>>.firstMessage(): String? =
        values.asSequence()
            .flatMap { it.asSequence() }
            .firstOrNull(String::isNotBlank)

    private const val UNKNOWN_MESSAGE = "Something went wrong. Please try again."
}
