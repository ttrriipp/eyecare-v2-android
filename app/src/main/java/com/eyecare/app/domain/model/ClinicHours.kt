package com.eyecare.app.domain.model

/**
 * One weekday's operating hours. [weekday] follows the backend's Carbon convention
 * (0 = Sunday ... 6 = Saturday); [dayName] is the display source of truth so callers
 * never need to reason about that convention directly. [openTime]/[closeTime] are
 * nullable "HH:mm" clinic-local wall-clock strings, present only when [enabled].
 */
data class ClinicHoursDay(
    val weekday: Int,
    val dayName: String,
    val enabled: Boolean,
    val openTime: String?,
    val closeTime: String?,
)
