package com.eyecare.app.data.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val prefs: SharedPreferences,
) {
    private val tokenLock = Any()

    fun getToken(): String? = synchronized(tokenLock) {
        prefs.getString(KEY_TOKEN, null)
    }

    fun saveToken(token: String) = synchronized(tokenLock) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearToken() = synchronized(tokenLock) {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun clearTokenIfMatches(expectedToken: String): Boolean = synchronized(tokenLock) {
        if (prefs.getString(KEY_TOKEN, null) != expectedToken) {
            return@synchronized false
        }

        prefs.edit().remove(KEY_TOKEN).apply()
        true
    }

    fun getInstallationId(): String? = prefs.getString(KEY_INSTALLATION_ID, null)

    fun saveInstallationId(id: String) = prefs.edit().putString(KEY_INSTALLATION_ID, id).apply()

    companion object {
        const val KEY_TOKEN = "auth_token"
        const val KEY_INSTALLATION_ID = "installation_id"
    }
}
