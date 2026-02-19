/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.parser

import com.esposito.openwallet.core.domain.model.BarcodeFormat
import com.esposito.openwallet.core.domain.model.PassType
import com.esposito.openwallet.core.domain.model.WalletPass
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handler for OpenWallet's own JSON export format.
 * 
 * This allows passes exported from OpenWallet (via the Share feature)
 * to be re-imported on the same or another device running OpenWallet.
 *
 * The format is detected by looking for `"format": "openwallet"` in the JSON.
 */
class OpenWalletJsonHandler(
    private val gson: Gson
) : PassHandler {

    override val formatName: String = "OpenWallet JSON"
    override val supportedExtensions: List<String> = listOf("json")
    override val supportedMimeTypes: List<String> = listOf("application/json")

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    )

    override fun canHandle(fileName: String?, mimeType: String?, inputStream: InputStream): Boolean {
        return try {
            val content = inputStream.readBytes().toString(Charsets.UTF_8)
            val jsonObj = gson.fromJson(content, JsonObject::class.java)
            // Detect our format: must contain "format": "openwallet"
            jsonObj?.has("format") == true &&
                jsonObj.get("format")?.asString?.equals("openwallet", ignoreCase = true) == true
        } catch (_: Exception) {
            false
        }
    }

    override fun parsePass(
        inputStream: InputStream,
        fileName: String?,
        metadata: Map<String, Any>
    ): WalletPass? {
        return try {
            val content = inputStream.readBytes().toString(Charsets.UTF_8)
            val json = gson.fromJson(content, JsonObject::class.java) ?: return null

            // Verify format marker
            val format = json.get("format")?.asString
            if (format == null || !format.equals("openwallet", ignoreCase = true)) {
                return null
            }

            val passType = try {
                PassType.valueOf(json.get("type")?.asString ?: "GENERIC")
            } catch (_: Exception) {
                PassType.GENERIC
            }

            val barcodeFormat = try {
                val fmt = json.get("barcodeFormat")?.asString
                if (fmt != null) BarcodeFormat.valueOf(fmt) else null
            } catch (_: Exception) {
                null
            }

            // Restore passData
            val passData = when {
                json.has("passData") && json.get("passData").isJsonObject ->
                    gson.toJson(json.getAsJsonObject("passData"))
                json.has("passData") ->
                    json.get("passData").asString
                else -> "{}"
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
                passData = passData,
                barcodeData = json.get("barcodeData")?.asString,
                barcodeFormat = barcodeFormat,
                iconData = decodeBase64(json.get("iconData")?.asString),
                logoData = decodeBase64(json.get("logoData")?.asString),
                imageData = decodeBase64(json.get("imageData")?.asString),
                stripImageData = decodeBase64(json.get("stripImageData")?.asString),
                thumbnailData = decodeBase64(json.get("thumbnailData")?.asString),
                filePath = fileName,
                isImported = true
            )
        } catch (e: Exception) {
            throw PassParsingException(
                "Failed to parse OpenWallet JSON: ${e.message}",
                e,
                formatName,
                fileName
            )
        }
    }

    override fun validatePass(pass: WalletPass): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (pass.title.isBlank()) {
            errors.add("Pass title is required")
        }
        if (pass.organizationName.isBlank()) {
            errors.add("Organization name is required")
        }
        if (pass.barcodeData == null) {
            warnings.add("Pass has no barcode data")
        }

        return if (errors.isEmpty()) {
            if (warnings.isEmpty()) ValidationResult.success()
            else ValidationResult.withWarnings(warnings)
        } else {
            ValidationResult.failure(errors)
        }
    }

    private fun decodeBase64(encoded: String?): ByteArray? {
        if (encoded.isNullOrBlank()) return null
        return try {
            android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDate(dateString: String?): Date? {
        if (dateString == null) return null
        for (format in dateFormats) {
            try {
                return format.parse(dateString)
            } catch (_: Exception) {
                // Try next format
            }
        }
        return null
    }
}
