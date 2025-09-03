/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.util

import androidx.compose.ui.text.input.OffsetMapping

/**
 * Credit card number offset mapping for formatting with spaces
 */
class CreditCardOffsetMapping : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        return when {
            offset <= 4 -> offset
            offset <= 8 -> offset + 1
            offset <= 12 -> offset + 2
            else -> offset + 3
        }
    }

    override fun transformedToOriginal(offset: Int): Int {
        return when {
            offset <= 4 -> offset
            offset <= 9 -> offset - 1
            offset <= 14 -> offset - 2
            else -> offset - 3
        }
    }
}

/**
 * IBAN offset mapping for formatting with spaces
 */
class IBANOffsetMapping : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        return offset + (offset / 4)
    }

    override fun transformedToOriginal(offset: Int): Int {
        return offset - (offset / 5)
    }
}

/**
 * SWIFT code offset mapping for formatting with spaces
 */
class SWIFTOffsetMapping : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        return when {
            offset <= 4 -> offset
            offset <= 6 -> offset + 1
            offset <= 8 -> offset + 2
            else -> offset + 3
        }
    }

    override fun transformedToOriginal(offset: Int): Int {
        return when {
            offset <= 4 -> offset
            offset <= 7 -> offset - 1
            offset <= 10 -> offset - 2
            else -> offset - 3
        }
    }
}

/**
 * ABA routing number offset mapping for formatting with dashes
 */
class ABAOffsetMapping : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        return when {
            offset <= 3 -> offset
            offset <= 6 -> offset + 1
            else -> offset + 2
        }
    }

    override fun transformedToOriginal(offset: Int): Int {
        return when {
            offset <= 3 -> offset
            offset <= 7 -> offset - 1
            else -> offset - 2
        }
    }
}