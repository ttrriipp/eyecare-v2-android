package com.eyecare.app.presentation.home

import com.eyecare.app.domain.model.ClinicHoursDay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime

class HomeScreenTest {

    @Test
    fun `timeOfDayGreeting returns morning before noon`() {
        assertEquals("Good morning", timeOfDayGreeting(LocalTime.of(5, 0)))
        assertEquals("Good morning", timeOfDayGreeting(LocalTime.of(11, 59)))
    }

    @Test
    fun `timeOfDayGreeting returns afternoon from noon to before 5pm`() {
        assertEquals("Good afternoon", timeOfDayGreeting(LocalTime.of(12, 0)))
        assertEquals("Good afternoon", timeOfDayGreeting(LocalTime.of(16, 59)))
    }

    @Test
    fun `timeOfDayGreeting returns evening from 5pm through early morning`() {
        assertEquals("Good evening", timeOfDayGreeting(LocalTime.of(17, 0)))
        assertEquals("Good evening", timeOfDayGreeting(LocalTime.of(23, 59)))
        assertEquals("Good evening", timeOfDayGreeting(LocalTime.of(0, 0)))
        assertEquals("Good evening", timeOfDayGreeting(LocalTime.of(4, 59)))
    }

    private fun clinicDay(
        enabled: Boolean = true,
        openTime: String? = "09:00",
        closeTime: String? = "17:00",
    ) = ClinicHoursDay(
        weekday = 3,
        dayName = "Wednesday",
        enabled = enabled,
        openTime = openTime,
        closeTime = closeTime,
    )

    @Test
    fun `rangeLabel formats open and close time for an enabled day`() {
        val day = clinicDay(openTime = "09:00", closeTime = "17:00")

        assertEquals("9:00 AM – 5:00 PM", day.rangeLabel())
    }

    @Test
    fun `rangeLabel reports Closed for a disabled day`() {
        val day = clinicDay(enabled = false, openTime = null, closeTime = null)

        assertEquals("Closed", day.rangeLabel())
    }
}
