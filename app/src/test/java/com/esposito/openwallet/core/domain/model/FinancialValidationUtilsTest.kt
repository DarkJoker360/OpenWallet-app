/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

package com.esposito.openwallet.core.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialValidationUtilsTest {

    @Test
    fun `validates known credit card numbers`() {
        assertTrue(FinancialValidationUtils.validateCardNumber("4111 1111 1111 1111"))
        assertTrue(FinancialValidationUtils.validateCardNumber("378282246310005"))
    }

    @Test
    fun `rejects malformed credit card numbers`() {
        assertFalse(FinancialValidationUtils.validateCardNumber("4111 1111 1111 1112"))
        assertFalse(FinancialValidationUtils.validateCardNumber("1234"))
        assertFalse(FinancialValidationUtils.validateCardNumber(""))
    }

    @Test
    fun `validates known IBANs`() {
        assertTrue(FinancialValidationUtils.validateIBAN("GB82 WEST 1234 5698 7654 32"))
        assertTrue(FinancialValidationUtils.validateIBAN("IT60 X054 2811 1010 0000 0123 456"))
    }

    @Test
    fun `rejects malformed IBANs`() {
        assertFalse(FinancialValidationUtils.validateIBAN("GB82 WEST 1234 5698 7654 31"))
        assertFalse(FinancialValidationUtils.validateIBAN(null))
    }

    @Test
    fun `validates SWIFT and ABA values`() {
        assertTrue(FinancialValidationUtils.validateSWIFTCode("DEUTDEFF"))
        assertTrue(FinancialValidationUtils.validateSWIFTCode("DEUTDEFF500"))
        assertTrue(FinancialValidationUtils.validateABARoutingNumber("021000021"))
    }

    @Test
    fun `rejects invalid SWIFT and ABA values`() {
        assertFalse(FinancialValidationUtils.validateSWIFTCode("BAD"))
        assertFalse(FinancialValidationUtils.validateABARoutingNumber("021000022"))
    }
}
