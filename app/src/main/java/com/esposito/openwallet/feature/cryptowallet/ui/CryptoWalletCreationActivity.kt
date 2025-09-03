/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.cryptowallet.ui

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esposito.openwallet.core.domain.model.SupportedBlockchain
import com.esposito.openwallet.feature.scanning.ui.BarcodeScanActivity
import com.esposito.openwallet.core.ui.theme.OpenWalletTheme
import com.esposito.openwallet.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CryptoWalletCreationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if we received a pre-filled address from QR scan
        val prefilledAddress = intent.getStringExtra("prefilled_address")
        
        setContent {
            OpenWalletTheme {
                CryptoWalletCreationScreen(
                    onBack = { finish() },
                    onWalletCreated = { finish() },
                    prefilledAddress = prefilledAddress
                )
            }
        }
    }
    
    companion object {
        fun createIntent(context: android.content.Context, prefilledAddress: String? = null): Intent {
            return Intent(context, CryptoWalletCreationActivity::class.java).apply {
                prefilledAddress?.let { putExtra("prefilled_address", it) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoWalletCreationScreen(
    viewModel: CryptoWalletCreationViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onWalletCreated: () -> Unit,
    prefilledAddress: String? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf(prefilledAddress ?: "") }
    var selectedBlockchain by remember { mutableStateOf(SupportedBlockchain.BITCOIN) }
    var selectedNetwork by remember { mutableStateOf("Mainnet") }
    var selectedToken by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }

    // Handle UI state changes
    LaunchedEffect(uiState.isWalletCreated) {
        if (uiState.isWalletCreated) {
            onWalletCreated()
        }
    }

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearMessages()
        }
    }
    
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearMessages()
        }
    }

    val qrScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scannedData = result.data?.getStringExtra(BarcodeScanActivity.EXTRA_SCANNED_DATA)
            if (!scannedData.isNullOrBlank()) {
                // Validate and set the scanned address
                val trimmedAddress = scannedData.trim()
                address = trimmedAddress
                
                // Try to detect blockchain based on address format
                detectBlockchainFromAddress(trimmedAddress)?.let { detectedBlockchain: SupportedBlockchain ->
                    selectedBlockchain = detectedBlockchain
                    selectedNetwork = detectedBlockchain.networks.first()
                    selectedToken = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.add_crypto_wallet),
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Wallet Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.wallet_name_label)) },
                placeholder = { Text(stringResource(R.string.wallet_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            var blockchainExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = blockchainExpanded,
                onExpandedChange = { blockchainExpanded = !blockchainExpanded }
            ) {
                OutlinedTextField(
                    value = selectedBlockchain.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.blockchain_label)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockchainExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = blockchainExpanded,
                    onDismissRequest = { blockchainExpanded = false }
                ) {
                    SupportedBlockchain.entries.forEach { blockchain ->
                        DropdownMenuItem(
                            text = { Text("${blockchain.displayName} (${blockchain.symbol})") },
                            onClick = {
                                selectedBlockchain = blockchain
                                selectedNetwork = blockchain.networks.first()
                                selectedToken = null // Reset token when blockchain changes
                                blockchainExpanded = false
                            }
                        )
                    }
                }
            }
            var networkExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = networkExpanded,
                onExpandedChange = { networkExpanded = !networkExpanded }
            ) {
                OutlinedTextField(
                    value = selectedNetwork,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.network_label)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = networkExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = networkExpanded,
                    onDismissRequest = { networkExpanded = false }
                ) {
                    selectedBlockchain.networks.forEach { network ->
                        DropdownMenuItem(
                            text = { Text(network) },
                            onClick = {
                                selectedNetwork = network
                                networkExpanded = false
                            }
                        )
                    }
                }
            }
            if (selectedBlockchain.supportedTokens.isNotEmpty()) {
                var tokenExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = tokenExpanded,
                    onExpandedChange = { tokenExpanded = !tokenExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedToken ?: stringResource(R.string.native_token_format, selectedBlockchain.symbol),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.token_coin_label)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = tokenExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        supportingText = {
                            Text(stringResource(R.string.select_token_description, selectedBlockchain.symbol))
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = tokenExpanded,
                        onDismissRequest = { tokenExpanded = false }
                    ) {
                        // Native blockchain option
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.native_token_format, selectedBlockchain.symbol)) },
                            onClick = {
                                selectedToken = null
                                tokenExpanded = false
                            }
                        )
                        // Token options
                        selectedBlockchain.supportedTokens.forEach { token ->
                            DropdownMenuItem(
                                text = { Text(token) },
                                onClick = {
                                    selectedToken = token
                                    tokenExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { 
                        address = it.trim()
                        // Auto-add prefix if needed and not present
                        if (selectedBlockchain.addressPrefix.isNotEmpty() && 
                            !address.startsWith(selectedBlockchain.addressPrefix)) {
                            address = selectedBlockchain.addressPrefix + address
                        }
                    },
                    label = { Text(stringResource(R.string.wallet_address_label)) },
                    placeholder = { 
                        Text(
                            if (selectedBlockchain.addressPrefix.isNotEmpty()) 
                                "${selectedBlockchain.addressPrefix}..." 
                            else 
                                stringResource(R.string.enter_wallet_address)
                        ) 
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    supportingText = {
                        Text(stringResource(R.string.address_receive_description, selectedBlockchain.symbol))
                    }
                )
                IconButton(
                    onClick = {
                        val intent = BarcodeScanActivity.createIntent(context)
                        qrScannerLauncher.launch(intent)
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.scan_qr_code),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Description (Optional)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description_optional_label)) },
                placeholder = { Text(stringResource(R.string.description_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            Spacer(modifier = Modifier.weight(1f))

            // Create Button
            Button(
                onClick = {
                    viewModel.createCryptoWallet(
                        name = name.trim(),
                        address = address.trim(),
                        blockchain = selectedBlockchain.displayName,
                        network = selectedNetwork,
                        symbol = selectedBlockchain.symbol,
                        tokenSymbol = selectedToken,
                        description = description.trim().takeIf { it.isNotBlank() }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading && name.isNotBlank() && address.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.create_crypto_wallet))
                }
            }
        }
    }
}

/**
 * Attempt to detect blockchain type based on address format
 */
private fun detectBlockchainFromAddress(address: String): SupportedBlockchain? {
    return when {
        // Bitcoin addresses
        address.startsWith("1") || address.startsWith("3") || address.startsWith("bc1") -> 
            SupportedBlockchain.BITCOIN
        
        // Ethereum addresses
        address.startsWith("0x") && address.length == 42 -> 
            SupportedBlockchain.ETHEREUM
        
        // Tron addresses
        address.startsWith("T") && address.length == 34 -> 
            SupportedBlockchain.TRON
        
        // Litecoin addresses
        address.startsWith("L") || address.startsWith("M") || address.startsWith("ltc1") -> 
            SupportedBlockchain.LITECOIN
        
        // Cardano addresses
        address.startsWith("addr") -> 
            SupportedBlockchain.CARDANO
        
        // Solana addresses (usually 32-44 characters, base58 encoded)
        address.length in 32..44 && address.all { it.isLetterOrDigit() } -> 
            SupportedBlockchain.SOLANA
        
        // BSC uses same format as Ethereum
        address.startsWith("0x") && address.length == 42 -> 
            SupportedBlockchain.BINANCE_SMART_CHAIN
        
        // Monero addresses
        address.startsWith("4") && address.length == 95 -> 
            SupportedBlockchain.MONERO
        
        else -> null // Unknown format
    }
}
