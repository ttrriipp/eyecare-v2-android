package com.eyecare.app.domain.model

private const val PHILIPPINES_COUNTRY_CODE = "63"
private const val PHILIPPINES_LOCAL_PREFIX = "0"
private const val PHILIPPINES_LOCAL_LENGTH = 10

fun toPhilippineLocalDigits(value: String): String {
    val trimmedValue = value.trim()
    val digits = value.filter(Char::isDigit)
    val localDigits = when {
        trimmedValue.startsWith("+") && digits.startsWith(PHILIPPINES_COUNTRY_CODE) ->
            digits.removePrefix(PHILIPPINES_COUNTRY_CODE)
        digits.startsWith(PHILIPPINES_COUNTRY_CODE) && digits.length > PHILIPPINES_LOCAL_LENGTH ->
            digits.removePrefix(PHILIPPINES_COUNTRY_CODE)
        digits.startsWith(PHILIPPINES_LOCAL_PREFIX) && digits.length > PHILIPPINES_LOCAL_LENGTH ->
            digits.removePrefix(PHILIPPINES_LOCAL_PREFIX)
        else -> digits
    }
    return localDigits.take(PHILIPPINES_LOCAL_LENGTH)
}

fun toPhilippineE164(value: String): String {
    val localDigits = toPhilippineLocalDigits(value)
    return if (localDigits.isEmpty()) "" else "+$PHILIPPINES_COUNTRY_CODE$localDigits"
}
