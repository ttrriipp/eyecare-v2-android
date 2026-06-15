package com.eyecare.app.presentation.appointments.booking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimeFormatTest {

    @Test
    fun `09_00 formats as 9_00 AM`() {
        assertEquals("9:00 AM", formatTimeSlot("09:00"))
    }

    @Test
    fun `10_00 formats as 10_00 AM`() {
        assertEquals("10:00 AM", formatTimeSlot("10:00"))
    }

    @Test
    fun `11_30 formats as 11_30 AM`() {
        assertEquals("11:30 AM", formatTimeSlot("11:30"))
    }

    @Test
    fun `12_00 formats as 12_00 PM`() {
        assertEquals("12:00 PM", formatTimeSlot("12:00"))
    }

    @Test
    fun `14_00 formats as 2_00 PM`() {
        assertEquals("2:00 PM", formatTimeSlot("14:00"))
    }

    @Test
    fun `15_30 formats as 3_30 PM`() {
        assertEquals("3:30 PM", formatTimeSlot("15:30"))
    }
}
