package dev.zhar.abc.domain

import kotlin.math.abs
import kotlin.math.roundToInt

object PortfolioCalculator {
    private const val EPSILON = 1e-10

    fun calculate(
        entries: List<LedgerEntry>,
        quotes: Map<String, AssetQuote>,
        trackedPositions: List<TrackedPosition> = emptyList(),
        portfolioNames: Map<Long, String> = emptyMap(),
    ): PortfolioSnapshot {
        if (entries.isEmpty() && trackedPositions.isEmpty()) return PortfolioSnapshot()

        val manual = entries.groupBy { it.portfolioId to it.symbol.uppercase() }.mapNotNull { (key, grouped) ->
            calculateManualHolding(
                sourceKey = "manual:${key.first}",
                sourceLabel = portfolioNames[key.first] ?: "Lançamentos",
                symbol = key.second,
                entries = grouped.sortedBy { it.timestamp },
                quote = quotes[key.second],
            )
        }
        val watched = trackedPositions.mapNotNull { calculateTrackedHolding(it, quotes[it.symbol.uppercase()]) }
            .groupBy { it.sourceKey to it.symbol }
            .map { mergeSameSource(it.value) }
        val raw = manual + watched
        val total = raw.sumOf { it.currentValueUsd }
        val holdings = raw.map {
            it.copy(allocationPercent = if (total > EPSILON) it.currentValueUsd / total * 100.0 else 0.0)
        }.sortedByDescending { it.currentValueUsd }

        val cost = holdings.sumOf { it.remainingCostUsd }
        val unrealized = holdings.sumOf { it.unrealizedPnlUsd }
        val realized = holdings.sumOf { it.realizedPnlUsd }
        val change = weightedChange(holdings, absolute = false)
        val expectedMove = (weightedChange(holdings, absolute = true) ?: 0.0).coerceIn(0.0, 35.0)
        val symbolTotals = holdings.groupBy { it.symbol }.mapValues { row -> row.value.sumOf { it.currentValueUsd } }
        val largest = if (total > EPSILON) (symbolTotals.values.maxOrNull() ?: 0.0) / total * 100.0 else 0.0
        val missing = holdings.count { !it.hasLivePrice }
        val uniqueAssets = symbolTotals.size
        val (healthScore, healthLabel) = health(uniqueAssets, largest, missing)
        val (riskScore, riskLabel) = risk(uniqueAssets, holdings, largest, missing, expectedMove)
        val pnlPercent = if (cost > EPSILON) unrealized / cost * 100.0 else 0.0
        val performance = (50.0 + pnlPercent * 1.35 + (change ?: 0.0) * 1.7).roundToInt().coerceIn(0, 100)
        val exactCost = holdings.filter { it.basisIsExact }.sumOf { it.remainingCostUsd }
        val pricedValue = holdings.filter { it.hasLivePrice }.sumOf { it.currentValueUsd }
        val changed = holdings.filter { it.change24hPercent != null }

        return PortfolioSnapshot(
            holdings = holdings,
            totalValueUsd = total,
            remainingCostUsd = cost,
            unrealizedPnlUsd = unrealized,
            realizedPnlUsd = realized,
            unrealizedPnlPercent = pnlPercent,
            change24hPercent = change,
            healthScore = healthScore,
            healthLabel = healthLabel,
            largestAllocationPercent = largest,
            missingLivePrices = missing,
            riskScore = riskScore,
            riskLabel = riskLabel,
            performanceScore = performance,
            performanceLabel = performanceLabel(performance),
            expectedLowUsd = total * (1.0 - expectedMove / 100.0),
            expectedHighUsd = total * (1.0 + expectedMove / 100.0),
            trackedWalletCount = trackedPositions.map { it.walletId }.distinct().size,
            trackedValueUsd = watched.sumOf { it.currentValueUsd },
            pnlIsEstimated = trackedPositions.any { !it.basisIsExact },
            pnlCoveragePercent = if (cost > EPSILON) exactCost / cost * 100.0 else 100.0,
            dataCoveragePercent = if (total > EPSILON) pricedValue / total * 100.0 else 0.0,
            sourceCount = holdings.map { it.sourceKey }.distinct().size,
            best24hSymbol = changed.maxByOrNull { it.change24hPercent ?: Double.NEGATIVE_INFINITY }?.symbol,
            worst24hSymbol = changed.minByOrNull { it.change24hPercent ?: Double.POSITIVE_INFINITY }?.symbol,
        )
    }

