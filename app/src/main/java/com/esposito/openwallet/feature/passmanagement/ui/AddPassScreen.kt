/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.passmanagement.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esposito.openwallet.feature.scanning.ui.BarcodeScanActivity
import com.esposito.openwallet.R
import com.esposito.openwallet.core.util.FileImportHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPassScreen(
    onBackClick: () -> Unit,
    onPassCreated: () -> Unit
) {
    val viewModel: AddPassViewModel = hiltViewModel()
    val context = LocalContext.current
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val passFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            val fileName = FileImportHandler.getFileName(context, fileUri)
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                viewModel.importPass(inputStream, fileName)
            }
        }
    }

    val createPassLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onPassCreated()
        }
    }

    val scanBarcodeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scannedData = result.data?.getStringExtra(BarcodeScanActivity.EXTRA_SCANNED_DATA)
            val barcodeFormat = result.data?.getStringExtra(BarcodeScanActivity.EXTRA_BARCODE_FORMAT)
            val intent = PassCreationActivity.createIntent(
                context = context,
                scannedData = scannedData,
                barcodeFormat = barcodeFormat
            )
            createPassLauncher.launch(intent)
        }
    }
    
    LaunchedEffect(importState) {
        when (val currentState = importState) {
            is AddPassViewModel.ImportState.Success -> {
                onPassCreated()
                viewModel.resetImportState()
            }
            is AddPassViewModel.ImportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Import failed: ${currentState.message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetImportState()
            }
            else -> { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.add_pass),
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = stringResource(R.string.choose_how_to_add_pass),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(R.string.add_pass_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            item {
                AddPassOptionCard(
                    icon = Icons.Default.GetApp,
                    title = stringResource(R.string.import_pass_file),
                    subtitle = stringResource(R.string.import_pass_description),
                    description = stringResource(R.string.import_file_supported_formats),
                    buttonText = if (importState is AddPassViewModel.ImportState.Loading) stringResource(R.string.importing) else stringResource(R.string.choose_file),
                    isLoading = importState is AddPassViewModel.ImportState.Loading,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { 
                        passFileLauncher.launch("*/*")
                    }
                )
            }

            item {
                AddPassOptionCard(
                    icon = Icons.Default.QrCodeScanner,
                    title = stringResource(R.string.scan_barcode_qr),
                    subtitle = stringResource(R.string.scan_barcode_description),
                    description = stringResource(R.string.scan_barcode_quick_create),
                    buttonText = stringResource(R.string.open_camera),
                    backgroundColor = MaterialTheme.colorScheme.tertiaryFixedDim,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        val intent = BarcodeScanActivity.createIntent(context)
                        scanBarcodeLauncher.launch(intent)
                    }
                )
            }

            item {
                AddPassOptionCard(
                    icon = Icons.Default.Add,
                    title = stringResource(R.string.create_new_pass),
                    subtitle = stringResource(R.string.create_pass_description),
                    description = stringResource(R.string.create_manual_pass_description),
                    buttonText = stringResource(R.string.start_creating),
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        val intent = PassCreationActivity.createIntent(context)
                        createPassLauncher.launch(intent)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AddPassOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String,
    buttonText: String,
    backgroundColor: Color,
    iconColor: Color,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = iconColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = iconColor
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
