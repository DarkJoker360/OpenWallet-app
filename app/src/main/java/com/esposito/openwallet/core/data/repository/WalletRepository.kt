/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.data.repository

import com.esposito.openwallet.core.data.local.dao.CreditCardDao
import com.esposito.openwallet.core.data.local.database.WalletPassDao
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.model.CryptoWallet
import com.esposito.openwallet.core.domain.model.PassType
import com.esposito.openwallet.core.domain.model.WalletPass
import com.esposito.openwallet.core.domain.parser.ParseResult
import com.esposito.openwallet.core.domain.parser.PassManager
import com.esposito.openwallet.core.domain.repository.CreditCardRepository
import com.esposito.openwallet.core.domain.repository.CryptoWalletRepository
import com.esposito.openwallet.core.domain.repository.WalletPassRepository
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import javax.inject.Inject

class WalletRepository @Inject constructor(
    private val walletPassDao: WalletPassDao,
    private val creditCardDao: CreditCardDao,
    private val passManager: PassManager
) : WalletPassRepository, CreditCardRepository, CryptoWalletRepository {
    // WalletPassRepository interface methods
    override fun getAllPasses(): Flow<List<WalletPass>> = walletPassDao.getAllPasses()
    
    override suspend fun getPass(id: String): WalletPass? = walletPassDao.getPassById(id)
    
    override suspend fun insertPass(pass: WalletPass): Long {
        walletPassDao.insertPass(pass)
        return 0L // DAO doesn't return Long, but interface expects it
    }
    
    override suspend fun updatePass(pass: WalletPass) = walletPassDao.updatePass(pass)
    
    override suspend fun deletePass(pass: WalletPass) = walletPassDao.deletePass(pass)
    
    override suspend fun deletePass(id: String) = walletPassDao.deletePassById(id)
    
    override suspend fun getPassesByType(type: String): List<WalletPass> = 
        walletPassDao.getPassesByTypeSync(PassType.valueOf(type.uppercase()))

    // Additional WalletPass methods

    suspend fun getAllPassesSync(): List<WalletPass> = walletPassDao.getAllPassesSync()

    suspend fun getPassById(id: String): WalletPass? = walletPassDao.getPassById(id)

    /**
     * Import any supported pass format using the PassManager
     * 
     * @param inputStream The file content to parse
     * @param fileName Original filename (helps with format detection)
     * @param mimeType MIME type (optional, helps with format detection)
     * @param metadata Additional metadata (optional)
     * @return ImportResult with detailed information about the import
     */
    suspend fun importPass(
        inputStream: InputStream,
        fileName: String? = null,
        mimeType: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): ImportResult {
        return try {
            when (val parseResult = passManager.parsePass(inputStream, fileName, mimeType, metadata)) {
                is ParseResult.Success -> {
                    insertPass(parseResult.pass)
                    ImportResult.Success(
                        pass = parseResult.pass,
                        formatName = parseResult.formatName,
                        warnings = parseResult.warnings
                    )
                }
                is ParseResult.Failure -> {
                    ImportResult.Failure(
                        error = parseResult.error,
                        errorCode = parseResult.errorCode,
                        formatName = parseResult.formatName
                    )
                }
            }
        } catch (e: Exception) {
            ImportResult.Failure(
                error = "Unexpected error during import: ${e.message}",
                errorCode = "UNEXPECTED_ERROR"
            )
        }
    }

    // CryptoWalletRepository interface methods  
    override fun getAllCryptoWallets(): Flow<List<CryptoWallet>> = walletPassDao.getAllCryptoWallets()

    override suspend fun getCryptoWallet(id: String): CryptoWallet? = walletPassDao.getCryptoWalletById(id.toLongOrNull() ?: 0)

    override suspend fun insertCryptoWallet(wallet: CryptoWallet): Long = walletPassDao.insertCryptoWallet(wallet)

    override suspend fun updateCryptoWallet(wallet: CryptoWallet) = walletPassDao.updateCryptoWallet(wallet)

    override suspend fun deleteCryptoWallet(wallet: CryptoWallet) = walletPassDao.deleteCryptoWallet(wallet)

    override suspend fun deleteCryptoWallet(id: String) = 
        walletPassDao.getCryptoWalletById(id.toLongOrNull() ?: 0)?.let { 
            walletPassDao.deleteCryptoWallet(it) 
        } ?: Unit

    // Additional Crypto Wallet methods

    suspend fun getAllCryptoWalletsSync(): List<CryptoWallet> = walletPassDao.getAllCryptoWalletsSync()

    suspend fun getCryptoWalletById(id: Long): CryptoWallet? = walletPassDao.getCryptoWalletById(id)

    // ============ Credit Card Operations ============
    
    // CreditCardRepository interface methods
    override fun getAllCreditCards(): Flow<List<CreditCard>> = creditCardDao.getAllCreditCards()
    
    override suspend fun getCreditCard(id: String): CreditCard? = creditCardDao.getCreditCardById(id)
    
    override suspend fun insertCreditCard(creditCard: CreditCard): Long = creditCardDao.insertCreditCard(creditCard)
    
    override suspend fun updateCreditCard(creditCard: CreditCard) = creditCardDao.updateCreditCard(creditCard)
    
    override suspend fun deleteCreditCard(creditCard: CreditCard) = creditCardDao.deleteCreditCard(creditCard)
    
    override suspend fun deleteCreditCard(id: String) = creditCardDao.deleteCreditCardById(id)

    // Additional Credit Card methods

    suspend fun getAllCreditCardsSync(): List<CreditCard> = creditCardDao.getAllCreditCardsSync()

    suspend fun deleteAllCreditCards() = creditCardDao.deleteAllCreditCards()
}

/**
 * Result of pass import operation
 */
sealed class ImportResult {
    data class Success(
        val pass: WalletPass,
        val formatName: String,
        val warnings: List<String> = emptyList()
    ) : ImportResult()
    
    data class Failure(
        val error: String,
        val errorCode: String,
        val formatName: String? = null
    ) : ImportResult()
}
