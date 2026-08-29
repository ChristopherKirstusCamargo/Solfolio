package dev.zhar.abc.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zhar.abc.data.market.HistoricalCandle
import dev.zhar.abc.domain.*
import dev.zhar.abc.ui.components.AssetAvatar
import dev.zhar.abc.ui.components.FeedBadge
import dev.zhar.abc.ui.theme.AbcGreen
import dev.zhar.abc.ui.theme.AbcRed
import dev.zhar.abc.ui.theme.SolfolioLayout
import dev.zhar.abc.util.usdToDisplay
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val MARKET_REFRESH_SECONDS = 60

@Composable
fun MarketScreen(
    quotes: Map<String, AssetQuote>,
    settings: AppSettings,
    feedStatus: FeedStatus,
    onLoadHistory: (String, Int, (Result<List<HistoricalCandle>>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedAsset by remember { mutableStateOf<AssetDefinition?>(null) }
    if (selectedAsset != null) {
        MarketHistoryScreen(selectedAsset!!, quotes[selectedAsset!!.symbol], settings, { selectedAsset = null }, onLoadHistory, modifier)
        return
    }

    var displayedQuotes by remember { mutableStateOf<Map<String, AssetQuote>>(emptyMap()) }
    var secondsLeft by remember { mutableIntStateOf(MARKET_REFRESH_SECONDS) }
    val latestQuotes by rememberUpdatedState(quotes)

    LaunchedEffect(quotes) {
        if (displayedQuotes.isEmpty() && quotes.isNotEmpty()) {
            displayedQuotes = quotes
            secondsLeft = MARKET_REFRESH_SECONDS
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1_000L)
            if (secondsLeft > 1) secondsLeft -= 1 else {
                displayedQuotes = latestQuotes
                secondsLeft = MARKET_REFRESH_SECONDS
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(SolfolioLayout.screenHorizontal, 16.dp, SolfolioLayout.screenHorizontal, SolfolioLayout.screenBottom),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Mercado", style = MaterialTheme.typography.headlineMedium)
                    Text("Preços atuais e histórico.", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                FeedBadge(feedStatus)
            }
        }
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Atualiza em ${formatCountdown(secondsLeft)}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("Toque para ver o gráfico", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
        items(AssetCatalog.defaults, key = { it.symbol }) { asset ->
            val quote = displayedQuotes[asset.symbol]
            Card(onClick = { selectedAsset = asset }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssetAvatar(asset.symbol, size = 38.dp)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(asset.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(asset.symbol, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(quote?.let { formatMarketPrice(it.priceUsd, settings) } ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MarketHistoryScreen(
    asset: AssetDefinition,
    quote: AssetQuote?,
    settings: AppSettings,
    onBack: () -> Unit,
    onLoadHistory: (String, Int, (Result<List<HistoricalCandle>>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var days by rememberSaveable(asset.symbol) { mutableIntStateOf(30) }
    var candles by remember(asset.symbol) { mutableStateOf<List<HistoricalCandle>>(emptyList()) }
    var loading by remember(asset.symbol) { mutableStateOf(true) }
    var error by remember(asset.symbol) { mutableStateOf<String?>(null) }

    LaunchedEffect(asset.productId, days) {
        loading = true
        error = null
        onLoadHistory(asset.productId, days) { result ->
            result.onSuccess { candles = it }.onFailure { error = it.message ?: "Histórico indisponível." }
            loading = false
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(SolfolioLayout.screenHorizontal, 14.dp, SolfolioLayout.screenHorizontal, SolfolioLayout.screenBottom),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Voltar") }
                AssetAvatar(asset.symbol, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(asset.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(asset.symbol, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(quote?.let { formatMarketPrice(it.priceUsd, settings) } ?: "—", style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 90).forEach { option ->
                    FilterChip(selected = days == option, onClick = { days = option }, label = { Text("${option}d") }, modifier = Modifier.weight(1f))
                }
            }
        }
        when {
            loading -> item { Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            error != null -> item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(error!!, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
            else -> {
                item { HistoryChart(candles, Modifier.fillMaxWidth().height(220.dp)) }
                item { HistorySummary(candles, settings) }
                item { Text("Histórico da Coinbase. Não é previsão de preço.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun HistoryChart(candles: List<HistoricalCandle>, modifier: Modifier = Modifier) {
    val closes = candles.map { it.closeUsd }
    val color = if ((closes.lastOrNull() ?: 0.0) >= (closes.firstOrNull() ?: 0.0)) AbcGreen else AbcRed
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Canvas(modifier.padding(18.dp)) {
            if (closes.size < 2) return@Canvas
            val min = closes.min()
            val max = closes.max()
            val range = (max - min).takeIf { it > 0 } ?: 1.0
            val path = Path()
            closes.forEachIndexed { index, value ->
                val x = index * size.width / (closes.size - 1)
                val y = size.height - ((value - min) / range * size.height).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color.copy(alpha = .12f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, brush = Brush.horizontalGradient(listOf(color.copy(alpha = .45f), color)), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            val lastY = size.height - ((closes.last() - min) / range * size.height).toFloat()
            drawCircle(color, 5.dp.toPx(), Offset(size.width, lastY))
        }
    }
}

@Composable
private fun HistorySummary(candles: List<HistoricalCandle>, settings: AppSettings) {
    val first = candles.first()
    val last = candles.last()
    val change = ((last.closeUsd / first.openUsd) - 1.0) * 100.0
    val high = candles.maxOf { it.highUsd }
    val low = candles.minOf { it.lowUsd }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            HistoryValue("Variação", String.format(Locale("pt", "BR"), "%+.2f%%", change), if (change >= 0) AbcGreen else AbcRed)
            HistoryValue("Máxima", formatMarketPrice(high, settings), MaterialTheme.colorScheme.onSurface)
            HistoryValue("Mínima", formatMarketPrice(low, settings), MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable private fun HistoryValue(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, color = color, fontWeight = FontWeight.SemiBold) }
}

internal fun formatCountdown(seconds: Int): String = String.format(Locale.ROOT, "%d:%02d", seconds.coerceAtLeast(0) / 60, seconds.coerceAtLeast(0) % 60)

private fun formatMarketPrice(priceUsd: Double, settings: AppSettings): String {
    val value = usdToDisplay(priceUsd, settings)
    val locale = if (settings.displayCurrency == DisplayCurrency.BRL) Locale("pt", "BR") else Locale.US
    val decimals = when { value >= 1.0 -> 2; value >= 0.01 -> 4; else -> 8 }
    return NumberFormat.getCurrencyInstance(locale).apply { minimumFractionDigits = decimals; maximumFractionDigits = decimals }.format(value)
}
