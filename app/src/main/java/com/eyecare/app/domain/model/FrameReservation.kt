package com.eyecare.app.domain.model

import java.math.BigDecimal

data class FrameReservation(
    val id: Int,
    val appointmentId: Int?,
    val status: ReservationStatus,
    val expiresAt: String?,
    val createdAt: String,
    val items: List<FrameReservationItem>,
)

data class FrameReservationItem(
    val id: Int,
    val productVariantId: Int,
    val variantName: String,
    val variantSku: String,
    val price: BigDecimal,
    val frameName: String,
    val frameBrand: String,
    val images: List<String>,
)

enum class ReservationStatus {
    REQUESTED,
    PREPARED,
    TRIED_ON,
    CONVERTED,
    RELEASED,
    CANCELLED,
    UNKNOWN;

    companion object {
        fun fromApi(value: String): ReservationStatus = entries.firstOrNull {
            it != UNKNOWN && it.name.equals(value, ignoreCase = true)
        } ?: UNKNOWN
    }
}

val FrameReservation.isCancellable: Boolean
    get() = status == ReservationStatus.REQUESTED || status == ReservationStatus.PREPARED
