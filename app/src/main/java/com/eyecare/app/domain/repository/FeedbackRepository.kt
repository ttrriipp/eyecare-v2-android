package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Feedback

interface FeedbackRepository {
    suspend fun submitFeedback(
        appointmentId: Int?,
        orderId: Int?,
        rating: Int,
        comment: String?,
    ): Result<Feedback>

    suspend fun getFeedbackHistory(): Result<List<Feedback>>
}
