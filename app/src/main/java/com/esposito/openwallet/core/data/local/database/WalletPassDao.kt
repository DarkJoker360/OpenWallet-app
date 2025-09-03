/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.data.local.database

import androidx.room.*
import com.esposito.openwallet.core.domain.model.PassType
import com.esposito.openwallet.core.domain.model.WalletPass
import com.esposito.openwallet.core.domain.model.CryptoWallet
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletPassDao {
    @Query("SELECT * FROM wallet_passes ORDER BY createdAt DESC")
    fun getAllPasses(): Flow<List<WalletPass>>

    @Query("SELECT * FROM wallet_passes ORDER BY createdAt DESC")
    suspend fun getAllPassesSync(): List<WalletPass>

    @Query("SELECT * FROM wallet_passes WHERE type = :type ORDER BY createdAt DESC")
    suspend fun getPassesByTypeSync(type: PassType): List<WalletPass>

    @Query("SELECT * FROM wallet_passes WHERE type = :type ORDER BY createdAt DESC")
    fun getPassesByType(type: PassType): Flow<List<WalletPass>>

    @Query("SELECT * FROM wallet_passes WHERE id = :id")
    suspend fun getPassById(id: String): WalletPass?

    @Query("SELECT * FROM wallet_passes WHERE voided = 0 ORDER BY createdAt DESC")
    fun getActivePasses(): Flow<List<WalletPass>>

    @Query("SELECT * FROM wallet_passes WHERE expirationDate > :currentTime AND voided = 0 ORDER BY expirationDate ASC")
    fun getValidPasses(currentTime: Long): Flow<List<WalletPass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPass(pass: WalletPass)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasses(passes: List<WalletPass>)

    @Update
    suspend fun updatePass(pass: WalletPass)

    @Delete
    suspend fun deletePass(pass: WalletPass)

    @Query("DELETE FROM wallet_passes WHERE id = :id")
    suspend fun deletePassById(id: String)

    @Query("SELECT COUNT(*) FROM wallet_passes WHERE type = :type")
    suspend fun getPassCountByType(type: PassType): Int

    @Query("SELECT * FROM wallet_passes WHERE title LIKE '%' || :query || '%' OR organizationName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchPasses(query: String): Flow<List<WalletPass>>

    @Query("UPDATE wallet_passes SET voided = 1 WHERE id = :id")
    suspend fun voidPass(id: String)

    // Crypto Wallet methods
    @Query("SELECT * FROM crypto_wallets ORDER BY createdAt DESC")
    fun getAllCryptoWallets(): Flow<List<CryptoWallet>>

    @Query("SELECT * FROM crypto_wallets ORDER BY createdAt DESC")
    suspend fun getAllCryptoWalletsSync(): List<CryptoWallet>

    @Query("SELECT * FROM crypto_wallets WHERE id = :id")
    suspend fun getCryptoWalletById(id: Long): CryptoWallet?

    @Query("SELECT * FROM crypto_wallets WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveCryptoWallets(): Flow<List<CryptoWallet>>

    @Query("SELECT * FROM crypto_wallets WHERE blockchain = :blockchain ORDER BY createdAt DESC")
    fun getCryptoWalletsByBlockchain(blockchain: String): Flow<List<CryptoWallet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCryptoWallet(wallet: CryptoWallet): Long

    @Update
    suspend fun updateCryptoWallet(wallet: CryptoWallet)

    @Delete
    suspend fun deleteCryptoWallet(wallet: CryptoWallet)

    @Query("DELETE FROM crypto_wallets WHERE id = :id")
    suspend fun deleteCryptoWalletById(id: Long)

    @Query("SELECT * FROM crypto_wallets WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' OR blockchain LIKE '%' || :query || '%'")
    fun searchCryptoWallets(query: String): Flow<List<CryptoWallet>>
}
