package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.FrameReservationApiService
import com.eyecare.app.data.remote.dto.ApiErrorBody
import com.eyecare.app.data.remote.dto.FrameReservationDtos
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.FrameReservationError
import com.eyecare.app.domain.model.FrameReservationItem
import com.eyecare.app.domain.model.ReservationAppointment
import com.eyecare.app.domain.model.ReservationStatus
import com.eyecare.app.domain.repository.FrameReservationRepository
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

class FrameReservationRepositoryImpl @Inject constructor(
    private val api: FrameReservationApiService,
    private val json: Json,
) : FrameReservationRepository {

    override suspend fun getReservations(): Result<List<FrameReservation>> = runCatching {
        api.getReservations().data.map { it.toDomain() }
    }

    override suspend fun createReservation(
        variantIds: List<Int>,
        appointmentId: Int,
    ): Result<FrameReservation> = runCatching {
        api.createReservation(
            FrameReservationDtos.CreateReservationRequest(
                appointmentId = appointmentId,
                items = variantIds.map { FrameReservationDtos.CreateReservationItemRequest(it) },
            ),
        ).data.toDomain()
    }.recoverCatching { throwable ->
        if (throwable is HttpException && throwable.code() == 422) {
            val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
            val parsed = json.decodeFromString<ApiErrorBody>(body)
            throw FrameReservationError.ValidationError(parsed.errors ?: emptyMap())
        }
        throw throwable
    }

    override suspend fun cancelReservation(reservationId: Int): Result<FrameReservation> = runCatching {
        api.cancelReservation(reservationId).data.toDomain()
    }

    private fun FrameReservationDtos.ReservationDto.toDomain() = FrameReservation(
        id = id,
        appointment = appointment?.toDomain()
            ?: ReservationAppointment(
                id = appointmentId ?: 0,
                appointmentNumber = null,
                status = AppointmentStatus.UNKNOWN,
                scheduledAt = "",
                durationMinutes = 0,
            ),
        status = ReservationStatus.fromApi(status),
        expiresAt = expiresAt,
        createdAt = createdAt,
        items = items.map { it.toDomain() },
    )

    private fun FrameReservationDtos.ReservationAppointmentDto.toDomain() = ReservationAppointment(
        id = id,
        appointmentNumber = appointmentNumber,
        status = AppointmentStatus.from(status),
        scheduledAt = scheduledAt,
        durationMinutes = durationMinutes,
    )

    private fun FrameReservationDtos.ReservationItemDto.toDomain() = FrameReservationItem(
        id = id,
        productVariantId = productVariantId,
        variantName = variant.name,
        variantSku = variant.sku,
        price = variant.price,
        frameName = variant.product.name,
        frameBrand = variant.product.brand,
        images = variant.images,
    )
}
