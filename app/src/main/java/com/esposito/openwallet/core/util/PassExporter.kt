/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.esposito.openwallet.R
import com.esposito.openwallet.core.domain.model.BarcodeFormat
import com.esposito.openwallet.core.domain.model.PassType
import com.esposito.openwallet.core.domain.model.WalletPass
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PassExporter {

    private const val SHARED_PASSES_DIR = "shared_passes"
    private val gson = GsonBuilder().setPrettyPrinting().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)

    fun sharePass(context: Context, pass: WalletPass) {
        try {
            val file = exportAsPkPass(context, pass)
            launchShareSheet(context, file, pass)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.share_pass_failed, e.message ?: "Unknown error"),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun exportAsPkPass(context: Context, pass: WalletPass): File {
        val dir = getSharedDir(context)
        val safeTitle = pass.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(50)
        val file = File(dir, "${safeTitle}.pkpass")

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            // 1) Build pass.json (standard PKPass format for third-party compatibility)
            val passJson = buildPkPassJson(pass)
            zip.putNextEntry(ZipEntry("pass.json"))
            zip.write(passJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2) Embed openwallet.json sidecar for 1:1 OpenWallet restoration
            val openWalletJson = buildOpenWalletSidecar(pass)
            zip.putNextEntry(ZipEntry("openwallet.json"))
            zip.write(openWalletJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 3) Add images
            addImageEntry(zip, "icon.png", pass.iconData)
            addImageEntry(zip, "icon@2x.png", pass.iconData)
            addImageEntry(zip, "logo.png", pass.logoData)
            addImageEntry(zip, "logo@2x.png", pass.logoData)
            addImageEntry(zip, "strip.png", pass.stripImageData)
            addImageEntry(zip, "strip@2x.png", pass.stripImageData)
            addImageEntry(zip, "background.png", pass.imageData)
            addImageEntry(zip, "background@2x.png", pass.imageData)
            addImageEntry(zip, "thumbnail.png", pass.thumbnailData)
            addImageEntry(zip, "thumbnail@2x.png", pass.thumbnailData)
        }

        return file
    }

    /**
     * Build the openwallet.json sidecar that preserves the EXACT WalletPass state.
     * This is the authoritative source when OpenWallet re-imports the pass.
     * Every field is stored exactly as it exists in the database.
     */
    private fun buildOpenWalletSidecar(pass: WalletPass): String {
        val root = JsonObject()

        // Format marker so we know this is our sidecar
        root.addProperty("format", "openwallet")
        root.addProperty("version", 1)

        // === Core identity — stored exactly as-is ===
        root.addProperty("id", pass.id)
        root.addProperty("type", pass.type.name)
        root.addProperty("title", pass.title)
        root.addProperty("organizationName", pass.organizationName)

        // === Optional text fields ===
        if (pass.description != null) root.addProperty("description", pass.description)
        if (pass.logoText != null) root.addProperty("logoText", pass.logoText)
        if (pass.serialNumber != null) root.addProperty("serialNumber", pass.serialNumber)

        // === Colors — exact hex values ===
        if (pass.foregroundColor != null) root.addProperty("foregroundColor", pass.foregroundColor)
        if (pass.backgroundColor != null) root.addProperty("backgroundColor", pass.backgroundColor)
        if (pass.labelColor != null) root.addProperty("labelColor", pass.labelColor)

        // === Dates ===
        if (pass.relevantDate != null) root.addProperty("relevantDate", dateFormat.format(pass.relevantDate))
        if (pass.expirationDate != null) root.addProperty("expirationDate", dateFormat.format(pass.expirationDate))
        root.addProperty("createdAt", dateFormat.format(pass.createdAt))
        root.addProperty("updatedAt", dateFormat.format(pass.updatedAt))

        // === State flags ===
        root.addProperty("voided", pass.voided)
        root.addProperty("isImported", pass.isImported)

        // === Barcode — exact data and format ===
        if (pass.barcodeData != null) root.addProperty("barcodeData", pass.barcodeData)
        if (pass.barcodeFormat != null) root.addProperty("barcodeFormat", pass.barcodeFormat.name)

        // === passData — the full JSON blob, preserved exactly ===
        root.addProperty("passData", pass.passData)

        // === File path ===
        if (pass.filePath != null) root.addProperty("filePath", pass.filePath)

        return gson.toJson(root)
    }

    /**
     * Exports the pass as a portable JSON file (OpenWallet format).
     */
    private fun exportAsJson(context: Context, pass: WalletPass): File {
        val dir = getSharedDir(context)
        val safeTitle = pass.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(50)
        val file = File(dir, "${safeTitle}.json")

        val json = buildOpenWalletJson(pass)
        file.writeText(json, Charsets.UTF_8)

        return file
    }

    /**
     * Build a PKPass-compatible pass.json from WalletPass data.
     */
    private fun buildPkPassJson(pass: WalletPass): String {
        val root = JsonObject()

        root.addProperty("formatVersion", 1)
        root.addProperty("passTypeIdentifier", "pass.com.esposito.openwallet.${pass.type.name.lowercase()}")
        root.addProperty("serialNumber", pass.serialNumber ?: pass.id)
        root.addProperty("teamIdentifier", "OPENWALLET")
        root.addProperty("organizationName", pass.organizationName)
        root.addProperty("description", pass.description ?: pass.title)

        pass.logoText?.let { root.addProperty("logoText", it) }
        pass.foregroundColor?.let { root.addProperty("foregroundColor", it) }
        pass.backgroundColor?.let { root.addProperty("backgroundColor", it) }
        pass.labelColor?.let { root.addProperty("labelColor", it) }
        pass.relevantDate?.let { root.addProperty("relevantDate", dateFormat.format(it)) }
        pass.expirationDate?.let { root.addProperty("expirationDate", dateFormat.format(it)) }

        if (pass.voided) {
            root.addProperty("voided", true)
        }

        // Barcodes
        if (pass.barcodeData != null && pass.barcodeData.isNotBlank()) {
            val barcodesArray = JsonArray()
            val barcodeObj = JsonObject()
            barcodeObj.addProperty("message", pass.barcodeData)
            barcodeObj.addProperty("format", mapToPkBarcodeFormat(pass.barcodeFormat))
            barcodeObj.addProperty("messageEncoding", "iso-8859-1")
            barcodesArray.add(barcodeObj)
            root.add("barcodes", barcodesArray)
        }

        // Pass structure — try to parse passData back to restore fields
        val structureKey = mapPassTypeToStructureKey(pass.type)
        try {
            val passDataObj = gson.fromJson(pass.passData, JsonObject::class.java)
            if (passDataObj != null) {
                val structure = rebuildPassStructure(passDataObj, pass.type)
                root.add(structureKey, structure)
            } else {
                root.add(structureKey, buildMinimalStructure(pass))
            }
        } catch (_: Exception) {
            root.add(structureKey, buildMinimalStructure(pass))
        }

        return gson.toJson(root)
    }

    /**
     * Build an OpenWallet-format JSON that can be re-imported.
     */
    private fun buildOpenWalletJson(pass: WalletPass): String {
        val root = JsonObject()

        root.addProperty("format", "openwallet")
        root.addProperty("version", 1)
        root.addProperty("id", pass.id)
        root.addProperty("type", pass.type.name)
        root.addProperty("title", pass.title)
        root.addProperty("organizationName", pass.organizationName)
        pass.description?.let { root.addProperty("description", it) }
        pass.logoText?.let { root.addProperty("logoText", it) }
        pass.foregroundColor?.let { root.addProperty("foregroundColor", it) }
        pass.backgroundColor?.let { root.addProperty("backgroundColor", it) }
        pass.labelColor?.let { root.addProperty("labelColor", it) }
        pass.serialNumber?.let { root.addProperty("serialNumber", it) }
        pass.relevantDate?.let { root.addProperty("relevantDate", dateFormat.format(it)) }
        pass.expirationDate?.let { root.addProperty("expirationDate", dateFormat.format(it)) }
        root.addProperty("voided", pass.voided)
        pass.barcodeData?.let { root.addProperty("barcodeData", it) }
        pass.barcodeFormat?.let { root.addProperty("barcodeFormat", it.name) }

        try {
            val passDataObj = gson.fromJson(pass.passData, JsonObject::class.java)
            root.add("passData", passDataObj)
        } catch (_: Exception) {
            root.addProperty("passData", pass.passData)
        }

        pass.iconData?.let { root.addProperty("iconData", android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)) }
        pass.logoData?.let { root.addProperty("logoData", android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)) }
        pass.imageData?.let { root.addProperty("imageData", android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)) }
        pass.stripImageData?.let { root.addProperty("stripImageData", android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)) }
        pass.thumbnailData?.let { root.addProperty("thumbnailData", android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)) }

        return gson.toJson(root)
    }

    private fun rebuildPassStructure(passDataObj: JsonObject, passType: PassType): JsonObject {
        val structure = JsonObject()
        val fieldKeys = listOf("headerFields", "primaryFields", "secondaryFields", "auxiliaryFields", "backFields")
        for (key in fieldKeys) {
            if (passDataObj.has(key)) {
                structure.add(key, passDataObj.get(key))
            }
        }

        if (passType == PassType.BOARDING_PASS && passDataObj.has("transitType")) {
            structure.addProperty("transitType", passDataObj.get("transitType").asString)
        }
        if (fieldKeys.none { structure.has(it) }) {
            return buildStructureFromPassData(passDataObj)
        }

        return structure
    }

    private fun buildStructureFromPassData(passDataObj: JsonObject): JsonObject {
        val structure = JsonObject()
        val primaryFields = JsonArray()
        val secondaryFields = JsonArray()

        for (entry in passDataObj.entrySet()) {
            if (entry.value.isJsonPrimitive) {
                val field = JsonObject()
                field.addProperty("key", entry.key)
                field.addProperty("label", entry.key.replaceFirstChar { it.uppercase() })
                field.addProperty("value", entry.value.asString)

                if (primaryFields.size() < 2) {
                    primaryFields.add(field)
                } else {
                    secondaryFields.add(field)
                }
            }
        }

        if (primaryFields.size() > 0) structure.add("primaryFields", primaryFields)
        if (secondaryFields.size() > 0) structure.add("secondaryFields", secondaryFields)

        return structure
    }

    private fun buildMinimalStructure(pass: WalletPass): JsonObject {
        val structure = JsonObject()
        val primaryFields = JsonArray()
        val field = JsonObject()
        field.addProperty("key", "title")
        field.addProperty("label", pass.organizationName)
        field.addProperty("value", pass.title)
        primaryFields.add(field)
        structure.add("primaryFields", primaryFields)
        return structure
    }

    private fun mapPassTypeToStructureKey(type: PassType): String {
        return when (type) {
            PassType.BOARDING_PASS -> "boardingPass"
            PassType.COUPON -> "coupon"
            PassType.EVENT_TICKET, PassType.MOVIE_TICKET, PassType.CONCERT_TICKET, PassType.SPORTS_TICKET -> "eventTicket"
            PassType.STORE_CARD, PassType.LOYALTY_CARD, PassType.MEMBERSHIP_CARD -> "storeCard"
            else -> "generic"
        }
    }

    private fun mapToPkBarcodeFormat(format: BarcodeFormat?): String {
        return when (format) {
            BarcodeFormat.QR -> "PKBarcodeFormatQR"
            BarcodeFormat.PDF417 -> "PKBarcodeFormatPDF417"
            BarcodeFormat.AZTEC -> "PKBarcodeFormatAztec"
            BarcodeFormat.CODE128 -> "PKBarcodeFormatCode128"
            BarcodeFormat.EAN13 -> "PKBarcodeFormatCode128"
            BarcodeFormat.UPC_A -> "PKBarcodeFormatCode128"
            BarcodeFormat.DATA_MATRIX -> "PKBarcodeFormatQR"
            else -> "PKBarcodeFormatQR"
        }
    }

    private fun addImageEntry(zip: ZipOutputStream, name: String, data: ByteArray?) {
        if (data == null) return
        zip.putNextEntry(ZipEntry(name))
        zip.write(data)
        zip.closeEntry()
    }

    private fun getSharedDir(context: Context): File {
        val dir = File(context.cacheDir, SHARED_PASSES_DIR)
        if (!dir.exists()) dir.mkdirs()
        cleanOldFiles(dir)
        return dir
    }

    private fun cleanOldFiles(dir: File) {
        val oneHourAgo = System.currentTimeMillis() - 3600_000
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < oneHourAgo) {
                file.delete()
            }
        }
    }

    private fun launchShareSheet(context: Context, file: File, pass: WalletPass) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val mimeType = if (file.name.endsWith(".pkpass")) {
            "application/vnd.apple.pkpass"
        } else {
            "application/json"
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, pass.title)
            putExtra(
                Intent.EXTRA_TEXT,
                context.getString(R.string.share_pass_message, pass.title, pass.organizationName)
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_pass_title))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
