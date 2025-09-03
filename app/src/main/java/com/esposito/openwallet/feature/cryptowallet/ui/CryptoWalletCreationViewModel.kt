/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.cryptowallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.esposito.openwallet.core.domain.model.CryptoWallet
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CryptoWalletCreationViewModel @Inject constructor(
    application: Application,
    private val walletRepository: WalletRepository
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(CryptoWalletCreationUiState())
    val uiState: StateFlow<CryptoWalletCreationUiState> = _uiState.asStateFlow()

    fun createCryptoWallet(
        name: String,
        address: String,
        blockchain: String,
        network: String,
        symbol: String,
        tokenSymbol: String? = null,
        description: String? = null
    ) {
        // Validate input
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = getApplication<Application>().getString(R.string.wallet_name_required))
            return
        }
        
        if (address.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = getApplication<Application>().getString(R.string.wallet_address_required))
            return
        }
        
        if (blockchain.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = getApplication<Application>().getString(R.string.blockchain_required))
            return
        }
        
        if (network.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = getApplication<Application>().getString(R.string.network_required))
            return
        }
        
        if (symbol.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = getApplication<Application>().getString(R.string.symbol_required))
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val wallet = CryptoWallet(
                    name = name.trim(),
                    address = address.trim(),
                    blockchain = blockchain.trim(),
                    network = network.trim(),
                    symbol = symbol.trim().uppercase(),
                    tokenSymbol = tokenSymbol?.trim()?.uppercase(),
                    description = description?.trim()
                )
                
                walletRepository.insertCryptoWallet(wallet)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isWalletCreated = true,
                    successMessage = getApplication<Application>().getString(R.string.crypto_wallet_created_successfully)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = getApplication<Application>().getString(R.string.failed_to_create_crypto_wallet, e.message)
                )
            }
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

/**
 * Represents the UI state for the crypto wallet creation screen
 */
data class CryptoWalletCreationUiState(
    val isLoading: Boolean = false,
    val isWalletCreated: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
