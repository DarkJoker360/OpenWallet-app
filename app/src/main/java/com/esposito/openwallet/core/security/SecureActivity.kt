/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */
package com.esposito.openwallet.core.security

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.esposito.openwallet.feature.authentication.domain.AuthenticationSessionManager
import com.esposito.openwallet.feature.authentication.ui.AuthenticationActivity

/**
 * Base activity that automatically applies security settings to all activities that extend it.
 * This includes screenshot blocking and handling app lifecycle for security.
 */
abstract class SecureActivity : FragmentActivity() {
    
    private lateinit var securityManager: SecurityManager
    private var isAppInBackground = false
    
    // Activity result launcher for authentication
    private val authenticationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            AuthenticationActivity.RESULT_AUTHENTICATED -> {
                // Authentication successful, mark session as authenticated
                // This will reset the background access flag
                AuthenticationSessionManager.markAuthenticated()
            }
            AuthenticationActivity.RESULT_FAILED -> {
                // Authentication failed or cancelled, close the app
                finishAffinity()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        securityManager = SecurityManager(this)
        
        // Apply screenshot blocking immediately when activity is created
        securityManager.applyScreenshotBlocking(this)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Re-apply screenshot blocking when returning to the activity
        securityManager.applyScreenshotBlocking(this)
        
        // Check authentication - this will catch recents access
        if (AuthenticationSessionManager.requiresAuthentication(this)) {
            startAuthenticationFlow()
        } else {
            // Refresh session to keep it active during app usage
            AuthenticationSessionManager.refreshSession()
        }
        
        isAppInBackground = false
    }
    
    override fun onPause() {
        super.onPause()
        // Just mark as potentially going to background
        isAppInBackground = true
    }
    
    override fun onUserInteraction() {
        super.onUserInteraction()
        // Refresh session on user interaction
        AuthenticationSessionManager.refreshSession()
    }
    
    /**
     * Start the authentication flow using the dedicated AuthenticationActivity
     */
    private fun startAuthenticationFlow() {
        val intent = AuthenticationActivity.createIntent(this)
        authenticationLauncher.launch(intent)
    }
    
    /**
     * Force authentication check (can be called by subclasses)
     */
    protected fun requireAuthentication() {
        if (securityManager.isAppLockEnabled()) {
            AuthenticationSessionManager.clearSession()
            startAuthenticationFlow()
        }
    }
}
