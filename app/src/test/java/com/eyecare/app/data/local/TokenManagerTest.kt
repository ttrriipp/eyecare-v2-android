package com.eyecare.app.data.local

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TokenManagerTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var tokenManager: TokenManager

    @BeforeEach
    fun setup() {
        editor = mockk(relaxed = true)
        prefs = mockk {
            every { edit() } returns editor
        }
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        tokenManager = TokenManager(prefs)
    }

    @Test
    fun `getToken returns null when no token stored`() {
        every { prefs.getString(TokenManager.KEY_TOKEN, null) } returns null
        assertNull(tokenManager.getToken())
    }

    @Test
    fun `getToken returns stored token`() {
        every { prefs.getString(TokenManager.KEY_TOKEN, null) } returns "abc123"
        assertEquals("abc123", tokenManager.getToken())
    }

    @Test
    fun `saveToken writes token to prefs`() {
        tokenManager.saveToken("mytoken")
        verify { editor.putString(TokenManager.KEY_TOKEN, "mytoken") }
        verify { editor.apply() }
    }

    @Test
    fun `clearToken removes token from prefs`() {
        tokenManager.clearToken()
        verify { editor.remove(TokenManager.KEY_TOKEN) }
        verify { editor.apply() }
    }

    @Test
    fun `clearTokenIfMatches clears the current token`() {
        every { prefs.getString(TokenManager.KEY_TOKEN, null) } returns "expired-token"

        assertTrue(tokenManager.clearTokenIfMatches("expired-token"))

        verify { editor.remove(TokenManager.KEY_TOKEN) }
        verify { editor.apply() }
    }

    @Test
    fun `clearTokenIfMatches preserves a newer token`() {
        every { prefs.getString(TokenManager.KEY_TOKEN, null) } returns "new-token"

        assertFalse(tokenManager.clearTokenIfMatches("expired-token"))

        verify(exactly = 0) { editor.remove(TokenManager.KEY_TOKEN) }
    }

    @Test
    fun `getInstallationId returns null when not stored`() {
        every { prefs.getString(TokenManager.KEY_INSTALLATION_ID, null) } returns null
        assertNull(tokenManager.getInstallationId())
    }

    @Test
    fun `getInstallationId returns stored value`() {
        every { prefs.getString(TokenManager.KEY_INSTALLATION_ID, null) } returns "uuid-123"
        assertEquals("uuid-123", tokenManager.getInstallationId())
    }

    @Test
    fun `saveInstallationId writes to prefs`() {
        tokenManager.saveInstallationId("uuid-456")
        verify { editor.putString(TokenManager.KEY_INSTALLATION_ID, "uuid-456") }
        verify { editor.apply() }
    }
}
