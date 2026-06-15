package com.eyecare.app.presentation.appointments.booking

/**
 * Converts a 24-hour "HH:mm" slot to "h:mm AM/PM" display format.
 * e.g. "14:00" → "2:00 PM", "09:00" → "9:00 AM"
 */
internal fun formatTimeSlot(time24: String): String {
    val (hourStr, minute) = time24.split(":")
    val hour = hourStr.toInt()
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$hour12:$minute $amPm"
}
