/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.passmanagement.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.esposito.openwallet.core.domain.model.BoardingPassData
import com.esposito.openwallet.core.domain.model.PassType
import com.esposito.openwallet.core.domain.model.WalletPass
import com.esposito.openwallet.core.util.PassTypeUtils
import com.esposito.openwallet.R
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PKPassCard(
    pass: WalletPass,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Use specialized card for boarding passes
    if (pass.type == PassType.BOARDING_PASS) {
        BoardingPassCard(
            pass = pass,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
        )
    } else {
        GenericPKPassCard(
            pass = pass,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoardingPassCard(
    pass: WalletPass,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val gson = remember { Gson() }
    val boardingData = remember(pass.passData) {
        try {
            gson.fromJson(pass.passData, BoardingPassData::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    // Parse pass colors
    val backgroundColor = parsePassColor(pass.backgroundColor) ?: Color(0xFF073590)
    val foregroundColor = parsePassColor(pass.foregroundColor) ?: Color.White
    
    // Resolve airline name: logoText → organizationName → fallback
    val airlineName = remember(pass.logoText, pass.organizationName, pass.title) {
        pass.logoText?.takeIf { it.isNotBlank() }
            ?: pass.organizationName.takeIf { 
                it.isNotBlank() && it != pass.title && !it.contains("→") 
            }
            ?: ""
    }

    val routeInfo = remember(pass.title, boardingData) {
        val departure = boardingData?.departureLocation
            ?: run {
                val afterDot = pass.title.substringAfter("· ", "").substringAfter("· ", "")
                afterDot.substringBefore(" →").substringBefore(" ->").trim().takeIf { 
                    it.isNotBlank() && it.length <= 5 
                }
            }
        val destination = boardingData?.destinationLocation
            ?: run {
                pass.title.substringAfter("→ ", "").substringAfter("-> ", "").trim().takeIf { 
                    it.isNotBlank() && it.length <= 5 
                }
            }
        val flight = boardingData?.flightNumber
            ?: pass.title.substringBefore(" ·").trim().takeIf { 
                it.isNotBlank() && it.length <= 10 && it != pass.title 
            }
        Triple(departure, destination, flight)
    }
    val (departure, destination, flightNumber) = routeInfo
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            backgroundColor.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ─── TOP ROW: Airline name (left) + Flight chip (right) ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Airline name (bold, left-aligned)
                    Text(
                        text = airlineName.ifBlank { pass.organizationName },
                        style = MaterialTheme.typography.titleLarge,
                        color = foregroundColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Flight number chip
                    Box(
                        modifier = Modifier
                            .background(
                                foregroundColor.copy(alpha = 0.15f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AirplanemodeActive,
                                contentDescription = null,
                                tint = foregroundColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = flightNumber ?: "Flight",
                                style = MaterialTheme.typography.labelMedium,
                                color = foregroundColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                if (departure != null && destination != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = departure,
                            style = MaterialTheme.typography.displaySmall,
                            color = foregroundColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        )
                        
                        Icon(
                            imageVector = Icons.Filled.AirplanemodeActive,
                            contentDescription = null,
                            tint = foregroundColor.copy(alpha = 0.4f),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .size(24.dp)
                        )
                        
                        Text(
                            text = destination,
                            style = MaterialTheme.typography.displaySmall,
                            color = foregroundColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        )
                    }
                } else {
                    Text(
                        text = pass.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = foregroundColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        boardingData?.passengerName?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = foregroundColor.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    boardingData?.seat?.let { seat ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SEAT",
                                style = MaterialTheme.typography.labelSmall,
                                color = foregroundColor.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = seat,
                                style = MaterialTheme.typography.titleMedium,
                                color = foregroundColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GenericPKPassCard(
    pass: WalletPass,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = PassTypeUtils.getPassTypeColor(pass.type.name)
    val secondaryColor = primaryColor.copy(alpha = 0.7f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pass.organizationName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = pass.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Icon(
                        imageVector = PassTypeUtils.getPassTypeIcon(pass.type.name),
                        contentDescription = PassTypeUtils.getPassTypeDisplayName(context, pass.type),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Content area
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Serial number if available
                pass.serialNumber?.let { serialNumber ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.serial_number),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = serialNumber,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Description if available
                pass.description?.let { description ->
                    if (description != pass.title && !description.lowercase().contains("pass")) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Footer with actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        // Barcode indicator
                        if (!pass.barcodeData.isNullOrEmpty()) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = stringResource(R.string.has_barcode_description),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        // PKPass indicator
                        if (pass.isImported) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = stringResource(R.string.imported_pkpass_description),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parsePassColor(colorString: String?): Color? {
    if (colorString == null) return null
    
    val rgbRegex = Regex("""rgb\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""")
    val match = rgbRegex.find(colorString)
    
    return if (match != null) {
        val (r, g, b) = match.destructured
        Color(r.toInt(), g.toInt(), b.toInt())
    } else {
        try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            null
        }
    }
}
