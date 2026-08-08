package com.esposito.openwallet.core.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoAddressValidatorTest {
    @Test
    fun validatesCommonAddresses() {
        assertTrue(CryptoAddressValidator.isValid("0x0000000000000000000000000000000000000001", SupportedBlockchain.ETHEREUM, "Mainnet"))
        assertTrue(CryptoAddressValidator.isValid("bc1qaaaaaaaaaaaaaaaaaaaaaa", SupportedBlockchain.BITCOIN, "Mainnet"))
    }

    @Test
    fun rejectsWrongNetworkAndMalformedAddresses() {
        assertFalse(CryptoAddressValidator.isValid("0x1234", SupportedBlockchain.ETHEREUM, "Mainnet"))
        assertFalse(CryptoAddressValidator.isValid("0x0000000000000000000000000000000000000001", SupportedBlockchain.ETHEREUM, "Unknown"))
    }
}
