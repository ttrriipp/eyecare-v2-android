package com.eyecare.app.data.local

import android.os.Build
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdentityProvider @Inject constructor(
    private val tokenManager: TokenManager,
) {
    fun getOrCreateInstallationId(): String {
        tokenManager.getInstallationId()?.let { return it }
        val id = UUID.randomUUID().toString()
        tokenManager.saveInstallationId(id)
        return id
    }

    fun deviceName(): String {
        val manufacturer = Build.MANUFACTURER ?: "Android"
        val model = Build.MODEL ?: "Device"
        return "$manufacturer $model"
    }
}
