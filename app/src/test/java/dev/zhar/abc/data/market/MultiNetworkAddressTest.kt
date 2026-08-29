package dev.zhar.abc.data.market

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiNetworkAddressTest {
    @Test fun `validates bitcoin formats`() {
        assertTrue(BitcoinWalletService.isValidAddress("1BoatSLRHtKNngkdXEeobR76b53LETtpyT"))
        assertTrue(BitcoinWalletService.isValidAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kygt080"))
        assertFalse(BitcoinWalletService.isValidAddress("not-a-bitcoin-address"))
    }

    @Test fun `validates ethereum format`() {
        assertTrue(EthereumWalletService.isValidAddress("0x0000000000000000000000000000000000000000"))
        assertFalse(EthereumWalletService.isValidAddress("0x1234"))
    }
}
