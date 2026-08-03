package com.eyecare.app.presentation.home

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
}
