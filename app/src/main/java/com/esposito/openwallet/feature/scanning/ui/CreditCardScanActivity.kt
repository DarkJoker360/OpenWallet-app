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
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.esposito.openwallet.R
import com.esposito.openwallet.core.ui.theme.OpenWalletTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Serializable
import java.util.Calendar

/**
 * Simple data class for credit card scan results
 */
data class CreditCardScanResult(
    val cardNumber: String = "",
    val cardholderName: String = "",
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val confidence: Float = 0.0f
) : Serializable {

    fun hasCardNumber(): Boolean = cardNumber.length in 13..19 && cardNumber.all { it.isDigit() }
    fun hasName(): Boolean = cardholderName.length > 2
    fun hasExpiry(): Boolean = expiryMonth.isNotEmpty() && expiryYear.isNotEmpty()
    
    fun isSimilarTo(other: CreditCardScanResult): Boolean {
        return cardNumber == other.cardNumber && 
               cardholderName == other.cardholderName &&
               expiryMonth == other.expiryMonth &&
               expiryYear == other.expiryYear
    }
    
    fun getDataQuality(): String {
        val items = mutableListOf<String>()
        if (hasCardNumber()) items.add("number")
        if (hasName()) items.add("name")
        if (hasExpiry()) items.add("expiry")
        
        return when (items.size) {
            0 -> "no data"
            1 -> "minimal (${items[0]} only)"
            2 -> "partial (${items.joinToString(", ")})"
            3 -> "complete"
            else -> "unknown"
        }
    }
}

/**
 * Smart scanning helper functions
 */
fun isResultSignificantlyBetter(current: CreditCardScanResult, new: CreditCardScanResult): Boolean {
    val currentScore = getCompletionScore(current)
    val newScore = getCompletionScore(new)
    return newScore > currentScore + 0.1f
}

fun getCompletionScore(result: CreditCardScanResult): Float {
    var score = 0f
    if (result.hasCardNumber()) score += 0.6f
    if (result.hasName()) score += 0.2f
    if (result.hasExpiry()) score += 0.2f
    return score
}

fun shouldCompleteScanning(result: CreditCardScanResult, stableCount: Int, startTime: Long): Boolean {
    val elapsedTime = System.currentTimeMillis() - startTime
    val hasMinimumData = result.hasCardNumber()
    val hasGoodData = result.hasCardNumber() && (result.hasName() || result.hasExpiry())
    val hasCompleteData = result.hasCardNumber() && result.hasName() && result.hasExpiry()
    
    return when {
        // Complete data found - finish quickly
        hasCompleteData && stableCount >= 2 -> true
        // Good data found - wait a bit longer
        hasGoodData && stableCount >= 3 && elapsedTime > 3000 -> true
        // Minimum data found - wait longer but complete eventually
        hasMinimumData && stableCount >= 5 && elapsedTime > 5000 -> true
        // Timeout scenarios
        hasMinimumData && elapsedTime > 10000 -> true // 10 seconds max
        hasGoodData && elapsedTime > 8000 -> true // 8 seconds for good data
        hasCompleteData && elapsedTime > 4000 -> true // 4 seconds for complete data
        else -> false
    }
}

fun getSmartScanStatus(result: CreditCardScanResult, context: Context): String {
    val quality = result.getDataQuality()
    return when {
        result.hasCardNumber() && result.hasName() && result.hasExpiry() -> 
            context.getString(R.string.complete_card_data_detected)
        result.hasCardNumber() && (result.hasName() || result.hasExpiry()) -> 
            context.getString(R.string.good_data_found_scanning_more, quality)
        result.hasCardNumber() -> 
            context.getString(R.string.card_number_found_looking_for_more)
        else -> 
            context.getString(R.string.scanning_for_card_details)
    }
}

