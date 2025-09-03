/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.model

import androidx.fragment.app.FragmentActivity
import com.esposito.openwallet.core.security.EnhancedCreditCardEncryption
import com.esposito.openwallet.core.security.SecureStorage
import com.esposito.openwallet.core.util.SecureLogger
import java.util.UUID

/**
 * Enhanced credit card model with secure storage integration
 * Supports both traditional storage and hardware-backed encryption
 */
data class SecureCreditCard(
    val id: String = "",
    val cardHolderName: String = "",
    val bankName: String = "",
    val cardType: String = "",
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val notes: String = "",
    val color: String = "#2196F3",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Encrypted sensitive data - stored as encrypted strings
    val encryptedCardNumber: String? = null,
    val encryptedCVV: String? = null,
    val encryptedIBAN: String? = null,
    
    // Flag to indicate if enhanced security is enabled for this card
    val hasEnhancedSecurity: Boolean = false
) {
    
    /**
     * Creates a copy of this card with updated encrypted data
     */
    fun withEncryptedData(
        encryptedCardNumber: String? = this.encryptedCardNumber,
        encryptedCVV: String? = this.encryptedCVV,
        encryptedIBAN: String? = this.encryptedIBAN
    ): SecureCreditCard {
        return copy(
            encryptedCardNumber = encryptedCardNumber,
            encryptedCVV = encryptedCVV,
            encryptedIBAN = encryptedIBAN,
            hasEnhancedSecurity = true,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        /**
         * Creates a new credit card with encrypted sensitive data using batch encryption
         */
        fun createWithEncryption(
            activity: FragmentActivity,
            cardHolderName: String,
            bankName: String,
            cardType: String,
            cardNumber: String,
            cvv: String,
            iban: String,
            expiryMonth: String,
            expiryYear: String,
            notes: String = "",
            color: String = "#2196F3",
            onSuccess: (SecureCreditCard) -> Unit,
            onError: (String) -> Unit
        ) {
            val id = UUID.randomUUID().toString()
            val baseCard = SecureCreditCard(
                id = id,
                cardHolderName = cardHolderName,
                bankName = bankName,
                cardType = cardType,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                notes = notes,
                color = color
            )
            
            // Use combined setup and batch encryption with single biometric prompt
            EnhancedCreditCardEncryption.setupAndEncryptCreditCardBatch(
                activity = activity,
                cardNumber = cardNumber,
                cvv = cvv,
                iban = iban,
                onSuccess = { encryptedCardNumber, encryptedCVV, encryptedIBAN ->
                    val secureCard = baseCard.withEncryptedData(
                        encryptedCardNumber = encryptedCardNumber?.toStorageString(),
                        encryptedCVV = encryptedCVV?.toStorageString(),
                        encryptedIBAN = encryptedIBAN?.toStorageString()
                    )
                    onSuccess(secureCard)
                },
                onError = onError
            )
        }
        
        /**
         * Decrypts sensitive data from a secure credit card
         */
        fun decryptSensitiveData(
            activity: FragmentActivity,
            card: SecureCreditCard,
            onSuccess: (DecryptedCreditCardData) -> Unit,
            onError: (String) -> Unit
        ) {
            SecureLogger.d("SecureCreditCard", "Starting batch decryption")
            val decryptedData = DecryptedCreditCardData()
            
            // Collect all encrypted data that needs decryption
            val encryptionTasks = mutableListOf<Pair<String, String>>() // (fieldName, encryptedData)
            
            card.encryptedCardNumber?.let { encryptedString ->
                val cleanData = encryptedString.removePrefix("ENHANCED:")
                encryptionTasks.add("cardNumber" to cleanData)
            }
            
            card.encryptedCVV?.let { encryptedString ->
                val cleanData = encryptedString.removePrefix("ENHANCED:")
                encryptionTasks.add("cvv" to cleanData)
            }
            
            card.encryptedIBAN?.let { encryptedString ->
                val cleanData = encryptedString.removePrefix("ENHANCED:")
                encryptionTasks.add("iban" to cleanData)
            }
            
            if (encryptionTasks.isEmpty()) {
                SecureLogger.w("SecureCreditCard", "No encrypted data found")
                onSuccess(decryptedData)
                return
            }
            
            SecureLogger.d("SecureCreditCard", "Found ${encryptionTasks.size} fields to decrypt")
            
            // Use single authentication session to decrypt all fields
            decryptAllFieldsInSingleSession(activity, encryptionTasks, decryptedData, onSuccess, onError)
        }
        
        private fun decryptAllFieldsInSingleSession(
            activity: FragmentActivity,
            tasks: List<Pair<String, String>>,
            decryptedData: DecryptedCreditCardData,
            onSuccess: (DecryptedCreditCardData) -> Unit,
            onError: (String) -> Unit
        ) {
            if (tasks.isEmpty()) {
                onSuccess(decryptedData)
                return
            }
            
            val (fieldName, encryptedString) = tasks.first()
            val remainingTasks = tasks.drop(1)
            
            
            val encryptedData = EnhancedCreditCardEncryption.EncryptedData.fromStorageString(encryptedString)
            if (encryptedData == null) {
                SecureLogger.w("SecureCreditCard", "Failed to parse field, skipping")
                decryptRemainingFields(remainingTasks, decryptedData, onSuccess, onError)
                return
            }
            
            // Use the appropriate decryption method based on field type
            when (fieldName) {
                "cardNumber" -> {
                    EnhancedCreditCardEncryption.decryptCardNumber(
                        activity = activity,
                        encryptedCardNumber = encryptedData,
                        onSuccess = { decryptedValue ->
                            decryptedData.cardNumber = decryptedValue
                            
                            // Continue with remaining tasks using same auth session
                            decryptRemainingFields(remainingTasks, decryptedData, onSuccess, onError)
                        },
                        onError = { error ->
                            SecureLogger.e("SecureCreditCard", "Field decryption failed")
                            // Continue with remaining tasks
                            decryptRemainingFields(remainingTasks, decryptedData, onSuccess, onError)
                        }
                    )
                }
                "cvv" -> {
                    EnhancedCreditCardEncryption.decryptCVV(
                        activity = activity,
                        encryptedCVV = encryptedData,
                        onSuccess = { decryptedValue ->
                            decryptedData.cvv = decryptedValue
                            
                            // Continue with remaining tasks using same auth session
                            decryptRemainingFields(remainingTasks, decryptedData, onSuccess, onError)
                        },
                        onError = { error ->
                            SecureLogger.e("SecureCreditCard", "Field decryption failed")
                            // Continue with remaining tasks
                            decryptRemainingFields(remainingTasks, decryptedData, onSuccess, onError)
                        }
                    )
                }
                "iban" -> {
                    EnhancedCreditCardEncryption.decryptIBAN(
                        activity = activity,
                        encryptedIBAN = encryptedData,
                        onSuccess = { decryptedValue ->
                            decryptedData.iban = decryptedValue
                            
                            // Continue with remaining tasks using same auth session
                            decryptRemainingFields(remainingTasks, decryptedData, onSuccess, onError)
                        },
                        onError = { error ->
                            SecureLogger.e("SecureCreditCard", "Field decryption failed")
                            // Continue with remaining tasks
                            decryptRemainingFields(remainingTasks, decryptedData, onSuccess, onError)
                        }
                    )
                }
                else -> {
                    SecureLogger.w("SecureCreditCard", "Unknown field type")
                    decryptRemainingFields(remainingTasks, decryptedData, onSuccess, onError)
                }
            }
        }
        
        private fun decryptRemainingFields(
            tasks: List<Pair<String, String>>,
            decryptedData: DecryptedCreditCardData,
            onSuccess: (DecryptedCreditCardData) -> Unit,
            onError: (String) -> Unit
        ) {
            if (tasks.isEmpty()) {
                SecureLogger.d("SecureCreditCard", "All fields processed successfully")
                onSuccess(decryptedData)
                return
            }
            
            val (fieldName, encryptedString) = tasks.first()
            val remainingTasks = tasks.drop(1)
            
            
            val encryptedData = EnhancedCreditCardEncryption.EncryptedData.fromStorageString(encryptedString)
            if (encryptedData == null) {
                SecureLogger.w("SecureCreditCard", "Failed to parse field, skipping")
                decryptRemainingFields(remainingTasks, decryptedData, onSuccess, onError)
                return
            }
            
            try {
                // Use direct decryption without biometric prompt for subsequent fields
                val decryptedValue = SecureStorage.decrypt(encryptedData.encryptedData)
                
                if (decryptedValue != null) {
                    when (fieldName) {
                        "cardNumber" -> {
                            decryptedData.cardNumber = decryptedValue
                        }
                        "cvv" -> {
                            decryptedData.cvv = decryptedValue
                        }
                        "iban" -> {
                            decryptedData.iban = decryptedValue
                        }
                    }
                } else {
                    SecureLogger.w("SecureCreditCard", "Direct decryption failed")
                }
            } catch (e: Exception) {
                SecureLogger.e("SecureCreditCard", "Direct decryption failed", e)
            }
            
            // Continue with remaining tasks
            decryptRemainingFields(remainingTasks, decryptedData, onSuccess, onError)
        }
    }
    
    /**
     * Data class for holding decrypted sensitive information
     */
    data class DecryptedCreditCardData(
        var cardNumber: String? = null,
        var cvv: String? = null,
        var iban: String? = null
    )
}
