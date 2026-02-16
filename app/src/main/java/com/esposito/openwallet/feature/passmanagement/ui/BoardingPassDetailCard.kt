/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.passmanagement.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esposito.openwallet.R
import com.esposito.openwallet.core.domain.model.BarcodeFormat
import com.esposito.openwallet.core.domain.model.BoardingPassData
import com.esposito.openwallet.core.domain.model.PassFieldData
import com.esposito.openwallet.core.domain.model.WalletPass
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat as ZxingBarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BoardingPassDetailCard(
    pass: WalletPass,
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
    
    if (boardingData == null) {
        Text("Unable to parse boarding pass data")
        return
    }
  
    val backgroundColor = parsePassColor(pass.backgroundColor) ?: Color(0xFF073590)
    val foregroundColor = parsePassColor(pass.foregroundColor) ?: Color.White
    val labelColor = parsePassColor(pass.labelColor) ?: Color(0xFFF1C933)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            BoardingPassHeader(
                organizationName = pass.organizationName,
                logoText = pass.logoText,
                transitType = boardingData.transitType,
                foregroundColor = foregroundColor,
                labelColor = labelColor
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        
            RouteDisplay(
                departureCode = boardingData.departureLocation,
                destinationCode = boardingData.destinationLocation,
                transitType = boardingData.transitType,
                foregroundColor = foregroundColor,
                labelColor = labelColor
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            BoardingPassInfoGrid(
                boardingData = boardingData,
                headerFields = boardingData.headerFields,
                foregroundColor = foregroundColor,
                labelColor = labelColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Secondary info (Seat, Gate, Boarding Door, Queue)
            SecondaryInfoRow(
                boardingData = boardingData,
                foregroundColor = foregroundColor,
                labelColor = labelColor
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Divider with dots (like Apple Wallet)
            DottedDivider(color = foregroundColor.copy(alpha = 0.3f))
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Barcode section
            if (pass.barcodeData != null) {
                BarcodeSection(
                    barcodeData = pass.barcodeData,
                    barcodeFormat = pass.barcodeFormat ?: BarcodeFormat.AZTEC,
                    altText = boardingData.confirmationCode,
                    backgroundColor = Color.White
                )
            }
        }
    }
}

@Composable
private fun BoardingPassHeader(
    organizationName: String,
    logoText: String?,
    transitType: String?,
    foregroundColor: Color,
    labelColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = logoText ?: organizationName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = foregroundColor
            )
            if (logoText != null && logoText != organizationName) {
                Text(
                    text = organizationName,
                    style = MaterialTheme.typography.bodySmall,
                    color = foregroundColor.copy(alpha = 0.7f)
                )
            }
        }
        
        Icon(
            imageVector = getTransitIcon(transitType),
            contentDescription = null,
            tint = foregroundColor,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun RouteDisplay(
    departureCode: String?,
    destinationCode: String?,
    transitType: String?,
    foregroundColor: Color,
    labelColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Departure
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = departureCode ?: "---",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = foregroundColor,
                fontSize = 42.sp
            )
            Text(
                text = stringResource(R.string.from_label),
                style = MaterialTheme.typography.bodySmall,
                color = labelColor
            )
        }
        
        // Transit icon and arrow
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = getTransitIcon(transitType),
                contentDescription = null,
                tint = foregroundColor.copy(alpha = 0.8f),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "→",
                style = MaterialTheme.typography.titleLarge,
                color = foregroundColor.copy(alpha = 0.6f)
            )
        }
        
        // Destination
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = destinationCode ?: "---",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = foregroundColor,
                fontSize = 42.sp
            )
            Text(
                text = stringResource(R.string.to_label),
                style = MaterialTheme.typography.bodySmall,
                color = labelColor
            )
        }
    }
}

