/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("DEPRECATION") // TODO: inspect

package com.esposito.openwallet.core.data.local.database

import android.content.Context
import android.util.Base64
import com.esposito.openwallet.core.util.SecureLogger
import androidx.core.content.edit
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.esposito.openwallet.core.data.local.dao.CreditCardDao
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.model.CryptoWallet
import com.esposito.openwallet.core.domain.model.WalletPass
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.GeneralSecurityException
import javax.crypto.KeyGenerator

@Database(
    entities = [WalletPass::class, CryptoWallet::class, CreditCard::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SecureWalletDatabase : RoomDatabase() {
    abstract fun walletPassDao(): WalletPassDao
    abstract fun creditCardDao(): CreditCardDao

    companion object {
        private const val TAG = "SecureWalletDatabase"
        const val DATABASE_NAME = "secure_wallet_database"
        private const val KEY_ALIAS = "wallet_db_key"
        private const val PREFS_NAME = "wallet_secure_prefs"
        private const val AES_KEY_SIZE = 256
        private const val SQL_CIPHER_LIBRARY = "sqlcipher"
        
        init {
            System.loadLibrary(SQL_CIPHER_LIBRARY)
        }
        
        @Volatile
        private var INSTANCE: SecureWalletDatabase? = null

        fun getDatabase(context: Context): SecureWalletDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabase(context: Context): SecureWalletDatabase {
            val passphrase = getOrCreatePassphrase(context)
            val factory = SupportOpenHelperFactory(passphrase)
            
            return Room.databaseBuilder(
                context.applicationContext,
                SecureWalletDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(*DatabaseMigrations.getAllMigrations())
                .build()
        }

        private fun getOrCreatePassphrase(context: Context): ByteArray {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                
                val sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                val existingKey = sharedPreferences.getString(KEY_ALIAS, null)
                
                if (existingKey != null) {
                    Base64.decode(existingKey, Base64.DEFAULT)
                } else {
                    generateAndStoreNewKey(sharedPreferences)
                }
            } catch (e: GeneralSecurityException) {
                SecureLogger.e(TAG, "Failed to create encrypted preferences", e)
                generateFallbackKey()
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Unexpected error creating database passphrase", e)
                generateFallbackKey()
            }
        }
        
        private fun generateAndStoreNewKey(sharedPreferences: android.content.SharedPreferences): ByteArray {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(AES_KEY_SIZE)
            val secretKey = keyGen.generateKey()
            val keyBytes = secretKey.encoded
            
            val encodedKey = Base64.encodeToString(keyBytes, Base64.DEFAULT)
            sharedPreferences.edit { putString(KEY_ALIAS, encodedKey) }
            
            return keyBytes
        }
        
        private fun generateFallbackKey(): ByteArray {
            SecureLogger.w(TAG, "Using fallback key generation - security may be reduced")
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(AES_KEY_SIZE)
            return keyGen.generateKey().encoded
        }
    }
}
