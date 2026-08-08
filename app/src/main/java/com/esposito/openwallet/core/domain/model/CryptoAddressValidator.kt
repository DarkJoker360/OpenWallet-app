/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.model

import java.util.regex.Pattern

object CryptoAddressValidator {
    fun isValid(address: String, blockchain: SupportedBlockchain, network: String): Boolean {
        val value = address.trim()
        if (value.isEmpty() || network !in blockchain.networks) return false

        return when (blockchain) {
            SupportedBlockchain.BITCOIN, SupportedBlockchain.LITECOIN ->
                bitcoinLike(value, blockchain, network)
            SupportedBlockchain.ETHEREUM,
            SupportedBlockchain.POLYGON,
            SupportedBlockchain.BINANCE_SMART_CHAIN,
            SupportedBlockchain.AVALANCHE,
            SupportedBlockchain.ARBITRUM,
            SupportedBlockchain.OPTIMISM,
            SupportedBlockchain.FANTOM,
            SupportedBlockchain.CHAINLINK -> HEX_ADDRESS.matcher(value).matches()
            SupportedBlockchain.TRON -> value.length in 34..36 && value.startsWith("T")
            SupportedBlockchain.SOLANA -> value.length in 32..44 && BASE58.matcher(value).matches()
            SupportedBlockchain.CARDANO -> value.startsWith("addr") && value.length >= 50
            SupportedBlockchain.COSMOS -> value.startsWith("cosmos1") && value.length >= 39
            SupportedBlockchain.POLKADOT -> value.length in 46..48 && BASE58.matcher(value).matches()
            SupportedBlockchain.STELLAR -> value.length == 56 && value.startsWith("G")
            SupportedBlockchain.RIPPLE -> value.length in 25..35 && value.startsWith("r")
            SupportedBlockchain.MONERO -> value.length in 95..106 && (value.startsWith("4") || value.startsWith("8"))
        }
    }

    private fun bitcoinLike(value: String, blockchain: SupportedBlockchain, network: String): Boolean {
        val prefix = when {
            blockchain == SupportedBlockchain.LITECOIN -> listOf("L", "M", "ltc1")
            network == "Testnet" -> listOf("m", "n", "2", "tb1")
            else -> listOf("1", "3", "bc1")
        }
        return value.length in 26..90 && prefix.any(value::startsWith)
    }

    private val HEX_ADDRESS = Pattern.compile("0x[a-fA-F0-9]{40}")
    private val BASE58 = Pattern.compile("[1-9A-HJ-NP-Za-km-z]+")
}
