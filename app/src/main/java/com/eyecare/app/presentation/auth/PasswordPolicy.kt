package com.eyecare.app.presentation.auth

internal const val MIN_PASSWORD_LENGTH = 8

internal data class PasswordValidation(
    val hasMinimumLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasNumber: Boolean,
    val hasSpecialCharacter: Boolean,
) {
    val isValid: Boolean
        get() = hasMinimumLength &&
            hasUppercase &&
            hasLowercase &&
            hasNumber &&
            hasSpecialCharacter
}

internal fun passwordValidation(password: String): PasswordValidation = PasswordValidation(
    hasMinimumLength = password.length >= MIN_PASSWORD_LENGTH,
    hasUppercase = password.any { it in 'A'..'Z' },
    hasLowercase = password.any { it in 'a'..'z' },
    hasNumber = password.any { it in '0'..'9' },
    hasSpecialCharacter = password.any { !it.isLetterOrDigit() && !it.isWhitespace() },
)

internal fun passwordMeetsPolicy(password: String): Boolean = passwordValidation(password).isValid
