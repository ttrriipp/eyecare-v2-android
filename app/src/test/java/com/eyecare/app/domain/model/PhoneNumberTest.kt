package com.eyecare.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PhoneNumberTest {

    @Test
    fun `local input becomes Philippine E164`() {
        assertEquals("+639514646068", toPhilippineE164("9514646068"))
    }

    @Test
    fun `national and international input normalize to the same local digits`() {
        assertEquals("9514646068", toPhilippineLocalDigits("09514646068"))
        assertEquals("9514646068", toPhilippineLocalDigits("+63 951 464 6068"))
    }

    @Test
    fun `empty input stays empty`() {
        assertEquals("", toPhilippineE164(""))
    }

    @Test
    fun `partial international prefix is not shown as editable digits`() {
        assertEquals("", toPhilippineLocalDigits("+63"))
        assertEquals("9", toPhilippineLocalDigits("+639"))
        assertEquals("+639", toPhilippineE164("9"))
    }
}
