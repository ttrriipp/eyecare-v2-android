package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.FrameReservation

interface FrameReservationRepository {
    suspend fun getReservations(): Result<List<FrameReservation>>
    suspend fun createReservation(variantIds: List<Int>, appointmentId: Int? = null): Result<FrameReservation>
    suspend fun cancelReservation(reservationId: Int): Result<FrameReservation>
}
