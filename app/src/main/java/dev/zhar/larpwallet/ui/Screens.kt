package dev.zhar.larpwallet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zhar.larpwallet.model.Collectible
import dev.zhar.larpwallet.model.SimTransaction
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletScreen(
    state: WalletState,
    onAssetClick: (TokenAsset) -> Unit,
    onEditTotal: () -> Unit,
    onAddAsset: () -> Unit,
    onReceive: () -> Unit,
    onSend: () -> Unit,
    onSwap: () -> Unit,
    onToggleBalance: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 28.dp,
        ),
    ) {
        item {
            AccountHeader(
                accountName = state.accountName,
                hideBalances = state.hideBalances,
                onToggleBalance = onToggleBalance,
            )
            Spacer(Modifier.height(24.dp))
        }

        item {
            BalancePanel(state = state, onEditTotal = onEditTotal)
            Spacer(Modifier.height(22.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionButton(
                    icon = Icons.Outlined.ArrowDownward,
                    label = "Receber",
                    onClick = onReceive,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    icon = Icons.Outlined.ArrowUpward,
                    label = "Enviar",
                    onClick = onSend,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    icon = Icons.Outlined.CurrencyExchange,
                    label = "Trocar",
                    onClick = onSwap,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    icon = Icons.Outlined.Add,
                    label = "Adicionar",
                    onClick = onAddAsset,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(28.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Seus ativos",
                    color = SoftWhite,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onAddAsset) {
                    Text("Gerenciar", color = PurpleBright)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (state.assets.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Add,
                    title = "Nenhum ativo no cenário",
                    body = "Adicione um token e defina quantidade, preço e variação.",
                )
            }
        } else {
            items(state.assets, key = { it.id }) { asset ->
                AssetRow(asset = asset, state = state, onClick = { onAssetClick(asset) })
            }
        }
    }
}

@Composable
private fun AccountHeader(
    accountName: String,
    hideBalances: Boolean,
    onToggleBalance: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(
                    Brush.linearGradient(listOf(Purple, Aqua)),
                    CircleShape,
                )
                .border(2.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("L", color = Color(0xFF100D14), fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = accountName,
                color = SoftWhite,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "DEMO…LARP  •  local",
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        IconButton(onClick = onToggleBalance) {
            Icon(
                imageVector = if (hideBalances) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = if (hideBalances) "Mostrar saldo" else "Ocultar saldo",
                tint = SoftWhite,
            )
        }
    }
}

@Composable
private fun BalancePanel(state: WalletState, onEditTotal: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF20192D), Color(0xFF14121A), Color(0xFF111820)),
                ),
            )
            .border(1.dp, Color(0xFF382F44), RoundedCornerShape(28.dp))
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Saldo total",
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEditTotal, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Editar saldo total",
                        tint = PurpleBright,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (state.hideBalances) "••••••••" else formatMoney(state.totalBrl, state),
                color = SoftWhite,
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChangePill(change = state.weightedChange24h)
                Spacer(Modifier.width(9.dp))
                Text("nas últimas 24h", color = MutedText, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(18.dp))
            BalanceChart(
                positive = state.weightedChange24h >= 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("00h", color = MutedText.copy(alpha = 0.65f), fontSize = 11.sp)
                Text("Agora", color = MutedText.copy(alpha = 0.65f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun AssetDetailScreen(
    asset: TokenAsset,
    state: WalletState,
    onBack: () -> Unit,
    onReceive: () -> Unit,
    onSend: () -> Unit,
    onSwap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var range by remember { mutableStateOf("1D") }
    var confirmDelete by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Voltar", tint = SoftWhite)
                }
                Spacer(Modifier.width(4.dp))
                TokenMark(asset = asset, size = 40)
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(asset.name, color = SoftWhite, style = MaterialTheme.typography.titleMedium)
                    Text(asset.symbol, color = MutedText, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Editar ativo", tint = SoftWhite)
                }
            }
            Spacer(Modifier.height(25.dp))
        }

        item {
            Text(
                text = if (state.hideBalances) "••••••" else formatMoney(asset.priceBrl, state),
                color = SoftWhite,
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChangePill(change = asset.change24h)
                Spacer(Modifier.width(8.dp))
                Text("preço fictício", color = MutedText, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(22.dp))
            BalanceChart(
                positive = asset.change24h >= 0,
                selectedFraction = 0.78f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("1H", "1D", "1S", "1M", "1A").forEach { item ->
                    FilterChip(
                        selected = range == item,
                        onClick = { range = item },
                        label = { Text(item) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = MutedText,
                            selectedContainerColor = Purple.copy(alpha = 0.17f),
                            selectedLabelColor = PurpleBright,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = range == item,
                            borderColor = Hairline,
                            selectedBorderColor = Purple.copy(alpha = 0.4f),
                        ),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionButton(Icons.Outlined.ArrowDownward, "Receber", onReceive, Modifier.weight(1f))
                ActionButton(Icons.Outlined.ArrowUpward, "Enviar", onSend, Modifier.weight(1f))
                ActionButton(Icons.Outlined.CurrencyExchange, "Trocar", onSwap, Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            SectionCard {
                Text("Sua posição", color = SoftWhite, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(18.dp))
                DetailLine(
                    label = "Quantidade",
                    value = if (state.hideBalances) "••••" else "${formatQuantity(asset.quantity)} ${asset.symbol}",
                )
                Spacer(Modifier.height(14.dp))
                DetailLine(
                    label = "Valor",
                    value = if (state.hideBalances) "••••" else formatMoney(asset.valueBrl, state),
                )
                Spacer(Modifier.height(14.dp))
                DetailLine(label = "Origem", value = "Definido manualmente")
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            Button(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Negative.copy(alpha = 0.12f),
                    contentColor = Negative,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Remover do cenário")
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = CardSurface,
            title = { Text("Remover ${asset.symbol}?") },
            text = { Text("Isso apaga apenas os dados locais desta simulação.", color = MutedText) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Remover", color = Negative) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = SoftWhite) }
            },
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MutedText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, color = SoftWhite, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CollectiblesScreen(
    state: WalletState,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
    ) {
        item {
            ScreenHeader(
                title = "Coleções",
                subtitle = "Itens cenográficos salvos somente neste aparelho",
                action = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Outlined.Add, contentDescription = "Adicionar colecionável", tint = PurpleBright)
                    }
                },
            )
            Spacer(Modifier.height(22.dp))
        }
        if (state.collectibles.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Coleção vazia",
                    body = "Crie um item fictício para compor o cenário.",
                )
            }
        } else {
            items(state.collectibles.chunked(2), key = { row -> row.joinToString { it.id } }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    row.forEach { item ->
                        CollectibleCard(item = item, onDelete = { onDelete(item.id) }, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CollectibleCard(
    item: Collectible,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorFromHex(item.accentHex)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .border(1.dp, Hairline, RoundedCornerShape(22.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.95f), Color(0xFF17121F), Aqua.copy(alpha = 0.48f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remover item", tint = Color.White.copy(alpha = 0.82f))
            }
        }
        Column(modifier = Modifier.padding(13.dp)) {
            Text(
                item.name,
                color = SoftWhite,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                item.collection,
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ActivityScreen(state: WalletState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
    ) {
        item {
            ScreenHeader(
                title = "Atividade",
                subtitle = "Histórico inteiramente fictício",
            )
            Spacer(Modifier.height(22.dp))
        }
        if (state.transactions.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Tune,
                    title = "Nenhuma atividade",
                    body = "Seus ajustes e transações simuladas aparecerão aqui.",
                )
            }
        } else {
            items(state.transactions, key = { it.id }) { transaction ->
                TransactionRow(transaction = transaction, state = state)
                HorizontalDivider(color = Hairline.copy(alpha = 0.7f), modifier = Modifier.padding(start = 57.dp))
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: SimTransaction, state: WalletState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransactionIcon(transaction.kind)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.title, color = SoftWhite, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(
                "${transaction.subtitle} • ${formatDate(transaction.timestamp)}",
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            val prefix = when {
                transaction.amount > 0 && transaction.kind == TransactionKind.RECEIVED -> "+"
                else -> ""
            }
            Text(
                text = if (state.hideBalances) "••••" else "$prefix${formatQuantity(transaction.amount)} ${transaction.symbol}",
                color = when (transaction.kind) {
                    TransactionKind.RECEIVED -> Positive
                    TransactionKind.SENT -> Negative
                    else -> SoftWhite
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (state.hideBalances) "••••" else formatMoney(kotlin.math.abs(transaction.valueBrl), state),
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun SettingsScreen(
    state: WalletState,
    onToggleCurrency: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onEditProfile: () -> Unit,
    onExchangeRate: () -> Unit,
    onAbout: () -> Unit,
    onReset: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
    ) {
        item {
            ScreenHeader(title = "Ajustes", subtitle = "Controle completo do cenário local")
            Spacer(Modifier.height(22.dp))
        }

        item {
            SettingsSectionTitle("Conta")
            SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                SettingsRow(
                    icon = Icons.Outlined.Edit,
                    title = "Nome da conta",
                    subtitle = state.accountName,
                    onClick = onEditProfile,
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Outlined.Lock,
                    title = "Dados locais",
                    subtitle = "Sem login, seed ou conexão de carteira",
                    onClick = onAbout,
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            SettingsSectionTitle("Exibição")
            SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                SettingsToggleRow(
                    icon = Icons.Outlined.Language,
                    title = "Mostrar em dólar",
                    subtitle = if (state.useUsd) "USD ativo" else "BRL ativo",
                    checked = state.useUsd,
                    onCheckedChange = onToggleCurrency,
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Outlined.CurrencyExchange,
                    title = "Cotação USD/BRL",
                    subtitle = "R$ ${String.format(Locale("pt", "BR"), "%.2f", state.usdBrlRate)} — definida por você",
                    onClick = onExchangeRate,
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Outlined.Vibration,
                    title = "Resposta tátil",
                    subtitle = "Vibração leve nas ações",
                    checked = state.haptics,
                    onCheckedChange = onToggleHaptics,
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            SettingsSectionTitle("Segurança da simulação")
            SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                SettingsRow(
                    icon = Icons.Outlined.Fingerprint,
                    title = "Nenhuma chave privada",
                    subtitle = "O app não cria, importa ou assina carteiras reais",
                    onClick = onAbout,
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "Sobre o LARP Wallet",
                    subtitle = "Limites, privacidade e finalidade",
                    onClick = onAbout,
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Negative.copy(alpha = 0.12f),
                    contentColor = Negative,
                ),
            ) {
                Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text("Restaurar cenário de exemplo")
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "LARP Wallet 1.0.0  •  By Christopher",
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun ScreenHeader(
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SoftWhite, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = MutedText, style = MaterialTheme.typography.bodyMedium)
        }
        action?.invoke()
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(Locale.ROOT),
        color = MutedText,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 5.dp, bottom = 10.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(icon)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SoftWhite, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MutedText, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MutedText)
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(icon)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SoftWhite, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MutedText, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF17101F),
                checkedTrackColor = PurpleBright,
                uncheckedThumbColor = MutedText,
                uncheckedTrackColor = Color(0xFF2A2630),
            ),
        )
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Purple.copy(alpha = 0.12f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = PurpleBright, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = Hairline, modifier = Modifier.padding(horizontal = 14.dp))
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val delta = now - timestamp
    return when {
        delta in 0 until 60_000 -> "agora"
        delta in 60_000 until 3_600_000 -> "há ${delta / 60_000} min"
        delta in 3_600_000 until 86_400_000 -> "há ${delta / 3_600_000} h"
        else -> SimpleDateFormat("dd MMM", Locale("pt", "BR")).format(Date(timestamp))
    }
}
