package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.PatientIntakeApiService
import com.eyecare.app.data.remote.dto.ApiErrorBody
import com.eyecare.app.data.remote.dto.PatientIntakeDtos
import com.eyecare.app.domain.model.IntakeStatus
import com.eyecare.app.domain.model.PatientIntake
import com.eyecare.app.domain.repository.PatientIntakeRepository
import com.eyecare.app.domain.repository.SaveIntakeRequest
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

class PatientIntakeRepositoryImpl @Inject constructor(
    private val api: PatientIntakeApiService,
    private val json: Json,
) : PatientIntakeRepository {

    override suspend fun getIntake(appointmentId: Int): Result<PatientIntake?> = runCatching {
        api.getIntake(appointmentId).data?.toDomain()
    }

    override suspend fun saveIntake(
        appointmentId: Int,
        request: SaveIntakeRequest,
    ): Result<PatientIntake> = runCatching {
        api.saveIntake(
            appointmentId,
            PatientIntakeDtos.SaveIntakeRequest(
                fullName = request.fullName,
                dateOfBirth = request.dateOfBirth,
                gender = request.gender,
                occupation = request.occupation,
                address = request.address,
                phone = request.phone,
                email = request.email,
                chiefComplaint = request.chiefComplaint,
                pastOcularHistory = request.pastOcularHistory,
                pastSurgicalHistory = request.pastSurgicalHistory,
                pastMedicalHistory = request.pastMedicalHistory,
                allergies = request.allergies,
                medications = request.medications,
            ),
        ).data!!.toDomain()
    }.recoverCatching { throwable ->
        if (throwable is HttpException && throwable.code() == 422) {
            val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
            val parsed = json.decodeFromString<ApiErrorBody>(body)
            throw IntakeError.ValidationError(parsed.errors ?: emptyMap())
        }
        throw throwable
    }

    override suspend fun submitIntake(appointmentId: Int): Result<PatientIntake> = runCatching {
        api.submitIntake(appointmentId).data!!.toDomain()
    }.recoverCatching { throwable ->
        if (throwable is HttpException && throwable.code() == 422) {
            val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
            val parsed = json.decodeFromString<ApiErrorBody>(body)
            throw IntakeError.ValidationError(parsed.errors ?: emptyMap())
        }
        throw throwable
    }

    private fun PatientIntakeDtos.PatientIntakeDto.toDomain() = PatientIntake(
        id = id,
        patientId = patientId,
        appointmentId = appointmentId,
        status = IntakeStatus.from(status),
        appointmentType = appointmentType,
        fullName = fullName,
        dateOfBirth = dateOfBirth,
        gender = gender,
        occupation = occupation,
        address = address,
        phone = phone,
        email = email,
        chiefComplaint = chiefComplaint,
        pastOcularHistory = pastOcularHistory,
        pastSurgicalHistory = pastSurgicalHistory,
        pastMedicalHistory = pastMedicalHistory,
        allergies = allergies,
        medications = medications,
        submittedAt = submittedAt,
        verifiedAt = verifiedAt,
    )
}

sealed class IntakeError(message: String) : Exception(message) {
    data class ValidationError(val fieldErrors: Map<String, List<String>>) :
        IntakeError(fieldErrors.values.flatten().firstOrNull() ?: "Validation failed")
}