fun getCompletionMessage(result: CreditCardScanResult, context: Context): String {
    return when {
        result.hasCardNumber() && result.hasName() && result.hasExpiry() -> 
            context.getString(R.string.all_card_details_detected)
        result.hasCardNumber() && result.hasExpiry() -> 
            context.getString(R.string.essential_details_found)
        result.hasCardNumber() && result.hasName() -> 
            context.getString(R.string.card_number_and_name_found)
        result.hasCardNumber() -> 
            context.getString(R.string.card_number_detected)
        else -> 
            context.getString(R.string.partial_data_detected)
    }
}

class CreditCardScanActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_SCAN_RESULT = "scan_result"
        
        fun createIntent(context: Context): Intent {
            return Intent(context, CreditCardScanActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenWalletTheme {
                CreditCardScanScreen(
                    onCardScanned = { result ->
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_SCAN_RESULT, result)
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
fun CreditCardScanScreen(
    onCardScanned: (CreditCardScanResult) -> Unit,
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
    var isFlashlightOn by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf(context.getString(R.string.position_card_in_frame)) }
    var currentScanResult by remember { mutableStateOf(CreditCardScanResult()) }
    var scanStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var stableDetectionCount by remember { mutableIntStateOf(0) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.credit_card_scanner_permission_required), Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = Color.Black,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                CameraPreview(
                    onCardDetected = { result ->
                        if (isScanningEnabled) {
                            // Smart completion logic
                            val isSignificantlyBetter = isResultSignificantlyBetter(currentScanResult, result)
                            
                            if (isSignificantlyBetter) {
                                currentScanResult = result
                                stableDetectionCount = 1
                                scanStatus = getSmartScanStatus(result, context)
                            } else if (result.isSimilarTo(currentScanResult)) {
                                stableDetectionCount++
                                scanStatus = getSmartScanStatus(result, context)
                                
                                // Auto-complete based on intelligent criteria
                                if (shouldCompleteScanning(result, stableDetectionCount, scanStartTime)) {
                                    isScanningEnabled = false
                                    showCompletionDialog = true
                                }
                            }
                        }
                    },
                    onScanStatusChanged = { status ->
                        if (isScanningEnabled) {
                            scanStatus = status
                        }
                    },
                    lifecycleOwner = lifecycleOwner,
                    isFlashlightOn = isFlashlightOn
                )
                
                // Scanning overlay
                ScanningOverlay()
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
                        text = stringResource(R.string.camera_permission_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) }
                    ) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            }

            // Top bar with back button and controls
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.credit_card_scanner_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = scanStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Bottom instructions and controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
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
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.scan_instructions_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.scan_instructions_lighting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.scan_instructions_steady),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Flashlight control
                    if (hasCameraPermission) {
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
                                contentDescription = stringResource(R.string.toggle_flashlight),
                                tint = if (isFlashlightOn) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Smart completion dialog
    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { showCompletionDialog = false },
            title = { Text(stringResource(R.string.card_data_detected)) },
            text = {
                Column {
                    Text(stringResource(R.string.found_following_information))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (currentScanResult.cardNumber.isNotEmpty()) {
                        Text(stringResource(R.string.card_number_format, currentScanResult.cardNumber.take(4)))
                    }
                    if (currentScanResult.cardholderName.isNotEmpty()) {
                        Text(stringResource(R.string.cardholder_format, currentScanResult.cardholderName))
                    }
                    if (currentScanResult.expiryMonth.isNotEmpty() && currentScanResult.expiryYear.isNotEmpty()) {
                        Text(stringResource(R.string.expiry_format, currentScanResult.expiryMonth, currentScanResult.expiryYear))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getCompletionMessage(currentScanResult, context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompletionDialog = false
                        onCardScanned(currentScanResult)
                    }
                ) {
                    Text(stringResource(R.string.use_this_data))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCompletionDialog = false
                        isScanningEnabled = true
                        stableDetectionCount = 0
                        scanStartTime = System.currentTimeMillis()
                        scanStatus = context.getString(R.string.continue_scanning_for_more_details)
                    }
                ) {
                    Text(stringResource(R.string.continue_scanning))
                }
            }
        )
    }
}

