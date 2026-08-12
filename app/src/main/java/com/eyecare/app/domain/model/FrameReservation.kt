package com.eyecare.app.domain.model

import java.math.BigDecimal

data class ReservationAppointment(
    val id: Int,
    val appointmentNumber: String?,
    val status: AppointmentStatus,
    val scheduledAt: String,
    val durationMinutes: Int,
)

data class FrameReservation(
    val id: Int,
    val appointment: ReservationAppointment,
    /** False while the clinic only has the request; true once the frames are pulled and held. */
    val isHeld: Boolean,
    val expiresAt: String?,
    val createdAt: String,
    val items: List<FrameReservationItem>,
)

/** Maximum number of items allowed in a single reservation. */
const val MAX_RESERVATION_ITEMS = 5

data class FrameReservationItem(
    val id: Int,
    val productVariantId: Int,
    val variantName: String,
    val variantSku: String,
    val price: BigDecimal,
    val compareAtPrice: BigDecimal?,
    val frameId: Int,
    val frameName: String,
    val frameBrand: String,
    val frameCategory: String,
    val frameDescription: String?,
    val attributes: Map<String, String>?,
    val images: List<String>,
)

/** DELETE succeeds for the owner in either state, so cancelling is always offered. */
val FrameReservation.isCancellable: Boolean
    get() = true

/** Contract: adding is rejected once the clinic has pulled the frames. */
val FrameReservation.canAddItems: Boolean
    get() = !isHeld && items.size < MAX_RESERVATION_ITEMS

/** Removing items is also gated on !isHeld (conservative — can be relaxed later). */
val FrameReservation.canRemoveItems: Boolean
    get() = !isHeld

/**
 * Combined list price of the reserved frames. This is an indicative value only —
 * a reservation holds stock, it never charges the patient.
 */
val FrameReservation.totalValue: BigDecimal
    get() = items.fold(BigDecimal.ZERO) { total, item -> total + item.price }
