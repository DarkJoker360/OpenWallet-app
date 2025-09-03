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

/**
 * ViewModel for Crypto Wallet Detail screen following Google's architecture patterns
 * 
 * Features:
 * - Hilt dependency injection
 * - StateFlow for reactive UI
 * - Proper error handling
 * - Clean separation of concerns
 */
@HiltViewModel
class CryptoWalletDetailViewModel @Inject constructor(
    application: Application,
    private val repository: WalletRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CryptoWalletDetailUiState())
    val uiState: StateFlow<CryptoWalletDetailUiState> = _uiState.asStateFlow()

    fun loadWallet(id: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val wallet = repository.getCryptoWalletById(id)
                
                _uiState.value = _uiState.value.copy(
                    wallet = wallet,
                    isLoading = false,
                    error = if (wallet == null) getApplication<Application>().getString(R.string.wallet_not_found) else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = getApplication<Application>().getString(R.string.failed_to_load_wallet, e.message)
                )
            }
        }
    }

    fun deleteWallet(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isDeleting = true)
                
                _uiState.value.wallet?.let { wallet ->
                    repository.deleteCryptoWallet(wallet)
                    onDeleted()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = getApplication<Application>().getString(R.string.failed_to_delete_wallet, e.message)
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for the Crypto Wallet Detail screen
 * Encapsulates all the state in a single data class for better testability
 */
data class CryptoWalletDetailUiState(
    val wallet: CryptoWallet? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null
)
