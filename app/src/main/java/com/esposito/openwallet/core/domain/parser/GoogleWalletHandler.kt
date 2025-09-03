/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.parser

import android.content.Context
import com.esposito.openwallet.R
import com.esposito.openwallet.core.domain.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonElement
import java.io.InputStream
import java.text.SimpleDateFormat
import java.text.ParseException
import java.util.*

/**
 * Handler for Google Wallet format passes (JSON-based)
 * Supports both Google Wallet Pay API and Google Wallet Class/Object formats
 */
class GoogleWalletHandler(
    private val gson: Gson,
    private val context: Context
) : PassHandler {
    
    override val formatName: String = "Google Wallet JSON"
    override val supportedExtensions: List<String> = listOf("json", "gwpass")
    override val supportedMimeTypes: List<String> = listOf(
        "application/json",
        "application/vnd.google.wallet+json"
    )
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    
    override fun canHandle(fileName: String?, mimeType: String?, inputStream: InputStream): Boolean {
        // Check file extension first - be more permissive with JSON files
        fileName?.let { name ->
            val extension = name.substringAfterLast('.', "").lowercase()
            if (supportedExtensions.contains(extension)) {
                return true // Accept any .json file
            }
        }
        
        // Check MIME type - accept any JSON MIME type
        mimeType?.let { type ->
            if (supportedMimeTypes.any { type.contains(it, ignoreCase = true) }) {
                return true
            }
        }
        
        // For files without clear extension/MIME type, try to parse as JSON
        return try {
            val content = inputStream.readBytes().toString(Charsets.UTF_8)
            val jsonObject = gson.fromJson(content, JsonObject::class.java)
            
            // Check if it's a Google Wallet format (class/object structure)
            if (jsonObject.has("class") && jsonObject.has("object")) {
                return true
            }
            
            // Check if it's a Google Wallet JWT format (iss/aud/payload structure)
            if (jsonObject.has("iss") || jsonObject.has("payload")) {
                return true
            }
            
            // Accept any valid JSON as we can create a generic pass from it
            true
        } catch (_: Exception) {
            false
        }
    }
    
    override fun parsePass(
        inputStream: InputStream,
        fileName: String?,
        metadata: Map<String, Any>
    ): WalletPass? {
        val content = inputStream.readBytes().toString(Charsets.UTF_8)
        
        // Try to parse as Google Wallet format first
        try {
            val googlePass = gson.fromJson(content, GoogleWalletPassData::class.java)
            if (googlePass != null && (googlePass.payload != null || googlePass.iss != null)) {
                return convertToWalletPass(googlePass, fileName)
            }
        } catch (_: Exception) {
            // If Google Wallet parsing fails, continue to other formats
        }
        
        // Try to parse as Google Wallet class/object format (your format)
        try {
            val classObjectFormat = gson.fromJson(content, GoogleWalletClassObjectFormat::class.java)
            if (classObjectFormat != null) {
                return parseGoogleWalletClassObjectFormat(classObjectFormat, fileName ?: "unknown.json")
            }
        } catch (_: Exception) {
            // Continue to other parsing attempts
        }
        
        // Try to parse as Google Wallet class/object format (your format)
        try {
            val jsonObject = gson.fromJson(content, JsonObject::class.java)
            if (jsonObject.has("class") && jsonObject.has("object")) {
                return parseGoogleWalletClassObject(jsonObject, fileName ?: "unknown.json")
            }
        } catch (_: Exception) {
            // Continue to generic parsing
        }
        
        // Fallback: Try to parse as generic JSON and create a basic pass
        try {
            val jsonObject = gson.fromJson(content, JsonObject::class.java)
            return createGenericPassFromJson(jsonObject, fileName ?: "unknown.json")
        } catch (e: Exception) {
            throw PassParsingException(
                "Failed to parse JSON file: ${e.message}",
                e,
                formatName,
                fileName
            )
        }
    }
    
    override fun validatePass(pass: WalletPass): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate required fields
        if (pass.title.isBlank()) {
            errors.add(context.getString(R.string.pass_title_required))
        }
        
        if (pass.organizationName.isBlank()) {
            errors.add(context.getString(R.string.organization_name_required))
        }
        
        // Check if pass has expired
        pass.expirationDate?.let { expiryDate ->
            if (expiryDate.before(Date())) {
                warnings.add(context.getString(R.string.pass_has_expired))
            }
        }
        
        if (pass.organizationName.isBlank()) {
            errors.add(context.getString(R.string.organization_name_required))
        }
        
        // Check if pass has expired
        pass.expirationDate?.let { expiryDate ->
            if (expiryDate.before(Date())) {
                warnings.add(context.getString(R.string.pass_has_expired))
            }
        }
        
        if (pass.organizationName.isBlank()) {
            errors.add(context.getString(R.string.organization_name_required))
        }
        
        // Google Wallet specific validations
        if (pass.serialNumber.isNullOrBlank()) {
            warnings.add("Google Wallet passes should have a serial number/ID")
        }
        
        // Check if pass has expired
        pass.expirationDate?.let { expiry ->
            if (expiry.before(Date())) {
                warnings.add(context.getString(R.string.pass_has_expired))
            }
        }
        
        return if (errors.isEmpty()) {
            if (warnings.isEmpty()) {
                ValidationResult.success()
            } else {
                ValidationResult.withWarnings(warnings)
            }
        } else {
            ValidationResult.failure(errors)
        }
    }
    
    private fun convertToWalletPass(googlePass: GoogleWalletPassData, fileName: String?): WalletPass? {
        val payload = googlePass.payload ?: return null
        
        // Try to extract pass from different object types
        val passObject = extractPassObject(payload)
        val passInfo = extractPassInfo(passObject)
        
        return WalletPass(
            id = passInfo.id ?: UUID.randomUUID().toString(),
            type = determinePassType(passObject),
            title = passInfo.title ?: context.getString(R.string.google_wallet_pass_default),
            description = passInfo.description,
            organizationName = passInfo.issuer ?: context.getString(R.string.google_wallet_default),
            logoText = passInfo.issuer,
            foregroundColor = passInfo.foregroundColor,
            backgroundColor = passInfo.backgroundColor,
            serialNumber = passInfo.id,
            relevantDate = parseGoogleDate(passInfo.validTimeInterval?.start?.date),
            expirationDate = parseGoogleDate(passInfo.validTimeInterval?.end?.date),
            voided = passInfo.state == "EXPIRED" || passInfo.state == "INACTIVE",
            passData = gson.toJson(createPassSpecificData(passObject)),
            barcodeData = extractBarcodeData(passInfo.barcode),
            barcodeFormat = mapGoogleBarcodeFormat(passInfo.barcode?.type),
            filePath = fileName,
            isImported = true
        )
    }
    
    private fun parseGoogleWalletClassObjectFormat(classObjectFormat: GoogleWalletClassObjectFormat, fileName: String): WalletPass {
        val classData = classObjectFormat.classData
        val objectData = classObjectFormat.objectData
        
        // Extract information from class data
        val issuerName = classData.issuerName
        val backgroundColor = classData.hexBackgroundColor
        
        // Determine pass type based on content
        val passType = when {
            classData.flightHeader != null -> PassType.BOARDING_PASS
            classData.eventName != null -> PassType.EVENT_TICKET
            else -> PassType.GENERIC
        }
        
        // Extract title based on pass type
        val title = when (passType) {
            PassType.BOARDING_PASS -> {
                val flightNumber = classData.flightHeader?.flightNumber
                val origin = classData.origin?.airportIataCode
                val destination = classData.destination?.airportIataCode
                if (flightNumber != null && origin != null && destination != null) {
                    "Flight $flightNumber ($origin → $destination)"
                } else {
                    context.getString(R.string.flight_boarding_pass_default)
                }
            }
            PassType.EVENT_TICKET -> classData.eventName?.defaultValue?.value ?: "Event Ticket"
            else -> "Google Wallet Pass"
        }
        
        // Extract description based on pass type
        val description = when (passType) {
            PassType.BOARDING_PASS -> {
                val passengerName = objectData.passengerName
                val confirmationCode = objectData.reservationInfo?.confirmationCode
                val seatNumber = objectData.boardingAndSeatingInfo?.seatNumber
                buildString {
                    if (passengerName != null) append("Passenger: $passengerName")
                    if (confirmationCode != null) {
                        if (isNotEmpty()) append(" • ")
                        append("Confirmation: $confirmationCode")
                    }
                    if (seatNumber != null) {
                        if (isNotEmpty()) append(" • ")
                        append("Seat: $seatNumber")
                    }
                }
            }
            PassType.EVENT_TICKET -> {
                val ticketHolder = objectData.ticketHolderName
                val ticketNumber = objectData.ticketNumber
                buildString {
                    if (ticketHolder != null) append("Ticket Holder: $ticketHolder")
                    if (ticketNumber != null) {
                        if (isNotEmpty()) append(" • ")
                        append("Ticket #: $ticketNumber")
                    }
                }
            }
            else -> "Google Wallet Pass"
        }
        
        // Extract barcode information
        val barcodeData = objectData.barcode?.value
        val barcodeType = objectData.barcode?.type
        val barcodeFormat = when (barcodeType) {
            "QR_CODE" -> BarcodeFormat.QR
            "PDF_417" -> BarcodeFormat.PDF417
            "AZTEC" -> BarcodeFormat.AZTEC
            "CODE_128" -> BarcodeFormat.CODE128
            else -> BarcodeFormat.QR
        }
        
        // Extract dates
        val departureDateTime = classData.localScheduledDepartureDateTime
        val relevantDate = departureDateTime?.let { parseGoogleWalletDate(it) }
        
        return WalletPass(
            id = objectData.id,
            type = passType,
            title = title,
            description = description.ifEmpty { "Google Wallet Pass" },
            organizationName = issuerName,
            logoText = issuerName,
            foregroundColor = "#FFFFFF", // Default white text
            backgroundColor = backgroundColor,
            serialNumber = objectData.id,
            relevantDate = relevantDate,
            expirationDate = null, // Google Wallet passes may not have explicit expiration
            voided = objectData.state == "EXPIRED" || objectData.state == "INACTIVE",
            passData = gson.toJson(classObjectFormat), // Store the entire JSON for reference
            barcodeData = barcodeData,
            barcodeFormat = barcodeFormat,
            filePath = fileName,
            isImported = true
        )
    }
    
    private fun parseGoogleWalletClassObject(jsonObject: JsonObject, fileName: String): WalletPass {
        val classObj = jsonObject.getAsJsonObject("class")
        val objectObj = jsonObject.getAsJsonObject("object")
        
        // Extract information from class object
        val issuerName = classObj.get("issuerName")?.asString 
            ?: classObj.getAsJsonObject("localizedIssuerName")?.getAsJsonObject("defaultValue")?.get("value")?.asString
            ?: context.getString(R.string.unknown_issuer_default)
        
        val backgroundColor = classObj.get("hexBackgroundColor")?.asString
        
        // Extract information from object
        val objectId = objectObj.get("id")?.asString ?: "unknown_object"
        val state = objectObj.get("state")?.asString ?: "ACTIVE"
        
        // Determine pass type based on content
        val passType = when {
            classObj.has("flightHeader") -> PassType.BOARDING_PASS
            classObj.has("eventName") -> PassType.EVENT_TICKET
            classObj.has("merchantName") -> PassType.STORE_CARD
            else -> PassType.GENERIC
        }
        
        // Extract title based on pass type
        val title = when (passType) {
            PassType.BOARDING_PASS -> {
                val flightHeader = classObj.getAsJsonObject("flightHeader")
                val flightNumber = flightHeader?.get("flightNumber")?.asString
                val origin = classObj.getAsJsonObject("origin")?.get("airportIataCode")?.asString
                val destination = classObj.getAsJsonObject("destination")?.get("airportIataCode")?.asString
                "Flight $flightNumber ($origin → $destination)"
            }
            PassType.EVENT_TICKET -> classObj.get("eventName")?.asString ?: "Event Ticket"
            PassType.STORE_CARD -> classObj.get("merchantName")?.asString ?: "Store Card"
            else -> "Google Wallet Pass"
        }
        
        // Extract description based on pass type
        val description = when (passType) {
            PassType.BOARDING_PASS -> {
                val passengerName = objectObj.get("passengerName")?.asString
                val confirmationCode = objectObj.getAsJsonObject("reservationInfo")?.get("confirmationCode")?.asString
                val seatNumber = objectObj.getAsJsonObject("boardingAndSeatingInfo")?.get("seatNumber")?.asString
                buildString {
                    if (passengerName != null) append("Passenger: $passengerName")
                    if (confirmationCode != null) {
                        if (isNotEmpty()) append(" • ")
                        append("Confirmation: $confirmationCode")
                    }
                    if (seatNumber != null) {
                        if (isNotEmpty()) append(" • ")
                        append("Seat: $seatNumber")
                    }
                }
            }
            else -> "Google Wallet Pass"
        }
        
        // Extract barcode information
        val barcodeObj = objectObj.getAsJsonObject("barcode")
        val barcodeData = barcodeObj?.get("value")?.asString
        val barcodeType = barcodeObj?.get("type")?.asString
        val barcodeFormat = when (barcodeType) {
            "QR_CODE" -> BarcodeFormat.QR
            "PDF_417" -> BarcodeFormat.PDF417
            "AZTEC" -> BarcodeFormat.AZTEC
            "CODE_128" -> BarcodeFormat.CODE128
            else -> BarcodeFormat.QR
        }
        
        // Extract dates
        val departureDateTime = classObj.get("localScheduledDepartureDateTime")?.asString
        val relevantDate = departureDateTime?.let { parseGoogleWalletDate(it) }
        
        return WalletPass(
            id = objectId,
            type = passType,
            title = title,
            description = description.ifEmpty { "Google Wallet Pass" },
            organizationName = issuerName,
            logoText = issuerName,
            foregroundColor = "#FFFFFF", // Default white text
            backgroundColor = backgroundColor,
            serialNumber = objectId,
            relevantDate = relevantDate,
            expirationDate = null, // Google Wallet passes may not have explicit expiration
            voided = state == "EXPIRED" || state == "INACTIVE",
            passData = gson.toJson(jsonObject), // Store the entire JSON for reference
            barcodeData = barcodeData,
            barcodeFormat = barcodeFormat,
            filePath = fileName,
            isImported = true
        )
    }
    
    private fun parseGoogleWalletDate(dateTimeString: String): Date? {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.parse(dateTimeString)
    }
    
    private fun createGenericPassFromJson(jsonObject: Any?, fileName: String): WalletPass {
        val gson = Gson()
        val jsonElement = jsonObject as? JsonElement ?: gson.toJsonTree(jsonObject)
        
        val jsonObj = if (jsonElement.isJsonObject) {
            jsonElement.asJsonObject
        } else {
            JsonObject()
        }
        
        // Extract common fields from generic JSON
        val id = extractStringField(jsonObj, listOf("id", "identifier", "passId", "serialNumber"))
        val title = extractStringField(jsonObj, listOf("title", "name", "description", "eventName", "organizationName"))
        val description = extractStringField(jsonObj, listOf("description", "details", "info", "note"))
        val serialNumber = extractStringField(jsonObj, listOf("serialNumber", "serial", "number", "id", "identifier"))
        
        // Try to extract dates
        val relevantDate = extractDateField(jsonObj, listOf("relevantDate", "startDate", "validFrom", "issueDate"))
        val expirationDate = extractDateField(jsonObj, listOf("expirationDate", "endDate", "validTo", "expiry"))
        
        // Check for voided/expired status
        val voided = extractBooleanField(jsonObj, listOf("voided", "expired", "invalid", "cancelled")) ?: 
                    extractStringField(jsonObj, listOf("status", "state"))?.let { 
                        it.equals("expired", true) || it.equals("voided", true) || it.equals("cancelled", true) 
                    } ?: false
        
        return WalletPass(
            id = id ?: "generic_${System.currentTimeMillis()}",
            type = PassType.GENERIC, // Use GENERIC pass type for unknown JSON
            title = title ?: "Generic Pass",
            description = description ?: "Imported from JSON",
            organizationName = extractStringField(jsonObj, listOf("organizationName", "issuer", "issuerName", "company", "organization")) ?: "Unknown",
            serialNumber = serialNumber ?: "unknown",
            relevantDate = relevantDate?.let { Date(it) },
            expirationDate = expirationDate?.let { Date(it) },
            voided = voided,
            passData = gson.toJson(jsonObj),
            barcodeData = extractStringField(jsonObj, listOf("barcode", "qrCode", "barcodeData", "code")),
            barcodeFormat = BarcodeFormat.QR, // Default to QR code
            filePath = fileName,
            isImported = true
        )
    }
    
    private fun extractStringField(jsonObj: JsonObject, fieldNames: List<String>): String? {
        for (fieldName in fieldNames) {
            val element = jsonObj.get(fieldName)
            if (element != null && !element.isJsonNull) {
                return element.asString
            }
        }
        return null
    }
    
    private fun extractDateField(jsonObj: JsonObject, fieldNames: List<String>): Long? {
        for (fieldName in fieldNames) {
            val element = jsonObj.get(fieldName)
            if (element != null && !element.isJsonNull) {
                val dateStr = element.asString
                // Try different date formats
                return parseGenericDate(dateStr)
            }
        }
        return null
    }
    
    private fun extractBooleanField(jsonObj: JsonObject, fieldNames: List<String>): Boolean? {
        for (fieldName in fieldNames) {
            val element = jsonObj.get(fieldName)
            if (element != null && !element.isJsonNull) {
                return if (element.isJsonPrimitive && element.asJsonPrimitive.isBoolean) {
                    element.asBoolean
                } else {
                    element.asString.toBoolean()
                }
            }
        }
        return null
    }
    
    private fun parseGenericDate(dateStr: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'" to Locale.ROOT,
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to Locale.ROOT,
            "yyyy-MM-dd HH:mm:ss" to Locale.ROOT,
            "yyyy-MM-dd" to Locale.ROOT,
            "MM/dd/yyyy" to Locale.US,
            "dd/MM/yyyy" to Locale.UK
        )
        
        for ((format, locale) in formats) {
            try {
                val sdf = SimpleDateFormat(format, locale)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(dateStr)?.time
            } catch (_: ParseException) {
                // Try next format
            }
        }
        
        // Try to parse as timestamp
        return try {
            dateStr.toLong()
        } catch (_: NumberFormatException) {
            null
        }
    }
    
    private fun extractPassObject(payload: GoogleWalletPayload): Any? {
        return payload.eventTicketObjects?.firstOrNull() ?:
               payload.loyaltyObjects?.firstOrNull() ?:
               payload.offerObjects?.firstOrNull() ?:
               payload.transitObjects?.firstOrNull() ?:
               payload.giftCardObjects?.firstOrNull()
    }
    
    private fun extractPassInfo(passObject: Any?): GooglePassInfo {
        return when (passObject) {
            is GoogleEventTicket -> GooglePassInfo(
                id = passObject.id,
                title = passObject.eventName?.defaultValue?.value,
                description = passObject.eventName?.defaultValue?.value,
                issuer = passObject.issuerName,
                state = passObject.state,
                barcode = passObject.barcode,
                validTimeInterval = passObject.validTimeInterval
            )
            is GoogleLoyaltyObject -> GooglePassInfo(
                id = passObject.id,
                title = passObject.programName?.defaultValue?.value,
                description = passObject.programName?.defaultValue?.value,
                issuer = passObject.issuerName,
                state = passObject.state,
                barcode = passObject.barcode,
                validTimeInterval = passObject.validTimeInterval
            )
            is GoogleOfferObject -> GooglePassInfo(
                id = passObject.id,
                title = passObject.provider,
                description = passObject.title,
                issuer = passObject.provider,
                state = passObject.state,
                barcode = passObject.barcode,
                validTimeInterval = passObject.validTimeInterval
            )
            else -> GooglePassInfo()
        }
    }
    
    private fun determinePassType(passObject: Any?): PassType {
        return when (passObject) {
            is GoogleEventTicket -> PassType.EVENT_TICKET
            is GoogleLoyaltyObject -> PassType.LOYALTY_CARD
            is GoogleOfferObject -> PassType.COUPON
            is GoogleTransitObject -> PassType.TRANSIT_PASS
            is GoogleGiftCardObject -> PassType.GIFT_CARD
            else -> PassType.GENERIC
        }
    }
    
    private fun createPassSpecificData(passObject: Any?): Map<String, Any> {
        return when (passObject) {
            is GoogleEventTicket -> mapOf(
                "eventName" to (passObject.eventName?.defaultValue?.value ?: ""),
                "venue" to (passObject.venue?.name?.defaultValue?.value ?: ""),
                "seat" to (passObject.seatInfo?.seat?.defaultValue?.value ?: ""),
                "section" to (passObject.seatInfo?.section?.defaultValue?.value ?: ""),
                "row" to (passObject.seatInfo?.row?.defaultValue?.value ?: ""),
                "eventDateTime" to (passObject.eventDateTime?.date ?: "")
            )
            is GoogleLoyaltyObject -> mapOf(
                "programName" to (passObject.programName?.defaultValue?.value ?: ""),
                "accountName" to (passObject.accountName ?: ""),
                "accountId" to (passObject.accountId ?: "")
            )
            else -> mapOf("rawData" to (passObject ?: emptyMap<String, Any>()))
        }
    }
    
    private fun extractBarcodeData(barcode: GoogleBarcode?): String? {
        return barcode?.value
    }
    
    private fun mapGoogleBarcodeFormat(type: String?): BarcodeFormat {
        return when (type?.uppercase()) {
            "QR_CODE" -> BarcodeFormat.QR
            "PDF_417" -> BarcodeFormat.PDF417
            "AZTEC" -> BarcodeFormat.AZTEC
            "CODE_128" -> BarcodeFormat.CODE128
            "DATA_MATRIX" -> BarcodeFormat.DATA_MATRIX
            "UPC_A" -> BarcodeFormat.UPC_A
            "EAN_13" -> BarcodeFormat.EAN13
            else -> BarcodeFormat.QR
        }
    }
    
    private fun parseGoogleDate(dateString: String?): Date? {
        return dateString?.let {
            dateFormat.parse(it)
        }
    }
}

