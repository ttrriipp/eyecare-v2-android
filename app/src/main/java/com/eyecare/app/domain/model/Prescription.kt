package com.eyecare.app.domain.model

data class Prescription(
    val id: Int,
    val appointmentId: Int?,
    val odSphere: String?,
    val odCylinder: String?,
    val odAxis: Int?,
    val odAdd: String?,
    val odPrism: String?,
    val odBase: String?,
    val osSphere: String?,
    val osCylinder: String?,
    val osAxis: Int?,
    val osAdd: String?,
    val osPrism: String?,
    val osBase: String?,
    val pd: String?,
    val prescribedAt: String,
    val expiresAt: String?,
    val notes: String?,
)
