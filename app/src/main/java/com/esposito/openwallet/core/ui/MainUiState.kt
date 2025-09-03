/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.ui

import androidx.compose.runtime.Immutable
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.model.CryptoWallet
import com.esposito.openwallet.core.domain.model.Pass

/**
 * UI state for the main screen
 */
@Immutable
data class MainUiState(
    val passes: List<Pass> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val cryptoWallets: List<CryptoWallet> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Filtered passes based on current search query
     */
    val filteredPasses: List<Pass>
        get() = if (searchQuery.isBlank()) {
            passes
        } else {
            passes.filter { pass ->
                pass.organizationName.contains(searchQuery, ignoreCase = true) ||
                pass.description.contains(searchQuery, ignoreCase = true) ||
                pass.serialNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    
    /**
     * Filtered credit cards based on current search query
     */
    val filteredCreditCards: List<CreditCard>
        get() = if (searchQuery.isBlank()) {
            creditCards
        } else {
            creditCards.filter { card ->
                card.cardHolderName.contains(searchQuery, ignoreCase = true) ||
                card.issuerBank.contains(searchQuery, ignoreCase = true) ||
                card.cardType.name.contains(searchQuery, ignoreCase = true) ||
                card.cardNickname?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    
    /**
     * Filtered crypto wallets based on current search query
     */
    val filteredCryptoWallets: List<CryptoWallet>
        get() = if (searchQuery.isBlank()) {
            cryptoWallets
        } else {
            cryptoWallets.filter { wallet ->
                wallet.name.contains(searchQuery, ignoreCase = true) ||
                wallet.symbol.contains(searchQuery, ignoreCase = true) ||
                wallet.blockchain.contains(searchQuery, ignoreCase = true)
            }
        }
}