@Composable
fun CameraPreview(
    onCardDetected: (CreditCardScanResult) -> Unit,
    onScanStatusChanged: (String) -> Unit,
    lifecycleOwner: LifecycleOwner,
    isFlashlightOn: Boolean
) {
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    DisposableEffect(isFlashlightOn) {
        camera?.cameraControl?.enableTorch(isFlashlightOn)
        onDispose { }
    }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                            .build()
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analyzer ->
                        analyzer.setAnalyzer(
                            ContextCompat.getMainExecutor(ctx)
                        ) { imageProxy ->
                            processImageForCreditCard(imageProxy, onCardDetected, onScanStatusChanged, ctx)
                        }
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                    
                    // Enable flashlight if requested
                    if (isFlashlightOn) {
                        camera?.cameraControl?.enableTorch(true)
                    }
                    
                    SecureLogger.d("CreditCardScan", "Camera bound successfully")
                    
                } catch (exc: Exception) {
                    SecureLogger.e("CreditCardScan", "Camera binding failed", exc)
                    onScanStatusChanged(ctx.getString(R.string.camera_error_occurred))
                }
                
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processImageForCreditCard(
    imageProxy: ImageProxy,
    onCardDetected: (CreditCardScanResult) -> Unit,
    onScanStatusChanged: (String) -> Unit,
    context: Context
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        onScanStatusChanged(context.getString(R.string.scanning_text))
        
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val allText = visionText.textBlocks.joinToString(" ") { it.text }
                SecureLogger.d("CreditCardScan", "Detected text: $allText")
                
                if (allText.isNotBlank()) {
                    val result = extractCreditCardData(allText)
                    SecureLogger.d("CreditCardScan", "Credit card data extracted")
                    onCardDetected(result)
                } else {
                    onScanStatusChanged(context.getString(R.string.no_text_detected_adjust_lighting))
                }
            }
            .addOnFailureListener { e ->
                SecureLogger.e("CreditCardScan", "Text recognition failed", e)
                onScanStatusChanged(context.getString(R.string.scan_error_try_again))
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

private fun extractCreditCardData(text: String): CreditCardScanResult {
    SecureLogger.d("CreditCardScan", "Processing text: $text")
    
    // Regexs
    val cardNumberPattern = Regex("""(\d{4}[\s\-]?\d{4}[\s\-]?\d{4}[\s\-]?\d{3,4})""")
    val simpleNumberPattern = Regex("""\d{13,19}""")
    val expiryPattern = Regex("""(0[1-9]|1[0-2])[/\-]?(\d{2}|\d{4})""")
    val expiryPattern2 = Regex("""(\d{1,2})[/\-](\d{2,4})""")
    val namePattern = Regex("""([A-Z]{2,}\s+[A-Z]{2,}(?:\s+[A-Z]+)*)""")
    val namePattern2 = Regex("""([A-Z]+\s+[A-Z]+)""")
    
    var cardNumber = ""
    var expiryMonth = ""
    var expiryYear = ""
    var cardholderName = ""
    
    // Extract card number - multiple number patterns
    cardNumberPattern.find(text)?.let { match ->
        val candidate = match.value.replace(Regex("""[\s\-]"""), "")
        SecureLogger.d("CreditCardScan", "Found card candidate: $candidate")
        if (candidate.length in 13..19 && candidate.all { it.isDigit() }) {
            if (isValidLuhn(candidate)) {
                cardNumber = candidate
                SecureLogger.d("CreditCardScan", "Valid card number found")
            }
        }
    }
    
    // Extract card number - simple number pattern
    if (cardNumber.isEmpty()) {
        simpleNumberPattern.findAll(text).forEach { match ->
            val candidate = match.value
            SecureLogger.d("CreditCardScan", "Trying simple pattern: $candidate")
            if (candidate.length in 13..19 && isValidLuhn(candidate)) {
                cardNumber = candidate
                SecureLogger.d("CreditCardScan", "Valid simple card number found")
                return@forEach
            }
        }
    }
    
    // Extract expiry date
    expiryPattern.find(text)?.let { match ->
        expiryMonth = match.groupValues[1]
        expiryYear = match.groupValues[2]
        SecureLogger.d("CreditCardScan", "Found expiry: $expiryMonth/$expiryYear")
        
        // Convert 2-digit year to 4-digit if needed
        if (expiryYear.length == 2) {
            val year = expiryYear.toInt()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val currentDecade = currentYear % 100
            expiryYear = if (year < currentDecade - 5) {
                (2000 + year + 100).toString()
            } else {
                (2000 + year).toString()
            }
        }
    }
    
    // If first pattern fails, try second
    if (expiryMonth.isEmpty()) {
        expiryPattern2.find(text)?.let { match ->
            val month = match.groupValues[1].toIntOrNull()
            val year = match.groupValues[2]
            if (month != null && month in 1..12) {
                expiryMonth = month.toString().padStart(2, '0')
                expiryYear = year
                SecureLogger.d("CreditCardScan", "Found expiry (pattern 2): $expiryMonth/$expiryYear")
            }
        }
    }
    
    // Extract cardholder name
    namePattern.find(text)?.let { match ->
        val candidate = match.value.trim()
        // Avoid common false positives
        if (!candidate.contains(Regex("""(VISA|MASTERCARD|AMEX|DISCOVER|CREDIT|CARD|BANK|DEBIT|VALID|THRU)"""))) {
            cardholderName = candidate
            SecureLogger.d("CreditCardScan", "Found name: $cardholderName")
        }
    }
    
    // Try simpler name pattern if no name found
    if (cardholderName.isEmpty()) {
        namePattern2.findAll(text).forEach { match ->
            val candidate = match.value.trim()
            if (!candidate.contains(Regex("""(VISA|MASTERCARD|AMEX|DISCOVER|CREDIT|CARD|BANK|DEBIT|VALID|THRU)"""))) {
                cardholderName = candidate
                SecureLogger.d("CreditCardScan", "Found simple name: $cardholderName")
                return@forEach
            }
        }
    }
    
    val confidence = calculateConfidence(cardNumber, expiryMonth, expiryYear, cardholderName)
    SecureLogger.d("CreditCardScan", "Final result - Confidence: $confidence")
    
    return CreditCardScanResult(
        cardNumber = cardNumber,
        cardholderName = cardholderName,
        expiryMonth = expiryMonth,
        expiryYear = expiryYear,
        confidence = confidence
    )
}

private fun isValidLuhn(cardNumber: String): Boolean {
    if (cardNumber.length < 13) return false
    
    var sum = 0
    var alternate = false
    
    for (i in cardNumber.length - 1 downTo 0) {
        var digit = cardNumber[i].toString().toInt()
        
        if (alternate) {
            digit *= 2
            if (digit > 9) {
                digit = (digit % 10) + 1
            }
        }
        
        sum += digit
        alternate = !alternate
    }
    
    return sum % 10 == 0
}

private fun calculateConfidence(cardNumber: String, expiryMonth: String, expiryYear: String, cardholderName: String): Float {
    var confidence = 0f
    
    if (cardNumber.length in 13..19 && isValidLuhn(cardNumber)) {
        confidence += 0.5f // Main confidence from valid card number
    } else if (cardNumber.length in 13..19) {
        confidence += 0.2f // Partial credit for right length
    }
    
    if (expiryMonth.matches(Regex("""0[1-9]|1[0-2]""")) && expiryYear.length >= 2) {
        confidence += 0.25f
    }
    
    if (cardholderName.length > 3 && cardholderName.contains(" ")) {
        confidence += 0.25f
    }
    
    SecureLogger.d("CreditCardScan", "Confidence calculation: $confidence")
    
    return confidence
}

@Composable
fun ScanningOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val cardWidth = canvasWidth * 0.8f
        val cardHeight = cardWidth / 1.586f
        val cardLeft = (canvasWidth - cardWidth) / 2
        val cardTop = (canvasHeight - cardHeight) / 2

        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )

        drawRect(
            color = Color.Transparent,
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardWidth, cardHeight)
        )

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
