package com.eyecare.app.presentation.ar.capability

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.StatFs

/** Reads platform facts without initializing CameraX, MediaPipe, or SceneView. */
class AndroidArCapabilityProvider(
    private val context: Context,
) {

    private val applicationContext = context.applicationContext ?: context

    fun readFacts(): ArDeviceFacts {
        val activityManager = applicationContext.getSystemService(ActivityManager::class.java)
        val configuration = activityManager?.deviceConfigurationInfo
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        return ArDeviceFacts(
            apiLevel = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.toSet(),
            openGlEsVersion = OpenGlEsVersion.parse(configuration?.glEsVersion),
            totalRamBytes = memoryInfo.totalMem,
            hasFrontCamera = applicationContext.packageManager.hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FRONT,
            ),
            availableStorageBytes = availableStorageBytes(),
        )
    }

    private fun availableStorageBytes(): Long = runCatching {
        StatFs(applicationContext.filesDir.absolutePath).availableBytes
    }.getOrDefault(0L)
}
