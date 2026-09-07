package com.eyecare.app.presentation.auth

import com.eyecare.app.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WelcomeScreenTest {
    @Test
    fun `light app theme uses light logo`() {
        assertEquals(R.drawable.ic_eyecare_logo_1, welcomeLogoResource(darkTheme = false))
    }

    @Test
    fun `dark app theme uses dark logo`() {
        assertEquals(R.drawable.ic_eyecare_logo_dark, welcomeLogoResource(darkTheme = true))
    }
}
