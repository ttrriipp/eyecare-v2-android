package com.eyecare.app.domain.model

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val role: String,
    val patientNumber: String?,
    val fullName: String?,
    val dateOfBirth: String?,
    val occupation: String?,
    val address: String?,
    val gender: String?,
    val contactEmail: String?,
)
