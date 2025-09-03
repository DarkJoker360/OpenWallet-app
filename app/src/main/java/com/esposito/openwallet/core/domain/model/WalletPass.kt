/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.esposito.openwallet.R
import java.util.Date

@Entity(tableName = "wallet_passes")
data class WalletPass(
    @PrimaryKey val id: String,
    val type: PassType,
    val title: String,
    val description: String? = null,
    val organizationName: String,
    val logoText: String? = null,
    val foregroundColor: String? = null,
    val backgroundColor: String? = null,
    val labelColor: String? = null,
    val serialNumber: String? = null,
    val relevantDate: Date? = null,
    val expirationDate: Date? = null,
    val voided: Boolean = false,
    val passData: String, // JSON string of pass-specific data
    val barcodeData: String? = null,
    val barcodeFormat: BarcodeFormat? = null,
    val imageData: ByteArray? = null,
    val iconData: ByteArray? = null,
    val logoData: ByteArray? = null,
    val stripImageData: ByteArray? = null,
    val thumbnailData: ByteArray? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val filePath: String? = null,
    val isImported: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WalletPass

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

enum class PassType(
    val displayName: Int,
    val icon: String,
    val category: PassCategory,
    val supportsNFC: Boolean = false,
    val supportsLocationNotifications: Boolean = false,
    val supportsBarcodes: Boolean = true
) {
    // Payment
    CREDIT_CARD(R.string.pass_type_credit_card, "💳", PassCategory.PAYMENT),
    DEBIT_CARD(R.string.pass_type_debit_card, "💳", PassCategory.PAYMENT),
    GIFT_CARD(R.string.pass_type_gift_card, "🎁", PassCategory.RETAIL),
    STORE_CARD(R.string.pass_type_store_card, "🏪", PassCategory.RETAIL, supportsNFC = true),
    LOYALTY_CARD(R.string.pass_type_loyalty_card, "⭐", PassCategory.RETAIL, supportsNFC = true),
    MEMBERSHIP_CARD(R.string.pass_type_membership_card, "🏷️", PassCategory.RETAIL, supportsNFC = true),
    COUPON(R.string.pass_type_coupon, "🎫", PassCategory.RETAIL),
    
    // Transportation
    BOARDING_PASS(R.string.pass_type_flight, "✈️", PassCategory.TRAVEL, supportsLocationNotifications = true),
    TRANSIT_PASS(R.string.pass_type_transit_pass, "🚌", PassCategory.TRAVEL, supportsNFC = true, supportsLocationNotifications = true),
    TRAIN_TICKET(R.string.pass_type_train_ticket, "🚆", PassCategory.TRAVEL, supportsLocationNotifications = true),
    PARKING_PASS(R.string.pass_type_parking_pass, "🅿️", PassCategory.TRAVEL, supportsLocationNotifications = true),
    
    // Entertainment
    EVENT_TICKET(R.string.pass_type_event_ticket, "🎫", PassCategory.ENTERTAINMENT, supportsLocationNotifications = true),
    MOVIE_TICKET(R.string.pass_type_movie_ticket, "🎬", PassCategory.ENTERTAINMENT, supportsLocationNotifications = true),
    CONCERT_TICKET(R.string.pass_type_concert_ticket, "🎵", PassCategory.ENTERTAINMENT, supportsLocationNotifications = true),
    SPORTS_TICKET(R.string.pass_type_sports_ticket, "⚽", PassCategory.ENTERTAINMENT, supportsLocationNotifications = true),
    
    // Health & Medical
    HEALTH_CARD(R.string.pass_type_health_card, "🏥", PassCategory.HEALTH),
    VACCINATION_CARD(R.string.pass_type_vaccination_card, "💉", PassCategory.HEALTH),
    INSURANCE_CARD(R.string.pass_type_insurance_card, "🩺", PassCategory.HEALTH),
    PRESCRIPTION_CARD(R.string.pass_type_prescription_card, "💊", PassCategory.HEALTH),
    
    // Access & Security
    ACCESS_CARD(R.string.pass_type_access_card, "🔑", PassCategory.ACCESS, supportsNFC = true),
    EMPLOYEE_BADGE(R.string.pass_type_employee_badge, "👔", PassCategory.ACCESS, supportsNFC = true),
    STUDENT_ID(R.string.pass_type_student_id, "🎓", PassCategory.ACCESS, supportsNFC = true),
    HOTEL_KEY(R.string.pass_type_hotel_key, "🏨", PassCategory.ACCESS, supportsNFC = true),
    CAR_KEY(R.string.pass_type_car_key, "🚗", PassCategory.ACCESS, supportsNFC = true),
    
    // Government & Legal
    DRIVER_LICENSE(R.string.pass_type_driver_license, "🪪", PassCategory.GOVERNMENT),
    ID_CARD(R.string.pass_type_id_card, "🆔", PassCategory.GOVERNMENT),
    PASSPORT(R.string.pass_type_passport, "📘", PassCategory.GOVERNMENT),
    VISA(R.string.pass_type_visa, "📋", PassCategory.GOVERNMENT),
    
    // Generic & Others
    LIBRARY_CARD(R.string.pass_type_library_card, "📚", PassCategory.GENERIC),
    GYM_MEMBERSHIP(R.string.pass_type_gym_membership, "💪", PassCategory.GENERIC, supportsLocationNotifications = true),
    BUSINESS_CARD(R.string.pass_type_business_card, "💼", PassCategory.GENERIC),
    RESERVATION(R.string.pass_type_reservation, "📅", PassCategory.GENERIC, supportsLocationNotifications = true),
    GENERIC(R.string.pass_type_generic, "📄", PassCategory.GENERIC)
}

enum class PassCategory(val displayName: Int, val color: String) {
    PAYMENT(R.string.pass_category_payment, "#4CAF50"),
    RETAIL(R.string.pass_category_retail, "#FF9800"),
    TRAVEL(R.string.pass_category_travel, "#2196F3"),
    ENTERTAINMENT(R.string.pass_category_entertainment, "#E91E63"),
    HEALTH(R.string.pass_category_health, "#F44336"),
    ACCESS(R.string.pass_category_access, "#9C27B0"),
    GOVERNMENT(R.string.pass_category_government, "#607D8B"),
    GENERIC(R.string.pass_category_generic, "#795548")
}

enum class BarcodeFormat {
    QR,
    PDF417,
    AZTEC,
    CODE128,
    EAN13,
    UPC_A,
    DATA_MATRIX,
    NONE // For passes without barcodes
}

// Enhanced pass-specific data models following Google Wallet patterns

data class PaymentCardData(
    val cardNumber: String? = null,
    val expiryDate: String? = null,
    val cardholderName: String? = null,
    val cardType: String? = null, // Visa, Mastercard, etc.
    val bankName: String? = null,
    val iban: String? = null, // International Bank Account Number (optional)
    val swiftCode: String? = null, // SWIFT/BIC code (optional)
    val abaRoutingNumber: String? = null, // ABA routing number for US banks (optional)
    val balance: String? = null,
    val lastTransaction: String? = null,
    // Encrypted sensitive data
    val encryptedFullCardNumber: String? = null, // Full card number encrypted
    val encryptedCVV: String? = null, // CVV encrypted
    val encryptedIBAN: String? = null, // IBAN encrypted for security
    val maskedCardNumber: String? = null, // Masked version for display (•••• •••• •••• 1234)
    val maskedIBAN: String? = null // Masked IBAN for display (e.g., "DE89 •••• •••• •••• 0130 00")
)

data class LoyaltyCardData(
    val cardNumber: String? = null,
    val points: String? = null,
    val pointsLabel: String? = "Points",
    val tier: String? = null,
    val tierBenefits: List<String>? = null,
    val memberSince: Date? = null,
    val expirationDate: Date? = null,
    val nearbyOffers: List<String>? = null,
    val recentActivity: List<String>? = null
)

data class BoardingPassData(
    val transitType: String, // Air, Bus, Train, Ferry
    val gate: String? = null,
    val seat: String? = null,
    val boardingGroup: String? = null,
    val departureLocation: String? = null,
    val destinationLocation: String? = null,
    val flightNumber: String? = null,
    val confirmationCode: String? = null,
    val departureDate: Date? = null,
    val arrivalDate: Date? = null,
    val departureTime: String? = null,
    val arrivalTime: String? = null,
    val terminal: String? = null,
    val boardingTime: Date? = null,
    val aircraft: String? = null,
    val operatingCarrier: String? = null,
    val status: String? = null, // On Time, Delayed, Cancelled, Boarding
    val securityMessage: String? = null
)

data class EventTicketData(
    val eventName: String,
    val venue: String? = null,
    val venueAddress: String? = null,
    val eventDate: Date? = null,
    val eventTime: String? = null,
    val doorsOpen: String? = null,
    val section: String? = null,
    val row: String? = null,
    val seat: String? = null,
    val performer: String? = null,
    val genre: String? = null,
    val ageRestriction: String? = null,
    val dresscode: String? = null,
    val ticketType: String? = null, // General Admission, VIP, etc.
    val confirmationNumber: String? = null,
    val specialInstructions: String? = null
)

data class CouponData(
    val offer: String,
    val discountAmount: String? = null,
    val discountPercentage: String? = null,
    val minimumPurchase: String? = null,
    val expirationDate: Date? = null,
    val restrictions: String? = null,
    val promoCode: String? = null,
    val category: String? = null,
    val validStores: List<String>? = null,
    val maxUses: Int? = null,
    val usesRemaining: Int? = null,
    val transferable: Boolean = false
)

data class PassField(
    val key: String,
    val label: String? = null,
    val value: String,
    val textAlignment: TextAlignment = TextAlignment.NATURAL
)

enum class TextAlignment {
    NATURAL
}