// Google Wallet data structures
data class GoogleWalletPassData(
    val iss: String?,
    val aud: String?,
    val typ: String?,
    val payload: GoogleWalletPayload?
)

data class GoogleWalletPayload(
    val eventTicketObjects: List<GoogleEventTicket>?,
    val loyaltyObjects: List<GoogleLoyaltyObject>?,
    val offerObjects: List<GoogleOfferObject>?,
    val transitObjects: List<GoogleTransitObject>?,
    val giftCardObjects: List<GoogleGiftCardObject>?
)

data class GoogleEventTicket(
    val id: String?,
    val issuerName: String?,
    val state: String?,
    val barcode: GoogleBarcode?,
    val validTimeInterval: GoogleTimeInterval?,
    val eventName: GoogleLocalizedString?,
    val venue: GoogleVenue?,
    val eventDateTime: GoogleDateTime?,
    val seatInfo: GoogleSeatInfo?
)

data class GoogleLoyaltyObject(
    val id: String?,
    val issuerName: String?,
    val state: String?,
    val barcode: GoogleBarcode?,
    val validTimeInterval: GoogleTimeInterval?,
    val programName: GoogleLocalizedString?,
    val accountName: String?,
    val accountId: String?
)

data class GoogleOfferObject(
    val id: String?,
    val provider: String?,
    val title: String?,
    val state: String?,
    val barcode: GoogleBarcode?,
    val validTimeInterval: GoogleTimeInterval?
)

data class GoogleTransitObject(
    val id: String?,
    val state: String?,
    val barcode: GoogleBarcode?
)

data class GoogleGiftCardObject(
    val id: String?,
    val state: String?,
    val barcode: GoogleBarcode?
)

data class GoogleBarcode(
    val type: String?,
    val value: String?,
    val alternateText: String?
)

data class GoogleTimeInterval(
    val start: GoogleDateTime?,
    val end: GoogleDateTime?
)

data class GoogleDateTime(
    val date: String?
)

data class GoogleLocalizedString(
    val defaultValue: GoogleTranslatedString?
)

data class GoogleTranslatedString(
    val language: String?,
    val value: String?
)

data class GoogleVenue(
    val name: GoogleLocalizedString?,
    val address: GoogleLocalizedString?
)

data class GoogleSeatInfo(
    val seat: GoogleLocalizedString?,
    val row: GoogleLocalizedString?,
    val section: GoogleLocalizedString?
)

// Helper data class for extracting common pass info
private data class GooglePassInfo(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val issuer: String? = null,
    val state: String? = null,
    val barcode: GoogleBarcode? = null,
    val validTimeInterval: GoogleTimeInterval? = null,
    val foregroundColor: String? = null,
    val backgroundColor: String? = null
)
