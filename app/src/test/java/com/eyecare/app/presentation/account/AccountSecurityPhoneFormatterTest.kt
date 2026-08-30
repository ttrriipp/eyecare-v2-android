package com.eyecare.app.presentation.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountSecurityPhoneFormatterTest {

    @Test
    fun `backend masked phone is normalized to the plus 63 display pattern`() {
        assertEquals("+63 917***4567", formatMaskedPhilippinePhone("0917***4567"))
    }

    @Test
    fun `raw account phone is masked before display`() {
        assertEquals("+63 917***4567", formatMaskedPhilippinePhone("09171234567"))
        assertEquals("+63 917***4567", formatMaskedPhilippinePhone("+639171234567"))
    }

    @Test
    fun `missing phone has a safe empty state`() {
        assertEquals("Not provided", formatMaskedPhilippinePhone(null))
    }
}