@Composable
private fun BoardingPassInfoGrid(
    boardingData: BoardingPassData,
    headerFields: List<PassFieldData>,
    foregroundColor: Color,
    labelColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        boardingData.passengerName?.let { name ->
            InfoItem(
                label = stringResource(R.string.passenger_label),
                value = name,
                foregroundColor = foregroundColor,
                labelColor = labelColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            boardingData.flightNumber?.let { flight ->
                InfoItem(
                    label = stringResource(R.string.flight_label),
                    value = flight,
                    foregroundColor = foregroundColor,
                    labelColor = labelColor,
                    modifier = Modifier.weight(1f)
                )
            }
            
            val dateField = headerFields.find { it.key == "departDate" }
            val dateValue = dateField?.dateValue?.let { date ->
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
            } ?: boardingData.departureDate?.let { date ->
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
            }
            
            dateValue?.let { date ->
                InfoItem(
                    label = stringResource(R.string.date_label),
                    value = date,
                    foregroundColor = foregroundColor,
                    labelColor = labelColor,
                    modifier = Modifier.weight(1f),
                    alignment = Alignment.End
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            boardingData.gateCloseTime?.let { gateClose ->
                val gateCloseField = boardingData.secondaryFields.find { it.key == "gateClose" }
                val gateCloseValue = gateCloseField?.dateValue?.let { date ->
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                } ?: gateClose
                
                InfoItem(
                    label = stringResource(R.string.gate_closes_label),
                    value = gateCloseValue,
                    foregroundColor = foregroundColor,
                    labelColor = labelColor,
                    modifier = Modifier.weight(1f)
                )
            }
            
            val timeField = headerFields.find { it.key == "departTime" }
            val timeValue = timeField?.dateValue?.let { date ->
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            } ?: boardingData.departureTime
            
            timeValue?.let { time ->
                InfoItem(
                    label = stringResource(R.string.departure_time_label),
                    value = time,
                    foregroundColor = foregroundColor,
                    labelColor = labelColor,
                    modifier = Modifier.weight(1f),
                    alignment = Alignment.End
                )
            }
        }
    }
}

@Composable
private fun SecondaryInfoRow(
    boardingData: BoardingPassData,
    foregroundColor: Color,
    labelColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        boardingData.seat?.let { seat ->
            InfoItem(
                label = stringResource(R.string.seat_label),
                value = seat,
                foregroundColor = foregroundColor,
                labelColor = labelColor,
                alignment = Alignment.CenterHorizontally
            )
        }
        
        boardingData.boardingDoor?.let { door ->
            InfoItem(
                label = stringResource(R.string.door_label),
                value = formatBoardingDoor(door),
                foregroundColor = foregroundColor,
                labelColor = labelColor,
                alignment = Alignment.CenterHorizontally
            )
        }
        
        boardingData.queue?.let { queue ->
            InfoItem(
                label = stringResource(R.string.queue_label),
                value = formatQueue(queue),
                foregroundColor = foregroundColor,
                labelColor = labelColor,
                alignment = Alignment.CenterHorizontally
            )
        }
        
        boardingData.sequence?.let { seq ->
            InfoItem(
                label = stringResource(R.string.sequence_label),
                value = seq,
                foregroundColor = foregroundColor,
                labelColor = labelColor,
                alignment = Alignment.CenterHorizontally
            )
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    foregroundColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
    isHighlighted: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = alignment
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = if (isHighlighted) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = foregroundColor,
            fontSize = if (isHighlighted) 20.sp else 16.sp
        )
    }
}

@Composable
private fun DottedDivider(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = color
        )
    }
}

@Composable
private fun BarcodeSection(
    barcodeData: String,
    barcodeFormat: BarcodeFormat,
    altText: String?,
    backgroundColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val barcodeBitmap = remember(barcodeData, barcodeFormat) {
                try {
                    generateBarcode(barcodeData, barcodeFormat, 280, 280)
                } catch (e: Exception) {
                    null
                }
            }
            
            barcodeBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Boarding pass barcode",
                    modifier = Modifier.size(250.dp)
                )
            }
        }
        
        altText?.let { text ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }
    }
}

// Helper functions

private fun parsePassColor(colorString: String?): Color? {
    if (colorString == null) return null
    
    // Handle rgb(r,g,b) format
    val rgbRegex = Regex("""rgb\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""")
    val match = rgbRegex.find(colorString)
    
    return if (match != null) {
        val (r, g, b) = match.destructured
        Color(r.toInt(), g.toInt(), b.toInt())
    } else {
        // Try parsing as hex
        try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            null
        }
    }
}

private fun getTransitIcon(transitType: String?): ImageVector {
    return when (transitType?.uppercase()) {
        "PKTRANSITTYPEAIR", "AIR" -> Icons.Filled.AirplanemodeActive
        "PKTRANSITTYPEBUS", "BUS" -> Icons.Filled.DirectionsBus
        "PKTRANSITTYPETRAIN", "TRAIN" -> Icons.Filled.Train
        "PKTRANSITTYPEBOAT", "BOAT", "FERRY" -> Icons.Filled.DirectionsBoat
        else -> Icons.Filled.AirplanemodeActive // Default to airplane for boarding passes
    }
}

private fun formatBoardingDoor(door: String): String {
    return when (door.lowercase()) {
        "boarding_door_front" -> "Front"
        "boarding_door_rear" -> "Rear"
        else -> door
    }
}

private fun formatQueue(queue: String): String {
    return when (queue.lowercase()) {
        "queue_priority" -> "Priority"
        "queue_regular" -> "Regular"
        else -> queue
    }
}

private fun generateBarcode(data: String, format: BarcodeFormat, width: Int, height: Int): Bitmap? {
    return try {
        val zxingFormat = when (format) {
            BarcodeFormat.QR -> ZxingBarcodeFormat.QR_CODE
            BarcodeFormat.AZTEC -> ZxingBarcodeFormat.AZTEC
            BarcodeFormat.PDF417 -> ZxingBarcodeFormat.PDF_417
            BarcodeFormat.CODE128 -> ZxingBarcodeFormat.CODE_128
            BarcodeFormat.EAN13 -> ZxingBarcodeFormat.EAN_13
            BarcodeFormat.UPC_A -> ZxingBarcodeFormat.UPC_A
            BarcodeFormat.DATA_MATRIX -> ZxingBarcodeFormat.DATA_MATRIX
            else -> ZxingBarcodeFormat.QR_CODE
        }
        
        val writer = MultiFormatWriter()
        val bitMatrix: BitMatrix = writer.encode(data, zxingFormat, width, height)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
