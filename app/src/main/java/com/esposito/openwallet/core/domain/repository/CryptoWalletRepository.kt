/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.repository

import com.esposito.openwallet.core.domain.model.CryptoWallet
import kotlinx.coroutines.flow.Flow

interface CryptoWalletRepository {
    fun getAllCryptoWallets(): Flow<List<CryptoWallet>>
    suspend fun getCryptoWallet(id: String): CryptoWallet?
    suspend fun insertCryptoWallet(wallet: CryptoWallet): Long
    suspend fun updateCryptoWallet(wallet: CryptoWallet)
    suspend fun deleteCryptoWallet(wallet: CryptoWallet)
    suspend fun deleteCryptoWallet(id: String)
}