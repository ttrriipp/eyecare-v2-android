package com.eyecare.app.data.local

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
}
