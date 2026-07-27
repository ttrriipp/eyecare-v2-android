package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.FeedbackApiService
import com.eyecare.app.data.remote.dto.ApiErrorBody
import com.eyecare.app.data.remote.dto.FeedbackDtos
import com.eyecare.app.domain.model.Feedback
import com.eyecare.app.domain.repository.FeedbackRepository
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

class FeedbackRepositoryImpl @Inject constructor(
    private val api: FeedbackApiService,
    private val json: Json,
) : FeedbackRepository {

    override suspend fun submitFeedback(
        appointmentId: Int,
        rating: Int,
        comment: String?,
    ): Result<Feedback> = runCatching {
        api.submitFeedback(FeedbackDtos.SubmitFeedbackRequest(appointmentId, rating, comment)).data.toDomain()
    }.recoverCatching { throwable ->
        if (throwable is HttpException && throwable.code() == 422) {
            val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
            val parsed = json.decodeFromString<ApiErrorBody>(body)
            throw FeedbackValidationError(parsed.errors ?: emptyMap())
        }
        throw throwable
    }

    private fun FeedbackDtos.FeedbackDto.toDomain() = Feedback(
        id = id,
        appointmentId = appointmentId,
        rating = rating,
        comment = comment,
    )
}

class FeedbackValidationError(val fieldErrors: Map<String, List<String>>) :
    Exception(fieldErrors.values.flatten().firstOrNull() ?: "Validation failed")
