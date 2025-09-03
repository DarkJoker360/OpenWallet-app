/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.security

import androidx.fragment.app.FragmentActivity
import com.esposito.openwallet.R
import com.esposito.openwallet.core.util.SecureLogger

/**
 * Enhanced credit card encryption using hardware-backed Android Keystore
 * Provides maximum security for sensitive credit card information
 */
object EnhancedCreditCardEncryption {
    
    private const val TAG = "EnhancedCreditCardEncryption"
    
    /**
     * Decrypts sensitive credit card data with biometric authentication
     * @param activity The FragmentActivity for biometric prompts
     * @param encryptedData The encrypted data container
     * @param onSuccess Callback with decrypted data
     * @param onError Callback with error message
     */
    fun decryptSensitiveData(
        activity: FragmentActivity,
        encryptedData: EncryptedData,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val authHelper = AuthHelper(activity)
        
        authHelper.authenticateForDecryption(
            encryptedData = encryptedData.encryptedData,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    
    /**
     * Decrypts a credit card number with biometric authentication
     */
    fun decryptCardNumber(
        activity: FragmentActivity,
        encryptedCardNumber: EncryptedData,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        decryptSensitiveData(activity, encryptedCardNumber, onSuccess, onError)
    }
    
    /**
     * Decrypts a CVV with biometric authentication
     */
    fun decryptCVV(
        activity: FragmentActivity,
        encryptedCVV: EncryptedData,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        decryptSensitiveData(activity, encryptedCVV, onSuccess, onError)
    }
    
    /**
     * Decrypts an IBAN with biometric authentication
     */
    fun decryptIBAN(
        activity: FragmentActivity,
        encryptedIBAN: EncryptedData,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        decryptSensitiveData(activity, encryptedIBAN, onSuccess, onError)
    }
    
    /**
     * Sets up secure storage and encrypts credit card data with a single biometric prompt
     * This prevents multiple authentication prompts during first-time setup
     */
    fun setupAndEncryptCreditCardBatch(
        activity: FragmentActivity,
        cardNumber: String,
        cvv: String,
        iban: String,
        onSuccess: (cardNumber: EncryptedData?, cvv: EncryptedData?, iban: EncryptedData?) -> Unit,
        onError: (String) -> Unit
    ) {
        SecureLogger.d(TAG, "Setting up secure storage and batch encryption")
        
        val authHelper = AuthHelper(activity)
        
        // Check if secure storage is already set up
        if (authHelper.getSecurityInfo().isKeyGenerated) {
            SecureLogger.d(TAG, "Secure storage already set up, proceeding with batch encryption")
            encryptCreditCardBatch(activity, cardNumber, cvv, iban, onSuccess, onError)
            return
        }
        
        // Set up secure storage and encrypt in one session
        authHelper.setupSecureStorageAndEncrypt(
            fieldsToEncrypt = listOfNotNull(
                if (cardNumber.isNotEmpty()) "cardNumber" to cardNumber else null,
                if (cvv.isNotEmpty()) "cvv" to cvv else null,
                if (iban.isNotEmpty()) "iban" to iban else null
            ),
            onSuccess = { encryptedFields ->
                SecureLogger.d(TAG, "Setup and batch encryption successful")
                
                val encryptedCardNumber = encryptedFields["cardNumber"]
                val encryptedCVV = encryptedFields["cvv"]
                val encryptedIBAN = encryptedFields["iban"]
                
                onSuccess(encryptedCardNumber, encryptedCVV, encryptedIBAN)
            },
            onError = { error ->
                SecureLogger.e(TAG, "Setup and batch encryption failed", null)
                onError(activity.getString(R.string.setup_encryption_failed))
            }
        )
    }
    
    /**
     * Encrypts multiple credit card fields with a single biometric authentication
     * This prevents multiple biometric prompts during card creation
     */
    fun encryptCreditCardBatch(
        activity: FragmentActivity,
        cardNumber: String,
        cvv: String,
        iban: String,
        onSuccess: (cardNumber: EncryptedData?, cvv: EncryptedData?, iban: EncryptedData?) -> Unit,
        onError: (String) -> Unit
    ) {
        SecureLogger.d(TAG, "Starting batch encryption for credit card data")
        
        val authHelper = AuthHelper(activity)
        
        // Authenticate once and then encrypt all fields in the success callback
        authHelper.authenticateForBatchEncryption(
            fieldsToEncrypt = listOfNotNull(
                if (cardNumber.isNotEmpty()) "cardNumber" to cardNumber else null,
                if (cvv.isNotEmpty()) "cvv" to cvv else null,
                if (iban.isNotEmpty()) "iban" to iban else null
            ),
            onSuccess = { encryptedFields ->
                SecureLogger.d(TAG, "Batch encryption successful")
                
                val encryptedCardNumber = encryptedFields["cardNumber"]
                val encryptedCVV = encryptedFields["cvv"]
                val encryptedIBAN = encryptedFields["iban"]
                
                onSuccess(encryptedCardNumber, encryptedCVV, encryptedIBAN)
            },
            onError = { error ->
                SecureLogger.e(TAG, "Batch encryption failed", null)
                onError(activity.getString(R.string.encryption_failed_try_again))
            }
        )
    }
    
    /**
     * Container for encrypted data with metadata
     */
    data class EncryptedData(
        val iv: String,                    // Base64 encoded initialization vector
        val encryptedData: String,         // Base64 encoded encrypted data
        val timestamp: Long                // When the data was encrypted
    ) {
        /**
         * Converts to a storable string format
         */
        fun toStorageString(): String {
            return "$iv:$encryptedData:$timestamp"
        }
        
        companion object {
            /**
             * Creates EncryptedData from storage string format
             */
            fun fromStorageString(storageString: String): EncryptedData? {
                return try {
                    val parts = storageString.split(":")
                    if (parts.size == 3) {
                        EncryptedData(
                            iv = parts[0],
                            encryptedData = parts[1],
                            timestamp = parts[2].toLong()
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    SecureLogger.e(TAG, "Failed to parse encrypted data", e)
                    null
                }
            }
        }
    }
}
