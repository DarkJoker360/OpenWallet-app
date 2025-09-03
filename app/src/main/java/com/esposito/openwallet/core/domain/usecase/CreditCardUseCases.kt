/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.usecase

import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.repository.CreditCardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllCreditCardsUseCase @Inject constructor(
    private val repository: CreditCardRepository
) {
    operator fun invoke(): Flow<List<CreditCard>> = repository.getAllCreditCards()
}

class GetCreditCardUseCase @Inject constructor(
    private val repository: CreditCardRepository
) {
    suspend operator fun invoke(id: String): CreditCard? = repository.getCreditCard(id)
}

class AddCreditCardUseCase @Inject constructor(
    private val repository: CreditCardRepository
) {
    suspend operator fun invoke(creditCard: CreditCard): Long = repository.insertCreditCard(creditCard)
}

class UpdateCreditCardUseCase @Inject constructor(
    private val repository: CreditCardRepository
) {
    suspend operator fun invoke(creditCard: CreditCard) = repository.updateCreditCard(creditCard)
}

class DeleteCreditCardUseCase @Inject constructor(
    private val repository: CreditCardRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteCreditCard(id)
}