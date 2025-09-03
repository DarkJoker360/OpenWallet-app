package com.esposito.openwallet.feature.creditcard.ui

import android.os.Bundle
import com.esposito.openwallet.core.util.SecureLogger
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.esposito.openwallet.R
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.model.CreditCardType
import com.esposito.openwallet.core.domain.model.FinancialValidationUtils
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.core.domain.model.SecureCreditCard
import com.esposito.openwallet.core.security.SecureActivity
import com.esposito.openwallet.core.util.ABAOffsetMapping
import com.esposito.openwallet.core.util.CreditCardOffsetMapping
import com.esposito.openwallet.core.util.IBANOffsetMapping
import com.esposito.openwallet.core.util.SWIFTOffsetMapping
import com.esposito.openwallet.core.ui.theme.OpenWalletTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class CreditCardCreationActivity : SecureActivity() {
    
    @Inject
    lateinit var walletRepository: WalletRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Extract scan data from intent extras
        val scannedCardNumber = intent.getStringExtra("card_number") ?: ""
        val scannedCardholderName = intent.getStringExtra("cardholder_name") ?: ""
        val scannedExpiryDate = intent.getStringExtra("expiry_date") ?: ""
        
        setContent {
            OpenWalletTheme {
                CreditCardCreationScreen(
                    onSave = { cardNumber, cardHolderName, expiryMonth, expiryYear, cvv, issuerBank, iban, swiftCode, abaRoutingNumber, cardNickname, detectedCardType ->
                        saveCreditCardWithRawData(
                            cardNumber, cardHolderName, expiryMonth, expiryYear, cvv, 
                            issuerBank, iban, swiftCode, abaRoutingNumber, cardNickname, detectedCardType
                        )
                    },
                    onCancel = {
                        finish()
                    },
                    initialCardNumber = scannedCardNumber,
                    initialCardholderName = scannedCardholderName,
                    initialExpiryDate = scannedExpiryDate
                )
            }
        }
    }
    
    // Removed old saveCreditCard method - now using saveCreditCardWithRawData directly
    
    private fun saveCreditCardWithRawData(
        cardNumber: String,
        cardHolderName: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvv: String,
        issuerBank: String,
        iban: String,
        swiftCode: String,
        abaRoutingNumber: String,
        cardNickname: String,
        detectedCardType: CreditCardType
    ) {
        SecureLogger.d("CreditCardCreation", "Starting secure credit card creation")
        
        SecureCreditCard.createWithEncryption(
            activity = this,
            cardHolderName = cardHolderName,
            bankName = issuerBank,
            cardType = detectedCardType.name,
            cardNumber = cardNumber,
            cvv = cvv,
            iban = iban,
            expiryMonth = expiryMonth.toString(),
            expiryYear = expiryYear.toString(),
            notes = cardNickname,
            color = detectedCardType.primaryColor,
            onSuccess = { secureCard ->
                SecureLogger.d("CreditCardCreation", "Credit card created successfully")
                lifecycleScope.launch {
                    try {
                        // Convert SecureCreditCard to CreditCard for database compatibility
                        val creditCard = CreditCard(
                            id = secureCard.id,
                            cardHolderName = secureCard.cardHolderName,
                            maskedCardNumber = if (cardNumber.length >= 4) {
                                "••••-••••-••••-${cardNumber.takeLast(4)}"
                            } else {
                                cardNumber
                            },
                            cardType = detectedCardType,
                            issuerBank = secureCard.bankName,
                            expiryMonth = secureCard.expiryMonth.toIntOrNull() ?: 1,
                            expiryYear = secureCard.expiryYear.toIntOrNull() ?: 2025,
                            cardNickname = secureCard.notes,
                            isActive = true,
                            isPrimary = false,
                            contactlessEnabled = true,
                            createdAt = Date(secureCard.createdAt),
                            updatedAt = Date(secureCard.updatedAt),
                            iban = iban.takeIf { it.isNotEmpty() }?.let { FinancialValidationUtils.maskIBAN(it) },
                            swiftCode = swiftCode.takeIf { it.isNotEmpty() },
                            abaRoutingNumber = abaRoutingNumber.takeIf { it.isNotEmpty() },
                            // Store enhanced encrypted data with special prefix to identify it
                            encryptedFullCardNumber = "ENHANCED:${secureCard.encryptedCardNumber}",
                            encryptedCVV = "ENHANCED:${secureCard.encryptedCVV}",
                            encryptedIBAN = if (secureCard.encryptedIBAN != null) "ENHANCED:${secureCard.encryptedIBAN}" else null
                        )
                        walletRepository.insertCreditCard(creditCard)
                        
                        SecureLogger.d("CreditCardCreation", "Credit card saved to database")
                        runOnUiThread {
                            Toast.makeText(this@CreditCardCreationActivity, 
                                getString(R.string.credit_card_saved_success), Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        SecureLogger.e("CreditCardCreation", "Database save failed", e)
                        runOnUiThread {
                            Toast.makeText(this@CreditCardCreationActivity, 
                                getString(R.string.failed_to_save_secure_card, e.message), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onError = { error ->
                SecureLogger.e("CreditCardCreation", "Credit card creation failed: $error")
                Toast.makeText(this, getString(R.string.enhanced_encryption_failed, error), Toast.LENGTH_LONG).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardCreationScreen(
    onSave: (String, String, Int, Int, String, String, String, String, String, String, CreditCardType) -> Unit,
    onCancel: () -> Unit,
    initialCardNumber: String = "",
    initialCardholderName: String = "",
    initialExpiryDate: String = ""
) {
    var cardNumber by rememberSaveable { mutableStateOf(initialCardNumber) }
    var cardHolderName by rememberSaveable { mutableStateOf(initialCardholderName) }
    var expiryMonth by rememberSaveable { mutableStateOf("") }
    var expiryYear by rememberSaveable { mutableStateOf("") }
    var cvv by rememberSaveable { mutableStateOf("") }
    var issuerBank by rememberSaveable { mutableStateOf("") }
    var iban by rememberSaveable { mutableStateOf("") }
    var swiftCode by rememberSaveable { mutableStateOf("") }
    var abaRoutingNumber by rememberSaveable { mutableStateOf("") }
    var cardNickname by rememberSaveable { mutableStateOf("") }
    
    // Parse initial expiry date if provided
    LaunchedEffect(initialExpiryDate) {
        if (initialExpiryDate.isNotEmpty() && initialExpiryDate.contains("/")) {
            val parts = initialExpiryDate.split("/")
            if (parts.size == 2) {
                expiryMonth = parts[0].padStart(2, '0')
                expiryYear = parts[1]
            }
        }
    }
    
    var showCvv by rememberSaveable { mutableStateOf(false) }
    var cardNumberError by rememberSaveable { mutableStateOf<String?>(null) }
    var expiryError by rememberSaveable { mutableStateOf<String?>(null) }
    var ibanError by rememberSaveable { mutableStateOf<String?>(null) }
    var swiftError by rememberSaveable { mutableStateOf<String?>(null) }
    var abaError by rememberSaveable { mutableStateOf<String?>(null) }
    var showPreview by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Auto-detect card type
    val detectedCardType = remember(cardNumber) {
        FinancialValidationUtils.detectCardType(cardNumber)
    }
    
    // Validate card number in real-time
    LaunchedEffect(cardNumber) {
        cardNumberError = when {
            cardNumber.isEmpty() -> null
            !FinancialValidationUtils.validateCardNumber(cardNumber) -> context.getString(R.string.invalid_card_number)
            else -> null
        }
    }
    
    // Validate expiry date
    LaunchedEffect(expiryMonth, expiryYear) {
        expiryError = when {
            expiryMonth.isEmpty() || expiryYear.isEmpty() -> null
            expiryMonth.toIntOrNull()?.let { it < 1 || it > 12 } == true -> context.getString(R.string.invalid_month)
            expiryYear.toIntOrNull()?.let { it < Calendar.getInstance().get(Calendar.YEAR) } == true -> context.getString(R.string.card_expired)
            else -> null
        }
    }
    
    // Validate IBAN in real-time
    LaunchedEffect(iban) {
        ibanError = when {
            iban.isEmpty() -> null // IBAN is optional
            !FinancialValidationUtils.validateIBAN(iban) -> context.getString(R.string.invalid_iban_format)
            else -> null
        }
    }
    
    // Validate SWIFT code in real-time
    LaunchedEffect(swiftCode) {
        swiftError = when {
            swiftCode.isEmpty() -> null // SWIFT is optional
            !FinancialValidationUtils.validateSWIFTCode(swiftCode) -> context.getString(R.string.invalid_swift_bic_format)
            else -> null
        }
    }
    
    // Validate ABA routing number in real-time
    LaunchedEffect(abaRoutingNumber) {
        abaError = when {
            abaRoutingNumber.isEmpty() -> null // ABA is optional
            !FinancialValidationUtils.validateABARoutingNumber(abaRoutingNumber) -> context.getString(R.string.invalid_aba_routing_number)
            else -> null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_credit_card)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showPreview = !showPreview }
                    ) {
                        Text(if (showPreview) stringResource(R.string.edit) else stringResource(R.string.preview))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showPreview && cardNumber.isNotEmpty() && cardHolderName.isNotEmpty()) {
            // Preview mode
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .verticalScroll(scrollState)
            ) {
                val previewCreditCardData = CreditCard(
                    id = "preview",
                    cardHolderName = cardHolderName,
                    maskedCardNumber = FinancialValidationUtils.maskCardNumber(cardNumber),
                    cardType = detectedCardType,
                    issuerBank = issuerBank.ifEmpty { context.getString(R.string.default_bank) },
                    expiryMonth = expiryMonth.toIntOrNull() ?: 1,
                    expiryYear = expiryYear.toIntOrNull() ?: 2025,
                    cardNickname = cardNickname.ifEmpty { null },
                    iban = iban.takeIf { it.isNotEmpty() },
                    swiftCode = swiftCode.takeIf { it.isNotEmpty() },
                    abaRoutingNumber = abaRoutingNumber.takeIf { it.isNotEmpty() },
                    contactlessEnabled = true
                )
                
                CreditCardComponent(
                    creditCardData = previewCreditCardData
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (cardNumberError == null && expiryError == null && ibanError == null && 
                            swiftError == null && abaError == null) {
                            onSave(
                                cardNumber,
                                cardHolderName,
                                expiryMonth.toIntOrNull() ?: 1,
                                expiryYear.toIntOrNull() ?: 2025,
                                cvv,
                                issuerBank.ifEmpty { context.getString(R.string.default_bank) },
                                iban,
                                swiftCode,
                                abaRoutingNumber,
                                cardNickname,
                                detectedCardType
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = cardNumberError == null && expiryError == null && ibanError == null &&
                             swiftError == null && abaError == null &&
                             cardNumber.isNotEmpty() && cardHolderName.isNotEmpty()
                ) {
                    Text(stringResource(R.string.save_credit_card))
                }
            }
        } else {
            // Form mode
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {    
                // Card Number
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { newValue ->
                        val cleaned = newValue.replace(Regex("[^0-9]"), "")
                        if (cleaned.length <= 19) { // Max card number length
                            cardNumber = cleaned
                        }
                    },
                    label = { Text(stringResource(R.string.card_number)) },
                    visualTransformation = VisualTransformation { text ->
                        val formatted = FinancialValidationUtils.formatCardNumber(text.text)
                        TransformedText(AnnotatedString(formatted), CreditCardOffsetMapping())
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = cardNumberError != null,
                    supportingText = cardNumberError?.let { { Text(it) } },
                    trailingIcon = {
                        if (detectedCardType != CreditCardType.UNKNOWN) {
                            Text(
                                text = detectedCardType.icon,
                                fontSize = 20.sp
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Cardholder Name
                OutlinedTextField(
                    value = cardHolderName,
                    onValueChange = { cardHolderName = it.uppercase() },
                    label = { Text(stringResource(R.string.cardholder_name_label)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Expiry Date and CVV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = expiryMonth,
                        onValueChange = { newValue ->
                            val cleaned = newValue.replace(Regex("[^0-9]"), "")
                            if (cleaned.length <= 2) {
                                expiryMonth = cleaned
                            }
                        },
                        label = { Text(stringResource(R.string.mm_label)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Right) }
                        ),
                        isError = expiryError != null,
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = expiryYear,
                        onValueChange = { newValue ->
                            val cleaned = newValue.replace(Regex("[^0-9]"), "")
                            if (cleaned.length <= 4) {
                                expiryYear = cleaned
                            }
                        },
                        label = { Text(stringResource(R.string.yyyy_label)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Right) }
                        ),
                        isError = expiryError != null,
                        supportingText = expiryError?.let { { Text(it) } },
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { newValue ->
                            val cleaned = newValue.replace(Regex("[^0-9]"), "")
                            if (cleaned.length <= 4) { // Amex can have 4-digit CVV
                                cvv = cleaned
                            }
                        },
                        label = { Text(stringResource(R.string.cvv)) },
                        visualTransformation = if (showCvv) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showCvv = !showCvv }) {
                                Icon(
                                    if (showCvv) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showCvv) stringResource(R.string.hide_cvv) else stringResource(R.string.show_cvv)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Issuer Bank
                OutlinedTextField(
                    value = issuerBank,
                    onValueChange = { issuerBank = it },
                    label = { Text(stringResource(R.string.issuer_bank_label)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // IBAN (Optional)
                OutlinedTextField(
                    value = iban,
                    onValueChange = { newValue ->
                        // Remove spaces and format IBAN
                        val cleaned = newValue.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
                        iban = cleaned
                    },
                    label = { Text(stringResource(R.string.iban_optional_label)) },
                    placeholder = { Text(stringResource(R.string.iban_placeholder)) },
                    visualTransformation = VisualTransformation { text ->
                        val formatted = FinancialValidationUtils.formatIBAN(text.text)
                        TransformedText(AnnotatedString(formatted), IBANOffsetMapping())
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = ibanError != null,
                    supportingText = ibanError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // SWIFT/BIC Code (Optional)
                OutlinedTextField(
                    value = swiftCode,
                    onValueChange = { newValue ->
                        // Remove spaces and format SWIFT code
                        val cleaned = newValue.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
                        swiftCode = cleaned
                    },
                    label = { Text(stringResource(R.string.swift_bic_label)) },
                    placeholder = { Text(stringResource(R.string.swift_bic_placeholder)) },
                    visualTransformation = VisualTransformation { text ->
                        val formatted = FinancialValidationUtils.formatSWIFTCode(text.text)
                        TransformedText(AnnotatedString(formatted), SWIFTOffsetMapping())
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = swiftError != null,
                    supportingText = swiftError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // ABA Routing Number (Optional)
                OutlinedTextField(
                    value = abaRoutingNumber,
                    onValueChange = { newValue ->
                        // Only allow digits
                        val cleaned = newValue.replace(Regex("[^0-9]"), "")
                        if (cleaned.length <= 9) { // ABA is exactly 9 digits
                            abaRoutingNumber = cleaned
                        }
                    },
                    label = { Text(stringResource(R.string.aba_routing_label)) },
                    placeholder = { Text(stringResource(R.string.aba_routing_placeholder)) },
                    visualTransformation = VisualTransformation { text ->
                        val formatted = FinancialValidationUtils.formatABARoutingNumber(text.text)
                        TransformedText(AnnotatedString(formatted), ABAOffsetMapping())
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = abaError != null,
                    supportingText = abaError?.let { { Text(it) } } ?: {
                        if (abaRoutingNumber.isNotEmpty()) {
                            Text(stringResource(R.string.us_bank_routing_number), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Card Nickname (Optional)
                OutlinedTextField(
                    value = cardNickname,
                    onValueChange = { cardNickname = it },
                    label = { Text(stringResource(R.string.card_nickname_label)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
