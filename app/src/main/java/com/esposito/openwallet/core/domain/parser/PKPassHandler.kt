/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.parser

import com.esposito.openwallet.core.domain.model.*
import com.esposito.openwallet.core.util.BarcodeFormatMapper
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipInputStream

/**
 * Handler for Apple Wallet PKPass files (.pkpass format)
 */
class PKPassHandler(
    private val gson: Gson
) : PassHandler {
    
    override val formatName: String = "PKPass"
    override val supportedExtensions: List<String> = listOf("pkpass")
    override val supportedMimeTypes: List<String> = listOf(
        "application/vnd.apple.pkpass",
        "application/x-apple-pkpass"
    )
    
    private val dateFormats = listOf(
        // With seconds
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),    // 2025-01-18T13:00:00+01:00
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US), // 2025-01-18T13:00:00.123+01:00
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),      // 2025-01-18T13:00:00+0100
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US),  // 2025-01-18T13:00:00.123+0100
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { 
            timeZone = TimeZone.getTimeZone("UTC") 
        },                                                           // 2025-01-18T13:00:00Z
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { 
            timeZone = TimeZone.getTimeZone("UTC") 
        },                                                           // 2025-01-18T13:00:00.123Z
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),       // 2025-01-18T13:00:00
        // WITHOUT seconds (Ryanair format!)
        SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX", Locale.US),       // 2025-01-18T13:00+01:00
        SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.US),         // 2025-01-18T13:00+0100
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply { 
            timeZone = TimeZone.getTimeZone("UTC") 
        },                                                           // 2025-12-26T16:20Z (Ryanair!)
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US),          // 2025-01-18T13:00
        // Date only
        SimpleDateFormat("yyyy-MM-dd", Locale.US)                    // 2025-01-18
    )
    
    override fun canHandle(fileName: String?, mimeType: String?, inputStream: InputStream): Boolean {
        // Check file extension
        fileName?.let { name ->
            if (supportedExtensions.any { ext -> 
                name.endsWith(".$ext", ignoreCase = true) 
            }) {
                return true
            }
        }
        
        // Check MIME type
        mimeType?.let { type ->
            if (supportedMimeTypes.contains(type)) {
                return true
            }
        }

        // Create a new ZipInputStream without affecting the original stream
        val zipStream = ZipInputStream(inputStream)
        var hasPassJson = false

        // Only check first few entries for efficiency
        var entryCount = 0
        var entry = zipStream.nextEntry
        while (entry != null && entryCount < 10) {
            if (entry.name == "pass.json") {
                hasPassJson = true
                break
            }
            zipStream.closeEntry()
            entry = zipStream.nextEntry
            entryCount++
        }

        return hasPassJson
    }
    
    override fun parsePass(
        inputStream: InputStream,
        fileName: String?,
        metadata: Map<String, Any>
    ): WalletPass? {
        return try {
            val zipInputStream = ZipInputStream(inputStream)
            var passJson: String? = null
            var openWalletJson: String? = null
            val images = mutableMapOf<String, ByteArray>()

            var entry = zipInputStream.nextEntry
            while (entry != null) {
                when {
                    entry.name == "pass.json" -> {
                        passJson = zipInputStream.readBytes().toString(Charsets.UTF_8)
                    }
                    entry.name == "openwallet.json" -> {
                        openWalletJson = zipInputStream.readBytes().toString(Charsets.UTF_8)
                    }
                    entry.name.endsWith(".png") -> {
                        images[entry.name] = zipInputStream.readBytes()
                    }
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            val sidecarResult = openWalletJson?.let { sidecar ->
                restoreFromOpenWalletSidecar(sidecar, images, fileName)
            }

            sidecarResult ?: passJson?.let { json ->
                parsePassJson(json, images, fileName)
            }
        } catch (e: Exception) {
            throw PassParsingException(
                "Failed to parse PKPass file: ${e.message}",
                e,
                formatName,
                fileName
            )
        }
    }

    private fun restoreFromOpenWalletSidecar(
        sidecarJson: String,
        images: Map<String, ByteArray>,
        fileName: String?
    ): WalletPass? {
        return try {
            val json = gson.fromJson(sidecarJson, JsonObject::class.java) ?: return null
            val format = json.get("format")?.asString
            if (format == null || !format.equals("openwallet", ignoreCase = true)) return null

            val passType = try {
                PassType.valueOf(json.get("type")?.asString ?: "GENERIC")
            } catch (_: Exception) {
                PassType.GENERIC
            }

            val barcodeFormat = try {
                val fmt = json.get("barcodeFormat")?.asString
                if (fmt != null) BarcodeFormat.valueOf(fmt) else BarcodeFormat.NONE
            } catch (_: Exception) {
                BarcodeFormat.NONE
            }

            WalletPass(
                id = json.get("id")?.asString ?: UUID.randomUUID().toString(),
                type = passType,
                title = json.get("title")?.asString ?: "Imported Pass",
                description = json.get("description")?.asString,
                organizationName = json.get("organizationName")?.asString ?: "Unknown",
                logoText = json.get("logoText")?.asString,
                foregroundColor = json.get("foregroundColor")?.asString,
                backgroundColor = json.get("backgroundColor")?.asString,
                labelColor = json.get("labelColor")?.asString,
                serialNumber = json.get("serialNumber")?.asString,
                relevantDate = parseDate(json.get("relevantDate")?.asString),
                expirationDate = parseDate(json.get("expirationDate")?.asString),
                voided = json.get("voided")?.asBoolean ?: false,
                passData = json.get("passData")?.asString ?: "{}",
                barcodeData = json.get("barcodeData")?.asString,
                barcodeFormat = barcodeFormat,
                imageData = images["background.png"] ?: images["background@2x.png"],
                iconData = images["icon.png"] ?: images["icon@2x.png"],
                logoData = images["logo.png"] ?: images["logo@2x.png"],
                stripImageData = images["strip.png"] ?: images["strip@2x.png"],
                thumbnailData = images["thumbnail.png"] ?: images["thumbnail@2x.png"],
                filePath = fileName,
                isImported = json.get("isImported")?.asBoolean ?: true,
                createdAt = parseDate(json.get("createdAt")?.asString) ?: Date(),
                updatedAt = parseDate(json.get("updatedAt")?.asString) ?: Date()
            )
        } catch (_: Exception) {
            null
        }
    }
    
    override fun validatePass(pass: WalletPass): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Basic validation
        if (pass.title.isBlank()) {
            errors.add("Pass title is required")
        }
        
        if (pass.organizationName.isBlank()) {
            errors.add("Organization name is required")
        }
        
        // Warnings for missing optional data
        if (pass.barcodeData == null) {
            warnings.add("Pass has no barcode data")
        }
        
        if (pass.iconData == null && pass.logoData == null) {
            warnings.add("Pass has no icon or logo images")
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

    private fun parsePassJson(json: String, images: Map<String, ByteArray>, fileName: String?): WalletPass? {
        val pkPass = gson.fromJson(json, PKPassData::class.java)
        val passType = determinePassType(pkPass)
        val barcodeInfo = pkPass.barcodes?.firstOrNull()
        val title = generatePassTitle(pkPass, passType)

        return WalletPass(
            id = pkPass.serialNumber ?: UUID.randomUUID().toString(),
            type = passType,
            title = title,
            description = pkPass.description,
            organizationName = pkPass.organizationName,
            logoText = pkPass.logoText,
            foregroundColor = pkPass.foregroundColor,
            backgroundColor = pkPass.backgroundColor,
            labelColor = pkPass.labelColor,
            serialNumber = pkPass.serialNumber,
            relevantDate = parseDate(pkPass.relevantDate),
            expirationDate = parseDate(pkPass.expirationDate),
            voided = pkPass.voided ?: false,
            passData = gson.toJson(createPassSpecificData(pkPass, passType)),
            barcodeData = barcodeInfo?.message,
            barcodeFormat = if (barcodeInfo?.message != null) {
                BarcodeFormatMapper.fromPkPassFormat(barcodeInfo.format)
            } else {
                BarcodeFormat.NONE
            },
            imageData = images["background.png"] ?: images["background@2x.png"],
            iconData = images["icon.png"] ?: images["icon@2x.png"],
            logoData = images["logo.png"] ?: images["logo@2x.png"],
            stripImageData = images["strip.png"] ?: images["strip@2x.png"],
            thumbnailData = images["thumbnail.png"] ?: images["thumbnail@2x.png"],
            filePath = fileName,
            isImported = true
        )
    }

    private fun determinePassType(pkPass: PKPassData): PassType {
        return when {
            pkPass.boardingPass != null -> PassType.BOARDING_PASS
            pkPass.coupon != null -> PassType.COUPON
            pkPass.eventTicket != null -> PassType.EVENT_TICKET
            pkPass.storeCard != null -> PassType.STORE_CARD
            pkPass.generic != null -> PassType.GENERIC
            else -> PassType.GENERIC
        }
    }
    
    /**
     * Generate a meaningful title for the pass based on its type and content
     */
    private fun generatePassTitle(pkPass: PKPassData, passType: PassType): String {
        return when (passType) {
            PassType.BOARDING_PASS -> {
                val bp = pkPass.boardingPass
                val allFields = bp?.headerFields.orEmpty() + 
                               bp?.primaryFields.orEmpty() + 
                               bp?.secondaryFields.orEmpty() + 
                               bp?.auxiliaryFields.orEmpty()
                
                val origin = findFieldValue(bp?.primaryFields, "origin", "departure", "from")
                val destination = findFieldValue(bp?.primaryFields, "destination", "arrival", "to")
                val flightNumber = findFieldValue(allFields, "flight", "flightNumber", "flightNo")
                
                when {
                    // "FR4238 · BRI → WMI" format
                    flightNumber != null && origin != null && destination != null -> 
                        "$flightNumber · $origin → $destination"
                    // "BRI → WMI" format
                    origin != null && destination != null -> 
                        "$origin → $destination"
                    // "FR4238" format
                    flightNumber != null -> 
                        flightNumber
                    // Fallback to organization name
                    else -> 
                        pkPass.organizationName
                }
            }
            PassType.EVENT_TICKET -> {
                val et = pkPass.eventTicket
                findFieldValue(et?.primaryFields, "event", "eventName") 
                    ?: pkPass.description 
                    ?: pkPass.organizationName
            }
            else -> {
                // For other types, use description or organization name
                val desc = pkPass.description
                if (desc != null && !desc.lowercase().contains("pass") && desc.length > 3) {
                    desc
                } else {
                    pkPass.organizationName
                }
            }
        }
    }

    private fun createPassSpecificData(pkPass: PKPassData, passType: PassType): Any {
        return when (passType) {
            PassType.BOARDING_PASS -> {
                val bp = pkPass.boardingPass
                
                // Convert all fields to PassFieldData for flexible display
                val headerFieldsData = convertFields(bp?.headerFields)
                val primaryFieldsData = convertFields(bp?.primaryFields)
                val secondaryFieldsData = convertFields(bp?.secondaryFields)
                val auxiliaryFieldsData = convertFields(bp?.auxiliaryFields)
                val backFieldsData = convertFields(bp?.backFields)

                val allFields = bp?.headerFields.orEmpty() + 
                               bp?.primaryFields.orEmpty() + 
                               bp?.secondaryFields.orEmpty() + 
                               bp?.auxiliaryFields.orEmpty() +
                               bp?.backFields.orEmpty()
                
                BoardingPassData(
                    transitType = bp?.transitType ?: "",
                    // Primary fields - search for origin/destination
                    departureLocation = findFieldValue(bp?.primaryFields, "origin", "departure", "from", "departureStation"),
                    destinationLocation = findFieldValue(bp?.primaryFields, "destination", "arrival", "to", "arrivalStation"),
                    // Passenger info - can be in auxiliary or secondary fields  
                    passengerName = findFieldValue(allFields, "passenger", "passengerName", "name", "traveler", "traveller"),
                    // Flight details
                    flightNumber = findFieldValue(allFields, "flight", "flightNumber", "flightNo", "trainNumber", "busNumber"),
                    confirmationCode = findFieldValue(allFields, "recordLocator", "confirmationCode", "confirmation", "booking", "pnr", "bookingRef"),
                    seat = findFieldValue(allFields, "seat", "seatNumber", "seatNo"),
                    gate = findFieldValue(allFields, "gate", "gateNumber"),
                    boardingGroup = findFieldValue(allFields, "boardingGroup", "group", "zone"),
                    terminal = findFieldValue(allFields, "terminal", "terminalNumber"),
                    // Times
                    departureTime = findFieldValue(allFields, "departTime", "departureTime", "departure"),
                    boardingTime = findFieldValue(allFields, "boardingTime", "boarding"),
                    gateCloseTime = findFieldValue(allFields, "gateClose", "gateCloseTime"),
                    // Additional info
                    boardingDoor = findFieldValue(allFields, "door", "boardingDoor"),
                    queue = findFieldValue(allFields, "queue", "queueName", "priority"),
                    sequence = findFieldValue(allFields, "seq", "sequence", "sequenceNumber"),
                    // Raw fields for flexible display
                    headerFields = headerFieldsData,
                    primaryFields = primaryFieldsData,
                    secondaryFields = secondaryFieldsData,
                    auxiliaryFields = auxiliaryFieldsData,
                    backFields = backFieldsData
                )
            }
            PassType.EVENT_TICKET -> {
                val et = pkPass.eventTicket
                EventTicketData(
                    eventName = getFieldValue(et?.primaryFields, "event") ?: pkPass.description ?: "",
                    venue = getFieldValue(et?.secondaryFields, "venue"),
                    eventDate = parseDate(getFieldValue(et?.headerFields, "date")),
                    section = getFieldValue(et?.auxiliaryFields, "section"),
                    row = getFieldValue(et?.auxiliaryFields, "row"),
                    seat = getFieldValue(et?.auxiliaryFields, "seat")
                )
            }
            PassType.COUPON -> {
                val coupon = pkPass.coupon
                CouponData(
                    offer = getFieldValue(coupon?.primaryFields, "offer") ?: pkPass.description ?: "",
                    expirationDate = parseDate(pkPass.expirationDate),
                    restrictions = getFieldValue(coupon?.secondaryFields, "restrictions"),
                    promoCode = getFieldValue(coupon?.auxiliaryFields, "code"),
                    discountAmount = getFieldValue(coupon?.headerFields, "discount")
                )
            }
            PassType.STORE_CARD -> {
                val sc = pkPass.storeCard
                LoyaltyCardData(
                    cardNumber = getFieldValue(sc?.secondaryFields, "cardNumber"),
                    points = getFieldValue(sc?.auxiliaryFields, "points"),
                    tier = getFieldValue(sc?.headerFields, "tier")
                )
            }
            else -> mapOf("raw" to pkPass)
        }
    }

    private fun getFieldValue(fields: List<PKPassField>?, key: String): String? {
        return fields?.find { it.key == key }?.value?.toString()
    }
    
    /**
     * Search for field value using multiple possible key names
     */
    private fun findFieldValue(fields: List<PKPassField>?, vararg possibleKeys: String): String? {
        if (fields == null) return null
        for (key in possibleKeys) {
            val field = fields.find { it.key.equals(key, ignoreCase = true) }
            if (field != null) {
                return field.value?.toString()
            }
        }
        return null
    }
    
    /**
     * Convert PKPass fields to PassFieldData format for storage
     */
    private fun convertFields(fields: List<PKPassField>?): List<PassFieldData> {
        return fields?.map { field ->
            PassFieldData(
                key = field.key,
                label = field.label,
                value = field.value?.toString() ?: "",
                dateValue = parseDate(field.value?.toString())
            )
        } ?: emptyList()
    }

    private fun parseDate(dateString: String?): Date? {
        if (dateString == null) return null
        for (format in dateFormats) {
            try {
                return format.parse(dateString)
            } catch (e: Exception) {
            }
        }
        return null
    }

}

// PKPass JSON structure data classes
data class PKPassData(
    val description: String?,
    @SerializedName("organizationName") val organizationName: String,
    @SerializedName("passTypeIdentifier") val passTypeIdentifier: String,
    @SerializedName("serialNumber") val serialNumber: String?,
    @SerializedName("teamIdentifier") val teamIdentifier: String,
    @SerializedName("formatVersion") val formatVersion: Int,
    @SerializedName("logoText") val logoText: String?,
    @SerializedName("foregroundColor") val foregroundColor: String?,
    @SerializedName("backgroundColor") val backgroundColor: String?,
    @SerializedName("labelColor") val labelColor: String?,
    @SerializedName("relevantDate") val relevantDate: String?,
    @SerializedName("expirationDate") val expirationDate: String?,
    val voided: Boolean?,
    val barcodes: List<PKPassBarcode>?,
    @SerializedName("boardingPass") val boardingPass: PKPassStructure?,
    val coupon: PKPassStructure?,
    @SerializedName("eventTicket") val eventTicket: PKPassStructure?,
    @SerializedName("storeCard") val storeCard: PKPassStructure?,
    val generic: PKPassStructure?
)

data class PKPassStructure(
    @SerializedName("transitType") val transitType: String?,
    @SerializedName("headerFields") val headerFields: List<PKPassField>?,
    @SerializedName("primaryFields") val primaryFields: List<PKPassField>?,
    @SerializedName("secondaryFields") val secondaryFields: List<PKPassField>?,
    @SerializedName("auxiliaryFields") val auxiliaryFields: List<PKPassField>?,
    @SerializedName("backFields") val backFields: List<PKPassField>?
)

data class PKPassField(
    val key: String,
    val label: String?,
    val value: Any?,
    @SerializedName("attributedValue") val attributedValue: String?,
    @SerializedName("changeMessage") val changeMessage: String?,
    @SerializedName("textAlignment") val textAlignment: String?
)

data class PKPassBarcode(
    val format: String,
    val message: String,
    @SerializedName("messageEncoding") val messageEncoding: String,
    @SerializedName("altText") val altText: String?
)
