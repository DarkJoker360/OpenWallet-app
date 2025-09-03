/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.creditcard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.esposito.openwallet.core.util.SecureLogger
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.esposito.openwallet.core.data.local.database.SecureWalletDatabase
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.model.FinancialValidationUtils
import com.esposito.openwallet.core.domain.model.PassType
import com.esposito.openwallet.core.domain.model.SecureCreditCard
import com.esposito.openwallet.core.domain.model.WalletPass
import com.esposito.openwallet.core.di.AppContainer
import com.esposito.openwallet.core.security.SecureActivity
import com.esposito.openwallet.core.ui.theme.OpenWalletTheme
import com.esposito.openwallet.R
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.Locale

class CreditCardDetailActivity : SecureActivity() {
    
    companion object {
        private const val EXTRA_PASS_ID = "pass_id"
        private const val EXTRA_CREDIT_CARD_ID = "credit_card_id"
        
        fun createIntent(context: Context, passId: String): Intent {
            return Intent(context, CreditCardDetailActivity::class.java).apply {
                putExtra(EXTRA_PASS_ID, passId)
            }
        }
        
        fun createIntentFromCreditCardId(context: Context, creditCardId: String): Intent {
            return Intent(context, CreditCardDetailActivity::class.java).apply {
                putExtra(EXTRA_CREDIT_CARD_ID, creditCardId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val passId = intent.getStringExtra(EXTRA_PASS_ID)
        val creditCardId = intent.getStringExtra(EXTRA_CREDIT_CARD_ID)
        
        if (passId == null && creditCardId == null) {
            finish()
            return
        }

        setContent {
            OpenWalletTheme {
                if (passId != null) {
                    CreditCardDetailScreen(
                        passId = passId,
                        onBack = { finish() }
                    )
                } else {
                    CreditCardDetailScreenFromCreditCard(
                        creditCardId = creditCardId!!,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDetailScreen(
    passId: String,
    onBack: () -> Unit
) {
    val context = LocalActivity.current as CreditCardDetailActivity
    
    // Load pass from database using passId
    var pass by remember { mutableStateOf<WalletPass?>(null) }
    var creditCardData by remember { mutableStateOf<CreditCard?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Global sensitive data state shared between components
    var showSensitiveData by remember { mutableStateOf(false) }
    var decryptedCardNumber by remember { mutableStateOf<String?>(null) }
    var decryptedCVV by remember { mutableStateOf<String?>(null) }
    var decryptedIBAN by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(passId) {
        try {
            // Load pass from database using passId
            val database = SecureWalletDatabase.getDatabase(context)
            val loadedPass = database.walletPassDao().getPassById(passId)
            pass = loadedPass
            if (loadedPass != null) {
                creditCardData = FinancialValidationUtils.extractCreditCardData(loadedPass)
                SecureLogger.d("CreditCardDetail", "Card data loaded")
            } else {
                SecureLogger.w("CreditCardDetail", "Pass not found")
            }
            isLoading = false
        } catch (e: Exception) {
            SecureLogger.e("CreditCardDetail", "Error loading pass", e)
            error = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(creditCardData?.cardNickname ?: creditCardData?.cardType?.displayName ?: stringResource(R.string.credit_card))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_card), tint = MaterialTheme.colorScheme.error)
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
                        Text(
                            text = stringResource(R.string.error_loading_credit_card),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            creditCardData != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        // Advanced credit card with rotation and all features
                        CreditCardComponent(
                            creditCardData = creditCardData!!,
                            onClick = { 
                                // Card click action if needed
                            },
                            showFlipButton = true,
                            cardHeight = 240.dp, // Larger card for detail view
                            showSensitiveData = showSensitiveData,
                            decryptedCardNumber = decryptedCardNumber,
                            decryptedCVV = decryptedCVV,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    item {
                        // Credit Card Details Section (Combined sensitive and non-sensitive info)
                        CreditCardDetailsCard(
                            creditCardData = creditCardData!!,
                            creditCardId = passId,
                            showSensitiveData = showSensitiveData,
                            decryptedCardNumber = decryptedCardNumber,
                            decryptedCVV = decryptedCVV,
                            decryptedIBAN = decryptedIBAN,
                            onSensitiveDataToggle = { show ->
                                showSensitiveData = show
                                if (!show) {
                                    decryptedCardNumber = null
                                    decryptedCVV = null
                                    decryptedIBAN = null
                                }
                            },
                            onDecryptedDataUpdate = { cardNumber, cvv, iban ->
                                decryptedCardNumber = cardNumber
                                decryptedCVV = cvv
                                decryptedIBAN = iban
                            }
                        )
                    }
                }
            }
        }
        
        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_credit_card)) },
                text = { 
                    Text(stringResource(R.string.delete_credit_card_confirmation))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pass?.let { walletPass ->
                                context.lifecycleScope.launch {
                                try {
                                    val repository = AppContainer.getRepository(context)
                                    repository.deletePass(walletPass)
                                    Toast.makeText(context, context.getString(R.string.credit_card_deleted), Toast.LENGTH_SHORT).show()
                                    onBack()
                                } catch (_: Exception) {
                                    Toast.makeText(context, context.getString(R.string.failed_to_delete_credit_card), Toast.LENGTH_SHORT).show()
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
private fun CardInfoRowWithCopy(
    label: String,
    value: String,
    showCopyButton: Boolean,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    isSensitive: Boolean = false,
    sensitiveValue: String? = null,
    maskedValue: String? = null,
    showSensitiveData: Boolean = false
) {
    val context = LocalActivity.current as CreditCardDetailActivity
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    isSensitive -> {
                        if (showSensitiveData) {
                            when (sensitiveValue) {
                                "NOT_IMPLEMENTED" -> context.getString(R.string.decryption_not_yet_implemented)
                                "NO_ENCRYPTED_DATA" -> context.getString(R.string.no_encrypted_data_available)
                                "NO_PASS_FOUND" -> context.getString(R.string.encrypted_data_not_found)
                                "DECRYPTION_ERROR" -> context.getString(R.string.decryption_failed)
                                else -> sensitiveValue ?: maskedValue ?: value
                            }
                        } else {
                            maskedValue ?: value
                        }
                    }
                    else -> value
                },
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                color = if (isSensitive && showSensitiveData && sensitiveValue == null) 
                            MaterialTheme.colorScheme.error 
                        else MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Show copy button based on conditions
        val shouldShowCopyButton = when {
            isSensitive -> showCopyButton && showSensitiveData && sensitiveValue != null && 
                          !listOf("NOT_IMPLEMENTED", "NO_ENCRYPTED_DATA", "NO_PASS_FOUND", "DECRYPTION_ERROR").contains(sensitiveValue)
            else -> showCopyButton
        }
        
        if (shouldShowCopyButton) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = context.getString(R.string.copy_content_description, label),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CreditCardDetailsCard(
    creditCardData: CreditCard,
    creditCardId: String,
    showSensitiveData: Boolean,
    decryptedCardNumber: String?,
    decryptedCVV: String?,
    decryptedIBAN: String?,
    onSensitiveDataToggle: (Boolean) -> Unit,
    onDecryptedDataUpdate: (String?, String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalActivity.current as CreditCardDetailActivity
    
    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.copied_to_clipboard, label), Toast.LENGTH_SHORT).show()
    }
    
    // Decrypt data when authentication succeeds
    LaunchedEffect(showSensitiveData) {
        if (showSensitiveData && (decryptedCardNumber == null || decryptedCVV == null || (creditCardData.iban != null && decryptedIBAN == null))) {
            // Decrypt sensitive data using enhanced security-aware method
            try {
                val database = SecureWalletDatabase.getDatabase(context)
                val creditCard = database.creditCardDao().getCreditCardById(creditCardId)
                
                creditCard?.let { card ->
                    SecureLogger.d("CreditCardDetail", "Starting decryption")
                    
                    // Only enhanced security - no migration, no legacy fallback
                    val secureCard = SecureCreditCard(
                        id = card.id,
                        cardHolderName = card.cardHolderName,
                        bankName = card.issuerBank,
                        cardType = card.cardType.name,
                        expiryMonth = card.expiryMonth.toString().padStart(2, '0'),
                        expiryYear = card.expiryYear.toString(),
                        encryptedCardNumber = card.encryptedFullCardNumber,
                        encryptedCVV = card.encryptedCVV,
                        encryptedIBAN = card.encryptedIBAN,
                        hasEnhancedSecurity = true
                    )
                    
                    SecureCreditCard.decryptSensitiveData(
                        activity = context,
                        card = secureCard,
                        onSuccess = { decryptedData ->
                            onDecryptedDataUpdate(
                                decryptedData.cardNumber ?: "",
                                decryptedData.cvv ?: "",
                                decryptedData.iban ?: ""
                            )
                            SecureLogger.d("CreditCardDetail", "Decryption completed")
                        },
                        onError = { error ->
                            SecureLogger.e("CreditCardDetail", "Decryption failed")
                            Toast.makeText(context, context.getString(R.string.failed_to_decrypt_sensitive_data), Toast.LENGTH_LONG).show()
                        }
                    )
                } ?: run {
                    SecureLogger.w("CreditCardDetail", "Credit card not found")
                    onDecryptedDataUpdate("NO_CREDIT_CARD_FOUND", "NO_CREDIT_CARD_FOUND", "NO_CREDIT_CARD_FOUND")
                }
            } catch (e: Exception) {
                SecureLogger.e("CreditCardDetail", "Decryption error", e)
                onDecryptedDataUpdate("DECRYPTION_ERROR", "DECRYPTION_ERROR", "DECRYPTION_ERROR")
            }
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.credit_card_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Card Type with no copy button
            CardInfoRowWithCopy(stringResource(R.string.card_type), creditCardData.cardType.displayName, showCopyButton = false, onCopy = { })
            
            // Issuer with copy button
            CardInfoRowWithCopy(stringResource(R.string.issuer), creditCardData.issuerBank, showCopyButton = true, onCopy = {
                copyToClipboard(context.getString(R.string.issuer), creditCardData.issuerBank)
            })
            
            // SWIFT/BIC with copy button
            creditCardData.swiftCode?.let { swift ->
                CardInfoRowWithCopy(stringResource(R.string.swift_bic), swift, showCopyButton = true, onCopy = {
                    copyToClipboard(context.getString(R.string.swift_bic), swift)
                })
            }
            
            // ABA Routing with copy button  
            creditCardData.abaRoutingNumber?.let { aba ->
                CardInfoRowWithCopy(stringResource(R.string.aba_routing), aba, showCopyButton = true, onCopy = {
                    copyToClipboard(context.getString(R.string.aba_routing), aba)
                })
            }
            
            // Status
            CardInfoRowWithCopy(stringResource(R.string.status), if (creditCardData.isActive) stringResource(R.string.active) else stringResource(R.string.inactive), showCopyButton = false, onCopy = { })
            
            // Primary Payment Method
            if (creditCardData.isPrimary) {
                CardInfoRowWithCopy(stringResource(R.string.payment_method), stringResource(R.string.primary), showCopyButton = false, onCopy = { })
            }
            
            // Full Card Number Section
            CardInfoRowWithCopy(
                label = stringResource(R.string.card_number),
                value = creditCardData.maskedCardNumber,
                showCopyButton = true,
                onCopy = { decryptedCardNumber?.let { copyToClipboard(context.getString(R.string.card_number), it) } },
                isSensitive = true,
                sensitiveValue = decryptedCardNumber?.chunked(4)?.joinToString(" "),
                maskedValue = creditCardData.maskedCardNumber,
                showSensitiveData = showSensitiveData
            )
            
            // CVV Section
            CardInfoRowWithCopy(
                label = stringResource(R.string.cvv_code),
                value = "•••",
                showCopyButton = true,
                onCopy = { decryptedCVV?.let { copyToClipboard(context.getString(R.string.cvv), it) } },
                isSensitive = true,
                sensitiveValue = decryptedCVV,
                maskedValue = "•••",
                showSensitiveData = showSensitiveData
            )
            
            // IBAN Section (only show if IBAN exists)
            if (creditCardData.iban != null) {
                Column {
                    CardInfoRowWithCopy(
                        label = stringResource(R.string.iban),
                        value = creditCardData.iban, // This is already masked in the CreditCard model
                        showCopyButton = true,
                        onCopy = { decryptedIBAN?.let { copyToClipboard(context.getString(R.string.iban), it) } },
                        isSensitive = true,
                        sensitiveValue = decryptedIBAN?.let { FinancialValidationUtils.formatIBAN(it) },
                        maskedValue = creditCardData.iban, // This is already masked
                        showSensitiveData = showSensitiveData
                    )
                    
                    // TODO: Add country extraction methods to FinancialValidationUtils if needed
                    /*
                    // Show country info if IBAN is available
                    if (showSensitiveData && decryptedIBAN != null) {
                        val countryCode = IBANUtils.extractCountryCode(decryptedIBAN)
                        val countryName = countryCode?.let { IBANUtils.getCountryName(it) }
                        if (countryName != null) {
                            Text(
                                text = "Country: $countryName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 0.dp, top = 4.dp)
                            )
                        }
                    }
                    */
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Show Full Details Button
            Button(
                onClick = {
                    if (showSensitiveData) {
                        // Hide sensitive data
                        onSensitiveDataToggle(false)
                    } else {
                        // Directly toggle to show - let the decryption handle authentication
                        SecureLogger.d("CreditCardDetail", "Decryption requested")
                        onSensitiveDataToggle(true)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showSensitiveData) 
                        MaterialTheme.colorScheme.error 
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (showSensitiveData) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showSensitiveData) stringResource(R.string.hide_sensitive_details) else stringResource(R.string.show_sensitive_details),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDetailScreenFromCreditCard(
    creditCardId: String,
    onBack: () -> Unit
) {
    val context = LocalActivity.current as CreditCardDetailActivity
    
    // Load credit card directly from new table
    var creditCard by remember { mutableStateOf<CreditCard?>(null) }
    var creditCardData by remember { mutableStateOf<CreditCard?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Global sensitive data state shared between components
    var showSensitiveData by remember { mutableStateOf(false) }
    var decryptedCardNumber by remember { mutableStateOf<String?>(null) }
    var decryptedCVV by remember { mutableStateOf<String?>(null) }
    var decryptedIBAN by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(creditCardId) {
        try {
            val database = SecureWalletDatabase.getDatabase(context)
            val loadedCreditCard = database.creditCardDao().getCreditCardById(creditCardId)
            creditCard = loadedCreditCard
            if (loadedCreditCard != null) {
                // Convert CreditCard entity to CreditCard for UI
                creditCardData = CreditCard(
                    id = loadedCreditCard.id,
                    maskedCardNumber = loadedCreditCard.maskedCardNumber,
                    cardHolderName = loadedCreditCard.cardHolderName,
                    cardType = loadedCreditCard.cardType,
                    issuerBank = loadedCreditCard.issuerBank,
                    expiryMonth = loadedCreditCard.expiryMonth,
                    expiryYear = loadedCreditCard.expiryYear,
                    cardNickname = loadedCreditCard.cardNickname,
                    isActive = loadedCreditCard.isActive,
                    isPrimary = loadedCreditCard.isPrimary,
                    contactlessEnabled = loadedCreditCard.contactlessEnabled,
                    iban = loadedCreditCard.iban,
                    swiftCode = loadedCreditCard.swiftCode,
                    abaRoutingNumber = loadedCreditCard.abaRoutingNumber,
                    encryptedFullCardNumber = loadedCreditCard.encryptedFullCardNumber,
                    encryptedCVV = loadedCreditCard.encryptedCVV,
                    encryptedIBAN = loadedCreditCard.encryptedIBAN
                )
                SecureLogger.d("CreditCardDetail", "Card data loaded")
            } else {
                SecureLogger.w("CreditCardDetail", "Credit card not found")
                error = context.getString(R.string.credit_card_not_found)
            }
            isLoading = false
        } catch (e: Exception) {
            SecureLogger.e("CreditCardDetail", "Error loading credit card", e)
            error = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(creditCardData?.cardNickname ?: creditCardData?.cardType?.displayName ?: stringResource(R.string.credit_card))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_card), tint = MaterialTheme.colorScheme.error)
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
                        Text(
                            text = stringResource(R.string.error_loading_credit_card),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            creditCardData != null && creditCard != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        // Create a fake WalletPass for the CreditCardComponent to work with
                        val fakePass = WalletPass(
                            id = creditCard!!.id,
                            type = PassType.CREDIT_CARD,
                            title = creditCardData!!.cardNickname ?: "${creditCardData!!.cardType.displayName} Card",
                            description = "Expires ${String.format("%02d", creditCardData!!.expiryMonth, Locale.getDefault())}/${creditCardData!!.expiryYear}",
                            organizationName = creditCardData!!.issuerBank,
                            logoText = creditCardData!!.cardType.displayName,
                            foregroundColor = "#FFFFFF",
                            backgroundColor = creditCardData!!.cardType.primaryColor,
                            serialNumber = creditCardData!!.maskedCardNumber.takeLast(4),
                            passData = "{}",
                            barcodeData = null,
                            barcodeFormat = null
                        )
                        
                        // Advanced credit card with rotation and all features
                        CreditCardComponent(
                            creditCardData = creditCardData!!,
                            onClick = { 
                                // Card click action if needed
                            },
                            showFlipButton = true,
                            cardHeight = 240.dp, // Larger card for detail view
                            showSensitiveData = showSensitiveData,
                            decryptedCardNumber = decryptedCardNumber,
                            decryptedCVV = decryptedCVV,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    item {
                        // Credit Card Details Section (Combined sensitive and non-sensitive info)
                        CreditCardDetailsCard(
                            creditCardData = creditCardData!!,
                            creditCardId = creditCardId,
                            showSensitiveData = showSensitiveData,
                            decryptedCardNumber = decryptedCardNumber,
                            decryptedCVV = decryptedCVV,
                            decryptedIBAN = decryptedIBAN,
                            onSensitiveDataToggle = { show ->
                                showSensitiveData = show
                                if (!show) {
                                    decryptedCardNumber = null
                                    decryptedCVV = null
                                    decryptedIBAN = null
                                }
                            },
                            onDecryptedDataUpdate = { cardNumber, cvv, iban ->
                                decryptedCardNumber = cardNumber
                                decryptedCVV = cvv
                                decryptedIBAN = iban
                            }
                        )
                    }
                }
            }
        }
        
        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_credit_card)) },
                text = { 
                    Text(stringResource(R.string.delete_credit_card_confirmation))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            creditCard?.let { card ->
                                context.lifecycleScope.launch {
                                    try {
                                        val database = SecureWalletDatabase.getDatabase(context)
                                        database.creditCardDao().deleteCreditCard(card)
                                        Toast.makeText(context, context.getString(R.string.credit_card_deleted), Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } catch (_: Exception) {
                                        Toast.makeText(context, context.getString(R.string.failed_to_delete_credit_card), Toast.LENGTH_SHORT).show()
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
