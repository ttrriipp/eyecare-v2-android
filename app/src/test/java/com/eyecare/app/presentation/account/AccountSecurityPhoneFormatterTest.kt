package com.eyecare.app.presentation.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountSecurityPhoneFormatterTest {

    @Test
    fun `backend masked phone fallback is normalized without inventing digits`() {
        assertEquals("+63 917 *** 4567", formatPhilippinePhone("0917***4567"))
    }

    @Test
    fun `raw account phone is fully visible in the plus 63 display pattern`() {
        assertEquals("+63 917 123 4567", formatPhilippinePhone("09171234567"))
        assertEquals("+63 917 123 4567", formatPhilippinePhone("+639171234567"))
    }

    @Test
    fun `missing phone has a safe empty state`() {
        assertEquals("Not provided", formatPhilippinePhone(null))
    }
}
