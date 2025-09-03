/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * Unified credit card model that serves as both entity and data class
 * Consolidated from CreditCard, CreditCardData, and CreditCardEntity
 */
@Entity(tableName = "credit_cards")
@Immutable
data class CreditCard(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cardHolderName: String,
    val maskedCardNumber: String, // "****-****-****-1234" 
    val cardType: CreditCardType,
    val issuerBank: String,
    val expiryMonth: Int, // 1-12
    val expiryYear: Int, // 4-digit year
    val cardNickname: String? = null,
    val isActive: Boolean = true,
    val isPrimary: Boolean = false,
    val contactlessEnabled: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val iban: String? = null, // Masked IBAN (e.g., "DE89 **** **** **** 0000")
    val swiftCode: String? = null,
    val abaRoutingNumber: String? = null,
    
    // Encrypted sensitive data (optional)
    val encryptedFullCardNumber: String? = null,
    val encryptedCVV: String? = null,
    val encryptedIBAN: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CreditCard
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "CreditCard(id='$id', cardHolderName='$cardHolderName', maskedCardNumber='$maskedCardNumber', cardType=$cardType)"
    }
}

/**
 * Credit card types/networks
 */
enum class CreditCardType(
    val displayName: String,
    val icon: String,
    val primaryColor: String,
    val binRanges: List<IntRange> // Bank Identification Number ranges
) {
    VISA("Visa", "💳", "#1A1F71", listOf(4000..4999)),
    MASTERCARD("Mastercard", "💳", "#EB001B", listOf(5100..5599, 2221..2720)),
    AMERICAN_EXPRESS("American Express", "💳", "#006FCF", listOf(3400..3499, 3700..3799)),
    DISCOVER("Discover", "💳", "#FF6000", listOf(6011..6011, 6221..6229, 6440..6499, 6500..6599)),
    DINERS_CLUB("Diners Club", "💳", "#0079BE", listOf(3000..3059, 3600..3699, 3800..3899)),
    JCB("JCB", "💳", "#006633", listOf(3528..3589)),
    UNIONPAY("UnionPay", "💳", "#E21836", listOf(6200..6299)),
    MAESTRO("Maestro", "💳", "#6C2C2F", listOf(5018..5018, 5020..5020, 5038..5038, 5893..5893)),
    UNKNOWN("Unknown", "💳", "#6B7280", emptyList())
}
