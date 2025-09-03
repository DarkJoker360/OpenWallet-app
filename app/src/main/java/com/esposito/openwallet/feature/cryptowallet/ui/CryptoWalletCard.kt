/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.cryptowallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.esposito.openwallet.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.esposito.openwallet.core.domain.model.CryptoWallet
import com.esposito.openwallet.core.ui.components.BaseCard
import com.esposito.openwallet.core.ui.components.CategoryChip
import com.esposito.openwallet.core.ui.components.StandardCardLayout

/**
 * High-quality crypto wallet card component
 * Uses consolidated BaseCard component for consistent styling
 */
@Composable
fun CryptoWalletCard(
    wallet: CryptoWallet,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cryptoStyle = getCryptoStyleFromWallet(wallet)
    
    BaseCard(
        onClick = onClick,
        modifier = modifier,
        height = 140.dp,
        backgroundBrush = Brush.linearGradient(
            colors = listOf(
                cryptoStyle.backgroundColor.copy(alpha = 0.9f),
                cryptoStyle.backgroundColor.copy(alpha = 0.7f),
                cryptoStyle.backgroundColor.copy(alpha = 0.5f)
            ),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
        ),
        overlayBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.2f)
            )
        ),
        elevation = 6.dp,
        cornerRadius = 20.dp,
        contentPadding = 20.dp
    ) {
        StandardCardLayout(
            header = {
                // Crypto chip
                CategoryChip(
                    text = wallet.tokenSymbol ?: wallet.symbol,
                    icon = painterResource(cryptoStyle.iconResId),
                    backgroundColor = cryptoStyle.backgroundColor,
                    contentColor = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Address info
                Column(horizontalAlignment = Alignment.End) {
                    if (wallet.network.lowercase() != "mainnet") {
                        Text(
                            text = wallet.network,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    
                    Text(
                        text = "${wallet.address.take(6)}...${wallet.address.takeLast(4)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            footer = {
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (wallet.tokenSymbol != null) {
                        "${wallet.blockchain} • ${wallet.symbol}"
                    } else {
                        wallet.blockchain
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
    }
}
