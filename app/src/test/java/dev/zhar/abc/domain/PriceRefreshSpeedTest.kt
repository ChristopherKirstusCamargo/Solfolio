package dev.zhar.abc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceRefreshSpeedTest {
    @Test
    fun `intervals match the options shown in settings`() {
        assertEquals(60_000L, PriceRefreshSpeed.DISABLED.intervalMs)
        assertEquals(45_000L, PriceRefreshSpeed.VERY_LOW.intervalMs)
        assertEquals(30_000L, PriceRefreshSpeed.LOW.intervalMs)
        assertEquals(15_000L, PriceRefreshSpeed.MEDIUM.intervalMs)
        assertEquals(5_000L, PriceRefreshSpeed.HIGH.intervalMs)
        assertTrue(PriceRefreshSpeed.INSTANT.intervalMs < 1_000L)
    }
}
