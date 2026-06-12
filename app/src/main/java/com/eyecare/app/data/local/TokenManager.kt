package com.eyecare.app.data.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val prefs: SharedPreferences,
) {
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()

    fun clearToken() = prefs.edit().remove(KEY_TOKEN).apply()

    companion object {
        const val KEY_TOKEN = "auth_token"
    }
}
