package dev.zhar.larpwallet.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zhar.larpwallet.model.TokenAsset
import dev.zhar.larpwallet.model.TransactionKind
import dev.zhar.larpwallet.model.WalletState
import dev.zhar.larpwallet.ui.theme.Aqua
import dev.zhar.larpwallet.ui.theme.CardSurface
import dev.zhar.larpwallet.ui.theme.Hairline
import dev.zhar.larpwallet.ui.theme.MutedText
import dev.zhar.larpwallet.ui.theme.Negative
import dev.zhar.larpwallet.ui.theme.Positive
import dev.zhar.larpwallet.ui.theme.Purple
import dev.zhar.larpwallet.ui.theme.PurpleBright
import dev.zhar.larpwallet.ui.theme.SoftWhite
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun SimulationStrip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF17121F))
            .border(width = 1.dp, color = Color(0xFF382A4F))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(PurpleBright, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "SIMULAÇÃO  •  SEM VALOR REAL",
            color = PurpleBright,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
fun TokenMark(
    asset: TokenAsset,
    modifier: Modifier = Modifier,
    size: Int = 46,
) {
    val color = colorFromHex(asset.colorHex)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.54f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = asset.symbol.take(2),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (asset.symbol.length > 1) (size * 0.31).sp else (size * 0.4).sp,
        )
    }
}

@Composable
fun AssetRow(
    asset: TokenAsset,
    state: WalletState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokenMark(asset = asset)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = asset.name,
                color = SoftWhite,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatQuantity(asset.quantity)} ${asset.symbol}",
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.width(8.dp))
                ChangePill(change = asset.change24h, compact = true)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (state.hideBalances) "••••••" else formatMoney(asset.valueBrl, state),
                color = SoftWhite,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (state.hideBalances) "••••" else formatMoney(asset.priceBrl, state),
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun ChangePill(change: Double, compact: Boolean = false) {
    val positive = change >= 0
    val tint by animateColorAsState(
        targetValue = if (positive) Positive else Negative,
        label = "changeTint",
    )
    Surface(
        color = tint.copy(alpha = 0.11f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 6.dp else 9.dp, vertical = if (compact) 3.dp else 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (positive) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(if (compact) 11.dp else 14.dp),
            )
            Text(
                text = "${formatPercent(abs(change))}",
                color = tint,
                fontSize = if (compact) 11.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color(0xFF24202A), RoundedCornerShape(17.dp))
                .border(1.dp, Hairline, RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = PurpleBright, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(text = label, color = SoftWhite, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BalanceChart(
    positive: Boolean,
    modifier: Modifier = Modifier,
    selectedFraction: Float? = null,
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 55f),
        label = "chartReveal",
    )
    val points = if (positive) {
        listOf(0.62f, 0.58f, 0.64f, 0.55f, 0.51f, 0.56f, 0.42f, 0.47f, 0.39f, 0.31f, 0.36f, 0.24f, 0.29f, 0.18f)
    } else {
        listOf(0.22f, 0.28f, 0.24f, 0.35f, 0.31f, 0.44f, 0.39f, 0.52f, 0.47f, 0.58f, 0.54f, 0.66f, 0.61f, 0.72f)
    }
    val lineColor = if (positive) Aqua else Negative

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val usableWidth = size.width * progress
        val step = size.width / (points.lastIndex.coerceAtLeast(1))
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = index * step
            if (x > usableWidth) return@forEachIndexed
            val y = value * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(Purple, lineColor)),
            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
        )
        selectedFraction?.coerceIn(0f, 1f)?.let { fraction ->
            val index = (fraction * points.lastIndex).toInt().coerceIn(0, points.lastIndex)
            val x = index * step
            val y = points[index] * size.height
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color.White, radius = 2.4.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(24.dp))
            .border(1.dp, Hairline, RoundedCornerShape(24.dp))
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color(0xFF211C29), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = PurpleBright, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = SoftWhite, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(7.dp))
        Text(body, color = MutedText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun TransactionIcon(kind: TransactionKind, modifier: Modifier = Modifier) {
    val icon = when (kind) {
        TransactionKind.RECEIVED -> Icons.Outlined.ArrowDownward
        TransactionKind.SENT -> Icons.Outlined.ArrowUpward
        TransactionKind.SWAPPED -> Icons.Outlined.CurrencyExchange
        TransactionKind.ADJUSTED -> Icons.Outlined.Edit
    }
    val tint = when (kind) {
        TransactionKind.RECEIVED -> Positive
        TransactionKind.SENT -> Negative
        TransactionKind.SWAPPED -> PurpleBright
        TransactionKind.ADJUSTED -> Aqua
    }
    Box(
        modifier = modifier
            .size(44.dp)
            .background(tint.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
    }
}

fun colorFromHex(value: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(value))
}.getOrDefault(Purple)

fun formatMoney(valueBrl: Double, state: WalletState): String {
    val locale = if (state.useUsd) Locale.US else Locale("pt", "BR")
    val amount = if (state.useUsd) valueBrl / state.usdBrlRate.coerceAtLeast(0.01) else valueBrl
    return NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }.format(amount)
}

fun formatQuantity(value: Double): String {
    val digits = when {
        abs(value) >= 1_000 -> 2
        abs(value) >= 1 -> 4
        else -> 6
    }
    return NumberFormat.getNumberInstance(Locale("pt", "BR")).apply {
        maximumFractionDigits = digits
        minimumFractionDigits = 0
    }.format(value)
}

fun formatPercent(value: Double): String = NumberFormat.getNumberInstance(Locale("pt", "BR")).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 2
}.format(value) + "%"
