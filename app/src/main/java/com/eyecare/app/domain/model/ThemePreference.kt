package com.eyecare.app.domain.model

enum class ThemePreference(
    val storedValue: String,
) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> systemIsDark
    }

    companion object {
        fun fromStoredValue(value: String?): ThemePreference =
            entries.firstOrNull { it.storedValue == value } ?: LIGHT
    }
}
