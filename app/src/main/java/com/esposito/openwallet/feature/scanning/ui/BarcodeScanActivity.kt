/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.scanning.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.esposito.openwallet.core.util.SecureLogger
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.esposito.openwallet.core.security.SecureActivity
import com.esposito.openwallet.core.ui.theme.OpenWalletTheme
import com.esposito.openwallet.R
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScanActivity : SecureActivity() {
    
    companion object {
        const val EXTRA_SCANNED_DATA = "scanned_data"
        const val EXTRA_BARCODE_FORMAT = "barcode_format"
        
        fun createIntent(context: Context): Intent {
            return Intent(context, BarcodeScanActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenWalletTheme {
                BarcodeScanScreen(
                    onBarcodeScanned = { data, format ->
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_SCANNED_DATA, data)
                            putExtra(EXTRA_BARCODE_FORMAT, format)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onBackPressed = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun BarcodeScanScreen(
    onBarcodeScanned: (String, String) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isScanningEnabled by remember { mutableStateOf(true) }
    var scanMode by remember { mutableStateOf("qrcode") }
    var isFlashlightOn by remember { mutableStateOf(false) } // "qrcode" or "barcode"
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.barcode_camera_permission_description), Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Edge-to-edge layout with proper system bar handling
    Scaffold(
        containerColor = Color.Black,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                CameraPreview(
                    onBarcodeScanned = { data, format ->
                        if (isScanningEnabled) {
                            isScanningEnabled = false
                            onBarcodeScanned(data, format)
                        }
                    },
                    lifecycleOwner = lifecycleOwner,
                    scanMode = scanMode,
                    isFlashlightOn = isFlashlightOn
                )
            } else {
                // Permission denied state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.camera_permission_required),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.barcode_camera_permission_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) }
                    ) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            }

            // Top bar with back button and controls - positioned with system bar padding
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = paddingValues.calculateTopPadding() + 16.dp
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Top row with back button and title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.scanner),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Scanning overlay - centered properly
            if (hasCameraPermission) {
                ScanningOverlay(
                    modifier = Modifier.align(Alignment.Center),
                    scanMode = scanMode
                )
            }

            // Bottom controls with mode selection and flashlight
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Instructions
                    Icon(
                        imageVector = if (scanMode == "qrcode") Icons.Default.QrCode
                                     else Icons.Default.ViewStream,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (scanMode == "qrcode") stringResource(R.string.position_qr_code_frame)
                               else stringResource(R.string.position_barcode_frame),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // QR Code selection
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { scanMode = "qrcode" }
                        ) {
                            RadioButton(
                                selected = scanMode == "qrcode",
                                onClick = { scanMode = "qrcode" }
                            )
                            Text(
                                text = stringResource(R.string.qr_code),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Flashlight button
                        IconButton(
                            onClick = { isFlashlightOn = !isFlashlightOn },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (isFlashlightOn) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                           else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn
                                             else Icons.Default.FlashlightOff,
                                contentDescription = stringResource(R.string.toggle_flashlight_description),
                                tint = if (isFlashlightOn) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Barcode selection
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { scanMode = "barcode" }
                        ) {
                            RadioButton(
                                selected = scanMode == "barcode",
                                onClick = { scanMode = "barcode" }
                            )
                            Text(
                                text = stringResource(R.string.barcode),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    onBarcodeScanned: (String, String) -> Unit,
    lifecycleOwner: LifecycleOwner,
    scanMode: String,
    isFlashlightOn: Boolean
) {
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    // Update flashlight when state changes OR when camera is recreated
    LaunchedEffect(isFlashlightOn, camera) {
        camera?.let {
            if (it.cameraInfo.hasFlashUnit()) {
                it.cameraControl.enableTorch(isFlashlightOn)
            }
        }
    }
    
    key(scanMode) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                previewView
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(onBarcodeScanned, scanMode))
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val newCamera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                        camera = newCamera
                        
                        // Restore flashlight state immediately after camera binding
                        if (newCamera.cameraInfo.hasFlashUnit()) {
                            newCamera.cameraControl.enableTorch(isFlashlightOn)
                        }
                    } catch (e: Exception) {
                        SecureLogger.e("BarcodeScan", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(previewView.context))
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ScanningOverlay(
    modifier: Modifier = Modifier,
    scanMode: String
) {
    Box(
        modifier = modifier.size(320.dp),
        contentAlignment = Alignment.Center
    ) {
        // Adaptive scanning frame based on scan mode (enlarged)
        val frameSize = if (scanMode == "qrcode") 260.dp else 340.dp
        val frameHeight = if (scanMode == "barcode") 150.dp else frameSize
        
        Card(
            modifier = Modifier
                .size(width = frameSize, height = frameHeight)
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(
                width = 4.dp,
                color = Color.White
            )
        ) {}
    }
}

class BarcodeAnalyzer(
    private val onBarcodeScanned: (String, String) -> Unit,
    scanMode: String
) : ImageAnalysis.Analyzer {
    
    private var scanner = createScanner(scanMode)
    
    private fun createScanner(mode: String) = if (mode == "qrcode") {
        BarcodeScanning.getClient(
            com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    } else {
        // For barcode mode, exclude QR codes to avoid confusion
        BarcodeScanning.getClient(
            com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_CODABAR,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_PDF417,
                    Barcode.FORMAT_AZTEC,
                    Barcode.FORMAT_DATA_MATRIX
                    // Note: Removed Barcode.FORMAT_QR_CODE from barcode mode
                )
                .build()
        )
    }
    
    private var lastScannedTime = 0L
    private val scanThrottleMs = 2000L // 2 second throttle
    
    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val currentTime = System.currentTimeMillis()
                    if (barcodes.isNotEmpty() && currentTime - lastScannedTime > scanThrottleMs) {
                        lastScannedTime = currentTime
                        val barcode = barcodes.first()
                        val rawValue = barcode.rawValue
                        val format = when (barcode.format) {
                            Barcode.FORMAT_QR_CODE -> "QR_CODE"
                            Barcode.FORMAT_CODE_128 -> "CODE_128"
                            Barcode.FORMAT_CODE_39 -> "CODE_39"
                            Barcode.FORMAT_EAN_13 -> "EAN_13"
                            Barcode.FORMAT_EAN_8 -> "EAN_8"
                            Barcode.FORMAT_UPC_A -> "UPC_A"
                            Barcode.FORMAT_UPC_E -> "UPC_E"
                            Barcode.FORMAT_PDF417 -> "PDF417"
                            Barcode.FORMAT_AZTEC -> "AZTEC"
                            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
                            else -> "UNKNOWN"
                        }
                        rawValue?.let { onBarcodeScanned(it, format) }
                    }
                }
                .addOnFailureListener {
                    // Handle scanning failure
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
