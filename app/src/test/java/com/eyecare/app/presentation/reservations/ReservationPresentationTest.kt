package com.eyecare.app.presentation.reservations

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReservationPresentationTest {

    @Test
    fun `unheld chip label is Requested`() {
        assertEquals("Requested", reservationChipLabel(false))
    }

    @Test
    fun `held chip label is Set aside`() {
        assertEquals("Set aside", reservationChipLabel(true))
    }

    @Test
    fun `unheld explanation uses contract copy with em dash`() {
        val expected = "Request sent \u2014 the clinic will set these aside before your visit."
        assertEquals(expected, reservationExplanation(false, null))
    }

    @Test
    fun `held explanation includes formatted expiry`() {
        val result = reservationExplanation(true, "2026-08-20T17:00:00+08:00")
        assertTrue(result.startsWith("Set aside for your visit until"))
        assertTrue(result.contains("Aug 20, 2026"))
        assertTrue(result.endsWith("."))
    }

    @Test
    fun `held explanation without expiry omits until clause`() {
        val result = reservationExplanation(true, null)
        assertEquals("Set aside for your visit.", result)
    }

    @Test
    fun `held explanation with blank expiry omits until clause`() {
        val result = reservationExplanation(true, "")
        assertEquals("Set aside for your visit.", result)
    }
}
