/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class BarcodeResult(
    val data: String,
    val format: String
)

object BarcodeImageDecoder {

    suspend fun decodeFromUri(context: Context, uri: Uri): BarcodeResult? {
        return suspendCoroutine { continuation ->
            try {
                val image = InputImage.fromFilePath(context, uri)

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_CODABAR,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_ITF,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_PDF417,
                        Barcode.FORMAT_AZTEC,
                        Barcode.FORMAT_DATA_MATRIX
                    )
                    .build()

                val scanner = BarcodeScanning.getClient(options)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcode = barcodes.first()
                            val rawValue = barcode.rawValue
                            if (rawValue != null) {
                                val format = BarcodeFormatMapper.fromMlKit(barcode.format)
                                continuation.resume(BarcodeResult(rawValue, format))
                            } else {
                                continuation.resume(null)
                            }
                        } else {
                            continuation.resume(null)
                        }
                        scanner.close()
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                        scanner.close()
                    }
            } catch (_: Exception) {
                continuation.resume(null)
            }
        }
    }
}
