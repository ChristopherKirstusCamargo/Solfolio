package dev.zhar.abc.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zhar.abc.domain.AppSettings
import dev.zhar.abc.domain.FeedStatus
import dev.zhar.abc.domain.Holding
import dev.zhar.abc.domain.PortfolioSnapshot
import dev.zhar.abc.domain.PortfolioHistoryPoint
import dev.zhar.abc.ui.PortfolioView
import dev.zhar.abc.ui.components.AssetAvatar
import dev.zhar.abc.ui.components.EmptyState
import dev.zhar.abc.ui.components.HeroBalanceCard
import dev.zhar.abc.ui.components.Sparkline
import dev.zhar.abc.ui.components.SolfolioMark
import dev.zhar.abc.ui.theme.AbcGreen
import dev.zhar.abc.ui.theme.AbcRed
import dev.zhar.abc.ui.theme.SolfolioLayout
import dev.zhar.abc.util.formatMoney
import dev.zhar.abc.util.formatPercent
import dev.zhar.abc.util.formatQuantity
import dev.zhar.abc.util.hiddenOr

@Composable
fun DashboardScreen(
    portfolioName: String,
    portfolios: List<PortfolioView>,
    selectedPortfolioId: Long?,
    snapshot: PortfolioSnapshot,
    history: List<PortfolioHistoryPoint>,
    settings: AppSettings,
    feedStatus: FeedStatus,
    onSelectPortfolio: (Long?) -> Unit,
    onAddEntry: () -> Unit,
    onDeleteManualPosition: (Long, String, (Result<Unit>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteHolding by remember { mutableStateOf<Holding?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = SolfolioLayout.screenHorizontal,
            end = SolfolioLayout.screenHorizontal,
            top = SolfolioLayout.screenTop,
            bottom = SolfolioLayout.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    SolfolioMark(size = 46.dp)
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = "Solfolio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onAddEntry) {
                    Icon(Icons.Rounded.Add, contentDescription = "Novo lançamento", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 12.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedPortfolioId == null,
                        onClick = { onSelectPortfolio(null) },
                        label = { Text("Todos") },
                    )
                }
                items(portfolios, key = { it.id }) { portfolio ->
                    FilterChip(
                        selected = selectedPortfolioId == portfolio.id,
                        onClick = { onSelectPortfolio(portfolio.id) },
                        label = { Text(portfolio.name) },
                    )
                }
            }
        }

        item {
            HeroBalanceCard(
                portfolioName = portfolioName,
                snapshot = snapshot,
                settings = settings,
                feedStatus = feedStatus,
                history = history,
            )
        }

        if (snapshot.holdings.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricCard(
                        label = "Custo restante",
                        value = hiddenOr(formatMoney(snapshot.remainingCostUsd, settings), settings.hideBalances),
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        label = "Variação 24 h",
                        value = hiddenOr(formatPercent(snapshot.change24hPercent), settings.hideBalances),
                        valueColor = when {
                            snapshot.change24hPercent == null -> MaterialTheme.colorScheme.onSurface
                            snapshot.change24hPercent >= 0 -> AbcGreen
                            else -> AbcRed
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Seus ativos", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${snapshot.holdings.size} posições",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(snapshot.holdings, key = { "${it.sourceKey}:${it.symbol}" }) { holding ->
                AssetHoldingRow(
                    holding = holding,
                    settings = settings,
                    hidden = settings.hideBalances,
                    onDelete = holding.sourceKey.removePrefix("manual:").toLongOrNull()?.let { portfolioId ->
                        { deleteHolding = holding; deleteError = null }
                    },
                )
            }
        } else {
            item {
                EmptyState(
                    title = "Sua carteira começa aqui",
                    message = "Adicione uma compra ou um endereço público.",
                ) {
                    Button(onClick = onAddEntry) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.size(7.dp))
                        Text("Adicionar primeira compra")
                    }
                }
            }
        }
    }
    deleteHolding?.let { holding ->
        val portfolioId = holding.sourceKey.removePrefix("manual:").toLongOrNull()
        AlertDialog(
            onDismissRequest = { deleteHolding = null },
            title = { Text("Remover ${holding.symbol}?") },
            text = { Column { Text("Todos os lançamentos manuais desta posição serão excluídos deste portfólio."); deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
            confirmButton = { TextButton(onClick = { if (portfolioId != null) onDeleteManualPosition(portfolioId, holding.symbol) { result -> result.onSuccess { deleteHolding = null }.onFailure { deleteError = it.message ?: "Não foi possível remover." } } }) { Text("Remover", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteHolding = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = value,
                fontWeight = FontWeight.SemiBold,
                fontSize = when { value.length <= 15 -> 18.sp; value.length <= 20 -> 15.sp; else -> 12.sp },
                color = valueColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AssetHoldingRow(
    holding: Holding,
    settings: AppSettings,
    hidden: Boolean,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssetAvatar(symbol = holding.symbol)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = holding.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = holding.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = hiddenOr("${formatQuantity(holding.quantity)} ${holding.symbol}", hidden),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = holding.sourceLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                AnimatedContent(
                    targetState = hiddenOr(formatMoney(holding.currentValueUsd, settings), hidden),
                    transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
                    label = "asset-value",
                ) { value ->
                    Text(value, fontSize = when { value.length <= 15 -> 18.sp; value.length <= 20 -> 15.sp; else -> 12.sp }, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                Spacer(Modifier.height(4.dp))
                val changeColor = when {
                    holding.change24hPercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    holding.change24hPercent >= 0 -> AbcGreen
                    else -> AbcRed
                }
                Text(
                    text = hiddenOr(formatPercent(holding.change24hPercent), hidden),
                    color = changeColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Remover ${holding.symbol}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
