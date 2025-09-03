/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.esposito.openwallet.R
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.feature.passmanagement.ui.WalletPassCard
import com.esposito.openwallet.feature.cryptowallet.ui.CryptoWalletCard
import com.esposito.openwallet.core.ui.MainUiState
import com.esposito.openwallet.feature.creditcard.ui.CreditCardComponent

@Composable
fun MainContent(
    uiState: MainUiState,
    paddingValues: PaddingValues,
    onNavigateToPassDetail: (String) -> Unit,
    onNavigateToCreditCardDetail: (String) -> Unit,
    onNavigateToCryptoWalletDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        val hasAnyContent = uiState.filteredPasses.isNotEmpty() || 
                           uiState.filteredCreditCards.isNotEmpty() || 
                           uiState.filteredCryptoWallets.isNotEmpty()
        
        if (!hasAnyContent) {
            EmptyState(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            WalletItemsList(
                uiState = uiState,
                onNavigateToPassDetail = onNavigateToPassDetail,
                onNavigateToCreditCardDetail = onNavigateToCreditCardDetail,
                onNavigateToCryptoWalletDetail = onNavigateToCryptoWalletDetail
            )
        }
    }
}

@Composable
private fun WalletItemsList(
    uiState: MainUiState,
    onNavigateToPassDetail: (String) -> Unit,
    onNavigateToCreditCardDetail: (String) -> Unit,
    onNavigateToCryptoWalletDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, 
            end = 16.dp, 
            top = 8.dp, 
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Crypto Wallets Section
        if (uiState.filteredCryptoWallets.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.crypto_wallets))
            }
            
            items(
                items = uiState.filteredCryptoWallets,
                key = { wallet -> wallet.id },
                contentType = { "crypto_wallet" }
            ) { wallet ->
                CryptoWalletCard(
                    wallet = wallet,
                    onClick = {                 onNavigateToCryptoWalletDetail(wallet.id.toString()) }
                )
            }
            
            if (uiState.filteredCreditCards.isNotEmpty() || uiState.filteredPasses.isNotEmpty()) {
                item { SectionSpacer() }
            }
        }
        
        // Credit Cards Section
        if (uiState.filteredCreditCards.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.credit_cards))
            }
            
            items(
                items = uiState.filteredCreditCards,
                key = { creditCard -> creditCard.id },
                contentType = { "credit_card" }
            ) { creditCard ->
                CreditCardItem(
                    creditCard = creditCard,
                    onNavigateToCreditCardDetail = onNavigateToCreditCardDetail
                )
            }
            
            if (uiState.filteredPasses.isNotEmpty()) {
                item { SectionSpacer() }
            }
        }
        
        // Passes & Cards Section
        if (uiState.filteredPasses.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.passes_and_cards))
            }
            
            items(
                items = uiState.filteredPasses,
                key = { pass -> pass.id },
                contentType = { pass -> "pass_${pass.type}" }
            ) { pass ->
                // Convert Pass back to WalletPass for the component
                val walletPass = com.esposito.openwallet.core.domain.model.WalletPass(
                    id = pass.id,
                    title = pass.organizationName, // organizationName now contains the actual pass name/title
                    organizationName = pass.organizationName,
                    description = pass.description,
                    type = com.esposito.openwallet.core.domain.model.PassType.valueOf(pass.type),
                    serialNumber = pass.serialNumber,
                    barcodeData = pass.barcodeMessage,
                    relevantDate = pass.relevantDate,
                    expirationDate = pass.expirationDate,
                    backgroundColor = pass.backgroundColor,
                    foregroundColor = pass.foregroundColor,
                    labelColor = pass.labelColor,
                    logoText = pass.logoText,
                    voided = pass.isVoided,
                    createdAt = pass.createdAt,
                    updatedAt = pass.updatedAt,
                    passData = "{}"
                )
                
                WalletPassCard(
                    pass = walletPass,
                    onClick = { onNavigateToPassDetail(pass.id) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SectionSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.height(16.dp))
}

@Composable
private fun CreditCardItem(
    creditCard: CreditCard,
    onNavigateToCreditCardDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CreditCardComponent(
        creditCardData = CreditCard(
            maskedCardNumber = creditCard.maskedCardNumber,
            cardHolderName = creditCard.cardHolderName,
            expiryMonth = creditCard.expiryMonth,
            expiryYear = creditCard.expiryYear,
            cardType = creditCard.cardType,
            issuerBank = creditCard.issuerBank,
            iban = null
        ),
        onClick = { onNavigateToCreditCardDetail(creditCard.id) },
        modifier = modifier.fillMaxWidth()
    )
}
