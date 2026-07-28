package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

object PrescriptionDtos {

    @Serializable
    data class EyeMeasurementDto(
        val value: String? = null,
        val sphere: String? = null,
        val cylinder: String? = null,
    )

    @Serializable
    data class MeasurementGroupDto(
        val od: EyeMeasurementDto,
        val os: EyeMeasurementDto,
    )

    @Serializable
    data class MeasurementsDto(
        val main: MeasurementGroupDto,
        val add: MeasurementGroupDto,
    )

    @Serializable
    data class PrescriptionDto(
        val id: Int,
        @SerialName("appointment_id") val appointmentId: Int? = null,
        @SerialName("previous_prescription_id") val previousPrescriptionId: Int? = null,
        @SerialName("is_current") val isCurrent: Boolean = true,
        val date: String,
        val measurements: MeasurementsDto,
        val remarks: String? = null,
    )

    @Serializable
    data class PrescriptionListResponse(
        val data: List<PrescriptionDto>,
        val links: JsonElement? = null,
        val meta: PaginationMeta? = null,
    )

    @Serializable
    data class PrescriptionResponse(val data: PrescriptionDto)
}
