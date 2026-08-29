package dev.zhar.abc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedAnalysisEngineTest {
    @Test fun `single asset is correctly identified as concentrated`() {
        val snapshot = PortfolioCalculator.calculate(
            entries = listOf(LedgerEntry(1, 1, "SOL", "Solana", TransactionKind.BUY, 2.0, 10.0, 0.0, 1)),
            quotes = mapOf("SOL" to AssetQuote("SOL", 20.0)),
        )
        val result = AdvancedAnalysisEngine.calculate(snapshot, emptyList())
        assertEquals(1.0, result.effectiveAssetCount, 0.0001)
        assertEquals(100.0, result.topThreePercent, 0.0001)
        assertNull(result.annualizedVolatilityPercent)
        assertTrue(result.riskScore >= 50)
    }

    @Test fun `balanced assets improve diversification`() {
        val entries = (1L..6L).map { id ->
            LedgerEntry(id, 1, "A$id", "Ativo $id", TransactionKind.BUY, 1.0, 10.0, 0.0, id)
        }
        val quotes = entries.associate { it.symbol to AssetQuote(it.symbol, 10.0) }
        val result = AdvancedAnalysisEngine.calculate(PortfolioCalculator.calculate(entries, quotes), emptyList())
        assertEquals(6.0, result.effectiveAssetCount, 0.0001)
        assertEquals(100, result.diversificationScore)
    }
}
