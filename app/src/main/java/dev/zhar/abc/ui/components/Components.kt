package dev.zhar.abc.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Token
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import dev.zhar.abc.domain.AppSettings
import dev.zhar.abc.domain.FeedStatus
import dev.zhar.abc.domain.PortfolioSnapshot
import dev.zhar.abc.domain.PortfolioHistoryPoint
import dev.zhar.abc.ui.theme.AbcGreen
import dev.zhar.abc.ui.theme.AbcMint
import dev.zhar.abc.ui.theme.AbcPurple
import dev.zhar.abc.ui.theme.AbcPurpleDeep
import dev.zhar.abc.ui.theme.AbcRed
import dev.zhar.abc.util.formatMoney
import dev.zhar.abc.util.formatPercent
import dev.zhar.abc.util.hiddenOr

@Composable
fun AssetAvatar(
    symbol: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val color = assetColor(symbol)
    Box(
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = 0.17f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (symbol.uppercase()) {
            "BTC" -> Text("₿", color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * .52f).sp)
            "ETH" -> Canvas(Modifier.size(size * .56f)) {
                val w = this.size.width; val h = this.size.height
                val top = Offset(w / 2f, 0f); val middle = Offset(w / 2f, h * .64f); val bottom = Offset(w / 2f, h)
                val upper = Path().apply { moveTo(top.x, top.y); lineTo(w, h * .56f); lineTo(middle.x, middle.y); lineTo(0f, h * .56f); close() }
                val lower = Path().apply { moveTo(0f, h * .66f); lineTo(bottom.x, bottom.y); lineTo(w, h * .66f); lineTo(middle.x, h * .78f); close() }
                drawPath(upper, color); drawPath(lower, color.copy(alpha = .72f))
            }
            "SOL" -> SolfolioMark(size = size * .62f)
            "USDC" -> Text("$", color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * .48f).sp)
            "USDT" -> Text("₮", color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * .48f).sp)
            "DOGE" -> Text("Ð", color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * .48f).sp)
            "LTC" -> Text("Ł", color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * .48f).sp)
            "BCH" -> Text("₿", color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * .48f).sp)
            "XRP" -> Text("✕", color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * .42f).sp)
            "ADA" -> Text("₳", color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * .45f).sp)
            "DOT" -> Text("●", color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * .45f).sp)
            "XLM" -> Text("✦", color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * .48f).sp)
            "ATOM" -> Text("⚛", color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * .43f).sp)
            else -> Icon(Icons.Rounded.Token, contentDescription = null, tint = color, modifier = Modifier.size(size * .52f))
        }
    }
}

@Composable
fun SolfolioMark(modifier: Modifier = Modifier, size: Dp = 42.dp) {
    val mint = MaterialTheme.colorScheme.secondary; val violet = MaterialTheme.colorScheme.primary; val blue = MaterialTheme.colorScheme.tertiary
    Canvas(modifier.size(size)) {
        val canvasSize = this.size
        fun bar(top: Float, color: Color, reverse: Boolean) {
            val path = Path().apply {
                if (reverse) {
                    moveTo(canvasSize.width * .22f, top); lineTo(canvasSize.width * .76f, top)
                    lineTo(canvasSize.width * .88f, top + canvasSize.height * .18f); lineTo(canvasSize.width * .34f, top + canvasSize.height * .18f)
                } else {
                    moveTo(canvasSize.width * .32f, top); lineTo(canvasSize.width * .88f, top)
                    lineTo(canvasSize.width * .76f, top + canvasSize.height * .18f); lineTo(canvasSize.width * .20f, top + canvasSize.height * .18f)
                }; close()
            }; drawPath(path, color)
        }
        bar(canvasSize.height * .14f, mint, false); bar(canvasSize.height * .41f, blue, true); bar(canvasSize.height * .68f, violet, false)
    }
}

fun assetColor(symbol: String): Color = when (symbol.uppercase()) {
    "BTC" -> Color(0xFFF7931A)
    "ETH" -> Color(0xFF8A92FF)
    "SOL" -> Color(0xFF61F2C4)
    "USDC" -> Color(0xFF4D8DFF)
    "USDT" -> Color(0xFF26A17B)
    "XRP" -> Color(0xFF65D5E8)
    "ADA" -> Color(0xFF6B8DFF)
    "DOGE" -> Color(0xFFE2B84B)
    "AVAX" -> Color(0xFFFF6475)
    "LINK" -> Color(0xFF5C7EFF)
    "LTC" -> Color(0xFFA8A9AD)
    "SHIB" -> Color(0xFFFF7A45)
    "POL" -> Color(0xFF9A6CFF)
    "DOT" -> Color(0xFFE6007A)
    "BCH" -> Color(0xFF8DC351)
    "UNI" -> Color(0xFFFF4FA3)
    "XLM" -> Color(0xFF79D7FF)
    "ATOM" -> Color(0xFF8C8CFF)
    "NEAR" -> Color(0xFF63E6BE)
    "AAVE" -> Color(0xFFB46CFF)
    else -> AbcPurple
}

