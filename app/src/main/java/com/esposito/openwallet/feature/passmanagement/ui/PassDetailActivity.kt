/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.passmanagement.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.esposito.openwallet.core.di.AppContainer
import com.esposito.openwallet.core.domain.model.BarcodeFormat
import com.esposito.openwallet.core.domain.model.WalletPass
import com.esposito.openwallet.core.ui.theme.OpenWalletTheme
import com.esposito.openwallet.core.util.PassTypeUtils
import com.esposito.openwallet.R
import kotlinx.coroutines.launch
import java.text.DateFormat

class PassDetailActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_PASS_ID = "extra_pass_id"
        
        fun createIntent(context: Context, passId: String): Intent {
            return Intent(context, PassDetailActivity::class.java).apply {
                putExtra(EXTRA_PASS_ID, passId)
            }
        }
    }
    
    private var originalBrightness: Float = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val passId = intent.getStringExtra(EXTRA_PASS_ID) ?: ""
        
        // Set maximum brightness
        setMaxBrightness()
        
        setContent {
            OpenWalletTheme {
                PassDetailScreen(
                    passId = passId,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Restore original brightness
        restoreOriginalBrightness()
    }
    
    private fun setMaxBrightness() {
        val layoutParams = window.attributes
        // Capture the current brightness before changing it
        originalBrightness = layoutParams.screenBrightness
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window.attributes = layoutParams
        
        // Keep screen on while viewing pass
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    private fun restoreOriginalBrightness() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = originalBrightness
        window.attributes = layoutParams
        
        // Remove keep screen on flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassDetailScreen(
    passId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = AppContainer.getRepository(context)
    val scope = rememberCoroutineScope()
    
    var walletPass by remember { mutableStateOf<WalletPass?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Load pass data
    LaunchedEffect(passId) {
        try {
            val pass = repository.getPassById(passId)
            walletPass = pass
            error = if (pass == null) context.getString(R.string.pass_not_found) else null
        } catch (e: Exception) {
            error = context.getString(R.string.error_loading_pass, e.message ?: "Unknown error")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(walletPass?.title ?: stringResource(R.string.pass_details)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            walletPass != null -> {
                PassDetailContent(
                    pass = walletPass!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                )
            }
        }
        
        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_pass)) },
                text = { 
                    Text(stringResource(R.string.delete_pass_confirmation))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            walletPass?.let { pass ->
                                scope.launch {
                                    try {
                                        repository.deletePass(pass)
                                        Toast.makeText(context, context.getString(R.string.pass_deleted), Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    } catch (_: Exception) {
                                        Toast.makeText(context, context.getString(R.string.failed_to_delete_pass), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            showDeleteDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.delete).uppercase(), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun PassDetailContent(
    pass: WalletPass,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Pass Info Card - Similar to crypto wallet header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = PassTypeUtils.getPassTypeIcon(pass.type.name),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = pass.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = PassTypeUtils.getPassTypeDisplayName(context, pass.type),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                if (pass.organizationName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pass.organizationName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // QR Code/Barcode Card - Similar to crypto wallet QR section  
        if (pass.barcodeData != null && !pass.barcodeData.isBlank()) {
            val effectiveFormat = when {
                pass.barcodeFormat == null || pass.barcodeFormat == BarcodeFormat.NONE -> BarcodeFormat.QR
                else -> pass.barcodeFormat
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (effectiveFormat) {
                            BarcodeFormat.QR -> stringResource(R.string.qr_code_label)
                            BarcodeFormat.PDF417 -> stringResource(R.string.pdf417_label)
                            BarcodeFormat.AZTEC -> stringResource(R.string.aztec_code_label)
                            BarcodeFormat.CODE128 -> stringResource(R.string.barcode_type_label)
                            BarcodeFormat.EAN13 -> stringResource(R.string.ean13_label)
                            BarcodeFormat.UPC_A -> stringResource(R.string.upc_a_label)
                            BarcodeFormat.DATA_MATRIX -> stringResource(R.string.data_matrix_label)
                            else -> stringResource(R.string.qr_code_label)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // QR Code/Barcode Display
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        QRCodeView(
                            data = pass.barcodeData,
                            format = effectiveFormat,
                            modifier = Modifier
                                .size(280.dp)
                                .background(Color.White)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Display barcode data text for verification
                    Text(
                        text = pass.barcodeData,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
        
        // Pass Information Card - Similar to crypto wallet info footer
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.pass_information),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Start
                )
                
                PassInfoRow(stringResource(R.string.type_label), PassTypeUtils.getPassTypeDisplayName(context, pass.type))

                if (pass.organizationName.isNotBlank()) {
                    PassInfoRow(stringResource(R.string.organization_label), pass.organizationName)
                }

                if (pass.serialNumber != null) {
                    PassInfoRow(stringResource(R.string.serial_number_label), pass.serialNumber)
                }
                
                pass.relevantDate?.let { date ->
                    PassInfoRow(stringResource(R.string.relevant_date_label), DateFormat.getDateTimeInstance().format(date))
                }
                
                pass.expirationDate?.let { date ->
                    PassInfoRow(stringResource(R.string.expires_label), DateFormat.getDateTimeInstance().format(date))
                }
                
                if (pass.description != null && pass.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = pass.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (pass.voided) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.pass_has_been_voided_warning),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Usage Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.present_screen_to_scanner_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.brightness_set_for_scanning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Bottom spacing
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PassInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}
