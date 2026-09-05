package com.eyecare.app.data.local

import android.content.Context
import com.eyecare.app.domain.model.ThemePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _themePreference = MutableStateFlow(
        ThemePreference.fromStoredValue(preferences.getString(KEY_THEME_PREFERENCE, null)),
    )

    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    fun setThemePreference(preference: ThemePreference) {
        preferences.edit().putString(KEY_THEME_PREFERENCE, preference.storedValue).apply()
        _themePreference.value = preference
    }

    private companion object {
        const val PREFS_NAME = "eyecare_app_preferences"
        const val KEY_THEME_PREFERENCE = "theme_preference"
    }
}
