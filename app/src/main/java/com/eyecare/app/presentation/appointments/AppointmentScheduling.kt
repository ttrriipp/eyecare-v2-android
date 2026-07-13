package com.eyecare.app.presentation.appointments

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val CLINIC_TIME_ZONE: ZoneId = ZoneId.of("Asia/Manila")

internal fun formatClinicScheduledAt(date: String, time: String): String =
    LocalDate.parse(date)
        .atTime(LocalTime.parse(time))
        .atZone(CLINIC_TIME_ZONE)
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
