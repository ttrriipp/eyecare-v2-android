package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.FrameReservation

interface FrameReservationRepository {
    suspend fun getReservations(): Result<List<FrameReservation>>
    suspend fun createReservation(variantIds: List<Int>, appointmentId: Int): Result<FrameReservation>
    suspend fun deleteReservation(reservationId: Int): Result<Unit>
    suspend fun addItem(reservationId: Int, variantId: Int): Result<FrameReservation>
    suspend fun removeItem(reservationId: Int, itemId: Int): Result<FrameReservation?>
}
