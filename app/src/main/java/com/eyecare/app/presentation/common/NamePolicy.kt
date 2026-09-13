package com.eyecare.app.presentation.common

private val personNamePattern = Regex("""[\p{L}\p{M}]+(?:[ '-][\p{L}\p{M}]+)*""")

/**
 * Person names may contain letters (including accented letters), spaces, hyphens, and apostrophes.
 * Separators must sit between name parts so punctuation, numbers, and emoji cannot be submitted.
 */
internal fun isValidPersonName(value: String): Boolean = value.matches(personNamePattern)

internal fun invalidPersonNameMessage(fieldName: String): String =
    "$fieldName can contain letters, spaces, hyphens, and apostrophes only"
