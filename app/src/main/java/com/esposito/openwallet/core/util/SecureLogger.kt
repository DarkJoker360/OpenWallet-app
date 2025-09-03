/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.util

import android.util.Log
import com.esposito.openwallet.BuildConfig

/**
 * Secure logging utility that prevents sensitive information from being logged in production builds.
 * Direct replacement for Android Log with automatic production safety.
 */
object SecureLogger {
    
    private const val SENSITIVE_DATA_PLACEHOLDER = "[REDACTED]"
    
    // Keywords that indicate sensitive data
    private val SENSITIVE_KEYWORDS = listOf(
        "password", "token", "key", "secret", "credential", "pin", "cvv", 
        "card", "number", "iban", "account", "encrypted", "decrypt", "auth",
        "biometric", "fingerprint", "face", "private", "sensitive"
    )
    
    /**
     * Check if logging is enabled 
     */
    fun isLoggingEnabled(): Boolean = BuildConfig.DEBUG
    
    /**
     * Verbose logging
     */
    fun v(tag: String, message: String) {
        if (isLoggingEnabled()) {
            Log.v(tag, sanitizeMessage(message))
        }
    }
    
    /**
     * Debug logging
     */
    fun d(tag: String, message: String) {
        if (isLoggingEnabled()) {
            Log.d(tag, sanitizeMessage(message))
        }
    }
    
    /**
     * Info logging
     */
    fun i(tag: String, message: String) {
        if (isLoggingEnabled()) {
            Log.i(tag, sanitizeMessage(message))
        }
    }
    
    /**
     * Warning logging
     */
    fun w(tag: String, message: String) {
        if (isLoggingEnabled()) {
            Log.w(tag, sanitizeMessage(message))
        }
    }
    
    /**
     * Error logging - always active but sanitized
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitizeMessage(message)
        if (throwable != null) {
            Log.e(tag, sanitizedMessage, sanitizeThrowable(throwable))
        } else {
            Log.e(tag, sanitizedMessage)
        }
    }
    
    /**
     * Sanitize log messages to remove sensitive information
     */
    private fun sanitizeMessage(message: String): String {
        var sanitized = message
        
        // Replace potential sensitive data patterns
        SENSITIVE_KEYWORDS.forEach { keyword ->
            if (sanitized.contains(keyword, ignoreCase = true)) {
                // Replace anything that looks like sensitive data
                sanitized = sanitized.replace(
                    Regex("$keyword[\\s:=]*[\\w-_@.]+", RegexOption.IGNORE_CASE),
                    "$keyword: $SENSITIVE_DATA_PLACEHOLDER"
                )
            }
        }
        
        // Remove potential card numbers (sequences of 13-19 digits)
        sanitized = sanitized.replace(
            Regex("\\b\\d{13,19}\\b"),
            SENSITIVE_DATA_PLACEHOLDER
        )
        
        // Remove potential IBANs (2 letters followed by 2 digits and alphanumeric characters)
        sanitized = sanitized.replace(
            Regex("\\b[A-Z]{2}\\d{2}[A-Z0-9]{4,}\\b"),
            SENSITIVE_DATA_PLACEHOLDER
        )
        
        // Remove potential email addresses to prevent user identification
        sanitized = sanitized.replace(
            Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
            SENSITIVE_DATA_PLACEHOLDER
        )
        
        return sanitized
    }
    
    /**
     * Sanitize throwable stack traces to remove sensitive information
     */
    private fun sanitizeThrowable(throwable: Throwable): Throwable {
        return if (isLoggingEnabled()) {
            throwable
        } else {
            // In production, only log the exception type and a generic message
            RuntimeException("${throwable.javaClass.simpleName}: [Exception details hidden in production]")
        }
    }
}