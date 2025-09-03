/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */
package com.esposito.openwallet.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.esposito.openwallet.R
import com.esposito.openwallet.core.util.SecureLogger
import kotlinx.coroutines.launch

/**
 * Handles biometric authentication for secure encryption/decryption operations
 * Integrates with SecureStorage for hardware-backed security
 */
class AuthHelper(private val activity: FragmentActivity) {
    
    private val tag = "AuthHelper"
    
    /**
     * Checks if biometric authentication is available and properly set up
     */
    fun isBiometricAvailable(): BiometricAvailability {
        val biometricManager = BiometricManager.from(activity)
        
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.UNSUPPORTED
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricAvailability.UNKNOWN
            else -> BiometricAvailability.UNKNOWN
        }
    }
    
    /**
     * Authenticates user and decrypts the provided data
     * @param encryptedData The encrypted data (with embedded IV)
     * @param onSuccess Callback with decrypted data
     * @param onError Callback with error message
     */
    fun authenticateForDecryption(
        encryptedData: String,
        onSuccess: (decryptedData: String) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        SecureLogger.d(tag, "Starting biometric authentication for decryption")
        
        if (!SecureStorage.isKeyAvailable()) {
            SecureLogger.e(tag, "Encryption key not available")
            onError(activity.getString(R.string.auth_failed_setup_security))
            return
        }
        
        val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    SecureLogger.d(tag, "Biometric authentication successful")
                    
                    activity.lifecycleScope.launch {
                        try {
                            val decryptedData = SecureStorage.decrypt(encryptedData)
                            if (decryptedData != null) {
                                SecureLogger.d(tag, "Decryption completed successfully")
                                onSuccess(decryptedData)
                            } else {
                                SecureLogger.e(tag, "Decryption failed - returned null")
                                onError(activity.getString(R.string.auth_failed_try_again))
                            }
                        } catch (e: Exception) {
                            SecureLogger.e(tag, "Decryption operation failed", e)
                            onError(activity.getString(R.string.auth_failed_try_again))
                        }
                    }
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    SecureLogger.w(tag, "Biometric authentication error occurred")
                    onError(activity.getString(R.string.auth_failed_try_again))
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    SecureLogger.w(tag, "Biometric authentication failed")
                    onError(activity.getString(R.string.auth_failed_try_again))
                }
            })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.access_credit_card_data))
            .setSubtitle(activity.getString(R.string.access_credit_card_subtitle))
            .setDescription(activity.getString(R.string.authenticate_to_decrypt))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    

    
    /**
     * Gets detailed information about the security capabilities
     */
    fun getSecurityInfo(): SecurityInfo {
        val biometricAvailability = isBiometricAvailable()
        val isKeyGenerated = SecureStorage.isKeyAvailable()
        
        return SecurityInfo(
            biometricAvailability = biometricAvailability,
            isKeyGenerated = isKeyGenerated,
            keyInfo = null // keyInfo is no longer available in simplified API
        )
    }
    
    enum class BiometricAvailability(val message: String) {
        AVAILABLE("Biometric authentication is available and ready"),
        NO_HARDWARE("No biometric hardware found"),
        HARDWARE_UNAVAILABLE("Biometric hardware is unavailable"),
        NOT_ENROLLED("No biometric credentials enrolled"),
        SECURITY_UPDATE_REQUIRED("Security update required"),
        UNSUPPORTED("Biometric authentication not supported"),
        UNKNOWN("Unknown biometric status")
    }
    
    data class SecurityInfo(
        val biometricAvailability: BiometricAvailability,
        val isKeyGenerated: Boolean,
        val keyInfo: Any?
    )
    
    /**
     * Sets up secure storage and encrypts multiple fields with a single authentication
     * This prevents double authentication during first-time setup
     */
    fun setupSecureStorageAndEncrypt(
        fieldsToEncrypt: List<Pair<String, String>>,
        onSuccess: (Map<String, EnhancedCreditCardEncryption.EncryptedData>) -> Unit,
        onError: (String) -> Unit
    ) {
        SecureLogger.d(tag, "Setting up secure storage and batch encryption")
        
        val availability = isBiometricAvailable()
        if (availability != BiometricAvailability.AVAILABLE) {
            onError(activity.getString(R.string.biometric_unavailable_check_settings))
            return
        }
        
        val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    SecureLogger.d(tag, "Setup authentication successful")
                    
                    activity.lifecycleScope.launch {
                        try {
                            if (!SecureStorage.isKeyAvailable()) {
                                SecureLogger.d(tag, "Initializing secure storage")
                            }
                            
                            val encryptedResults = mutableMapOf<String, EnhancedCreditCardEncryption.EncryptedData>()
                            
                            for ((fieldName, fieldData) in fieldsToEncrypt) {
                                SecureLogger.d(tag, "Processing field for encryption")
                                val encryptedData = SecureStorage.encrypt(fieldData)
                                if (encryptedData != null) {
                                    encryptedResults[fieldName] = EnhancedCreditCardEncryption.EncryptedData(
                                        iv = "",
                                        encryptedData = encryptedData,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    SecureLogger.d(tag, "Field encrypted successfully")
                                } else {
                                    SecureLogger.e(tag, "Field encryption failed")
                                    onError(activity.getString(R.string.encryption_failed_try_again))
                                    return@launch
                                }
                            }
                            
                            SecureLogger.d(tag, "All fields encrypted successfully")
                            onSuccess(encryptedResults)
                            
                        } catch (e: Exception) {
                            SecureLogger.e(tag, "Setup and encryption failed", e)
                            onError(activity.getString(R.string.setup_failed_try_again))
                        }
                    }
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    SecureLogger.w(tag, "Setup authentication error occurred")
                    onError(activity.getString(R.string.auth_failed_try_again))
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    SecureLogger.w(tag, "Setup authentication failed")
                    onError(activity.getString(R.string.auth_failed_try_again))
                }
            })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.setup_secure_credit_card_storage))
            .setSubtitle(activity.getString(R.string.setup_secure_storage_subtitle))
            .setDescription(activity.getString(R.string.setup_hardware_encryption_description))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * Authenticates once and encrypts multiple fields in a single session
     * This prevents multiple biometric prompts during batch operations
     */
    fun authenticateForBatchEncryption(
        fieldsToEncrypt: List<Pair<String, String>>,
        onSuccess: (Map<String, EnhancedCreditCardEncryption.EncryptedData>) -> Unit,
        onError: (String) -> Unit
    ) {
        SecureLogger.d(tag, "Starting batch encryption with single authentication")
        
        if (!SecureStorage.isKeyAvailable()) {
            SecureLogger.e(tag, "Encryption key not available")
            onError(activity.getString(R.string.auth_failed_setup_security))
            return
        }
        
        val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    SecureLogger.d(tag, "Batch authentication successful")
                    
                    activity.lifecycleScope.launch {
                        try {
                            val encryptedResults = mutableMapOf<String, EnhancedCreditCardEncryption.EncryptedData>()
                            
                            for ((fieldName, fieldData) in fieldsToEncrypt) {
                                SecureLogger.d(tag, "Processing field for batch encryption")
                                val encryptedData = SecureStorage.encrypt(fieldData)
                                if (encryptedData != null) {
                                    encryptedResults[fieldName] = EnhancedCreditCardEncryption.EncryptedData(
                                        iv = "",
                                        encryptedData = encryptedData,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    SecureLogger.d(tag, "Field encrypted successfully")
                                } else {
                                    SecureLogger.e(tag, "Field encryption failed")
                                    onError(activity.getString(R.string.encryption_failed_try_again))
                                    return@launch
                                }
                            }
                            
                            SecureLogger.d(tag, "All fields encrypted successfully in batch")
                            onSuccess(encryptedResults)
                            
                        } catch (e: Exception) {
                            SecureLogger.e(tag, "Batch encryption failed", e)
                                    onError(activity.getString(R.string.encryption_failed_try_again))
                        }
                    }
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    SecureLogger.w(tag, "Batch authentication error occurred")
                    onError(activity.getString(R.string.auth_failed_try_again))
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    SecureLogger.w(tag, "Batch authentication failed")
                    onError(activity.getString(R.string.auth_failed_try_again))
                }
            })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.secure_credit_card_data))
            .setSubtitle(activity.getString(R.string.secure_credit_card_subtitle))
            .setDescription(activity.getString(R.string.authenticate_encrypt_description))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
}
