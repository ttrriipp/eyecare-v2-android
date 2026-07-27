package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Feedback

interface FeedbackRepository {
    suspend fun submitFeedback(
        appointmentId: Int,
        rating: Int,
        comment: String?,
    ): Result<Feedback>
}
