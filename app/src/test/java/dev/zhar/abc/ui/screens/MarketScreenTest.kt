package dev.zhar.abc.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class MarketScreenTest {
    @Test
    fun `countdown uses minute and second format`() {
        assertEquals("1:00", formatCountdown(60))
        assertEquals("0:09", formatCountdown(9))
        assertEquals("0:00", formatCountdown(-1))
    }
}
