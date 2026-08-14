package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ClinicHoursDtos {

    @Serializable
    data class ClinicHoursDayDto(
        val weekday: Int,
        @SerialName("day_name") val dayName: String,
        val enabled: Boolean,
        @SerialName("open_time") val openTime: String? = null,
        @SerialName("close_time") val closeTime: String? = null,
    )

    @Serializable
    data class ClinicHoursListResponse(val data: List<ClinicHoursDayDto>)
}
