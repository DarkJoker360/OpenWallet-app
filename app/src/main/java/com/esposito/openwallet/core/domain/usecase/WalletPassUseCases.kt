/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.usecase

import com.esposito.openwallet.core.domain.model.WalletPass
import com.esposito.openwallet.core.domain.repository.WalletPassRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllPassesUseCase @Inject constructor(
    private val repository: WalletPassRepository
) {
    operator fun invoke(): Flow<List<WalletPass>> = repository.getAllPasses()
}

class GetPassUseCase @Inject constructor(
    private val repository: WalletPassRepository
) {
    suspend operator fun invoke(id: String): WalletPass? = repository.getPass(id)
}

class AddPassUseCase @Inject constructor(
    private val repository: WalletPassRepository
) {
    suspend operator fun invoke(pass: WalletPass): Long = repository.insertPass(pass)
}

class UpdatePassUseCase @Inject constructor(
    private val repository: WalletPassRepository
) {
    suspend operator fun invoke(pass: WalletPass) = repository.updatePass(pass)
}

class DeletePassUseCase @Inject constructor(
    private val repository: WalletPassRepository
) {
    suspend operator fun invoke(id: String) = repository.deletePass(id)
}

class GetPassesByTypeUseCase @Inject constructor(
    private val repository: WalletPassRepository
) {
    suspend operator fun invoke(type: String): List<WalletPass> = repository.getPassesByType(type)
}