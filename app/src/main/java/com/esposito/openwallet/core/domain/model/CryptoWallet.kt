/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crypto_wallets")
data class CryptoWallet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val blockchain: String, // e.g., "Bitcoin", "Ethereum", "Polygon", "Tron"
    val network: String, // e.g., "Mainnet", "Testnet"
    val symbol: String, // e.g., "BTC", "ETH", "MATIC", "TRX"
    val tokenSymbol: String? = null, // e.g., "USDT", "USDC", "DAI" for tokens on the blockchain
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SupportedBlockchain(
    val displayName: String,
    val symbol: String,
    val networks: List<String>,
    val addressPrefix: String = "",
    val supportedTokens: List<String> = emptyList()
) {
    BITCOIN("Bitcoin", "BTC", listOf("Mainnet", "Testnet"), ""),
    ETHEREUM("Ethereum", "ETH", listOf("Mainnet", "Goerli", "Sepolia"), "0x", 
        listOf("USDT", "USDC", "DAI", "WETH", "LINK", "UNI", "AAVE", "COMP", "MKR", "SNX")),
    POLYGON("Polygon", "MATIC", listOf("Mainnet", "Mumbai"), "0x", 
        listOf("USDT", "USDC", "DAI", "WETH", "WMATIC", "QUICK", "GHST", "CRV")),
    BINANCE_SMART_CHAIN("Binance Smart Chain", "BNB", listOf("Mainnet", "Testnet"), "0x", 
        listOf("USDT", "USDC", "BUSD", "CAKE", "XVS", "ALPHA", "BETH", "BTCB")),
    TRON("Tron", "TRX", listOf("Mainnet", "Shasta", "Nile"), "T", 
        listOf("USDT", "USDC", "USDD", "BTT", "JST", "SUN", "WIN", "APENFT")),
    SOLANA("Solana", "SOL", listOf("Mainnet", "Devnet", "Testnet"), "", 
        listOf("USDT", "USDC", "RAY", "SRM", "FIDA", "ROPE", "COPE", "STEP")),
    CARDANO("Cardano", "ADA", listOf("Mainnet", "Testnet"), "addr"),
    LITECOIN("Litecoin", "LTC", listOf("Mainnet", "Testnet"), ""),
    AVALANCHE("Avalanche", "AVAX", listOf("Mainnet", "Fuji"), "0x", 
        listOf("USDT", "USDC", "DAI", "WAVAX", "PNG", "JOE", "QI", "SPELL")),
    FANTOM("Fantom", "FTM", listOf("Mainnet", "Testnet"), "0x", 
        listOf("USDT", "USDC", "DAI", "WFTM", "BOO", "SPIRIT", "TOMB", "BASED")),
    ARBITRUM("Arbitrum", "ETH", listOf("Mainnet", "Goerli"), "0x", 
        listOf("USDT", "USDC", "DAI", "WETH", "ARB", "GMX", "MAGIC", "RDNT")),
    OPTIMISM("Optimism", "ETH", listOf("Mainnet", "Goerli"), "0x", 
        listOf("USDT", "USDC", "DAI", "WETH", "OP", "SNX", "THALES", "KWENTA")),
    COSMOS("Cosmos", "ATOM", listOf("Mainnet", "Testnet"), "cosmos"),
    POLKADOT("Polkadot", "DOT", listOf("Mainnet", "Westend"), ""),
    CHAINLINK("Chainlink", "LINK", listOf("Mainnet", "Testnet"), "0x"),
    STELLAR("Stellar", "XLM", listOf("Mainnet", "Testnet"), "G"),
    RIPPLE("Ripple", "XRP", listOf("Mainnet", "Testnet"), "r"),
    MONERO("Monero", "XMR", listOf("Mainnet", "Stagenet", "Testnet"), "4", 
        listOf()) // Monero addresses start with "4" (standard) or "8" (subaddress)
}
