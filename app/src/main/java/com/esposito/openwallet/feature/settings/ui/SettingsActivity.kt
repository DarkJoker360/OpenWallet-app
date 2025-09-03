/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.settings.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.fragment.app.FragmentActivity
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.esposito.openwallet.R
import com.esposito.openwallet.BuildConfig
import com.esposito.openwallet.core.data.local.manager.AppPreferencesManager
import com.esposito.openwallet.core.data.local.manager.TestDataManager
import com.esposito.openwallet.core.di.AppContainer
import com.esposito.openwallet.core.security.SecureActivity
import com.esposito.openwallet.core.security.SecurityManager
import com.esposito.openwallet.core.ui.theme.OpenWalletTheme

class SettingsActivity : SecureActivity() {
    
    private lateinit var appPrefs: AppPreferencesManager
    private lateinit var securityManager: SecurityManager
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        appPrefs = AppPreferencesManager(this)
        securityManager = SecurityManager(this)
        
        setContent {
            OpenWalletTheme {
                SettingsScreen(
                    onBackPressed = { finish() },
                    appPrefs = appPrefs,
                    securityManager = securityManager,
                    onScreenshotBlockingChanged = { enabled ->
                        appPrefs.isScreenshotBlockingEnabled = enabled
                        // Apply the change immediately to this activity
                        securityManager.applyScreenshotBlocking(this@SettingsActivity)
                    }
                )
            }
        }
    }
    
    /**
     * Authenticate using biometric before allowing security changes
     */
    fun authenticateWithBiometric(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!securityManager.isBiometricAvailable()) {
            onError(getString(R.string.biometric_authentication_not_available))
            return
        }
        
        securityManager.authenticateWithBiometric(
            activity = this,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    appPrefs: AppPreferencesManager,
    securityManager: SecurityManager,
    onScreenshotBlockingChanged: (Boolean) -> Unit
) {
    var isScreenshotBlockingEnabled by remember { 
        mutableStateOf(appPrefs.isScreenshotBlockingEnabled) 
    }
    var isBiometricLockEnabled by remember { 
        mutableStateOf(appPrefs.isBiometricLockEnabled) 
    }
    
    // Check biometric availability
    val isBiometricAvailable = securityManager.isBiometricAvailable()
    
    // Check if device has PIN/Pattern/Password
    val hasDeviceLock = securityManager.isDeviceSecure()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar with consistent styling
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
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

            // Settings Content with Material 3 design
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // App Info Section
                AppInfoCard()

                // Security Section
                SecuritySettingsCard(
                    isScreenshotBlockingEnabled = isScreenshotBlockingEnabled,
                    onScreenshotBlockingChanged = { enabled ->
                        isScreenshotBlockingEnabled = enabled
                        onScreenshotBlockingChanged(enabled)
                    },
                    isBiometricLockEnabled = isBiometricLockEnabled,
                    onBiometricLockChanged = { enabled ->
                        isBiometricLockEnabled = enabled
                        appPrefs.isBiometricLockEnabled = enabled
                    },
                    isBiometricAvailable = isBiometricAvailable,
                    hasDeviceLock = hasDeviceLock
                )
                
                // Developer Options Section (only in debug builds)
                @Suppress("KotlinConstantConditions")
                if (BuildConfig.ENABLE_DEVELOPER_OPTIONS) {
                    DeveloperOptionsCard(appPrefs = appPrefs)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SecuritySettingsCard(
    isScreenshotBlockingEnabled: Boolean,
    onScreenshotBlockingChanged: (Boolean) -> Unit,
    isBiometricLockEnabled: Boolean,
    onBiometricLockChanged: (Boolean) -> Unit,
    isBiometricAvailable: Boolean,
    hasDeviceLock: Boolean
) {
    val context = LocalActivity.current as SettingsActivity
    
    // Authentication callback for security settings
    val authenticateAndExecute = { action: () -> Unit ->
        if (isBiometricAvailable) {
            context.authenticateWithBiometric(
                onSuccess = { action() },
                onError = { error ->
                    Toast.makeText(context, context.getString(R.string.authentication_failed, error), Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, context.getString(R.string.biometric_authentication_not_available), Toast.LENGTH_SHORT).show()
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.security_settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Screenshot Blocking Setting
            SecuritySettingRow(
                title = stringResource(R.string.block_screenshots),
                subtitle = stringResource(R.string.block_screenshots_description),
                icon = Icons.Default.RemoveRedEye,
                isEnabled = isScreenshotBlockingEnabled,
                onToggle = { enabled ->
                        onScreenshotBlockingChanged(enabled)
                }
            )
            
            // Biometric Lock Setting
            SecuritySettingRow(
                title = stringResource(R.string.lock_with_pin_biometrics),
                subtitle = if (!hasDeviceLock) {
                    stringResource(R.string.setup_device_lock_first)
                } else if (!isBiometricAvailable) {
                    stringResource(R.string.use_device_pin_pattern_password)
                } else {
                    stringResource(R.string.use_biometric_or_device_lock)
                },
                icon = if (isBiometricAvailable) Icons.Default.Fingerprint else Icons.Default.Lock,
                isEnabled = isBiometricLockEnabled,
                onToggle = { enabled ->
                    authenticateAndExecute {
                        onBiometricLockChanged(enabled)
                    }
                },
                isAvailable = hasDeviceLock
            )
        }
    }
}

@Composable
private fun SecuritySettingRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    isAvailable: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isAvailable) MaterialTheme.colorScheme.onSurfaceVariant 
                      else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isAvailable) MaterialTheme.colorScheme.onSurfaceVariant 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            enabled = isAvailable
        )
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Icon
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            // App Name
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Version
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            // Description
            Text(
                text = stringResource(R.string.app_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DeveloperOptionsCard(appPrefs: AppPreferencesManager) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity ?: return // Ensure we have FragmentActivity
    var isGeneratingData by remember { mutableStateOf(false) }
    var isClearingData by remember { mutableStateOf(false) }
    var dataStats by remember { mutableStateOf<Map<String, Int>?>(null) }
    
    val testDataManager = remember { 
        TestDataManager(
            walletRepository = AppContainer.getRepository(context)
        )
    }
    
    // Load current data statistics
    LaunchedEffect(Unit) {
        dataStats = testDataManager.getDataStatistics()
    }
    
    // Refresh stats after operations
    LaunchedEffect(isGeneratingData, isClearingData) {
        if (!isGeneratingData && !isClearingData) {
            dataStats = testDataManager.getDataStatistics()
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.developer_options),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Current Data Stats
            if (dataStats != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.current_data),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.credit_cards_count, dataStats!!["creditCards"] as Int),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Text(
                                text = stringResource(R.string.crypto_wallets_count, dataStats!!["cryptoWallets"] as Int),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = stringResource(R.string.wallet_passes_count, dataStats!!["walletPasses"] as Int),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // Generate Test Data Button
            Button(
                onClick = {
                    isGeneratingData = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGeneratingData && !isClearingData,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isGeneratingData) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isGeneratingData) stringResource(R.string.generating_test_data) else stringResource(R.string.generate_test_data),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Clear All Data Button
            OutlinedButton(
                onClick = {
                    isClearingData = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGeneratingData && !isClearingData
            ) {
                if (isClearingData) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isClearingData) stringResource(R.string.clearing_data) else stringResource(R.string.clear_all_data),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Reset Onboarding Button
            OutlinedButton(
                onClick = {
                    appPrefs.clearOnboardingData()
                    Toast.makeText(context, context.getString(R.string.onboarding_reset_message), Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGeneratingData && !isClearingData
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.reset_onboarding),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    
    // Handle data generation in launched effect to avoid composition issues
    LaunchedEffect(isGeneratingData) {
        if (isGeneratingData) {
            try {
                testDataManager.generateMockupData(activity)
                dataStats = testDataManager.getDataStatistics()
                Toast.makeText(
                    context,
                    context.getString(R.string.test_data_generated_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_generating_test_data, e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isGeneratingData = false
            }
        }
    }
    
    // Handle data clearing in launched effect to avoid composition issues
    LaunchedEffect(isClearingData) {
        if (isClearingData) {
            try {
                testDataManager.clearAllData()
                dataStats = testDataManager.getDataStatistics()
                Toast.makeText(
                    context,
                    context.getString(R.string.all_data_cleared_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_clearing_data, e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isClearingData = false
            }
        }
    }
}