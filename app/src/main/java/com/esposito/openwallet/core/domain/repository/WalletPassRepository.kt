/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.repository

import com.esposito.openwallet.core.domain.model.WalletPass
import kotlinx.coroutines.flow.Flow

interface WalletPassRepository {
    fun getAllPasses(): Flow<List<WalletPass>>
    suspend fun getPass(id: String): WalletPass?
    suspend fun insertPass(pass: WalletPass): Long
    suspend fun updatePass(pass: WalletPass)
    suspend fun deletePass(pass: WalletPass)
    suspend fun deletePass(id: String)
    suspend fun getPassesByType(type: String): List<WalletPass>
}