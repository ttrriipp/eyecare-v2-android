package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object PrescriptionDtos {

    @Serializable
    data class PrescriptionDto(
        val id: Int,
        @SerialName("appointment_id") val appointmentId: Int? = null,
        @SerialName("od_sphere") val odSphere: String? = null,
        @SerialName("od_cylinder") val odCylinder: String? = null,
        @SerialName("od_axis") val odAxis: String? = null,
        @SerialName("od_add") val odAdd: String? = null,
        @SerialName("os_sphere") val osSphere: String? = null,
        @SerialName("os_cylinder") val osCylinder: String? = null,
        @SerialName("os_axis") val osAxis: String? = null,
        @SerialName("os_add") val osAdd: String? = null,
        val pd: String? = null,
        @SerialName("prescribed_at") val prescribedAt: String,
        @SerialName("expires_at") val expiresAt: String? = null,
        val notes: String? = null,
    )

    @Serializable data class PrescriptionListResponse(val data: List<PrescriptionDto>)
    @Serializable data class PrescriptionResponse(val data: PrescriptionDto)
}
