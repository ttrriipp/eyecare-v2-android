package com.eyecare.app.data.local

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeviceIdentityProviderTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var tokenManager: TokenManager
    private lateinit var provider: DeviceIdentityProvider

    @BeforeEach
    fun setup() {
        editor = mockk(relaxed = true)
        prefs = mockk {
            every { edit() } returns editor
        }
        every { editor.putString(any(), any()) } returns editor
        tokenManager = TokenManager(prefs)
        provider = DeviceIdentityProvider(tokenManager)
    }

    @Test
    fun `installation id is generated when none exists`() {
        every { prefs.getString(TokenManager.KEY_INSTALLATION_ID, null) } returns null
        val id = provider.getOrCreateInstallationId()
        assertTrue(id.isNotBlank())
        verify { editor.putString(TokenManager.KEY_INSTALLATION_ID, id) }
        verify { editor.apply() }
    }

    @Test
    fun `installation id is stable across calls`() {
        val stored = "existing-uuid"
        every { prefs.getString(TokenManager.KEY_INSTALLATION_ID, null) } returns stored
        assertEquals(stored, provider.getOrCreateInstallationId())
    }

    @Test
    fun `clearing token preserves installation id`() {
        val stored = "stable-uuid"
        every { prefs.getString(TokenManager.KEY_INSTALLATION_ID, null) } returns stored
        every { editor.remove(TokenManager.KEY_TOKEN) } returns editor
        tokenManager.clearToken()
        assertEquals(stored, provider.getOrCreateInstallationId())
    }

    @Test
    fun `device name is not blank`() {
        assertTrue(provider.deviceName().isNotBlank())
    }
}
