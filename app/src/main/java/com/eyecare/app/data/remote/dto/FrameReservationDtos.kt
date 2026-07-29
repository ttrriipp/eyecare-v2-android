package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

object FrameReservationDtos {

    @Serializable
    data class ReservationAppointmentDto(
        val id: Int,
        @SerialName("appointment_number") val appointmentNumber: String? = null,
        val status: String,
        @SerialName("scheduled_at") val scheduledAt: String,
        @SerialName("duration_minutes") val durationMinutes: Int,
    )

    @Serializable
    data class ReservationProductDto(
        val id: Int,
        val name: String,
        val slug: String,
        val description: String? = null,
        @SerialName("product_type") val productType: String = "frame",
        val brand: String? = null,
        val category: String? = null,
    )

    @Serializable
    data class ReservationVariantDto(
        val id: Int,
        val name: String,
        val sku: String,
        @Serializable(with = MoneyValueSerializer::class) val price: BigDecimal,
        @SerialName("compare_at_price") @Serializable(with = MoneyValueSerializer::class) val compareAtPrice: BigDecimal? = null,
        val attributes: kotlinx.serialization.json.JsonElement? = null,
        val images: List<String> = emptyList(),
        val product: ReservationProductDto,
    )

    @Serializable
    data class ReservationItemDto(
        val id: Int,
        @SerialName("product_variant_id") val productVariantId: Int,
        val variant: ReservationVariantDto,
    )

    @Serializable
    data class ReservationDto(
        val id: Int,
        @SerialName("appointment_id") val appointmentId: Int? = null,
        val appointment: ReservationAppointmentDto? = null,
        val status: String,
        @SerialName("expires_at") val expiresAt: String? = null,
        @SerialName("created_at") val createdAt: String,
        val items: List<ReservationItemDto> = emptyList(),
    )

    @Serializable
    data class ReservationListResponse(val data: List<ReservationDto>)

    @Serializable
    data class ReservationResponse(val data: ReservationDto)

    @Serializable
    data class CreateReservationItemRequest(
        @SerialName("product_variant_id") val productVariantId: Int,
    )

    @Serializable
    data class CreateReservationRequest(
        @SerialName("appointment_id") val appointmentId: Int,
        val items: List<CreateReservationItemRequest>,
    )
}
