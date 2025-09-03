package com.esposito.openwallet.feature.creditcard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.esposito.openwallet.R
import com.esposito.openwallet.core.domain.model.CreditCard
import com.esposito.openwallet.core.domain.model.CreditCardType
import com.esposito.openwallet.core.domain.model.FinancialValidationUtils
import java.util.Locale

/**
 * Credit card component with secure display and Material Design 3 styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardComponent(
    creditCardData: CreditCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    showFlipButton: Boolean = true,
    cardHeight: Dp = 200.dp,
    showSensitiveData: Boolean = false,
    decryptedCardNumber: String? = null,
    decryptedCVV: String? = null
) {
    var isFlipped by remember { mutableStateOf(false) }
    
    val cardRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        label = "card_rotation"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(
                elevation = 12.dp, 
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .clickable { onClick() }
            .graphicsLayer {
                rotationY = cardRotation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = getCreditCardGradient(creditCardData.cardType),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // Add a subtle overlay for depth
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
            if (cardRotation < 90f) {
                // Front of card
                CreditCardFront(
                    creditCardData = creditCardData,
                    showSensitiveData = showSensitiveData,
                    decryptedCardNumber = decryptedCardNumber,
                    onFlip = { isFlipped = true },
                    showFlipButton = showFlipButton
                )
            } else {
                // Back of card (flipped) - apply rotationY to correct mirrored text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = 180f
                        }
                ) {
                    CreditCardBack(
                        creditCardData = creditCardData,
                        showSensitiveData = showSensitiveData,
                        decryptedCVV = decryptedCVV,
                        onFlip = { isFlipped = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditCardFront(
    creditCardData: CreditCard,
    showSensitiveData: Boolean,
    decryptedCardNumber: String?,
    onFlip: () -> Unit,
    showFlipButton: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Add a subtle pattern background
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val pattern = 12.dp.toPx()
            for (x in 0 until (size.width / pattern).toInt()) {
                for (y in 0 until (size.height / pattern).toInt()) {
                    if ((x + y) % 2 == 0) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.02f),
                            radius = 1.dp.toPx(),
                            center = Offset(x * pattern + pattern / 2, y * pattern + pattern / 2)
                        )
                    }
                }
            }
        }
        // Card content with the same beautiful design as preview
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row - Bank name and Network logo
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
                
                // Card network logo/text
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
            
            // Bottom section - Card number, holder name, expiry and flip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    // Card number - no longer clickable, managed by Show Full Details button
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (showSensitiveData && decryptedCardNumber != null) {
                                FinancialValidationUtils.formatCardNumber(decryptedCardNumber)
                            } else {
                                "•••• •••• •••• ${creditCardData.maskedCardNumber.takeLast(4)}"
                            },
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
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
                                text = "${String.format(Locale.ROOT, "%02d", creditCardData.expiryMonth)}/${creditCardData.expiryYear.toString().takeLast(2)}",
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Flip to back button (only show if flip button is enabled)
                if (showFlipButton) {
                    IconButton(onClick = onFlip) {
                        Icon(
                            Icons.Default.FlipToBack,
                            contentDescription = stringResource(R.string.view_back),
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditCardBack(
    creditCardData: CreditCard,
    showSensitiveData: Boolean,
    decryptedCVV: String?,
    onFlip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top strip (magnetic stripe)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.Black)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // CVV section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.cvv),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                Color.White,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (showSensitiveData && decryptedCVV != null) {
                                decryptedCVV
                            } else {
                                "•••" // CVV is no longer stored directly, always show masked
                            },
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Bank info and flip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.issuer),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = creditCardData.issuerBank.uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                IconButton(onClick = onFlip) {
                    Icon(
                        Icons.Default.FlipToFront,
                        contentDescription = stringResource(R.string.view_front),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}



/**
 * Get gradient brush for credit card background based on card type
 */
fun getCreditCardGradient(cardType: CreditCardType?): Brush {
    return when (cardType) {
        CreditCardType.VISA -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF1A1F71),
                Color(0xFF1E3A8A)
            )
        )
        CreditCardType.MASTERCARD -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFEB001B),
                Color(0xFFF79E1B)
            )
        )
        CreditCardType.AMERICAN_EXPRESS -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF006FCF),
                Color(0xFF0099CC)
            )
        )
        CreditCardType.DISCOVER -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF6000),
                Color(0xFFFF8C00)
            )
        )
        CreditCardType.DINERS_CLUB -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF0079BE),
                Color(0xFF00A8CC)
            )
        )
        CreditCardType.JCB -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF006633),
                Color(0xFF008844)
            )
        )
        CreditCardType.UNIONPAY -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFE21836),
                Color(0xFFFF4466)
            )
        )
        CreditCardType.MAESTRO -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF6C2C2F),
                Color(0xFF8B4444)
            )
        )
        CreditCardType.UNKNOWN -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF6B7280),
                Color(0xFF9CA3AF)
            )
        )
        null -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF6B7280),
                Color(0xFF9CA3AF)
            )
        )
    }
}
