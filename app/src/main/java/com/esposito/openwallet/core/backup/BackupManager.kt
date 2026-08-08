/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.backup

import android.util.Base64
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.model.CryptoWallet
import com.esposito.openwallet.core.domain.model.WalletPass
import com.esposito.openwallet.core.util.SecureLogger
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Date
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * Creates and restores password-protected, fully offline backups of all wallet
 * data (passes, credit/debit cards and crypto wallets, including images).
 *
 * Format ("OWB1"): a small header followed by an AES-256-GCM encrypted, gzipped
 * JSON document. The encryption key is derived from the user's passphrase with
 * PBKDF2-HMAC-SHA256, so the passphrase never leaves the device and the backup
 * is useless without it. A wrong passphrase fails authentication during decrypt.
 */
class BackupManager @Inject constructor(private val repository: WalletRepository) {

    companion object {
        private const val TAG = "BackupManager"

        private val MAGIC = byteArrayOf('O'.code.toByte(), 'W'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte())
        private const val FORMAT_VERSION = 1
        private const val BACKUP_SCHEMA_VERSION = 1

        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private const val KEY_LENGTH_BITS = 256
        private const val PBKDF2_ITERATIONS = 210_000

        const val FILE_EXTENSION = "owbackup"
        const val MIME_TYPE = "application/octet-stream"
    }

    private val gson = GsonBuilder()
        // Store byte arrays (images) as compact Base64 strings rather than int arrays.
        .registerTypeAdapter(ByteArray::class.java, JsonSerializer<ByteArray> { src, _, _ ->
            JsonPrimitive(Base64.encodeToString(src, Base64.NO_WRAP))
        })
        .registerTypeAdapter(ByteArray::class.java, JsonDeserializer { json, _, _ ->
            Base64.decode(json.asString, Base64.NO_WRAP)
        })
        // Dates as epoch millis – locale- and format-independent.
        .registerTypeAdapter(Date::class.java, JsonSerializer<Date> { src, _, _ ->
            JsonPrimitive(src.time)
        })
        .registerTypeAdapter(Date::class.java, JsonDeserializer { json, _, _ ->
            Date(json.asLong)
        })
        .create()

    private data class BackupData(
        val schemaVersion: Int,
        val exportedAt: Long,
        val passes: List<WalletPass>,
        val creditCards: List<CreditCard>,
        val cryptoWallets: List<CryptoWallet>
    )

    /** Summary of what a restore operation imported. */
    data class RestoreSummary(val passes: Int, val creditCards: Int, val cryptoWallets: Int)

    /**
     * Build an encrypted backup of everything currently stored.
     * @return the raw bytes to write to the user-selected backup file.
     */
    suspend fun exportEncryptedBackup(passphrase: String): ByteArray = withContext(Dispatchers.IO) {
        val backup = BackupData(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            passes = repository.getAllPassesSync(),
            creditCards = repository.getAllCreditCardsSync(),
            cryptoWallets = repository.getAllCryptoWalletsSync()
        )

        val plainJson = gson.toJson(backup).toByteArray(Charsets.UTF_8)
        val compressed = gzip(plainJson)

        val salt = randomBytes(SALT_LENGTH)
        val iv = randomBytes(IV_LENGTH)
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(compressed)

        ByteArrayOutputStream().apply {
            write(MAGIC)
            write(FORMAT_VERSION)
            write(salt)
            write(iv)
            write(ciphertext)
        }.toByteArray()
    }

    /**
     * Decrypt and restore a backup previously produced by [exportEncryptedBackup].
     * Existing items with the same id are replaced (the DAOs use REPLACE).
     *
     * @throws BackupException if the file is not a valid backup or the passphrase is wrong.
     */
    suspend fun restoreEncryptedBackup(fileBytes: ByteArray, passphrase: String): RestoreSummary =
        withContext(Dispatchers.IO) {
            if (fileBytes.size < MAGIC.size + 1 + SALT_LENGTH + IV_LENGTH) {
                throw BackupException("File is too small to be a valid backup")
            }
            if (!fileBytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
                throw BackupException("Unrecognized backup file")
            }

            var offset = MAGIC.size
            val version = fileBytes[offset].toInt(); offset += 1
            if (version != FORMAT_VERSION) {
                throw BackupException("Unsupported backup version: $version")
            }
            val salt = fileBytes.copyOfRange(offset, offset + SALT_LENGTH); offset += SALT_LENGTH
            val iv = fileBytes.copyOfRange(offset, offset + IV_LENGTH); offset += IV_LENGTH
            val ciphertext = fileBytes.copyOfRange(offset, fileBytes.size)

            val key = deriveKey(passphrase, salt)
            val compressed = try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                cipher.doFinal(ciphertext)
            } catch (e: Exception) {
                // AEADBadTagException etc. – almost always a wrong passphrase or corruption.
                SecureLogger.w(TAG, "Backup decryption failed")
                throw BackupException("Incorrect passphrase or corrupted backup", e)
            }

            val json = gunzip(compressed).toString(Charsets.UTF_8)
            val backup = try {
                gson.fromJson(json, BackupData::class.java)
            } catch (e: Exception) {
                throw BackupException("Backup contents could not be read", e)
            } ?: throw BackupException("Backup is empty")

            if (backup.schemaVersion != BACKUP_SCHEMA_VERSION) {
                throw BackupException("Unsupported backup schema: ${backup.schemaVersion}")
            }

            repository.restoreBackup(
                passes = backup.passes,
                creditCards = backup.creditCards,
                cryptoWallets = backup.cryptoWallets
            )

            RestoreSummary(
                passes = backup.passes.size,
                creditCards = backup.creditCards.size,
                cryptoWallets = backup.cryptoWallets.size
            )
        }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun randomBytes(length: Int): ByteArray =
        ByteArray(length).also { SecureRandom().nextBytes(it) }

    private fun gzip(data: ByteArray): ByteArray =
        ByteArrayOutputStream().also { bos -> GZIPOutputStream(bos).use { it.write(data) } }.toByteArray()

    private fun gunzip(data: ByteArray): ByteArray =
        GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
}

/** Thrown when a backup cannot be produced or restored (bad file, wrong passphrase, etc.). */
class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
