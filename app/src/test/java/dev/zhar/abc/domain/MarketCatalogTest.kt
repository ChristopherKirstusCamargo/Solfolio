package dev.zhar.abc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketCatalogTest {
    @Test
    fun `market contains forty distinct USD products`() {
        assertEquals(40, AssetCatalog.defaults.size)
        assertEquals(40, AssetCatalog.defaults.map { it.symbol }.distinct().size)
        assertTrue(AssetCatalog.defaults.all { it.productId.endsWith("-USD") })
    }
}
