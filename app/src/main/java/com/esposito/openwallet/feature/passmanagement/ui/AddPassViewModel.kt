/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.passmanagement.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esposito.openwallet.core.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class AddPassViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun importPass(inputStream: InputStream, fileName: String? = null) {
        viewModelScope.launch {
            try {
                _importState.value = ImportState.Loading
                
                // Use the new multi-format import method
                val result = walletRepository.importPass(inputStream, fileName)
                
                _importState.value = when (result) {
                    is com.esposito.openwallet.core.data.repository.ImportResult.Success -> {
                        ImportState.Success
                    }
                    is com.esposito.openwallet.core.data.repository.ImportResult.Failure -> {
                        ImportState.Error(result.error)
                    }
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    sealed class ImportState {
        object Idle : ImportState()
        object Loading : ImportState()
        object Success : ImportState()
        data class Error(val message: String) : ImportState()
    }
}
