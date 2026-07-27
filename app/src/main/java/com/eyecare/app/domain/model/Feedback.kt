package com.eyecare.app.domain.model

data class Feedback(
    val id: Int,
    val appointmentId: Int,
    val rating: Int,
    val comment: String?,
)
