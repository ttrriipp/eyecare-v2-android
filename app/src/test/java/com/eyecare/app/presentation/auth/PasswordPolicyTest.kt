package com.eyecare.app.presentation.auth

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PasswordPolicyTest {

    @Test
    fun `password policy requires length and every composition rule`() {
        assertFalse(passwordMeetsPolicy("password123!"))
        assertFalse(passwordMeetsPolicy("PASSWORD123!"))
        assertFalse(passwordMeetsPolicy("PasswordOnly!"))
        assertFalse(passwordMeetsPolicy("Password1234"))
        assertFalse(passwordMeetsPolicy("Pass1!"))
        assertTrue(passwordMeetsPolicy("Abcdef1!"))
    }

    @Test
    fun `whitespace does not satisfy the special character requirement`() {
        assertFalse(passwordValidation("Password 123").hasSpecialCharacter)
    }
}
