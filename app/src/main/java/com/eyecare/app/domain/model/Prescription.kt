package com.eyecare.app.domain.model

data class Prescription(
    val id: Int,
    val appointmentId: Int?,
    val odSphere: String?,
    val odCylinder: String?,
    val odAxis: String?,
    val odAdd: String?,
    val osSphere: String?,
    val osCylinder: String?,
    val osAxis: String?,
    val osAdd: String?,
    val pd: String?,
    val prescribedAt: String,
    val expiresAt: String?,
    val notes: String?,
)
