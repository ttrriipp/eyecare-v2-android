package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.FrameReservationApiService
import com.eyecare.app.data.remote.dto.ApiErrorBody
import com.eyecare.app.data.remote.dto.FrameReservationDtos
import com.eyecare.app.data.remote.dto.PaginationMeta
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.FrameReservationError
import com.eyecare.app.domain.model.ReservationStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.math.BigDecimal

class FrameReservationRepositoryImplTest {

    private val api: FrameReservationApiService = mockk()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val repository = FrameReservationRepositoryImpl(api, json)

    private fun createAppointmentDto(
        id: Int = 1,
        number: String? = "APT-001",
        status: String = "scheduled",
        scheduledAt: String = "2026-08-01T10:00:00+08:00",
        duration: Int = 30,
    ) = FrameReservationDtos.ReservationAppointmentDto(
        id = id,
        appointmentNumber = number,
        status = status,
        scheduledAt = scheduledAt,
        durationMinutes = duration,
    )

    private fun createVariantDto() = FrameReservationDtos.ReservationVariantDto(
        id = 1,
        name = "Black / 52mm",
        sku = "RB-CR-BLK-52",
        price = BigDecimal("4500.00"),
        product = FrameReservationDtos.ReservationProductDto(
            id = 7, name = "Classic Rectangle", slug = "classic-rectangle",
            brand = "Ray-Ban",
        ),
    )

    private fun createReservationDto(
        id: Int = 1,
        status: String = "requested",
        appointmentDto: FrameReservationDtos.ReservationAppointmentDto = createAppointmentDto(),
    ) = FrameReservationDtos.ReservationDto(
        id = id,
        appointmentId = appointmentDto.id,
        appointment = appointmentDto,
        status = status,
        createdAt = "2026-07-27T10:00:00+08:00",
        items = listOf(
            FrameReservationDtos.ReservationItemDto(
                id = 1,
                productVariantId = 42,
                variant = createVariantDto(),
            ),
        ),
    )

    @Test
    fun `getReservations maps embedded appointment`() = runTest {
        val dto = createReservationDto()
        val response = FrameReservationDtos.ReservationListResponse(data = listOf(dto))
        coEvery { api.getReservations() } returns response

        val reservations = repository.getReservations().getOrThrow()
        assertEquals(1, reservations.size)

        val reservation = reservations[0]
        assertEquals(1, reservation.appointment.id)
        assertEquals("APT-001", reservation.appointment.appointmentNumber)
        assertEquals(AppointmentStatus.SCHEDULED, reservation.appointment.status)
        assertEquals("2026-08-01T10:00:00+08:00", reservation.appointment.scheduledAt)
        assertEquals(30, reservation.appointment.durationMinutes)
    }

    @Test
    fun `createReservation serializes non-null appointment_id`() = runTest {
        val dto = createReservationDto()
        val response = FrameReservationDtos.ReservationResponse(data = dto)
        coEvery { api.createReservation(any()) } returns response

        val result = repository.createReservation(listOf(42), 1)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().appointment.id)
    }

    @Test
    fun `cancelReservation maps embedded appointment`() = runTest {
        val dto = createReservationDto(status = "cancelled")
        val response = FrameReservationDtos.ReservationResponse(data = dto)
        coEvery { api.cancelReservation(1) } returns response

        val result = repository.cancelReservation(1).getOrThrow()
        assertEquals(ReservationStatus.CANCELLED, result.status)
        assertNotNull(result.appointment)
    }

    @Test
    fun `createReservation 422 maps to ValidationError`() = runTest {
        coEvery { api.createReservation(any()) } throws HttpException(
            Response.error<FrameReservationDtos.ReservationResponse>(
                422,
                okhttp3.ResponseBody.create(
                    "application/json".toMediaType(),
                    """{"message":"Validation failed","errors":{"appointment_id":["The selected appointment id is invalid."]}}""",
                ),
            )
        )

        val result = repository.createReservation(listOf(42), 999)
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is FrameReservationError.ValidationError)
        val validationError = error as FrameReservationError.ValidationError
        assertNotNull(validationError.fieldErrors["appointment_id"])
    }

    @Test
    fun `unknown embedded status maps through AppointmentStatus`() = runTest {
        val dto = createReservationDto(
            appointmentDto = createAppointmentDto(status = "some_unknown_status"),
        )
        val response = FrameReservationDtos.ReservationResponse(data = dto)
        coEvery { api.createReservation(any()) } returns response

        val result = repository.createReservation(listOf(42), 1).getOrThrow()
        assertEquals(AppointmentStatus.UNKNOWN, result.appointment.status)
    }
}
