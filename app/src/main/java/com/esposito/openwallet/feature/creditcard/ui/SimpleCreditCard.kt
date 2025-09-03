/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.creditcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esposito.openwallet.R
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.model.CreditCardType
import com.esposito.openwallet.core.domain.model.FinancialValidationUtils

/**
 * Clean credit card component without JSON parsing complexity
 * Takes CreditCard directly from the encrypted database
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCard(
    creditCardData: CreditCard,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp) // Reduced height for better consistency
            .padding(horizontal = 0.dp, vertical = 8.dp) // Consistent padding
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = getCreditCardGradient(creditCardData.cardType),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // Holographic overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.05f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
            
            CreditCardFront(creditCardData = creditCardData)
        }
    }
}

@Composable
private fun CreditCardFront(
    creditCardData: CreditCard
) {
    var showFullNumber by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row - Bank name and card network
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = creditCardData.cardNickname ?: creditCardData.issuerBank,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = when (creditCardData.cardType) {
                        CreditCardType.VISA -> stringResource(R.string.visa_short)
                        CreditCardType.MASTERCARD -> stringResource(R.string.mastercard_short)
                        CreditCardType.AMERICAN_EXPRESS -> stringResource(R.string.american_express_short)
                        CreditCardType.DISCOVER -> stringResource(R.string.discover_short)
                        else -> creditCardData.cardType.displayName.take(4).uppercase()
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            // Bottom section - Card details
            Column {
                // Card number
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "•••• ${creditCardData.maskedCardNumber.takeLast(4)}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Cardholder name
                    Column {
                        Text(
                            text = stringResource(R.string.cardholder),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = creditCardData.cardHolderName.take(20).uppercase(),
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    // Expiry date
                    Column {
                        Text(
                            text = stringResource(R.string.expires),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${String.format("%02d", creditCardData.expiryMonth)}/${creditCardData.expiryYear.toString().takeLast(2)}",
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
