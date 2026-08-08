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
    val showArchived: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val hasArchivedItems: Boolean
        get() = passes.any { it.isArchived } ||
            creditCards.any { it.isArchived } ||
            cryptoWallets.any { it.isArchived }

    /**
     * Filtered passes based on current search query
     */
    val filteredPasses: List<Pass>
        get() = if (searchQuery.isBlank()) {
            passes.filter { it.isArchived == showArchived }
        } else {
            passes.filter { pass ->
                (pass.isArchived == showArchived) && (
                    pass.organizationName.contains(searchQuery, ignoreCase = true) ||
                    pass.description.contains(searchQuery, ignoreCase = true) ||
                    pass.serialNumber.contains(searchQuery, ignoreCase = true) ||
                    pass.barcodeMessage?.contains(searchQuery, ignoreCase = true) == true ||
                    pass.tags.any { it.contains(searchQuery, ignoreCase = true) }
                )
            }
        }
    
    /**
     * Filtered credit cards based on current search query
     */
    val filteredCreditCards: List<CreditCard>
        get() = if (searchQuery.isBlank()) {
            creditCards.filter { it.isArchived == showArchived }
        } else {
            creditCards.filter { card ->
                (card.isArchived == showArchived) && (
                    card.cardHolderName.contains(searchQuery, ignoreCase = true) ||
                    card.issuerBank.contains(searchQuery, ignoreCase = true) ||
                    card.cardType.name.contains(searchQuery, ignoreCase = true) ||
                    card.cardNickname?.contains(searchQuery, ignoreCase = true) == true ||
                    card.tags.any { it.contains(searchQuery, ignoreCase = true) }
                )
            }
        }
    
    /**
     * Filtered crypto wallets based on current search query
     */
    val filteredCryptoWallets: List<CryptoWallet>
        get() = if (searchQuery.isBlank()) {
            cryptoWallets.filter { it.isArchived == showArchived }
        } else {
            cryptoWallets.filter { wallet ->
                (wallet.isArchived == showArchived) && (
                    wallet.name.contains(searchQuery, ignoreCase = true) ||
                    wallet.symbol.contains(searchQuery, ignoreCase = true) ||
                    wallet.blockchain.contains(searchQuery, ignoreCase = true) ||
                    wallet.address.contains(searchQuery, ignoreCase = true) ||
                    wallet.tags.any { it.contains(searchQuery, ignoreCase = true) }
                )
            }
        }
}
