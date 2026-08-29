package dev.zhar.abc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketCatalogTest {
    @Test
    fun `market contains twenty distinct USD products`() {
        assertEquals(20, AssetCatalog.defaults.size)
        assertEquals(20, AssetCatalog.defaults.map { it.symbol }.distinct().size)
        assertTrue(AssetCatalog.defaults.all { it.productId.endsWith("-USD") })
    }
}
