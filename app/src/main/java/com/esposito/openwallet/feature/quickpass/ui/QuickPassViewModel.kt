/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.quickpass.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.core.domain.model.WalletPass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickPassUiState(
    val passes: List<WalletPass> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class QuickPassViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickPassUiState())
    val uiState: StateFlow<QuickPassUiState> = _uiState.asStateFlow()

    init {
        loadPasses()
    }

    private fun loadPasses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val passes = walletRepository.getAllPassesSync()
                _uiState.update { it.copy(passes = passes, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
