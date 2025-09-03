/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("DEPRECATION") // TODO: inspect

package com.esposito.openwallet.core.data.local.manager

import android.content.Context
import android.content.SharedPreferences
import com.esposito.openwallet.core.util.SecureLogger
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.GeneralSecurityException

/**
 * Consolidated preferences manager that replaces multiple separate managers
 * Handles onboarding, security, and other app preferences in one place
 */
class AppPreferencesManager(context: Context) {
    
    // Use application context to prevent memory leaks
    private val appContext = context.applicationContext
    
    companion object {
        private const val TAG = "AppPreferencesManager"
        private const val PREFS_NAME = "app_prefs"
        
        // Onboarding preferences
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_APP_VERSION_ON_FIRST_LAUNCH = "app_version_on_first_launch"
        
        // Security preferences
        private const val KEY_SCREENSHOT_BLOCKING = "screenshot_blocking"
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock"

        private const val KEY_FIRST_LAUNCH = "first_launch"
        
        // Default values
        private const val DEFAULT_ONBOARDING_COMPLETED = false
        private const val DEFAULT_SCREENSHOT_BLOCKING = false
        private const val DEFAULT_BIOMETRIC_LOCK = false
        private const val DEFAULT_FIRST_LAUNCH = true
    }
    
    private val sharedPrefs: SharedPreferences by lazy {
        createSecurePreferences()
    }
    
    private fun createSecurePreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            SecureLogger.e(TAG, "Failed to create encrypted preferences, falling back to regular preferences", e)
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Unexpected error creating preferences", e)
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    // === ONBOARDING PREFERENCES ===
    
    var isOnboardingCompleted: Boolean
        get() = sharedPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, DEFAULT_ONBOARDING_COMPLETED)
        set(value) = sharedPrefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, value) }
    
    var appVersionOnFirstLaunch: String?
        get() = sharedPrefs.getString(KEY_APP_VERSION_ON_FIRST_LAUNCH, null)
        set(value) = sharedPrefs.edit { putString(KEY_APP_VERSION_ON_FIRST_LAUNCH, value) }
    
    // === SECURITY PREFERENCES ===
    
    var isScreenshotBlockingEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_SCREENSHOT_BLOCKING, DEFAULT_SCREENSHOT_BLOCKING)
        set(value) = sharedPrefs.edit { putBoolean(KEY_SCREENSHOT_BLOCKING, value) }
    
    var isBiometricLockEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_BIOMETRIC_LOCK, DEFAULT_BIOMETRIC_LOCK)
        set(value) = sharedPrefs.edit { putBoolean(KEY_BIOMETRIC_LOCK, value) }
    
    var isFirstLaunch: Boolean
        get() = sharedPrefs.getBoolean(KEY_FIRST_LAUNCH, DEFAULT_FIRST_LAUNCH)
        set(value) = sharedPrefs.edit { putBoolean(KEY_FIRST_LAUNCH, value) }

    /**
     * Clear only onboarding-related preferences
     */
    fun clearOnboardingData() {
        sharedPrefs.edit {
            remove(KEY_ONBOARDING_COMPLETED)
            remove(KEY_APP_VERSION_ON_FIRST_LAUNCH)
        }
        SecureLogger.d(TAG, "Onboarding data cleared")
    }
    
    /**
     * Get preference statistics for debugging
     */
    @Suppress("unused")
    fun getPreferenceStats(): Map<String, Any> {
        return mapOf(
            "onboardingCompleted" to isOnboardingCompleted,
            "screenshotBlocking" to isScreenshotBlockingEnabled,
            "biometricLock" to isBiometricLockEnabled,
            "firstLaunch" to isFirstLaunch,
            "appVersion" to (appVersionOnFirstLaunch ?: "null")
        )
    }
}