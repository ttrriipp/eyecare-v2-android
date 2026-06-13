package com.eyecare.app.di

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val PREFS_NAME = "eyecare_secure_prefs"

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences = createEncryptedPrefs(context)
}

/**
 * Creates EncryptedSharedPreferences, recovering from AEADBadTagException
 * which occurs after app reinstall: Android Keystore deletes encryption keys
 * on uninstall but the encrypted prefs file can survive (e.g. via backup).
 * Fix: delete the corrupted file and recreate with fresh keys.
 */
internal fun createEncryptedPrefs(context: Context): SharedPreferences {
    return try {
        buildEncryptedPrefs(context)
    } catch (e: Exception) {
        if (e.cause?.javaClass?.simpleName == "AEADBadTagException" ||
            e is javax.crypto.AEADBadTagException ||
            e.message?.contains("AEADBadTag") == true
        ) {
            Log.w("StorageModule", "EncryptedSharedPreferences corrupted after reinstall — clearing and recreating")
            // Delete both the prefs file and the associated keystore entry
            context.deleteSharedPreferences(PREFS_NAME)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        } else {
            Log.e("StorageModule", "Unexpected error creating EncryptedSharedPreferences: ${e.message}")
        }
        // Recreate from scratch — user will need to log in again
        buildEncryptedPrefs(context)
    }
}

private fun buildEncryptedPrefs(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
