package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.FeedbackApiService
import com.eyecare.app.data.remote.dto.FeedbackDtos
import com.eyecare.app.domain.model.Feedback
import com.eyecare.app.domain.repository.FeedbackRepository
import javax.inject.Inject

class FeedbackRepositoryImpl @Inject constructor(
    private val api: FeedbackApiService,
) : FeedbackRepository {

    override suspend fun submitFeedback(
        appointmentId: Int?, orderId: Int?, rating: Int, comment: String?,
    ): Result<Feedback> = runCatching {
        api.submitFeedback(FeedbackDtos.SubmitFeedbackRequest(appointmentId, orderId, rating, comment)).data.toDomain()
    }

    override suspend fun getFeedbackHistory(): Result<List<Feedback>> = runCatching {
        api.getFeedbackHistory().data.map { it.toDomain() }
    }

    private fun FeedbackDtos.FeedbackDto.toDomain() = Feedback(
        id = id, appointmentId = appointmentId, orderId = orderId,
        rating = rating, comment = comment,
    )
}