    private fun calculateManualHolding(sourceKey: String, sourceLabel: String, symbol: String, entries: List<LedgerEntry>, quote: AssetQuote?): Holding? {
        var quantity = 0.0
        var cost = 0.0
        var realized = 0.0
        entries.forEach { entry ->
            when (entry.kind) {
                TransactionKind.BUY -> {
                    quantity += entry.quantity
                    cost += entry.quantity * entry.unitPriceUsd + entry.feeUsd
                }
                TransactionKind.SELL -> {
                    if (quantity <= EPSILON) return@forEach
                    val sold = entry.quantity.coerceAtMost(quantity)
                    val removedCost = cost / quantity * sold
                    realized += sold * entry.unitPriceUsd - entry.feeUsd - removedCost
                    quantity -= sold
                    cost = (cost - removedCost).coerceAtLeast(0.0)
                }
            }
        }
        if (quantity <= EPSILON) return null
        val price = quote?.priceUsd?.takeIf { it > 0.0 } ?: entries.last().unitPriceUsd
        val value = quantity * price
        return Holding(sourceKey, sourceLabel, symbol, entries.last().assetName, quantity, cost / quantity, cost, price, value,
            value - cost, realized, quote?.change24hPercent, 0.0, quote?.recentPrices.orEmpty(), quote != null, true)
    }

    private fun calculateTrackedHolding(position: TrackedPosition, quote: AssetQuote?): Holding? {
        if (position.quantity <= EPSILON) return null
        val stablePrice = if (position.symbol.uppercase() in setOf("USDC", "USDT")) 1.0 else null
        val price = quote?.priceUsd?.takeIf { it > 0.0 } ?: stablePrice ?: position.fallbackPriceUsd?.takeIf { it > 0.0 } ?: return null
        val value = position.quantity * price
        return Holding("wallet:${position.walletId}", position.sourceLabel, position.symbol.uppercase(), position.name,
            position.quantity, position.basisUsd / position.quantity, position.basisUsd, price, value, value - position.basisUsd,
            0.0, quote?.change24hPercent ?: stablePrice?.let { 0.0 }, 0.0, quote?.recentPrices.orEmpty(), quote != null || stablePrice != null, position.basisIsExact)
    }

    private fun mergeSameSource(parts: List<Holding>): Holding {
        if (parts.size == 1) return parts.first()
        val quantity = parts.sumOf { it.quantity }
        val value = parts.sumOf { it.currentValueUsd }
        val cost = parts.sumOf { it.remainingCostUsd }
        return parts.first().copy(
            quantity = quantity,
            averageCostUsd = if (quantity > EPSILON) cost / quantity else 0.0,
            remainingCostUsd = cost,
            currentPriceUsd = if (quantity > EPSILON) value / quantity else 0.0,
            currentValueUsd = value,
            unrealizedPnlUsd = value - cost,
            basisIsExact = parts.all { it.basisIsExact },
        )
    }

    private fun weightedChange(holdings: List<Holding>, absolute: Boolean): Double? {
        val covered = holdings.filter { it.change24hPercent != null && it.currentValueUsd > EPSILON }
        val value = covered.sumOf { it.currentValueUsd }
        if (value <= EPSILON) return null
        return covered.sumOf { it.currentValueUsd * if (absolute) abs(it.change24hPercent ?: 0.0) else (it.change24hPercent ?: 0.0) } / value
    }

    private fun health(count: Int, largest: Double, missing: Int): Pair<Int, String> {
        if (count == 0) return 0 to "Sem dados"
        var score = 100
        score -= when { largest >= 90 -> 48; largest >= 75 -> 36; largest >= 60 -> 24; largest >= 45 -> 12; else -> 0 }
        score -= when (count) { 1 -> 22; 2 -> 10; else -> 0 }
        score -= (missing * 5).coerceAtMost(15)
        score = score.coerceIn(0, 100)
        return score to when { score < 40 -> "Muito concentrada"; score < 65 -> "Atenção"; score < 82 -> "Equilibrada"; else -> "Bem distribuída" }
    }

    private fun risk(count: Int, holdings: List<Holding>, largest: Double, missing: Int, expectedMove: Double): Pair<Int, String> {
        if (holdings.isEmpty()) return 0 to "Sem dados"
        val stable = holdings.filter { it.symbol in setOf("USDC", "USDT") }.sumOf { it.allocationPercent }
        var score = 12.0 + (largest * .45).coerceAtMost(43.0) + (expectedMove * 1.6).coerceAtMost(28.0) + (missing * 5.0).coerceAtMost(15.0)
        if (count == 1) score += 12.0 else if (count == 2) score += 5.0
        score -= (stable * .15).coerceAtMost(15.0)
        val value = score.roundToInt().coerceIn(0, 100)
        return value to when { value < 30 -> "Baixo"; value < 55 -> "Moderado"; value < 75 -> "Alto"; else -> "Muito alto" }
    }

    private fun performanceLabel(score: Int) = when { score < 30 -> "Muito abaixo"; score < 45 -> "Abaixo"; score < 60 -> "Neutro"; score < 78 -> "Positivo"; else -> "Muito positivo" }
}
