/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.ui.navigation

import android.content.Context
import com.esposito.openwallet.feature.creditcard.ui.AddCreditCardActivity
import com.esposito.openwallet.feature.creditcard.ui.CreditCardDetailActivity
import com.esposito.openwallet.feature.cryptowallet.ui.CryptoWalletCreationActivity
import com.esposito.openwallet.feature.cryptowallet.ui.CryptoWalletDetailActivity
import com.esposito.openwallet.feature.passmanagement.ui.PassDetailActivity
import com.esposito.openwallet.feature.settings.ui.SettingsActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized navigation handler
 */
@Singleton
class MainNavigationHandler @Inject constructor() {
    
    /**
     * Navigate to crypto wallet creation screen
     */
    fun navigateToCryptoWalletCreation(context: Context) {
        val intent = CryptoWalletCreationActivity.createIntent(context)
        context.startActivity(intent)
    }
    
    /**
     * Navigate to credit card addition screen
     */
    fun navigateToAddCreditCard(context: Context) {
        val intent = AddCreditCardActivity.createIntent(context)
        context.startActivity(intent)
    }
    
    /**
     * Navigate to pass detail screen
     */
    fun navigateToPassDetail(context: Context, passId: String) {
        val intent = PassDetailActivity.createIntent(context, passId)
        context.startActivity(intent)
    }
    
    /**
     * Navigate to crypto wallet detail screen
     */
    fun navigateToCryptoWalletDetail(context: Context, walletId: Long) {
        val intent = CryptoWalletDetailActivity.createIntent(context, walletId)
        context.startActivity(intent)
    }
    
    /**
     * Navigate to credit card detail screen
     */
    fun navigateToCreditCardDetail(context: Context, creditCardId: String) {
        val intent = CreditCardDetailActivity.createIntentFromCreditCardId(context, creditCardId)
        context.startActivity(intent)
    }
    
    /**
     * Navigate to settings screen
     */
    fun navigateToSettings(context: Context) {
        val intent = SettingsActivity.createIntent(context)
        context.startActivity(intent)
    }
}