@Composable
fun FeedBadge(status: FeedStatus, modifier: Modifier = Modifier) {
    val live = status == FeedStatus.LIVE
    val color by animateColorAsState(
        targetValue = if (live) AbcGreen else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "feed-color",
    )
    val text = when (status) {
        FeedStatus.LIVE -> "Ao vivo"
        FeedStatus.CONNECTING -> "Conectando"
        FeedStatus.RECONNECTING -> "Reconectando"
        FeedStatus.OFFLINE -> "Offline"
    }
    val icon = when (status) {
        FeedStatus.LIVE -> Icons.Rounded.Bolt
        FeedStatus.OFFLINE -> Icons.Rounded.CloudOff
        else -> Icons.Rounded.Sync
    }
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text = text, color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun HeroBalanceCard(
    portfolioName: String,
    snapshot: PortfolioSnapshot,
    settings: AppSettings,
    feedStatus: FeedStatus,
    history: List<PortfolioHistoryPoint> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val positive = snapshot.unrealizedPnlUsd >= 0
    val pnlColor = if (positive) MaterialTheme.colorScheme.secondary else Color(0xFFFFB2C0)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val balanceText = hiddenOr(formatMoney(snapshot.totalValueUsd, settings), settings.hideBalances)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF17152E), Color(0xFF202044), Color(0xFF101B2E), primary.copy(alpha = .18f)),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = portfolioName,
                            color = Color.White.copy(alpha = 0.74f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Patrimônio",
                            color = Color.White.copy(alpha = 0.58f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    FeedBadge(status = feedStatus)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = balanceText,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = when { balanceText.length <= 15 -> 40.sp; balanceText.length <= 19 -> 33.sp; balanceText.length <= 23 -> 27.sp; else -> 22.sp },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(if (snapshot.pnlIsEstimated) "P/L estimado" else "P/L não realizado", color = Color.White.copy(alpha = .64f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = hiddenOr(formatMoney(snapshot.unrealizedPnlUsd, settings), settings.hideBalances),
                        color = pnlColor,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = hiddenOr(formatPercent(snapshot.unrealizedPnlPercent), settings.hideBalances),
                        color = pnlColor,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .background(pnlColor.copy(alpha = 0.13f), CircleShape)
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** Builds a short portfolio-wide curve instead of presenting one asset as the whole portfolio. */
private fun portfolioTrend(snapshot: PortfolioSnapshot): List<Double> {
    val sampleCount = snapshot.holdings.maxOfOrNull { it.recentPrices.size } ?: 0
    if (sampleCount < 2) return emptyList()
    return (0 until sampleCount).map { index ->
        snapshot.holdings.sumOf { holding ->
            val prices = holding.recentPrices
            val alignedIndex = index - (sampleCount - prices.size)
            val price = prices.getOrNull(alignedIndex) ?: holding.currentPriceUsd
            holding.quantity * price
        }
    }
}

@Composable
fun Sparkline(
    points: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    animateOrb: Boolean = true,
) {
    val floatY = if (animateOrb) {
        val transition = rememberInfiniteTransition(label = "orb-float")
        val value by transition.animateFloat(
            initialValue = -1.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
            label = "orb-y",
        )
        value
    } else 0f
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val min = points.minOrNull() ?: return@Canvas
        val max = points.maxOrNull() ?: return@Canvas
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val stepX = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - min) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color.copy(alpha = 0.10f), style = Stroke(11.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, brush = Brush.horizontalGradient(listOf(color.copy(alpha = .08f), color.copy(alpha = .45f), color)), style = Stroke(2.3.dp.toPx(), cap = StrokeCap.Round))
        val lastValue = points.last()
        val orb = Offset(size.width, size.height - ((lastValue - min) / range * size.height).toFloat() + floatY.dp.toPx())
        drawCircle(color.copy(alpha = .10f), radius = 12.dp.toPx(), center = orb)
        drawCircle(color.copy(alpha = .25f), radius = 8.dp.toPx(), center = orb)
        drawCircle(color, radius = 4.8.dp.toPx(), center = orb)
        drawCircle(Color.White.copy(alpha = .88f), radius = 1.7.dp.toPx(), center = orb - Offset(1.2.dp.toPx(), 1.2.dp.toPx()))
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(AbcPurple.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(AbcPurple, CircleShape),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            action()
        }
    }
}
