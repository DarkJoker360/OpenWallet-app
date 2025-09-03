/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.authentication.domain

import android.content.Context

/**
 * Manages authentication session state across the application
 */
object AuthenticationSessionManager {
    
    private var isAuthenticated = false
    private var sessionStartTime = 0L
    private var appWasInBackground = false
    private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
    
    /**
     * Mark user as authenticated
     */
    fun markAuthenticated() {
        isAuthenticated = true
        sessionStartTime = System.currentTimeMillis()
        appWasInBackground = false
    }
    
    /**
     * Check if user is currently authenticated and session is still valid
     */
    fun isSessionValid(): Boolean {
        if (!isAuthenticated) return false
        
        val currentTime = System.currentTimeMillis()
        val sessionAge = currentTime - sessionStartTime
        
        return sessionAge < SESSION_TIMEOUT_MS
    }
    
    /**
     * Clear authentication session
     */
    fun clearSession() {
        isAuthenticated = false
        sessionStartTime = 0L
        appWasInBackground = false
    }
    
    /**
     * Check if authentication is required
     */
    fun requiresAuthentication(context: Context): Boolean {
        val appPrefs = com.esposito.openwallet.core.data.local.manager.AppPreferencesManager(context)
        
        if (!appPrefs.isBiometricLockEnabled) {
            return false
        }
        
        // Always require authentication if:
        // 1. Never authenticated before, OR
        // 2. Session expired, OR
        // 3. App was accessed from background/recents
        return !isAuthenticated || !isSessionValid() || appWasInBackground
    }
    
    /**
     * Refresh session timestamp (call when user interacts with app)
     */
    fun refreshSession() {
        if (isAuthenticated) {
            sessionStartTime = System.currentTimeMillis()
        }
    }
    
    /**
     * Mark app as going to background
     */
    fun markAppInBackground() {
        // Mark that app went to background/recents
        appWasInBackground = true
    }
    
    /**
     * Clear session when app truly goes to background (called from onStop)
     */
    fun clearSessionOnBackground() {
        // Don't clear session immediately, just mark as background access
        markAppInBackground()
    }
}
