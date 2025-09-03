/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.model

import com.esposito.openwallet.core.util.SecureLogger
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.math.BigInteger

/**
 * Consolidated financial validation and formatting utilities
 * Replaces BankingUtils, IBANUtils, and CreditCardUtils to eliminate duplication
 */
object FinancialValidationUtils {
    
    private const val TAG = "FinancialValidationUtils"
    private val gson = Gson()
    
    // IBAN country code to length mapping (ISO 13616)
    private val ibanCountryLengths = mapOf(
        "AD" to 24, "AE" to 23, "AL" to 28, "AT" to 20, "AZ" to 28, "BA" to 20, "BE" to 16,
        "BG" to 22, "BH" to 22, "BR" to 29, "BY" to 28, "CH" to 21, "CR" to 22, "CY" to 28,
        "CZ" to 24, "DE" to 22, "DK" to 18, "DO" to 28, "EE" to 20, "EG" to 29, "ES" to 24,
        "FI" to 18, "FO" to 18, "FR" to 27, "GB" to 22, "GE" to 22, "GI" to 23, "GL" to 18,
        "GR" to 27, "GT" to 28, "HR" to 21, "HU" to 28, "IE" to 22, "IL" to 23, "IS" to 26,
        "IT" to 27, "JO" to 30, "KW" to 30, "KZ" to 20, "LB" to 28, "LC" to 32, "LI" to 21,
        "LT" to 20, "LU" to 20, "LV" to 21, "MC" to 27, "MD" to 24, "ME" to 22, "MK" to 19,
        "MR" to 27, "MT" to 31, "MU" to 30, "NL" to 18, "NO" to 15, "PK" to 24, "PL" to 28,
        "PS" to 29, "PT" to 25, "QA" to 29, "RO" to 24, "RS" to 22, "SA" to 24, "SE" to 24,
        "SI" to 19, "SK" to 24, "SM" to 27, "TN" to 24, "TR" to 26, "UA" to 29, "VG" to 24,
        "XK" to 20
    )

    // === CREDIT CARD VALIDATION ===
    
    /**
     * Detect credit card type from card number using BIN ranges
     */
    fun detectCardType(cardNumber: String): CreditCardType {
        val cleanNumber = cardNumber.replace(Regex("[^0-9]"), "")
        if (cleanNumber.isEmpty()) return CreditCardType.UNKNOWN
        
        val firstDigits = cleanNumber.take(6).toIntOrNull() ?: return CreditCardType.UNKNOWN
        
        return CreditCardType.entries.find { cardType ->
            cardType.binRanges.any { range -> firstDigits >= range.first && firstDigits <= range.last }
        } ?: CreditCardType.UNKNOWN
    }
    
    /**
     * Validate credit card number using Luhn algorithm
     */
    fun validateCardNumber(cardNumber: String): Boolean {
        val cleanNumber = cardNumber.replace(Regex("[^0-9]"), "")
        if (cleanNumber.length < 13 || cleanNumber.length > 19) return false
        
        var sum = 0
        var alternate = false
        for (i in cleanNumber.length - 1 downTo 0) {
            var digit = cleanNumber[i].digitToInt()
            if (alternate) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
            alternate = !alternate
        }
        return sum % 10 == 0
    }
    
    /**
     * Format card number with proper spacing
     */
    fun formatCardNumber(cardNumber: String): String {
        val cleanNumber = cardNumber.replace(Regex("[^0-9]"), "")
        return when {
            cleanNumber.isEmpty() -> ""
            cleanNumber.length <= 4 -> cleanNumber
            cleanNumber.length <= 8 -> "${cleanNumber.take(4)} ${cleanNumber.drop(4)}"
            cleanNumber.length <= 12 -> "${cleanNumber.take(4)} ${cleanNumber.substring(4, 8)} ${cleanNumber.drop(8)}"
            else -> "${cleanNumber.take(4)} ${cleanNumber.substring(4, 8)} ${cleanNumber.substring(8, 12)} ${cleanNumber.drop(12)}"
        }
    }
    
    /**
     * Mask card number for display
     */
    fun maskCardNumber(cardNumber: String): String {
        val cleanNumber = cardNumber.replace(Regex("[^0-9]"), "")
        return if (cleanNumber.length >= 4) {
            "•••• •••• •••• ${cleanNumber.takeLast(4)}"
        } else "•••• •••• •••• ••••"
    }

    // === IBAN VALIDATION ===
    
    /**
     * Validate IBAN using MOD-97 algorithm (ISO 13616)
     */
    fun validateIBAN(iban: String?): Boolean {
        if (iban.isNullOrBlank()) return false
        
        val cleanedIban = iban.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
        if (cleanedIban.length < 15) return false
        
        val countryCode = cleanedIban.take(2)
        if (!countryCode.matches(Regex("[A-Z]{2}"))) return false
        
        // Check expected length for country
        val expectedLength = ibanCountryLengths[countryCode]
        if (expectedLength != null && cleanedIban.length != expectedLength) return false
        
        // MOD-97 validation
        //val checkDigits = cleanedIban.substring(2, 4)
        val rearranged = cleanedIban.drop(4) + cleanedIban.take(4)
        
        val numericString = rearranged.map { char ->
            if (char.isDigit()) char.toString()
            else (char.code - 'A'.code + 10).toString()
        }.joinToString("")
        
        return try {
            val remainder = BigInteger(numericString).remainder(BigInteger("97"))
            remainder == BigInteger.ONE
        } catch (_: NumberFormatException) {
            false
        }
    }
    
