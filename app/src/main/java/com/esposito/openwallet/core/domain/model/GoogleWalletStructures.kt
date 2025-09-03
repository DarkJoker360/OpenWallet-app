/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Data structures for Google Wallet passes based on the Google Wallet API
 * These structures follow the format used by the Google Wallet API
 */

// Common structures
data class GoogleWalletTranslatedString(
    val language: String,
    val value: String
)

data class GoogleWalletLocalizedString(
    val defaultValue: GoogleWalletTranslatedString,
    val translatedValues: List<GoogleWalletTranslatedString>? = null
)

data class GoogleWalletImageUri(
    val uri: String
)

data class GoogleWalletImage(
    val sourceUri: GoogleWalletImageUri,
    val contentDescription: GoogleWalletLocalizedString? = null
)

data class GoogleWalletBarcode(
    val type: String, // QR_CODE, PDF_417, AZTEC, CODE_128, etc.
    val value: String,
    @SerializedName("renderEncoding")
    val renderEncoding: String? = "UTF_8",
    val alternateText: String? = null,
    val showCodeText: GoogleWalletLocalizedString? = null
)

data class GoogleWalletDateTime(
    val date: String
)

data class GoogleWalletTimeInterval(
    val start: GoogleWalletDateTime? = null,
    val end: GoogleWalletDateTime? = null
)

data class GoogleWalletLatLongPoint(
    val latitude: Double,
    val longitude: Double
)

// Class/Object structure (as used in your JSON example)
data class GoogleWalletClassObjectFormat(
    @SerializedName("class")
    val classData: GoogleWalletClass,
    @SerializedName("object")
    val objectData: GoogleWalletObject
)

data class GoogleWalletClass(
    val id: String,
    val issuerName: String,
    val localizedIssuerName: GoogleWalletLocalizedString? = null,
    val reviewStatus: String? = "UNDER_REVIEW",
    val hexBackgroundColor: String? = null,
    val heroImage: GoogleWalletImage? = null,
    
    // Flight specific
    val flightHeader: GoogleWalletFlightHeader? = null,
    val origin: GoogleWalletAirport? = null,
    val destination: GoogleWalletAirport? = null,
    val localScheduledDepartureDateTime: String? = null,
    
    // Event specific
    val eventName: GoogleWalletLocalizedString? = null,
    val venue: GoogleWalletVenue? = null,
    val dateTime: GoogleWalletEventDateTime? = null,
    
    // Generic fields
    val logo: GoogleWalletImage? = null,
    val locations: List<GoogleWalletLatLongPoint>? = null
)

data class GoogleWalletObject(
    val id: String,
    val classId: String,
    val state: String = "ACTIVE", // ACTIVE, COMPLETED, EXPIRED, INACTIVE
    val barcode: GoogleWalletBarcode? = null,
    
    // Flight specific
    val passengerName: String? = null,
    val reservationInfo: GoogleWalletReservationInfo? = null,
    val boardingAndSeatingInfo: GoogleWalletBoardingAndSeatingInfo? = null,
    
    // Event specific
    val ticketHolderName: String? = null,
    val ticketNumber: String? = null,
    val seatInfo: GoogleWalletSeatInfo? = null,
    
    // Generic fields
    val validTimeInterval: GoogleWalletTimeInterval? = null,
    val hexBackgroundColor: String? = null,
    val locations: List<GoogleWalletLatLongPoint>? = null
)

// Flight specific structures
data class GoogleWalletFlightHeader(
    val carrier: GoogleWalletCarrier? = null,
    val flightNumber: String? = null
)

data class GoogleWalletCarrier(
    val carrierIataCode: String? = null,
    val airlineLogo: GoogleWalletImage? = null
)

data class GoogleWalletAirport(
    val airportIataCode: String? = null,
    val terminal: String? = null,
    val gate: String? = null
)

data class GoogleWalletReservationInfo(
    val confirmationCode: String? = null,
    val eticketNumber: String? = null
)

data class GoogleWalletBoardingAndSeatingInfo(
    val boardingGroup: String? = null,
    val seatNumber: String? = null,
    val seatClass: String? = null
)

// Event specific structures
data class GoogleWalletVenue(
    val name: GoogleWalletLocalizedString,
    val address: GoogleWalletLocalizedString
)

data class GoogleWalletEventDateTime(
    val doorsOpen: String? = null,
    val start: String? = null,
    val end: String? = null
)

data class GoogleWalletSeatInfo(
    val seat: GoogleWalletLocalizedString? = null,
    val row: GoogleWalletLocalizedString? = null,
    val section: GoogleWalletLocalizedString? = null,
    val gate: GoogleWalletLocalizedString? = null
)
