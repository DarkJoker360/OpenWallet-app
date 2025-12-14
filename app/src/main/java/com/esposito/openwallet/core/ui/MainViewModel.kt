/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.model.CryptoWallet
import com.esposito.openwallet.core.domain.model.Pass
import com.esposito.openwallet.core.domain.model.WalletPass
import com.esposito.openwallet.core.util.SecureLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Extension function to convert WalletPass to Pass
 * This handles the mapping between repository and UI models
 */
private fun WalletPass.toPass() = Pass(
    id = this.id,
    organizationName = this.title.takeIf { it.isNotBlank() } ?: this.organizationName,
    description = this.description ?: "",
    serialNumber = this.serialNumber ?: "",
    passType = this.type.name,
    barcodeMessage = this.barcodeData,
    barcodeFormat = this.barcodeFormat?.name,
    barcodeAltText = null,
    headerFields = emptyList(), // TODO: understand how to map
    primaryFields = emptyList(), // TODO: understand how to map
    secondaryFields = emptyList(), // TODO: understand how to map
    auxiliaryFields = emptyList(), // TODO: understand how to map
    backFields = emptyList(), // TODO: understand how to map
    locations = emptyList(), // TODO: understand how to map
    relevantDate = this.relevantDate,
    expirationDate = this.expirationDate,
    backgroundColor = this.backgroundColor,
    foregroundColor = this.foregroundColor,
    labelColor = this.labelColor,
    logoText = this.logoText,
    suppressStripShine = false,
    webServiceURL = null,
    authenticationToken = null,
    isVoided = this.voided,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    type = this.type.name
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val TAG = "MainViewModel"

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * Combined UI state for the main screen
     */
    val uiState: StateFlow<MainUiState> = 
        combine(
            walletRepository.getAllPasses(),
            walletRepository.getAllCreditCards(),
            walletRepository.getAllCryptoWallets(),
            _searchQuery,
            _isSearchActive,
            _isLoading,
            _errorMessage
        ) { data ->
            val passes = (data[0] as? List<*>)?.filterIsInstance<WalletPass>() ?: emptyList()
            val creditCards = (data[1] as? List<*>)?.filterIsInstance<CreditCard>() ?: emptyList()
            val cryptoWallets = (data[2] as? List<*>)?.filterIsInstance<CryptoWallet>() ?: emptyList()
            val searchQuery = data[3] as? String ?: ""
            val isSearchActive = data[4] as? Boolean ?: false
            val isLoading = data[5] as? Boolean ?: false
            val errorMessage = data[6] as? String?
            
            MainUiState(
                passes = passes.map { it.toPass() },
                creditCards = creditCards,
                cryptoWallets = cryptoWallets,
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                isLoading = isLoading,
                errorMessage = errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState()
        )

    /**
     * Update the search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Toggle search mode on/off
     */
    fun toggleSearch() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) {
            _searchQuery.value = ""
        }
    }

    /**
     * Close search and clear query
     */
    fun closeSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
    }

    /**
     * Delete a pass by ID
     */
    fun deletePass(passId: String) {
        viewModelScope.launch {
            try {
                SecureLogger.d(TAG, "Deleting pass: $passId")
                walletRepository.deletePass(passId)
                SecureLogger.d(TAG, "Pass deleted successfully: $passId")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to delete pass", e)
                _errorMessage.value = "Failed to delete pass: ${e.message}"
            }
        }
    }

    /**
     * Delete a credit card by ID
     */
    fun deleteCreditCard(creditCardId: String) {
        viewModelScope.launch {
            try {
                SecureLogger.d(TAG, "Deleting credit card: $creditCardId")
                walletRepository.deleteCreditCard(creditCardId)
                SecureLogger.d(TAG, "Credit card deleted successfully: $creditCardId")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to delete credit card", e)
                _errorMessage.value = "Failed to delete credit card: ${e.message}"
            }
        }
    }

    /**
     * Delete a crypto wallet by ID
     */
    fun deleteCryptoWallet(walletId: Long) {
        viewModelScope.launch {
            try {
                SecureLogger.d(TAG, "Deleting crypto wallet: $walletId")
                walletRepository.deleteCryptoWallet(walletId.toString())
                SecureLogger.d(TAG, "Crypto wallet deleted successfully: $walletId")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to delete crypto wallet", e)
                _errorMessage.value = "Failed to delete crypto wallet: ${e.message}"
            }
        }
    }
}
