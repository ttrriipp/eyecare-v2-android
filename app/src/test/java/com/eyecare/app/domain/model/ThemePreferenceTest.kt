package com.eyecare.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThemePreferenceTest {

    @Test
    fun `missing or unknown stored preference defaults to light`() {
        assertEquals(ThemePreference.LIGHT, ThemePreference.fromStoredValue(null))
        assertEquals(ThemePreference.LIGHT, ThemePreference.fromStoredValue("unknown"))
    }

    @Test
    fun `light and dark preferences ignore the device theme`() {
        assertFalse(ThemePreference.LIGHT.isDark(systemIsDark = true))
        assertFalse(ThemePreference.LIGHT.isDark(systemIsDark = false))
        assertTrue(ThemePreference.DARK.isDark(systemIsDark = true))
        assertTrue(ThemePreference.DARK.isDark(systemIsDark = false))
    }

    @Test
    fun `system preference follows the device theme`() {
        assertTrue(ThemePreference.SYSTEM.isDark(systemIsDark = true))
        assertFalse(ThemePreference.SYSTEM.isDark(systemIsDark = false))
    }
}
