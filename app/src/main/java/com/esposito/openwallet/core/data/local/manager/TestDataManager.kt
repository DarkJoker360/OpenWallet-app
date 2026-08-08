/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.data.local.manager

import com.esposito.openwallet.core.util.SecureLogger
import androidx.fragment.app.FragmentActivity
import com.esposito.openwallet.core.data.repository.WalletRepository
import com.esposito.openwallet.core.domain.model.BarcodeFormat
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.model.CreditCardType
import com.esposito.openwallet.core.domain.model.CryptoWallet
import com.esposito.openwallet.core.domain.model.FinancialValidationUtils
import com.esposito.openwallet.core.domain.model.PassType
import com.esposito.openwallet.core.domain.model.SecureCreditCard
import com.esposito.openwallet.core.domain.model.WalletPass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

/**
 * Manager for generating test/mockup data for development and testing purposes
 */
class TestDataManager(
    private val walletRepository: WalletRepository
) {
    
    companion object {
        private const val TAG = "TestDataManager"
        
        // Test data generation constants
        private const val CARD_CREATION_DELAY_MS = 500L
        private const val DATA_CLEAR_DELAY_MS = 1000L
        
        // Test card data prefixes
        private const val ENHANCED_ENCRYPTION_PREFIX = "ENHANCED:"
        
        // Banking constants
        private const val ABA_LENGTH = 9
        
        // IBAN Constants
        private const val IBAN_CHECK_DIGITS_LENGTH = 2
        private const val US_IBAN_BANK_LENGTH = 9
        private const val US_IBAN_ACCOUNT_LENGTH = 10
        private const val ITALIAN_IBAN_BANK_LENGTH = 5
        private const val ITALIAN_IBAN_BRANCH_LENGTH = 5
        private const val ITALIAN_IBAN_ACCOUNT_LENGTH = 12
        
        // Random number bounds
        private const val MIN_CHECK_DIGIT = 10
        private const val MAX_CHECK_DIGIT = 99
        private const val MIN_RANDOM_FIVE_DIGIT = 10000
        private const val MAX_RANDOM_FIVE_DIGIT = 99999
    }
    
    /**
     * Generate complete set of mockup data with enhanced encryption
     */
    suspend fun generateMockupData(activity: FragmentActivity) = withContext(Dispatchers.IO) {
        generateMockupCreditCardsWithEnhancedEncryption(activity)
        generateMockupCryptoWallets()
        generateMockupWalletPasses()
        generateMockupArchivedPasses()
    }
    
    /**
     * Clear all existing data
     */
    suspend fun clearAllData() {
        try {
            SecureLogger.d(TAG, "Starting to clear all data...")
            
            // Delete all wallet passes (excluding credit cards - those are now separate)
            val allPasses = walletRepository.getAllPassesSync()
            allPasses.forEach { pass ->
                walletRepository.deletePass(pass)
            }
            
            // Delete all crypto wallets
            val allWallets = walletRepository.getAllCryptoWalletsSync()
            allWallets.forEach { wallet ->
                walletRepository.deleteCryptoWallet(wallet)
            }
            
            // Delete all credit cards from new table
            walletRepository.deleteAllCreditCards()
            
            delay(DATA_CLEAR_DELAY_MS) // Longer delay for visual feedback
            SecureLogger.d(TAG, "Successfully cleared all data")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Error clearing all data", e)
            throw e
        }
    }
    
    /**
     * Get statistics of current data
     */
    suspend fun getDataStatistics(): Map<String, Int> = withContext(Dispatchers.IO) {
        val allPasses = walletRepository.getAllPassesSync()
        val walletPassesCount = allPasses.size // Now only wallet passes, no credit cards
        
        val allWallets = walletRepository.getAllCryptoWalletsSync()
        val cryptoWalletsCount = allWallets.size
        
        val allCreditCards = walletRepository.getAllCreditCardsSync()
        val creditCardsCount = allCreditCards.size
        
        mapOf(
            "creditCards" to creditCardsCount,
            "cryptoWallets" to cryptoWalletsCount,
            "walletPasses" to walletPassesCount
        )
    }
    
    /**
     * Generate mockup credit cards using proper enhanced encryption like manual creation
     */
    private suspend fun generateMockupCreditCardsWithEnhancedEncryption(activity: FragmentActivity) {
        SecureLogger.d(TAG, "Generating mockup credit cards with REAL enhanced encryption...")
        
        // Create test cards with banking information
        val testCardsData = listOf(
            TestCardData(
                id = "cc_visa_main",
                cardHolderName = "John Doe",
                fullCardNumber = "4532123456789012",
                cvv = "123",
                cardType = CreditCardType.VISA,
                issuerBank = "Chase Bank",
                expiryMonth = 12,
                expiryYear = 2027,
                cardNickname = "Main Visa",
                isPrimary = true,
                backgroundColor = "#1A1F71",
                swiftCode = generateSWIFTCode("CHASUS33"),
                abaNumber = generateABANumber("021000021"),
                ibanNumber = generateIBANNumber("US", "021000021", "1234567890")
            ),
            TestCardData(
                id = "cc_mastercard_shopping",
                cardHolderName = "Jane Smith", 
                fullCardNumber = "5555444433332222",
                cvv = "456",
                cardType = CreditCardType.MASTERCARD,
                issuerBank = "Bank of America",
                expiryMonth = 8,
                expiryYear = 2026,
                cardNickname = "Shopping Card",
                isPrimary = false,
                backgroundColor = "#EB001B",
                swiftCode = generateSWIFTCode("BOFAUS3N"),
                abaNumber = generateABANumber("026009593"),
                ibanNumber = generateIBANNumber("US", "026009593", "0987654321")
            ),
            TestCardData(
                id = "cc_amex_business",
                cardHolderName = "Robert Johnson",
                fullCardNumber = "378282246310005",
                cvv = "789",
                cardType = CreditCardType.AMERICAN_EXPRESS,
                issuerBank = "American Express",
                expiryMonth = 3,
                expiryYear = 2028,
                cardNickname = "Business Amex",
                isPrimary = false,
                backgroundColor = "#006FCF",
                swiftCode = generateSWIFTCode("AEIBUS33"),
                abaNumber = generateABANumber("074000010"),
                ibanNumber = generateIBANNumber("US", "074000010", "1122334455")
            ),
            TestCardData(
                id = "cc_discover_rewards",
                cardHolderName = "Mary Williams",
                fullCardNumber = "6011111111111117",
                cvv = "321",
                cardType = CreditCardType.DISCOVER,
                issuerBank = "Discover Financial",
                expiryMonth = 11,
                expiryYear = 2025,
                cardNickname = "Rewards Card",
                isPrimary = false,
                backgroundColor = "#FF6000",
                swiftCode = generateSWIFTCode("DISCUS33"),
                abaNumber = generateABANumber("011000206"),
                ibanNumber = generateIBANNumber("US", "011000206", "5566778899")
            ),
            TestCardData(
                id = "cc_eu_card",
                cardHolderName = "Alessandro Rossi",
                fullCardNumber = "4000000000000002",
                cvv = "567",
                cardType = CreditCardType.VISA,
                issuerBank = "Intesa Sanpaolo",
                expiryMonth = 6,
                expiryYear = 2029,
                cardNickname = "European Card",
                isPrimary = false,
                backgroundColor = "#1A1F71",
                swiftCode = generateSWIFTCode("BCITITMM"),
                abaNumber = null, // European banks don't use ABA
                ibanNumber = generateIBANNumber("IT", "60", "X0542811101000000123456")
            )
        )
        
        // Create each card using the same flow as manual creation
        for ((index, testCard) in testCardsData.withIndex()) {
            try {
                SecureLogger.d(TAG, "Creating secure test card ${index + 1}/${testCardsData.size}: ${testCard.cardNickname}")
                
                // Use the same creation flow as SecureCreditCard.createWithEncryption
                val secureCard = createSecureCreditCardWithEnhancedEncryption(
                    activity = activity,
                    testCard = testCard
                )
                
                // Convert to legacy CreditCard format and store
                val legacyCreditCard = convertSecureCardToLegacy(secureCard, testCard)
                walletRepository.insertCreditCard(legacyCreditCard)
                
                SecureLogger.d(TAG, "Successfully created secure test card: ${testCard.cardNickname}")
                
                // Small delay between cards to prevent overwhelming the biometric system
                delay(CARD_CREATION_DELAY_MS)
                
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error creating test card ${testCard.cardNickname}: ${e.message}", e)
                // Continue with other cards even if one fails
            }
        }
        
        SecureLogger.d(TAG, "Finished generating mockup credit cards with REAL enhanced encryption")
    }
    
    /**
     * Create a secure credit card using the same encryption flow as manual creation
     */
    private suspend fun createSecureCreditCardWithEnhancedEncryption(
        activity: FragmentActivity,
        testCard: TestCardData
    ): SecureCreditCard = suspendCancellableCoroutine { continuation ->
        
        // Switch to main thread for UI operations (biometric authentication)
        activity.runOnUiThread {
            SecureCreditCard.createWithEncryption(
                activity = activity,
                cardHolderName = testCard.cardHolderName,
                bankName = testCard.issuerBank,
                cardType = testCard.cardType.name,
                cardNumber = testCard.fullCardNumber,
                cvv = testCard.cvv,
                iban = testCard.ibanNumber ?: "",
                expiryMonth = testCard.expiryMonth.toString().padStart(2, '0'),
                expiryYear = testCard.expiryYear.toString(),
                notes = testCard.cardNickname,
                color = testCard.backgroundColor,
                onSuccess = { secureCard ->
                    SecureLogger.d(TAG, "Enhanced encryption successful for: ${testCard.cardNickname}")
                    continuation.resume(secureCard)
                },
                onError = { error ->
                    SecureLogger.e(TAG, "Enhanced encryption failed for ${testCard.cardNickname}: $error")
                    continuation.resumeWithException(Exception("Enhanced encryption failed: $error"))
                }
            )
        }
    }
    
    /**
     * Convert SecureCreditCard to legacy CreditCard format
     */
    private fun convertSecureCardToLegacy(secureCard: SecureCreditCard, testCard: TestCardData): CreditCard {
        val maskedCardNumber = FinancialValidationUtils.maskCardNumber(testCard.fullCardNumber)
        
        return CreditCard(
            id = testCard.id,  // Use our predefined ID
            cardHolderName = secureCard.cardHolderName,
            maskedCardNumber = maskedCardNumber,
            cardType = testCard.cardType,
            issuerBank = secureCard.bankName,
            expiryMonth = testCard.expiryMonth,
            expiryYear = testCard.expiryYear,
            cardNickname = testCard.cardNickname,
            isPrimary = testCard.isPrimary,
            isActive = true,
            iban = testCard.ibanNumber?.let { FinancialValidationUtils.maskIBAN(it) },
            swiftCode = testCard.swiftCode,
            abaRoutingNumber = testCard.abaNumber,
            // Store enhanced encrypted data with special prefix to identify it
            encryptedFullCardNumber = "$ENHANCED_ENCRYPTION_PREFIX${secureCard.encryptedCardNumber}",
            encryptedCVV = "$ENHANCED_ENCRYPTION_PREFIX${secureCard.encryptedCVV}",
            encryptedIBAN = if (secureCard.encryptedIBAN != null) "$ENHANCED_ENCRYPTION_PREFIX${secureCard.encryptedIBAN}" else null,
            contactlessEnabled = true
        )
    }
    
    /**
     * Generate a realistic SWIFT code based on bank identifier
     */
    private fun generateSWIFTCode(baseCode: String): String {
        // SWIFT codes are typically 8-11 characters (AAAABBCCXXX)
        // AAAA = Bank code, BB = Country code, CC = Location code, XXX = Branch code (optional)
        return if (baseCode.length >= 8) {
            baseCode.take(11) // Use provided code if valid
        } else {
            "TEST${baseCode.take(4).uppercase()}XXX" // Generate test code
        }
    }
    
    /**
     * Generate a realistic ABA routing number based on bank identifier
     */
    private fun generateABANumber(baseNumber: String): String {
        // ABA routing numbers are 9 digits
        return if (baseNumber.length == ABA_LENGTH && baseNumber.all { it.isDigit() }) {
            baseNumber
        } else {
            // Generate test ABA number (format: XXXXYYYYZ where Z is check digit)
            val testBase = (1..8).map { Random.nextInt(10) }.joinToString("")
            val checkDigit = calculateABACheckDigit(testBase)
            "$testBase$checkDigit"
        }
    }
    
    /**
     * Calculate ABA check digit using the standard algorithm
     */
    private fun calculateABACheckDigit(eightDigits: String): Int {
        val weights = intArrayOf(3, 7, 1, 3, 7, 1, 3, 7)
        val sum = eightDigits.mapIndexed { index, char ->
            char.digitToInt() * weights[index]
        }.sum()
        return (10 - (sum % 10)) % 10
    }
    
    /**
     * Generate a realistic IBAN number based on country and bank information
     */
    private fun generateIBANNumber(countryCode: String, bankIdentifier: String, accountNumber: String): String {
        return when (countryCode.uppercase()) {
            "US" -> {
                // US doesn't typically use IBAN, but we'll generate a test format
                "US${Random.nextInt(MIN_CHECK_DIGIT, MAX_CHECK_DIGIT)}${bankIdentifier.take(US_IBAN_BANK_LENGTH).padEnd(US_IBAN_BANK_LENGTH, '0')}${accountNumber.take(US_IBAN_ACCOUNT_LENGTH).padEnd(US_IBAN_ACCOUNT_LENGTH, '0')}"
            }
            "IT" -> {
                // Italian IBAN: IT + 2 check digits + 1 check char + 5 bank code + 5 branch + 12 account
                val checkDigits = String.format("%0${IBAN_CHECK_DIGITS_LENGTH}d", Random.nextInt(MIN_CHECK_DIGIT, MAX_CHECK_DIGIT))
                val checkChar = Random.nextInt(MIN_CHECK_DIGIT)
                val bankCode = bankIdentifier.take(ITALIAN_IBAN_BANK_LENGTH).padEnd(ITALIAN_IBAN_BANK_LENGTH, '0')
                val branchCode = String.format("%0${ITALIAN_IBAN_BRANCH_LENGTH}d", Random.nextInt(MIN_RANDOM_FIVE_DIGIT, MAX_RANDOM_FIVE_DIGIT))
                val account = accountNumber.take(ITALIAN_IBAN_ACCOUNT_LENGTH).padEnd(ITALIAN_IBAN_ACCOUNT_LENGTH, '0')
                "IT$checkDigits$checkChar$bankCode$branchCode$account"
            }
            "DE" -> {
                // German IBAN: DE + 2 check digits + 8 bank code + 10 account
                val checkDigits = String.format("%0${IBAN_CHECK_DIGITS_LENGTH}d", Random.nextInt(MIN_CHECK_DIGIT, MAX_CHECK_DIGIT))
                val bankCode = bankIdentifier.take(8).padEnd(8, '0')
                val account = accountNumber.take(10).padEnd(10, '0')
                "DE$checkDigits$bankCode$account"
            }
            "GB" -> {
                // UK IBAN: GB + 2 check digits + 4 bank code + 6 sort code + 8 account
                val checkDigits = String.format("%0${IBAN_CHECK_DIGITS_LENGTH}d", Random.nextInt(MIN_CHECK_DIGIT, MAX_CHECK_DIGIT))
                val bankCode = bankIdentifier.take(4).padEnd(4, '0').uppercase()
                val sortCode = String.format(Locale.getDefault(),"%06d", Random.nextInt(100000, 999999))
                val account = accountNumber.take(8).padEnd(8, '0')
                "GB$checkDigits$bankCode$sortCode$account"
            }
            "FR" -> {
                // French IBAN: FR + 2 check digits + 5 bank + 5 branch + 11 account + 2 key
                val checkDigits = String.format("%0${IBAN_CHECK_DIGITS_LENGTH}d", Random.nextInt(MIN_CHECK_DIGIT, MAX_CHECK_DIGIT))
                val bankCode = bankIdentifier.take(5).padEnd(5, '0')
                val branchCode = String.format(Locale.getDefault(),"%05d", Random.nextInt(MIN_RANDOM_FIVE_DIGIT, MAX_RANDOM_FIVE_DIGIT))
                val account = accountNumber.take(11).padEnd(11, '0')
                val key = String.format("%0${IBAN_CHECK_DIGITS_LENGTH}d", Random.nextInt(MIN_CHECK_DIGIT, MAX_CHECK_DIGIT))
                "FR$checkDigits$bankCode$branchCode$account$key"
            }
            else -> {
                // Generic test IBAN format
                val checkDigits = String.format("%0${IBAN_CHECK_DIGITS_LENGTH}d", Random.nextInt(MIN_CHECK_DIGIT, MAX_CHECK_DIGIT))
                val identifier = bankIdentifier.take(16).padEnd(16, '0')
                val account = accountNumber.take(6).padEnd(6, '0')
                "${countryCode.uppercase()}$checkDigits$identifier$account"
            }
        }
    }
    
    /**
     * Data class for test card information including banking details
     */
    private data class TestCardData(
        val id: String,
        val cardHolderName: String,
        val fullCardNumber: String,
        val cvv: String,
        val cardType: CreditCardType,
        val issuerBank: String,
        val expiryMonth: Int,
        val expiryYear: Int,
        val cardNickname: String,
        val isPrimary: Boolean,
        val backgroundColor: String,
        val swiftCode: String,
        val abaNumber: String?,
        val ibanNumber: String?
    )

    /**
     * Generate mockup crypto wallets with realistic addresses
     */
    private suspend fun generateMockupCryptoWallets() {
        val mockupWallets = listOf(
            CryptoWallet(
                name = "Bitcoin Main Wallet",
                address = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
                blockchain = "Bitcoin",
                network = "Mainnet",
                symbol = "BTC",
                description = "Primary Bitcoin storage wallet for long-term holdings",
                isActive = true
            ),
            CryptoWallet(
                name = "Ethereum DeFi Wallet",
                address = "0x742d35Cc6634C0532925a3b8D000B9f4C965DC48",
                blockchain = "Ethereum",
                network = "Mainnet",
                symbol = "ETH",
                description = "Wallet for DeFi transactions and smart contracts",
                isActive = true
            ),
            CryptoWallet(
                name = "USDT Savings",
                address = "0x8ba1f109551bD432803012645Hac136c22C501Fe",
                blockchain = "Ethereum",
                network = "Mainnet",
                symbol = "ETH",
                tokenSymbol = "USDT",
                description = "USDT stable coin savings for daily transactions",
                isActive = true
            ),
            CryptoWallet(
                name = "BNB Trading Wallet",
                address = "bnb1grpf0955h0ykzq3ar5nmum7y6gdfl6lxfn46h2",
                blockchain = "Binance Smart Chain",
                network = "Mainnet",
                symbol = "BNB",
                description = "Binance Coin for trading and transaction fees",
                isActive = true
            ),
            CryptoWallet(
                name = "Polygon MATIC",
                address = "0x1234567890abcdef1234567890abcdef12345678",
                blockchain = "Polygon",
                network = "Mainnet",
                symbol = "MATIC",
                description = "Polygon wallet for low-fee transactions",
                isActive = true
            ),
            CryptoWallet(
                name = "Solana SOL Wallet",
                address = "7dHbWXmci3dT8UFYWYZweBLXgycu7Y3iL6trKn1Y7ARj",
                blockchain = "Solana",
                network = "Mainnet",
                symbol = "SOL",
                description = "High-speed Solana blockchain wallet",
                isActive = true
            ),
            CryptoWallet(
                name = "USDC Stable",
                address = "0xA0b86a33E6C17De8Ec56c2f6FfE7E8C0F8B7c123",
                blockchain = "Ethereum",
                network = "Mainnet",
                symbol = "ETH",
                tokenSymbol = "USDC",
                description = "USD Coin for stable value storage",
                isActive = true
            ),
            CryptoWallet(
                name = "Bitcoin Lightning",
                address = "lnbc100u1p3xyzabc...", // Lightning network address format
                blockchain = "Bitcoin Lightning",
                network = "Mainnet",
                symbol = "BTC",
                description = "Lightning Network wallet for instant payments",
                isActive = true
            )
        )
        
        mockupWallets.forEach { wallet ->
            walletRepository.insertCryptoWallet(wallet)
        }
        
        SecureLogger.d(TAG, "Generated ${mockupWallets.size} crypto wallets")
    }
    
    /**
     * Generate mockup wallet passes with diverse types
     */
    private suspend fun generateMockupWalletPasses() {
        val mockupPasses = listOf(
            WalletPass(
                id = "starbucks_card",
                type = PassType.STORE_CARD,
                title = "Starbucks Card",
                description = "Get rewards for every purchase",
                organizationName = "Starbucks Corporation",
                serialNumber = "SB123456789",
                barcodeData = "starbucks://card/balance/123456789",
                barcodeFormat = BarcodeFormat.QR,
                backgroundColor = "#00704A",
                foregroundColor = "#FFFFFF",
                labelColor = "#FFFFFF",
                passData = """{"balance": "$25.50", "points": "150", "tier": "Gold Star"}""",
                voided = false
            ),
            WalletPass(
                id = "metro_card_nyc",
                type = PassType.TRANSIT_PASS,
                title = "MetroCard NYC",
                description = "New York City Metropolitan Transportation",
                organizationName = "MTA New York City Transit",
                serialNumber = "NYC987654321",
                barcodeData = "mta://transit/card/987654321",
                barcodeFormat = BarcodeFormat.CODE128,
                backgroundColor = "#0039A6",
                foregroundColor = "#FFFFFF",
                labelColor = "#FFFFFF",
                passData = """{"balance": "$12.75", "rides": "5", "type": "7-Day Unlimited"}""",
                voided = false
            ),
            WalletPass(
                id = "gym_membership",
                type = PassType.MEMBERSHIP_CARD,
                title = "FitLife Premium Membership",
                description = "Premium gym membership with full access",
                organizationName = "FitLife Gyms",
                serialNumber = "FL555444333",
                barcodeData = "fitlife://membership/premium/555444333",
                barcodeFormat = BarcodeFormat.QR,
                backgroundColor = "#FF6B35",
                foregroundColor = "#FFFFFF",
                labelColor = "#FFFFFF",
                passData = """{"memberLevel": "Premium", "expiryDate": "2025-12-31", "guestPasses": "2"}""",
                voided = false
            ),
            WalletPass(
                id = "airline_boarding_pass",
                type = PassType.BOARDING_PASS,
                title = "AA1234 · JFK → LAX",
                description = "American Airlines boarding pass",
                organizationName = "American Airlines",
                serialNumber = "AA1234567890",
                barcodeData = "M1DOE/JOHN              EA1234 JFKLAXAA 0123 234Y014A0001 100",
                barcodeFormat = BarcodeFormat.PDF417,
                backgroundColor = "rgb(196, 30, 58)",
                foregroundColor = "rgb(255, 255, 255)",
                labelColor = "rgb(200, 200, 200)",
                passData = """{
                    "transitType": "PKTransitTypeAir",
                    "departureLocation": "JFK",
                    "destinationLocation": "LAX",
                    "passengerName": "John Doe",
                    "flightNumber": "AA1234",
                    "confirmationCode": "ABCDEF",
                    "seat": "14A",
                    "gate": "B22",
                    "boardingGroup": "1",
                    "terminal": "8",
                    "departureTime": "15:30",
                    "boardingTime": "14:45",
                    "gateCloseTime": "15:15",
                    "boardingDoor": "boarding_door_front",
                    "queue": "queue_priority",
                    "sequence": "001",
                    "headerFields": [],
                    "primaryFields": [],
                    "secondaryFields": [],
                    "auxiliaryFields": [],
                    "backFields": []
                }""".trimIndent(),
                voided = false
            ),
            WalletPass(
                id = "movie_ticket",
                type = PassType.EVENT_TICKET,
                title = "Spider-Man: No Way Home",
                description = "AMC Theater Movie Ticket",
                organizationName = "AMC Theatres",
                serialNumber = "AMC789012345",
                barcodeData = "amc://ticket/spiderman/theater15/789012345",
                barcodeFormat = BarcodeFormat.QR,
                backgroundColor = "#000000",
                foregroundColor = "#FFFFFF",
                labelColor = "#FFD700",
                passData = """{"theater": "15", "showtime": "19:30", "seat": "G12", "date": "2024-07-20"}""",
                voided = false
            ),
            WalletPass(
                id = "library_card",
                type = PassType.MEMBERSHIP_CARD,
                title = "New York Public Library",
                description = "Library card for book borrowing and digital access",
                organizationName = "New York Public Library",
                serialNumber = "NYPL23456789",
                barcodeData = "nypl://member/23456789",
                barcodeFormat = BarcodeFormat.CODE128,
                backgroundColor = "#1E3A8A",
                foregroundColor = "#FFFFFF",
                labelColor = "#FFFFFF",
                passData = """{"memberSince": "2020-01-15", "booksCheckedOut": "3", "holds": "1"}""",
                voided = false
            ),
            WalletPass(
                id = "loyalty_drugstore",
                type = PassType.STORE_CARD,
                title = "CVS ExtraCare Card",
                description = "CVS Pharmacy loyalty and rewards card",
                organizationName = "CVS Pharmacy",
                serialNumber = "CVS456789012",
                barcodeData = "cvs://extracare/456789012",
                barcodeFormat = BarcodeFormat.CODE128,
                backgroundColor = "#CC0000",
                foregroundColor = "#FFFFFF",
                labelColor = "#FFFFFF",
                passData = """{"points": "1250", "rewards": "$5.50", "prescriptions": "12"}""",
                voided = false
            )
        )
        
        mockupPasses.forEach { pass ->
            walletRepository.insertPass(pass)
        }
        
        SecureLogger.d(TAG, "Generated ${mockupPasses.size} wallet passes")
    }

    /**
     * Generate deterministic archived passes for testing the archived-items flow.
     * The IDs are stable so this action is safe to run repeatedly.
     */
    suspend fun generateMockupArchivedPasses() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val archivedPasses = listOf(
            WalletPass(
                id = "archived_test_boarding_pass",
                type = PassType.BOARDING_PASS,
                title = "LH456 · FCO → FRA",
                description = "Archived test boarding pass",
                organizationName = "Lufthansa",
                serialNumber = "ARCHIVE-LH456",
                relevantDate = Date(now - 14 * 24 * 60 * 60 * 1000L),
                expirationDate = Date(now - 14 * 24 * 60 * 60 * 1000L),
                barcodeData = "M1ARCHIVED/TEST          LH456 FCOFRA 0001 012A001A0001",
                barcodeFormat = BarcodeFormat.PDF417,
                backgroundColor = "#263238",
                foregroundColor = "#FFFFFF",
                labelColor = "#B0BEC5",
                passData = "{\"flightNumber\":\"LH456\",\"seat\":\"12A\",\"status\":\"expired\"}",
                isArchived = true,
                tags = listOf("test", "archived", "expired")
            ),
            WalletPass(
                id = "archived_test_event_ticket",
                type = PassType.EVENT_TICKET,
                title = "OpenWallet Summit 2025",
                description = "Archived test event ticket",
                organizationName = "OpenWallet Events",
                serialNumber = "ARCHIVE-OW-2025",
                relevantDate = Date(now - 90 * 24 * 60 * 60 * 1000L),
                expirationDate = Date(now - 90 * 24 * 60 * 60 * 1000L),
                barcodeData = "https://tickets.example.test/archived/OW2025",
                barcodeFormat = BarcodeFormat.QR,
                backgroundColor = "#5E35B1",
                foregroundColor = "#FFFFFF",
                labelColor = "#D1C4E9",
                passData = "{\"venue\":\"Rome Auditorium\",\"seat\":\"B-14\",\"status\":\"used\"}",
                voided = true,
                isArchived = true,
                tags = listOf("test", "archived", "voided")
            ),
            WalletPass(
                id = "archived_test_loyalty_card",
                type = PassType.LOYALTY_CARD,
                title = "Green Market Rewards",
                description = "Archived test loyalty card",
                organizationName = "Green Market",
                serialNumber = "ARCHIVE-GM-001",
                barcodeData = "GM-ARCHIVED-001",
                barcodeFormat = BarcodeFormat.CODE128,
                backgroundColor = "#33691E",
                foregroundColor = "#FFFFFF",
                labelColor = "#DCEDC8",
                passData = "{\"points\":\"240\",\"status\":\"archived\"}",
                isArchived = true,
                tags = listOf("test", "archived", "loyalty")
            )
        )

        archivedPasses.forEach { pass -> walletRepository.insertPass(pass) }
        SecureLogger.d(TAG, "Generated ${archivedPasses.size} archived test passes")
    }
    /**
     * Generate a pass with relevant date 2 hours and 10 seconds from now
     * This allows testing the notification scheduler (which triggers 2 hours before)
     * Notification should appear in ~10 seconds.
     */
    suspend fun generateTestNotificationPass() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val notificationTime = now + (2 * 60 * 60 * 1000) + (10 * 1000)
        val pass = WalletPass(
            id = "test_notification_pass_${Random.nextInt(10000)}",
            type = PassType.EVENT_TICKET,
            title = "Test Notification Pass",
            description = "Notification should appear in 10 seconds",
            organizationName = "Test Org",
            relevantDate = java.util.Date(notificationTime),
            passData = "{}",
            backgroundColor = "#6750A4",
            foregroundColor = "#FFFFFF",
            labelColor = "#FFFFFF"
        )
        walletRepository.insertPass(pass)
        SecureLogger.d(TAG, "Generated test notification pass: ${pass.id}")
    }
}
