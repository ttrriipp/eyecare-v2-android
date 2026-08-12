package com.eyecare.app.presentation.eyewear

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EyewearPresentationTest {

    @Test
    fun `formatPeso formats correctly`() {
        assertEquals("\u20B11,400.00", formatPeso(BigDecimal("1400.00")))
        assertEquals("\u20B10.00", formatPeso(BigDecimal("0.00")))
    }

    @Test
    fun `isEdited returns true only for revisionNumber greater than 1`() {
        assertTrue(isEdited(2))
        assertTrue(isEdited(5))
        assertFalse(isEdited(1))
        assertFalse(isEdited(null))
    }
}
