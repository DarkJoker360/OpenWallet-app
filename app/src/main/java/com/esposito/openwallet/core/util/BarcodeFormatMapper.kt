/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.util

import com.esposito.openwallet.core.domain.model.BarcodeFormat
import com.google.mlkit.vision.barcode.common.Barcode

object BarcodeFormatMapper {

    fun fromMlKit(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_CODE_93 -> "CODE_93"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_ITF -> "ITF"
            else -> "QR_CODE"
        }
    }

    fun fromScannerString(scannerFormat: String): BarcodeFormat {
        return when (scannerFormat) {
            "QR_CODE" -> BarcodeFormat.QR
            "CODE_128" -> BarcodeFormat.CODE128
            "EAN_13" -> BarcodeFormat.EAN13
            "UPC_A" -> BarcodeFormat.UPC_A
            "PDF417" -> BarcodeFormat.PDF417
            "AZTEC" -> BarcodeFormat.AZTEC
            "DATA_MATRIX" -> BarcodeFormat.DATA_MATRIX
            "CODE_39", "CODE_93", "CODABAR", "EAN_8", "ITF", "UPC_E" -> BarcodeFormat.CODE128
            else -> BarcodeFormat.QR
        }
    }

    fun toScannerString(format: BarcodeFormat): String {
        return when (format) {
            BarcodeFormat.QR -> "QR_CODE"
            BarcodeFormat.CODE128 -> "CODE_128"
            BarcodeFormat.EAN13 -> "EAN_13"
            BarcodeFormat.UPC_A -> "UPC_A"
            BarcodeFormat.PDF417 -> "PDF417"
            BarcodeFormat.AZTEC -> "AZTEC"
            BarcodeFormat.DATA_MATRIX -> "DATA_MATRIX"
            BarcodeFormat.NONE -> "QR_CODE"
        }
    }

    fun fromPkPassFormat(format: String?): BarcodeFormat {
        return when (format?.uppercase()) {
            "PKBARCODEFORMATQR" -> BarcodeFormat.QR
            "PKBARCODEFORMATPDF417" -> BarcodeFormat.PDF417
            "PKBARCODEFORMATAZTEC" -> BarcodeFormat.AZTEC
            "PKBARCODEFORMATCODE128" -> BarcodeFormat.CODE128
            else -> BarcodeFormat.QR
        }
    }
}
