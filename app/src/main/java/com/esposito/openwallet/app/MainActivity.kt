/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.esposito.openwallet.core.data.local.manager.AppPreferencesManager
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.core.security.SecureActivity
import com.esposito.openwallet.core.ui.navigation.MainNavigationHandler
import com.esposito.openwallet.feature.authentication.ui.OnboardingActivity
import com.esposito.openwallet.feature.passmanagement.ui.AddPassScreen
import com.esposito.openwallet.core.ui.MainScreen
import com.esposito.openwallet.core.ui.MainViewModel
import com.esposito.openwallet.core.ui.theme.OpenWalletTheme
import com.esposito.openwallet.core.util.FileImportHandler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : SecureActivity() {
    
    @Inject
    lateinit var walletRepository: WalletRepository
    
    @Inject 
    lateinit var fileImportHandler: FileImportHandler
    
    @Inject
    lateinit var navigationHandler: MainNavigationHandler
    
    @Inject
    lateinit var appPreferences: AppPreferencesManager
    
    private val mainViewModel: MainViewModel by viewModels()
    
    private var pendingIntent: Intent? = null
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (shouldShowOnboarding()) {
            navigateToOnboarding()
            return
        }
        
        storePendingFileIntent()
        setupUI()
    }
    
    override fun onResume() {
        super.onResume()
        processPendingFileImport()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        // Handle file import intents
        if (isFileImportIntent(intent)) {
            pendingIntent = intent
        }
        
        // Handle ACTION_VIEW intents with data
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            pendingIntent = intent
        }
    }
    
    private fun shouldShowOnboarding(): Boolean = 
        !appPreferences.isOnboardingCompleted
    
    private fun navigateToOnboarding() {
        startActivity(OnboardingActivity.createIntent(this))
        finish()
    }
    
    private fun storePendingFileIntent() {
        if (isFileImportIntent(intent)) {
            pendingIntent = intent
        }
    }
    
    private fun isFileImportIntent(intent: Intent?): Boolean = 
        intent?.action == Intent.ACTION_VIEW && intent.data != null
    
    private fun setupUI() {
        setContent {
            OpenWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            val uiState by mainViewModel.uiState.collectAsState()
                            
                            MainScreen(
                                uiState = uiState,
                                onSearchQueryChange = mainViewModel::updateSearchQuery,
                                onToggleSearch = mainViewModel::toggleSearch,
                                onCloseSearch = mainViewModel::closeSearch,
                                onNavigateToAddPass = {
                                    navController.navigate("add_pass")
                                },
                                onNavigateToAddCryptoWallet = {
                                    navigationHandler.navigateToCryptoWalletCreation(this@MainActivity)
                                },
                                onNavigateToAddCreditCard = {
                                    navigationHandler.navigateToAddCreditCard(this@MainActivity)
                                },
                                onNavigateToPassDetail = { passId ->
                                    navigationHandler.navigateToPassDetail(this@MainActivity, passId)
                                },
                                onNavigateToCryptoWalletDetail = { walletId ->
                                    val id = walletId.toLongOrNull() ?: 0L
                                    navigationHandler.navigateToCryptoWalletDetail(this@MainActivity, id)
                                },
                                onNavigateToCreditCardDetail = { creditCardId ->
                                    navigationHandler.navigateToCreditCardDetail(this@MainActivity, creditCardId)
                                },
                                onNavigateToSettings = {
                                    navigationHandler.navigateToSettings(this@MainActivity)
                                },
                                onDeletePass = mainViewModel::deletePass,
                                onDeleteCreditCard = mainViewModel::deleteCreditCard,
                                onDeleteCryptoWallet = mainViewModel::deleteCryptoWallet
                            )
                        }
                        
                        composable("add_pass") {
                            AddPassScreen(
                                onBackClick = { navController.popBackStack() },
                                onPassCreated = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun processPendingFileImport() {
        pendingIntent?.let { intent ->
            intent.data?.let { uri ->
                fileImportHandler.handleFileImport(
                    context = this,
                    uri = uri,
                    repository = walletRepository,
                    lifecycleScope = lifecycleScope
                )
            }
            pendingIntent = null
        }
    }
}