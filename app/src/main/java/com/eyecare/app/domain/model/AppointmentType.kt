package com.eyecare.app.domain.model

data class AppointmentType(
    val id: Int,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val requiresReferral: Boolean,
)
