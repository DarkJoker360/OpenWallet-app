/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.feature.passmanagement.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.esposito.openwallet.R
import com.esposito.openwallet.core.domain.model.BarcodeFormat
import com.google.zxing.BarcodeFormat as ZXingBarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.oned.Code128Writer
import com.google.zxing.oned.EAN13Writer
import com.google.zxing.oned.UPCAWriter
import com.google.zxing.pdf417.PDF417Writer
import com.google.zxing.aztec.AztecWriter
import com.google.zxing.datamatrix.DataMatrixWriter
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap

@Composable
fun QRCodeView(
    data: String,
    format: BarcodeFormat,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    backgroundColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    foregroundColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val sizePx = with(density) { size.roundToPx() }
    
    var bitmap by remember(data, format, sizePx) {
        mutableStateOf<Bitmap?>(null)
    }
    
    var error by remember(data, format) {
        mutableStateOf<String?>(null)
    }
    
    LaunchedEffect(data, format, sizePx) {
        try {
            bitmap = generateBarcodeBitmap(
                data = data,
                format = format,
                width = sizePx,
                height = when (format) {
                    BarcodeFormat.QR, BarcodeFormat.AZTEC, BarcodeFormat.DATA_MATRIX -> sizePx
                    BarcodeFormat.PDF417 -> sizePx / 3 // PDF417 is wider than tall
                    else -> sizePx / 4 // Linear barcodes are much wider than tall
                },
                backgroundColor = backgroundColor.toArgb(),
                foregroundColor = foregroundColor.toArgb()
            )
            error = null
        } catch (e: Exception) {
            bitmap = null
            error = e.message ?: context.getString(R.string.failed_to_generate_barcode_fallback)
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.barcode_data_content_description, data),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
            
            error != null -> {
                Text(
                    text = stringResource(R.string.error_generating_barcode_with_error, error ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = androidx.compose.ui.graphics.Color.Red
                )
            }
            
            else -> {
                // Loading state could be added here if needed
                Text(
                    text = stringResource(R.string.generating),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun generateBarcodeBitmap(
    data: String,
    format: BarcodeFormat,
    width: Int,
    height: Int,
    backgroundColor: Int = Color.WHITE,
    foregroundColor: Int = Color.BLACK
): Bitmap {
    val writer = when (format) {
        BarcodeFormat.QR -> QRCodeWriter()
        BarcodeFormat.PDF417 -> PDF417Writer()
        BarcodeFormat.AZTEC -> AztecWriter()
        BarcodeFormat.CODE128 -> Code128Writer()
        BarcodeFormat.EAN13 -> EAN13Writer()
        BarcodeFormat.UPC_A -> UPCAWriter()
        BarcodeFormat.DATA_MATRIX -> DataMatrixWriter()
        BarcodeFormat.NONE -> throw IllegalArgumentException("Cannot generate barcode for NONE format")
    }
    
    val zxingFormat = when (format) {
        BarcodeFormat.QR -> ZXingBarcodeFormat.QR_CODE
        BarcodeFormat.PDF417 -> ZXingBarcodeFormat.PDF_417
        BarcodeFormat.AZTEC -> ZXingBarcodeFormat.AZTEC
        BarcodeFormat.CODE128 -> ZXingBarcodeFormat.CODE_128
        BarcodeFormat.EAN13 -> ZXingBarcodeFormat.EAN_13
        BarcodeFormat.UPC_A -> ZXingBarcodeFormat.UPC_A
        BarcodeFormat.DATA_MATRIX -> ZXingBarcodeFormat.DATA_MATRIX
        BarcodeFormat.NONE -> throw IllegalArgumentException("Cannot generate barcode for NONE format")
    }
    
    val hints = mutableMapOf<EncodeHintType, Any>()
    
    // Set hints based on format
    when (format) {
        BarcodeFormat.QR -> {
            hints[EncodeHintType.MARGIN] = 1 // Minimal margin for QR codes
        }
        BarcodeFormat.PDF417 -> {
            hints[EncodeHintType.PDF417_COMPACT] = true
            hints[EncodeHintType.MARGIN] = 5
        }
        else -> {
            hints[EncodeHintType.MARGIN] = 10
        }
    }
    
    try {
        val bitMatrix: BitMatrix = writer.encode(data, zxingFormat, width, height, hints)
        return bitMatrixToBitmap(bitMatrix, backgroundColor, foregroundColor)
    } catch (e: WriterException) {
        throw RuntimeException("Error generating barcode: ${e.message}", e)
    }
}

private fun bitMatrixToBitmap(
    matrix: BitMatrix,
    backgroundColor: Int = Color.WHITE,
    foregroundColor: Int = Color.BLACK
): Bitmap {
    val width = matrix.width
    val height = matrix.height
    val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
    
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap[x, y] = if (matrix[x, y]) foregroundColor else backgroundColor
        }
    }
    
    return bitmap
}
