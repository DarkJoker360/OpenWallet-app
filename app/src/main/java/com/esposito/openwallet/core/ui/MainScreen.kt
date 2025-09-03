/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.esposito.openwallet.R
import com.esposito.openwallet.core.ui.components.FabMenu
import com.esposito.openwallet.core.ui.components.MainContent

@Composable
fun MainScreen(
    uiState: MainUiState,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onNavigateToAddPass: () -> Unit,
    onNavigateToAddCreditCard: () -> Unit,
    onNavigateToAddCryptoWallet: () -> Unit,
    onNavigateToPassDetail: (String) -> Unit,
    onNavigateToCreditCardDetail: (String) -> Unit,
    onNavigateToCryptoWalletDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var showFabMenu by remember { mutableStateOf(false) }
    
    val hideKeyboard = LocalSoftwareKeyboardController.current
    
    LaunchedEffect(uiState.isSearchActive) {
        if (!uiState.isSearchActive) {
            hideKeyboard?.hide()
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = uiState.isSearchActive,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                SearchTopBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onCloseSearch = onCloseSearch
                )
            }
            
            AnimatedVisibility(
                visible = !uiState.isSearchActive,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                MainTopBar(
                    onSearchClick = onToggleSearch,
                    onSettingsClick = onNavigateToSettings
                )
            }
        },
        floatingActionButton = {
            FabMenu(
                showFabMenu = showFabMenu,
                onToggleFabMenu = { showFabMenu = !showFabMenu },
                onNavigateToAddPass = onNavigateToAddPass,
                onNavigateToAddCreditCard = onNavigateToAddCreditCard,
                onNavigateToAddCryptoWallet = onNavigateToAddCryptoWallet,
                onDismissFabMenu = { showFabMenu = false }
            )
        }
    ) { paddingValues ->
        MainContent(
            uiState = uiState,
            paddingValues = paddingValues,
            onNavigateToPassDetail = onNavigateToPassDetail,
            onNavigateToCreditCardDetail = onNavigateToCreditCardDetail,
            onNavigateToCryptoWalletDetail = onNavigateToCryptoWalletDetail
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = stringResource(R.string.app_name),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search)
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_passes_and_wallets)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseSearch) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.close_search)
                )
            }
        },
        modifier = modifier
    )
}
