package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentV1ApiService
import com.eyecare.app.data.remote.dto.ApiErrorBody
import com.eyecare.app.data.remote.dto.AppointmentV1Dtos
import com.eyecare.app.data.remote.dto.CancellationReasonRequest
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.VisitRating
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.PaginatedResult
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

class AppointmentV1RepositoryImpl @Inject constructor(
    private val api: AppointmentV1ApiService,
    private val json: Json,
) : AppointmentV1Repository {

    override suspend fun getAppointments(page: Int): Result<PaginatedResult<AppointmentV1>> = runCatching {
        val response = api.getAppointments(page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getAppointmentHistory(page: Int, perPage: Int): Result<PaginatedResult<AppointmentV1>> = safeApiCall {
        val response = api.getAppointmentHistory(filter = "history", page = page, perPage = perPage)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getAppointment(id: Int): Result<AppointmentV1> = runCatching {
        api.getAppointment(id).data.toDomain()
    }

    override suspend fun getAppointmentAvailability(
        date: String,
        appointmentId: Int?,
    ): Result<com.eyecare.app.domain.model.AppointmentAvailability> = runCatching {
        api.getAppointmentAvailability(date, appointmentId).data.toDomain()
    }

    override suspend fun cancelAppointment(id: Int, reasonDetails: String): Result<AppointmentV1> = safeApiCall {
        api.cancelAppointment(id, CancellationReasonRequest(reasonDetails)).data.toDomain()
    }

    override suspend fun rateAppointment(id: Int, rating: Int, comment: String?): Result<VisitRating> = runCatching {
        val response = api.rateAppointment(id, AppointmentV1Dtos.VisitRatingRequest(rating = rating, comment = comment))
        response.data.toDomain()
    }.recoverCatching { throwable ->
        when {
            throwable is HttpException && throwable.code() == 404 ->
                throw AppointmentError.NotFound
            throwable is HttpException && throwable.code() == 422 -> {
                val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
                val parsed = json.decodeFromString<ApiErrorBody>(body)
                throw AppointmentError.ValidationError(parsed.errors ?: emptyMap())
            }
            else -> throw throwable
        }
    }
}
