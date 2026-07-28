package com.eyecare.app.domain.model

data class EyeMeasurement(
    val value: String?,
    val sphere: String?,
    val cylinder: String?,
)

data class PrescriptionMeasurementGroup(
    val od: EyeMeasurement,
    val os: EyeMeasurement,
)

data class PrescriptionMeasurements(
    val main: PrescriptionMeasurementGroup,
    val add: PrescriptionMeasurementGroup,
)

data class Prescription(
    val id: Int,
    val appointmentId: Int?,
    val previousPrescriptionId: Int?,
    val isCurrent: Boolean,
    val date: String,
    val measurements: PrescriptionMeasurements,
    val remarks: String?,
)
