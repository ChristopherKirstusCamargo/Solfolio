package dev.zhar.abc.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.zhar.abc.domain.AppSettings
import dev.zhar.abc.domain.Holding
import dev.zhar.abc.domain.PortfolioSnapshot
import dev.zhar.abc.domain.PortfolioHistoryPoint
import dev.zhar.abc.domain.AdvancedPortfolioAnalysis
import dev.zhar.abc.domain.AdvancedAnalysisEngine
import dev.zhar.abc.ui.components.AssetAvatar
import dev.zhar.abc.ui.components.EmptyState
import dev.zhar.abc.ui.components.assetColor
import dev.zhar.abc.ui.theme.AbcAmber
import dev.zhar.abc.ui.theme.AbcGreen
import dev.zhar.abc.ui.theme.AbcPurple
import dev.zhar.abc.ui.theme.AbcRed
import dev.zhar.abc.ui.theme.SolfolioLayout
import dev.zhar.abc.util.formatMoney
import dev.zhar.abc.util.formatPercent
import dev.zhar.abc.util.hiddenOr

@Composable
fun AnalysisScreen(
    portfolioName: String,
    snapshot: PortfolioSnapshot,
    history: List<PortfolioHistoryPoint>,
    settings: AppSettings,
    onAddEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by rememberSaveable { mutableStateOf(AnalysisMode.SUMMARY) }
    val advanced = AdvancedAnalysisEngine.calculate(snapshot, history)
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = SolfolioLayout.screenHorizontal,
            end = SolfolioLayout.screenHorizontal,
            top = SolfolioLayout.screenTop,
            bottom = SolfolioLayout.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(SolfolioLayout.sectionSpacing),
    ) {
        item {
            Column {
                Text("Desempenho e risco", style = MaterialTheme.typography.headlineMedium)
                Text(
                    portfolioName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == AnalysisMode.SUMMARY, onClick = { mode = AnalysisMode.SUMMARY }, label = { Text("Resumo") }, modifier = Modifier.weight(1f))
                FilterChip(selected = mode == AnalysisMode.ADVANCED, onClick = { mode = AnalysisMode.ADVANCED }, label = { Text("Detalhada") }, modifier = Modifier.weight(1f))
            }
        }

        if (snapshot.holdings.isEmpty()) {
            item {
                EmptyState(
                    title = "Sem dados para analisar",
                    message = "Adicione uma posição para começar.",
                )
            }
        } else if (mode == AnalysisMode.ADVANCED) {
            item { AdvancedScoreCard(advanced) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreMetricCard("Diversificação", advanced.diversificationScore, scoreLabel(advanced.diversificationScore), "Equilíbrio entre ativos", Modifier.weight(1f))
                    ScoreMetricCard("Distribuição", advanced.concentrationScore, scoreLabel(advanced.concentrationScore), "Maior é mais equilibrado", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreMetricCard("Risco", advanced.riskScore, riskLabel(advanced.riskScore), "Maior exige mais atenção", Modifier.weight(1f))
                    ScoreMetricCard("Precisão", advanced.dataQualityScore, scoreLabel(advanced.dataQualityScore), "Preços e custos conhecidos", Modifier.weight(1f))
                }
            }
            item { Text("Por que essa nota existe?", style = MaterialTheme.typography.titleLarge) }
            items(advanced.insights, key = { it.title }) { insight -> InsightCard(insight.title, insight.explanation) }
            item {
                InsightCard(
                    "Histórico local",
                    if (advanced.annualizedVolatilityPercent == null) "${advanced.historyDays} dia(s) registrados. A volatilidade aparecerá quando houver histórico suficiente."
                    else "${advanced.historyDays} dia(s) registrados. Volatilidade anualizada observada: ${formatPercent(advanced.annualizedVolatilityPercent)}.",
                )
            }
            item {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Análise estatística local. Não utiliza IA e não constitui recomendação financeira.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            item { HealthScoreCard(snapshot) }
            item { PerformanceOverviewCard(snapshot, settings) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreMetricCard(
                        title = "Risco",
                        score = snapshot.riskScore,
                        label = snapshot.riskLabel,
                        supporting = "Exposição da carteira",
                        modifier = Modifier.weight(1f),
                    )
                    ScoreMetricCard(
                        title = "Desempenho",
                        score = snapshot.performanceScore,
                        label = snapshot.performanceLabel,
                        supporting = "P/L + últimas 24 h",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                InsightCard(
                    title = "Faixa indicativa de sensibilidade",
                    message = "Se a oscilação recente ponderada se repetisse, o valor ficaria entre ${hiddenOr(formatMoney(snapshot.expectedLowUsd, settings), settings.hideBalances)} e ${hiddenOr(formatMoney(snapshot.expectedHighUsd, settings), settings.hideBalances)}. Não é previsão de preço.",
                )
            }
            item {
                Text("Distribuição", style = MaterialTheme.typography.titleLarge)
            }
            items(snapshot.holdings, key = { "${it.sourceKey}:${it.symbol}" }) { holding ->
                AllocationRow(holding, settings)
            }
            item {
                Text("Leitura estrutural", style = MaterialTheme.typography.titleLarge)
            }
            item {
                InsightCard(
                    title = concentrationTitle(snapshot.largestAllocationPercent),
                    message = concentrationMessage(snapshot),
                )
            }
            if (snapshot.missingLivePrices > 0) {
                item {
                    InsightCard(
                        title = "Cotação incompleta",
                        message = "${snapshot.missingLivePrices} ativo(s) não receberam preço ao vivo. O app está usando o último preço registrado nessas posições.",
                    )
                }
            }
            item {
                InsightCard(
                    title = if (snapshot.pnlIsEstimated) "P/L parcialmente estimado" else "P/L por custo informado",
                    message = "A posição aberta está em ${hiddenOr(formatMoney(snapshot.unrealizedPnlUsd, settings), settings.hideBalances)} (${hiddenOr(formatPercent(snapshot.unrealizedPnlPercent), settings.hideBalances)}). ${formatPercent(snapshot.pnlCoveragePercent)} do custo possui base precisa; o restante usa a primeira sincronização como referência.",
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Os indicadores ajudam a entender a carteira, mas não garantem resultados futuros.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private enum class AnalysisMode { SUMMARY, ADVANCED }

@Composable
private fun AdvancedScoreCard(analysis: AdvancedPortfolioAnalysis) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(82.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) {
                Text(analysis.overallScore.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("Nota geral", style = MaterialTheme.typography.titleLarge)
                Text("${scoreLabel(analysis.overallScore)} · ${"%.1f".format(analysis.effectiveAssetCount)} ativos efetivos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Três maiores posições: ${"%.1f".format(analysis.topThreePercent)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun scoreLabel(score: Int) = when { score >= 82 -> "Muito boa"; score >= 65 -> "Boa"; score >= 45 -> "Atenção"; else -> "Baixa" }
private fun riskLabel(score: Int) = when { score >= 75 -> "Muito alto"; score >= 55 -> "Alto"; score >= 30 -> "Moderado"; else -> "Baixo" }

@Composable
private fun PerformanceOverviewCard(snapshot: PortfolioSnapshot, settings: AppSettings) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Resumo de desempenho", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryValue("Valor atual", hiddenOr(formatMoney(snapshot.totalValueUsd, settings), settings.hideBalances), Modifier.weight(1f))
                SummaryValue("Custo", hiddenOr(formatMoney(snapshot.remainingCostUsd, settings), settings.hideBalances), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryValue("P/L aberto", hiddenOr(formatMoney(snapshot.unrealizedPnlUsd, settings), settings.hideBalances), Modifier.weight(1f))
                SummaryValue("P/L realizado", hiddenOr(formatMoney(snapshot.realizedPnlUsd, settings), settings.hideBalances), Modifier.weight(1f))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("${snapshot.sourceCount} fontes separadas · ${formatPercent(snapshot.dataCoveragePercent)} com cotação · ${formatPercent(snapshot.pnlCoveragePercent)} do custo confirmado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .76f))
            if (snapshot.best24hSymbol != null) Text("Melhor 24 h: ${snapshot.best24hSymbol} · Pior 24 h: ${snapshot.worst24hSymbol ?: "—"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .76f))
        }
    }
}

@Composable private fun SummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    val style = if (value.length > 16) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium
    Column(modifier) { Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .72f)); Text(value, style = style, maxLines = 1) }
}

@Composable
private fun ScoreMetricCard(
    title: String,
    score: Int,
    label: String,
    supporting: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(score.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { (score / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                strokeCap = StrokeCap.Round,
            )
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HealthScoreCard(snapshot: PortfolioSnapshot) {
    val scoreColor = when {
        snapshot.healthScore >= 82 -> AbcGreen
        snapshot.healthScore >= 65 -> AbcPurple
        snapshot.healthScore >= 40 -> AbcAmber
        else -> AbcRed
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(104.dp)) {
                    val stroke = 11.dp.toPx()
                    val inset = stroke / 2
                    drawArc(
                        color = scoreColor.copy(alpha = 0.14f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = 360f * (snapshot.healthScore / 100f),
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        snapshot.healthScore.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = scoreColor,
                    )
                    Text("/ 100", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(snapshot.healthLabel, style = MaterialTheme.typography.titleLarge, color = scoreColor)
                Spacer(Modifier.height(7.dp))
                Text(
                    "Maior posição: ${formatPercent(snapshot.largestAllocationPercent)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${snapshot.holdings.size} ativos com saldo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AllocationRow(holding: Holding, settings: AppSettings) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetAvatar(holding.symbol, size = 34.dp)
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(holding.symbol, style = MaterialTheme.typography.titleMedium)
                Text(holding.sourceLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    hiddenOr(formatMoney(holding.currentValueUsd, settings), settings.hideBalances),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                hiddenOr(formatPercent(holding.allocationPercent), settings.hideBalances),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { (holding.allocationPercent / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color = assetColor(holding.symbol),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun InsightCard(title: String, message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun concentrationTitle(largest: Double): String = when {
    largest >= 90 -> "Concentração extrema"
    largest >= 75 -> "Concentração alta"
    largest >= 60 -> "Concentração relevante"
    else -> "Distribuição moderada"
}

private fun concentrationMessage(snapshot: PortfolioSnapshot): String {
    val largest = snapshot.holdings.firstOrNull()?.symbol ?: "O maior ativo"
    return "$largest representa ${formatPercent(snapshot.largestAllocationPercent)} do valor atual. Isso amplia a influência de um único ativo sobre todo o portfólio."
}
