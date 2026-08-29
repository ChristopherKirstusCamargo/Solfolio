package dev.zhar.abc.data.market

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SolanaWalletServiceTest {
    @Test fun `accepts public base58 address`() {
        assertTrue(SolanaWalletService.isValidAddress("So11111111111111111111111111111111111111112"))
    }

    @Test fun `rejects seed text private looking input and invalid base58`() {
        assertFalse(SolanaWalletService.isValidAddress("seed phrase never belongs here"))
        assertFalse(SolanaWalletService.isValidAddress("0OIl11111111111111111111111111111111"))
        assertFalse(SolanaWalletService.isValidAddress("short"))
    }
}