    /**
     * Format IBAN with spaces for readability
     */
    fun formatIBAN(iban: String): String {
        val cleaned = iban.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
        return cleaned.chunked(4).joinToString(" ")
    }
    
    /**
     * Mask IBAN for secure display - shows first 4 chars and last 4 chars
     */
    fun maskIBAN(iban: String): String {
        val cleaned = iban.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
        return if (cleaned.length >= 8) {
            val first4 = cleaned.take(4)
            val last4 = cleaned.takeLast(4)
            val middleLength = (cleaned.length - 8).coerceAtLeast(0)
            val maskedMiddle = "•".repeat(middleLength)
            formatIBAN("$first4$maskedMiddle$last4")
        } else {
            // For shorter IBANs, mask everything except country code
            val countryCode = if (cleaned.length >= 2) cleaned.take(2) else cleaned
            val maskedLength = (cleaned.length - 2).coerceAtLeast(0)
            formatIBAN("$countryCode${"•".repeat(maskedLength)}")
        }
    }

    // === SWIFT CODE VALIDATION ===
    
    /**
     * Validate SWIFT/BIC code format
     */
    fun validateSWIFTCode(swift: String?): Boolean {
        if (swift.isNullOrBlank()) return true // Optional field
        
        val cleaned = swift.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
        if (cleaned.length != 8 && cleaned.length != 11) return false
        
        // Bank code (4 letters) + Country code (2 letters) + Location code (2 alphanumeric)
        if (!cleaned.take(4).matches(Regex("[A-Z]{4}"))) return false
        if (!cleaned.substring(4, 6).matches(Regex("[A-Z]{2}"))) return false
        if (!cleaned.substring(6, 8).matches(Regex("[A-Z0-9]{2}"))) return false
        
        // Branch code (3 alphanumeric, optional)
        if (cleaned.length == 11 && !cleaned.substring(8, 11).matches(Regex("[A-Z0-9]{3}"))) return false
        
        return true
    }
    
    /**
     * Format SWIFT code with spaces
     */
    fun formatSWIFTCode(swift: String): String {
        val cleaned = swift.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
        return when (cleaned.length) {
            8 -> "${cleaned.take(4)} ${cleaned.substring(4, 6)} ${cleaned.substring(6, 8)}"
            11 -> "${cleaned.take(4)} ${cleaned.substring(4, 6)} ${cleaned.substring(6, 8)} ${cleaned.substring(8, 11)}"
            else -> cleaned
        }
    }

    // === ABA ROUTING NUMBER VALIDATION ===
    
    /**
     * Validate ABA routing number (US banks)
     */
    fun validateABARoutingNumber(routingNumber: String?): Boolean {
        if (routingNumber.isNullOrBlank()) return true // Optional field
        
        val cleaned = routingNumber.replace(Regex("[^0-9]"), "")
        if (cleaned.length != 9) return false
        
        // ABA checksum algorithm
        val weights = intArrayOf(3, 7, 1, 3, 7, 1, 3, 7, 1)
        val sum = cleaned.mapIndexed { index, char ->
            char.digitToInt() * weights[index]
        }.sum()
        
        return sum % 10 == 0
    }
    
    /**
     * Format ABA routing number
     */
    fun formatABARoutingNumber(routingNumber: String): String {
        val cleaned = routingNumber.replace(Regex("[^0-9]"), "")
        return if (cleaned.length == 9) {
            "${cleaned.take(3)}-${cleaned.substring(3, 6)}-${cleaned.drop(6)}"
        } else cleaned
    }

    // === LEGACY SUPPORT ===
    
    /**
     * Extract credit card data from WalletPass (legacy support)
     */
    fun extractCreditCardData(pass: WalletPass): CreditCard? {
        if (pass.type != PassType.CREDIT_CARD) return null
        
        return try {
            val paymentCardData = gson.fromJson(pass.passData, PaymentCardData::class.java)
            paymentCardData?.let {
                CreditCard(
                    cardHolderName = it.cardholderName ?: "",
                    maskedCardNumber = maskCardNumber(it.cardNumber ?: ""),
                    cardType = detectCardType(it.cardNumber ?: ""),
                    issuerBank = it.bankName ?: "Unknown",
                    expiryMonth = 12, // Default values
                    expiryYear = 2025,
                    iban = it.iban?.let { ibanValue -> maskIBAN(ibanValue) }
                )
            }
        } catch (e: JsonSyntaxException) {
            SecureLogger.e(TAG, "Failed to parse credit card data: ${e.message}")
            null
        }
    }
}