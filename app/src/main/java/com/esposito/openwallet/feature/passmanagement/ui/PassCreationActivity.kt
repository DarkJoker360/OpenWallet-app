/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.passmanagement.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.core.domain.model.PassType
import com.esposito.openwallet.core.domain.model.WalletPass
import com.esposito.openwallet.core.ui.theme.OpenWalletTheme
import com.esposito.openwallet.core.util.PassTypeUtils
import com.esposito.openwallet.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * Convert scanner barcode format to app's BarcodeFormat enum
 */
private fun mapToBarcodeFormat(scannerFormat: String): com.esposito.openwallet.core.domain.model.BarcodeFormat {
    return when (scannerFormat) {
        "QR_CODE" -> com.esposito.openwallet.core.domain.model.BarcodeFormat.QR
        "CODE_128" -> com.esposito.openwallet.core.domain.model.BarcodeFormat.CODE128
        "EAN_13" -> com.esposito.openwallet.core.domain.model.BarcodeFormat.EAN13
        "UPC_A" -> com.esposito.openwallet.core.domain.model.BarcodeFormat.UPC_A
        "PDF417" -> com.esposito.openwallet.core.domain.model.BarcodeFormat.PDF417
        "AZTEC" -> com.esposito.openwallet.core.domain.model.BarcodeFormat.AZTEC
        "DATA_MATRIX" -> com.esposito.openwallet.core.domain.model.BarcodeFormat.DATA_MATRIX
        // Map other supported formats to closest equivalent or QR as fallback
        "CODE_39", "CODE_93", "CODABAR", "EAN_8", "ITF", "UPC_E" -> com.esposito.openwallet.core.domain.model.BarcodeFormat.CODE128
        else -> com.esposito.openwallet.core.domain.model.BarcodeFormat.QR // Default fallback
    }
}

@AndroidEntryPoint
class PassCreationActivity : ComponentActivity() {
    
    @Inject
    lateinit var walletRepository: WalletRepository
    
