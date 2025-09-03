/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.repository

import com.esposito.openwallet.core.domain.model.CreditCard
import kotlinx.coroutines.flow.Flow

interface CreditCardRepository {
    fun getAllCreditCards(): Flow<List<CreditCard>>
    suspend fun getCreditCard(id: String): CreditCard?
    suspend fun insertCreditCard(creditCard: CreditCard): Long
    suspend fun updateCreditCard(creditCard: CreditCard)
    suspend fun deleteCreditCard(creditCard: CreditCard)
    suspend fun deleteCreditCard(id: String)
}