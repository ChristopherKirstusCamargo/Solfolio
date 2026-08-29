package dev.zhar.abc.domain

import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class AnalysisInsight(val title: String, val explanation: String)

data class AdvancedPortfolioAnalysis(
    val overallScore: Int,
    val diversificationScore: Int,
    val concentrationScore: Int,
    val riskScore: Int,
    val dataQualityScore: Int,
    val effectiveAssetCount: Double,
    val topThreePercent: Double,
    val annualizedVolatilityPercent: Double?,
    val historyDays: Int,
    val insights: List<AnalysisInsight>,
)

/** Transparent, deterministic analysis. No AI and no financial recommendation. */
object AdvancedAnalysisEngine {
    fun calculate(snapshot: PortfolioSnapshot, history: List<PortfolioHistoryPoint>): AdvancedPortfolioAnalysis {
        if (snapshot.holdings.isEmpty() || snapshot.totalValueUsd <= 0.0) {
            return AdvancedPortfolioAnalysis(0, 0, 0, 0, 0, 0.0, 0.0, null, history.size, emptyList())
        }
        val bySymbol = snapshot.holdings.groupBy { it.symbol }.mapValues { (_, rows) -> rows.sumOf { it.currentValueUsd } }
        val weights = bySymbol.values.map { (it / snapshot.totalValueUsd).coerceIn(0.0, 1.0) }.sortedDescending()
        val hhi = weights.sumOf { it * it }.coerceIn(0.0, 1.0)
        val effective = if (hhi > 0.0) 1.0 / hhi else 0.0
        val diversification = (((effective - 1.0) / 5.0) * 100.0).roundToInt().coerceIn(0, 100)
        val concentration = ((1.0 - hhi) * 100.0).roundToInt().coerceIn(0, 100)
        val topThree = weights.take(3).sum() * 100.0
        val volatility = annualizedVolatility(history)
        val volatilityRisk = volatility?.let { (it * 1.25).coerceIn(0.0, 100.0) } ?: 35.0
        val concentrationRisk = snapshot.largestAllocationPercent.coerceIn(0.0, 100.0)
        val coveragePenalty = (100.0 - snapshot.dataCoveragePercent).coerceIn(0.0, 100.0)
        val risk = (concentrationRisk * .52 + volatilityRisk * .33 + coveragePenalty * .15)
            .roundToInt().coerceIn(0, 100)
        val dataQuality = (snapshot.dataCoveragePercent * .55 + snapshot.pnlCoveragePercent * .45)
            .roundToInt().coerceIn(0, 100)
        val overall = (diversification * .30 + concentration * .25 + (100 - risk) * .25 + dataQuality * .20)
            .roundToInt().coerceIn(0, 100)

        val largest = bySymbol.maxByOrNull { it.value }
        val insights = buildList {
            largest?.let {
                val percent = it.value / snapshot.totalValueUsd * 100.0
                add(AnalysisInsight("Concentração principal", "${it.key} representa ${percent.roundToInt()}% do patrimônio e é a posição com maior influência no resultado."))
            }
            add(AnalysisInsight("Diversificação efetiva", "A distribuição atual equivale a aproximadamente ${"%.1f".format(effective)} ativos com pesos iguais."))
            if (snapshot.pnlCoveragePercent < 99.5) {
                add(AnalysisInsight("Base de custo parcial", "${snapshot.pnlCoveragePercent.roundToInt()}% do custo possui base confirmada. O restante é uma referência e reduz a precisão do P/L."))
            }
            if (volatility == null) {
                add(AnalysisInsight("Histórico em construção", "São necessários pelo menos 7 registros diários válidos para estimar a volatilidade do portfólio."))
            } else {
                add(AnalysisInsight("Oscilação histórica", "A volatilidade anualizada observada é ${volatility.roundToInt()}%. Ela descreve o passado e não prevê o próximo movimento."))
            }
        }
        return AdvancedPortfolioAnalysis(overall, diversification, concentration, risk, dataQuality, effective, topThree, volatility, history.size, insights)
    }

    private fun annualizedVolatility(history: List<PortfolioHistoryPoint>): Double? {
        val values = history.map { it.totalValueUsd }.filter { it > 0.0 }
        if (values.size < 7) return null
        val returns = values.zipWithNext { previous, current -> ln(current / previous) }
        if (returns.size < 2) return null
        val mean = returns.average()
        val variance = returns.sumOf { (it - mean) * (it - mean) } / (returns.size - 1)
        return sqrt(variance) * sqrt(365.0) * 100.0
    }
}
