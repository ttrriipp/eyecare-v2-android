package com.eyecare.app.presentation.settings

import androidx.lifecycle.ViewModel
import com.eyecare.app.data.local.ThemePreferences
import com.eyecare.app.domain.model.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    val themePreference: StateFlow<ThemePreference> = themePreferences.themePreference

    fun setThemePreference(preference: ThemePreference) {
        themePreferences.setThemePreference(preference)
    }
}
