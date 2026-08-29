package dev.zhar.abc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioCalculatorTest {
    @Test
    fun `moving average separates realized and unrealized pnl`() {
        val entries = listOf(
            LedgerEntry(
                id = 1,
                portfolioId = 1,
                symbol = "SOL",
                assetName = "Solana",
                kind = TransactionKind.BUY,
                quantity = 2.0,
                unitPriceUsd = 10.0,
                feeUsd = 1.0,
                timestamp = 1,
            ),
            LedgerEntry(
                id = 2,
                portfolioId = 1,
                symbol = "SOL",
                assetName = "Solana",
                kind = TransactionKind.SELL,
                quantity = 1.0,
                unitPriceUsd = 15.0,
                feeUsd = 1.0,
                timestamp = 2,
            ),
        )
        val result = PortfolioCalculator.calculate(
            entries,
            mapOf("SOL" to AssetQuote(symbol = "SOL", priceUsd = 12.0)),
        )

        assertEquals(1.0, result.holdings.single().quantity, 0.00001)
        assertEquals(10.5, result.remainingCostUsd, 0.00001)
        assertEquals(3.5, result.realizedPnlUsd, 0.00001)
        assertEquals(1.5, result.unrealizedPnlUsd, 0.00001)
    }

    @Test
    fun `allocation always sums to one hundred`() {
        val entries = listOf(
            buy(1, "BTC", 1.0, 10.0),
            buy(2, "ETH", 2.0, 10.0),
            buy(3, "SOL", 3.0, 10.0),
        )
        val quotes = mapOf(
            "BTC" to AssetQuote("BTC", 10.0),
            "ETH" to AssetQuote("ETH", 10.0),
            "SOL" to AssetQuote("SOL", 10.0),
        )
        val result = PortfolioCalculator.calculate(entries, quotes)

        assertEquals(100.0, result.holdings.sumOf { it.allocationPercent }, 0.00001)
        assertTrue(result.healthScore in 0..100)
        assertTrue(result.riskScore in 0..100)
        assertTrue(result.performanceScore in 0..100)
        assertTrue(result.expectedLowUsd <= result.totalValueUsd)
        assertTrue(result.expectedHighUsd >= result.totalValueUsd)
    }

    @Test
    fun `tracked wallet position is merged and marked estimated`() {
        val result = PortfolioCalculator.calculate(
            entries = emptyList(),
            quotes = mapOf("SOL" to AssetQuote("SOL", 20.0, change24hPercent = 5.0)),
            trackedPositions = listOf(TrackedPosition(7, "Carteira A", "SOL", "Solana", 3.0, 45.0, false)),
        )

        assertEquals(60.0, result.totalValueUsd, 0.00001)
        assertEquals(15.0, result.unrealizedPnlUsd, 0.00001)
        assertEquals(1, result.trackedWalletCount)
        assertTrue(result.pnlIsEstimated)
    }

    @Test
    fun `same asset from different addresses is never merged`() {
        val result = PortfolioCalculator.calculate(
            entries = emptyList(),
            quotes = mapOf("SOL" to AssetQuote("SOL", 20.0)),
            trackedPositions = listOf(
                TrackedPosition(7, "Carteira A", "SOL", "Solana", 2.0, 30.0, true),
                TrackedPosition(8, "Carteira B", "SOL", "Solana", 5.0, 90.0, true),
            ),
        )

        assertEquals(2, result.holdings.size)
        assertEquals(setOf("wallet:7", "wallet:8"), result.holdings.map { it.sourceKey }.toSet())
        assertEquals(140.0, result.totalValueUsd, 0.00001)
        assertEquals(100.0, result.pnlCoveragePercent, 0.00001)
    }

    @Test
    fun `tracked wallet remains visible while live quote reconnects`() {
        val result = PortfolioCalculator.calculate(
            entries = emptyList(),
            quotes = emptyMap(),
            trackedPositions = listOf(
                TrackedPosition(7, "Carteira A", "SOL", "Solana", 2.0, 30.0, true, fallbackPriceUsd = 18.0),
            ),
        )

        assertEquals(1, result.holdings.size)
        assertEquals(36.0, result.totalValueUsd, 0.00001)
        assertEquals(0.0, result.dataCoveragePercent, 0.00001)
    }

    private fun buy(id: Long, symbol: String, quantity: Double, price: Double) = LedgerEntry(
        id = id,
        portfolioId = 1,
        symbol = symbol,
        assetName = symbol,
        kind = TransactionKind.BUY,
        quantity = quantity,
        unitPriceUsd = price,
        feeUsd = 0.0,
        timestamp = id,
    )
}
