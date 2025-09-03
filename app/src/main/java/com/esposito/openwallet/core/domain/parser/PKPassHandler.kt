/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.parser

import com.esposito.openwallet.core.domain.model.*
import com.google.gson.Gson
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
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
    
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
            val images = mutableMapOf<String, ByteArray>()

            var entry = zipInputStream.nextEntry
            while (entry != null) {
                when {
                    entry.name == "pass.json" -> {
                        passJson = zipInputStream.readBytes().toString(Charsets.UTF_8)
                    }
                    entry.name.endsWith(".png") -> {
                        images[entry.name] = zipInputStream.readBytes()
                    }
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            passJson?.let { json ->
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

        return WalletPass(
            id = pkPass.serialNumber ?: UUID.randomUUID().toString(),
            type = passType,
            title = pkPass.description ?: pkPass.organizationName,
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
                mapBarcodeFormat(barcodeInfo.format)
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

    private fun createPassSpecificData(pkPass: PKPassData, passType: PassType): Any {
        return when (passType) {
            PassType.BOARDING_PASS -> {
                val bp = pkPass.boardingPass
                BoardingPassData(
                    transitType = bp?.transitType ?: "",
                    gate = getFieldValue(bp?.auxiliaryFields, "gate"),
                    seat = getFieldValue(bp?.secondaryFields, "seat"),
                    departureLocation = getFieldValue(bp?.primaryFields, "origin"),
                    destinationLocation = getFieldValue(bp?.primaryFields, "destination"),
                    flightNumber = getFieldValue(bp?.primaryFields, "flight"),
                    departureDate = parseDate(getFieldValue(bp?.headerFields, "departure")),
                    arrivalDate = parseDate(getFieldValue(bp?.headerFields, "arrival"))
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

    private fun parseDate(dateString: String?): Date? {
        return dateString?.let {
            dateFormat.parse(it)
        }
    }

    private fun mapBarcodeFormat(format: String?): BarcodeFormat {
        return when (format?.uppercase()) {
            "PKBarcodeFormatQR" -> BarcodeFormat.QR
            "PKBarcodeFormatPDF417" -> BarcodeFormat.PDF417
            "PKBarcodeFormatAztec" -> BarcodeFormat.AZTEC
            "PKBarcodeFormatCode128" -> BarcodeFormat.CODE128
            else -> BarcodeFormat.QR // Default to QR when format is unknown
        }
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