    companion object {
        const val EXTRA_PASS_TYPE = "pass_type"
        const val EXTRA_SCANNED_DATA = "scanned_data"
        const val EXTRA_BARCODE_FORMAT = "barcode_format"
        
        fun createIntent(
            context: Context, 
            passType: PassType? = null,
            scannedData: String? = null,
            barcodeFormat: String? = null
        ): Intent {
            return Intent(context, PassCreationActivity::class.java).apply {
                passType?.let { putExtra(EXTRA_PASS_TYPE, it.name) }
                scannedData?.let { putExtra(EXTRA_SCANNED_DATA, it) }
                barcodeFormat?.let { putExtra(EXTRA_BARCODE_FORMAT, it) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val passTypeName = intent.getStringExtra(EXTRA_PASS_TYPE)
        val initialPassType = passTypeName?.let { 
            try { 
                PassType.valueOf(it) 
            } catch (_: IllegalArgumentException) {
                null 
            } 
        }
        val scannedData = intent.getStringExtra(EXTRA_SCANNED_DATA)
        val barcodeFormat = intent.getStringExtra(EXTRA_BARCODE_FORMAT)
        
        setContent {
            OpenWalletTheme {
                PassCreationScreen(
                    initialPassType = initialPassType,
                    scannedData = scannedData,
                    barcodeFormat = barcodeFormat,
                    onPassCreated = { pass ->
                        savePassToDatabase(pass)
                    },
                    onBackPressed = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
    
    private fun savePassToDatabase(pass: WalletPass) {
        lifecycleScope.launch {
            try {
                walletRepository.insertPass(pass)
                runOnUiThread {
                    Toast.makeText(this@PassCreationActivity, getString(R.string.pass_created_successfully), Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@PassCreationActivity, getString(R.string.failed_to_create_pass, e.message ?: getString(R.string.unknown_error_occurred)), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassCreationScreen(
    initialPassType: PassType? = null,
    scannedData: String? = null,
    barcodeFormat: String? = null,
    onPassCreated: (WalletPass) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    var selectedPassType by remember { mutableStateOf(initialPassType ?: PassType.GENERIC) }
    var showPassTypeSelector by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // String resources
    val pleaseEnterPassName = stringResource(R.string.please_enter_pass_name)
    val failedToCreatePass = stringResource(R.string.failed_to_create_pass_generic)
    
    // Form fields
    var passName by remember { mutableStateOf("") }
    var passDescription by remember { mutableStateOf("") }
    var barcodeValue by remember { mutableStateOf(scannedData ?: "") }
    var selectedBarcodeFormat by remember { mutableStateOf(barcodeFormat ?: "QR_CODE") }
    var passNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var issuerName by remember { mutableStateOf("") }
    var backgroundColor by remember { mutableStateOf("#1976D2") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.create_new_pass),
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pass Type Selection
            var passTypeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = passTypeExpanded,
                onExpandedChange = { passTypeExpanded = !passTypeExpanded }
            ) {
                OutlinedTextField(
                    value = PassTypeUtils.getPassTypeDisplayName(context, selectedPassType),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.pass_type)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = passTypeExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = passTypeExpanded,
                    onDismissRequest = { passTypeExpanded = false }
                ) {
                    PassType.entries.forEach { passType ->
                        DropdownMenuItem(
                            text = { Text("${passType.icon} ${PassTypeUtils.getPassTypeDisplayName(context, passType)}") },
                            onClick = {
                                selectedPassType = passType
                                passTypeExpanded = false
                            }
                        )
                    }
                }
            }

            // Pass Name
            OutlinedTextField(
                value = passName,
                onValueChange = { passName = it },
                label = { Text(stringResource(R.string.pass_name_required_asterisk)) },
                placeholder = { Text(stringResource(R.string.pass_name_example)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Issuer/Organization Name
            OutlinedTextField(
                value = issuerName,
                onValueChange = { issuerName = it },
                label = { Text(stringResource(R.string.issuer_organization)) },
                placeholder = { Text(stringResource(R.string.issuer_example)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Pass Number/ID
            OutlinedTextField(
                value = passNumber,
                onValueChange = { passNumber = it },
                label = { Text(stringResource(R.string.pass_number_id)) },
                placeholder = { Text(stringResource(R.string.pass_number_example)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true
            )

            // Barcode Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.barcode_qr_code),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Barcode Value
                    OutlinedTextField(
                        value = barcodeValue,
                        onValueChange = { barcodeValue = it },
                        label = { Text(stringResource(R.string.barcode_data)) },
                        placeholder = { Text(stringResource(R.string.barcode_data_example)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        supportingText = {
                            Text(stringResource(R.string.barcode_data_encoding_description))
                        }
                    )

                    // Barcode Format Selection
                    var barcodeFormatExpanded by remember { mutableStateOf(false) }
                    val barcodeFormats = listOf("QR_CODE", "CODE_128", "EAN_13", "UPC_A", "PDF417", "AZTEC", "DATA_MATRIX")
                    
                    ExposedDropdownMenuBox(
                        expanded = barcodeFormatExpanded,
                        onExpandedChange = { barcodeFormatExpanded = !barcodeFormatExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBarcodeFormat,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.barcode_format)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = barcodeFormatExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = barcodeFormatExpanded,
                            onDismissRequest = { barcodeFormatExpanded = false }
                        ) {
                            barcodeFormats.forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format) },
                                    onClick = {
                                        selectedBarcodeFormat = format
                                        barcodeFormatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Description
            OutlinedTextField(
                value = passDescription,
                onValueChange = { passDescription = it },
                label = { Text(stringResource(R.string.description)) },
                placeholder = { Text(stringResource(R.string.optional_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            // Expiry Date (if applicable)
            if (selectedPassType == PassType.EVENT_TICKET || 
                selectedPassType == PassType.COUPON ||
                selectedPassType == PassType.STORE_CARD) {
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text(stringResource(R.string.expiry_date_optional)) },
                    placeholder = { Text(stringResource(R.string.expiry_date_format_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Create Button
            Button(
                onClick = {
                    if (passName.isBlank()) {
                        errorMessage = pleaseEnterPassName
                        showError = true
                        return@Button
                    }
                    
                    isLoading = true
                    try {
                        val newPass = WalletPass(
                            id = UUID.randomUUID().toString(),
                            type = selectedPassType,
                            title = passName.trim(),
                            description = passDescription.trim().takeIf { it.isNotBlank() },
                            organizationName = issuerName.trim().takeIf { it.isNotBlank() } ?: "",
                            passData = "{}",
                            barcodeData = barcodeValue.trim().takeIf { it.isNotBlank() },
                            barcodeFormat = if (barcodeValue.trim().isNotBlank()) 
                                mapToBarcodeFormat(selectedBarcodeFormat) 
                                else null,
                            backgroundColor = backgroundColor,
                            createdAt = Date(),
                            updatedAt = Date(),
                            isImported = false
                        )
                        onPassCreated(newPass)
                    } catch (e: Exception) {
                        errorMessage = e.message ?: failedToCreatePass
                        showError = true
                    } finally {
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && passName.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.create_pass))
                }
            }
        }
    }

    // Error Dialog
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Pass Type Selector Dialog
    if (showPassTypeSelector) {
        PassTypeSelector(
            selectedPassType = selectedPassType,
            onPassTypeSelected = { passType ->
                selectedPassType = passType
                showPassTypeSelector = false
            },
            onDismiss = { showPassTypeSelector = false }
        )
    }
}
