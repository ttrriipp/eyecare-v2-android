package com.eyecare.app.presentation.navigation

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BottomNavVisibilityTest {

    @Test
    fun `optical order detail hides bottom navigation`() {
        assertFalse(
            shouldShowBottomNav(
                "com.eyecare.app.presentation.navigation.OpticalOrderDetail/{orderId}",
            ),
        )
    }

    @Test
    fun `my orders list keeps bottom navigation hidden`() {
        assertFalse(shouldShowBottomNav("com.eyecare.app.presentation.navigation.MyOrders"))
    }

    @Test
    fun `appointment request wizard hides bottom navigation`() {
        assertFalse(
            shouldShowBottomNav(
                "com.eyecare.app.presentation.navigation.RequestAppointment",
            ),
        )
    }

    @Test
    fun `saved frames hides bottom navigation`() {
        assertFalse(
            shouldShowBottomNav(
                "com.eyecare.app.presentation.navigation.SavedFrames",
            ),
        )
    }

    @Test
    fun `main tabs keep bottom navigation visible`() {
        assertTrue(shouldShowBottomNav("com.eyecare.app.presentation.navigation.Home"))
    }
}
