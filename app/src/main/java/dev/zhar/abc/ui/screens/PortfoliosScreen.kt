package dev.zhar.abc.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.zhar.abc.data.TrackedWalletDraft
import dev.zhar.abc.domain.*
import dev.zhar.abc.ui.PortfolioView
import dev.zhar.abc.ui.TrackedWalletView
import dev.zhar.abc.ui.WalletAssetView
import dev.zhar.abc.ui.components.AssetAvatar
import dev.zhar.abc.ui.components.EmptyState
import dev.zhar.abc.ui.theme.SolfolioLayout
import dev.zhar.abc.util.formatMoney
import dev.zhar.abc.util.hiddenOr
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PortfoliosScreen(
    portfolios: List<PortfolioView>,
    trackedWallets: List<TrackedWalletView>,
    settings: AppSettings,
    onSelect: (Long?) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onCreateTracked: (TrackedWalletDraft, (Result<Long>) -> Unit) -> Unit,
    onDeleteTracked: (Long) -> Unit,
    onRefreshTracked: (Long) -> Unit,
    onRefreshAll: () -> Unit,
    onUpdateCost: (Long, Double?, (Result<Unit>) -> Unit) -> Unit,
    onUpdateAssetCost: (Long, String, Double?, (Result<Unit>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreate by remember { mutableStateOf(false) }
    var showTrack by remember { mutableStateOf(false) }
    var deletePortfolio by remember { mutableStateOf<PortfolioView?>(null) }
    var deleteWallet by remember { mutableStateOf<TrackedWalletView?>(null) }
    var editCost by remember { mutableStateOf<TrackedWalletView?>(null) }
    var editAssetCost by remember { mutableStateOf<Pair<TrackedWalletView, WalletAssetView>?>(null) }
    val combinedValue = portfolios.sumOf { it.snapshot.totalValueUsd }

    LazyColumn(
        modifier,
        contentPadding = PaddingValues(
            start = SolfolioLayout.screenHorizontal,
            end = SolfolioLayout.screenHorizontal,
            top = SolfolioLayout.screenTop,
            bottom = SolfolioLayout.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(SolfolioLayout.sectionSpacing),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Carteiras", style = MaterialTheme.typography.headlineMedium); Text("Organize seus investimentos.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton({ showCreate = true }) { Icon(Icons.Rounded.CreateNewFolder, "Novo portfólio") }
            }
        }
        item {
            Card(onClick = { onSelect(null) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.size(11.dp))
                    Column(Modifier.weight(1f)) { Text("Visão geral", style = MaterialTheme.typography.titleMedium); Text("${portfolios.size} portfólios · ${trackedWallets.size} endereços", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(hiddenOr(formatMoney(combinedValue, settings), settings.hideBalances), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { SectionRow("Portfólios") { TextButton({ showCreate = true }) { Icon(Icons.Rounded.Add, null); Text("Novo") } } }
        if (portfolios.isEmpty()) item { EmptyState("Nenhum portfólio", "Crie um espaço para suas posições.") { Button({ showCreate = true }) { Text("Criar") } } }
        else items(portfolios, key = { "p${it.id}" }) { portfolio ->
            Card(onClick = { onSelect(portfolio.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(start = 15.dp, top = 12.dp, bottom = 12.dp, end = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(portfolio.name, style = MaterialTheme.typography.titleMedium); Text("${portfolio.snapshot.holdings.size} posições · ${portfolio.trackedWalletCount} endereços", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Column(horizontalAlignment = Alignment.End) { Text(hiddenOr(formatMoney(portfolio.snapshot.totalValueUsd, settings), settings.hideBalances)); Text("P/L ${hiddenOr(formatMoney(portfolio.snapshot.unrealizedPnlUsd, settings), settings.hideBalances)}", style = MaterialTheme.typography.bodySmall, color = if (portfolio.snapshot.unrealizedPnlUsd >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error) }
                    IconButton({ deletePortfolio = portfolio }) { Icon(Icons.Rounded.DeleteOutline, "Excluir ${portfolio.name}") }
                }
            }
        }
        item {
            SectionRow("Endereços") {
                if (trackedWallets.isNotEmpty()) IconButton(onRefreshAll) { Icon(Icons.Rounded.Refresh, "Atualizar todos") }
            }
        }
        item {
            FilledTonalButton({ showTrack = true }, enabled = portfolios.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.AddLink, null); Spacer(Modifier.size(7.dp)); Text("Adicionar endereço público")
            }
        }
        item { Text("Acompanhe saldos de Solana, Bitcoin e Ethereum.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (trackedWallets.isEmpty()) item { EmptyState("Nenhum endereço", "Use o endereço público da carteira que deseja acompanhar.") { Button({ showTrack = true }, enabled = portfolios.isNotEmpty()) { Text("Adicionar endereço") } } }
        else items(trackedWallets, key = { "w${it.id}" }) { wallet ->
            WalletCard(wallet, settings, { onRefreshTracked(wallet.id) }, { editCost = wallet }, { asset -> editAssetCost = wallet to asset }, { deleteWallet = wallet })
        }
    }

    if (showCreate) CreatePortfolioDialog({ showCreate = false }) { onCreate(it); showCreate = false }
    if (showTrack) TrackWalletDialog(portfolios, settings, { showTrack = false }, onCreateTracked)
    editCost?.let { wallet -> CostBasisDialog(wallet, settings, { editCost = null }) { usd -> onUpdateCost(wallet.id, usd) { result -> result.onSuccess { editCost = null } } } }
    editAssetCost?.let { (wallet, asset) ->
        AssetCostBasisDialog(asset, settings, { editAssetCost = null }) { usd ->
            onUpdateAssetCost(wallet.id, asset.mint, usd) { result -> result.onSuccess { editAssetCost = null } }
        }
    }
    deletePortfolio?.let { p -> AlertDialog(onDismissRequest = { deletePortfolio = null }, title = { Text("Excluir ${p.name}?") }, text = { Text("Lançamentos e endereços vinculados serão removidos do aparelho.") }, confirmButton = { TextButton({ onDelete(p.id); deletePortfolio = null }) { Text("Excluir", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ deletePortfolio = null }) { Text("Cancelar") } }) }
    deleteWallet?.let { w -> AlertDialog(onDismissRequest = { deleteWallet = null }, title = { Text("Parar de rastrear?") }, text = { Text("${w.label} será removido. Nenhum saldo é alterado na blockchain.") }, confirmButton = { TextButton({ onDeleteTracked(w.id); deleteWallet = null }) { Text("Remover", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ deleteWallet = null }) { Text("Cancelar") } }) }
}

@Composable private fun SectionRow(title: String, action: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); action() }
}

@Composable private fun WalletCard(wallet: TrackedWalletView, settings: AppSettings, onRefresh: () -> Unit, onEditCost: () -> Unit, onEditAssetCost: (WalletAssetView) -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.animateContentSize()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssetAvatar(networkSymbol(wallet.network), size = 38.dp); Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) { Text(wallet.label, style = MaterialTheme.typography.titleMedium); Text("${networkLabel(wallet.network)} · ${wallet.address.take(5)}…${wallet.address.takeLast(5)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (wallet.syncing) CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp) else IconButton(onRefresh) { Icon(Icons.Rounded.Refresh, "Atualizar") }
                IconButton(onDelete) { Icon(Icons.Rounded.DeleteOutline, "Remover") }
            }
            wallet.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            wallet.assets.forEach { asset ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AssetAvatar(asset.symbol, size = 28.dp); Spacer(Modifier.size(8.dp)); Column(Modifier.weight(1f)) { Text(asset.name, style = MaterialTheme.typography.bodyMedium); Text("${asset.quantity.toReadable()} ${asset.symbol}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(hiddenOr(formatMoney(asset.valueUsd, settings), settings.hideBalances), fontWeight = FontWeight.Medium)
                        asset.costBasisUsd?.let { Text("Custo ${hiddenOr(formatMoney(it, settings), settings.hideBalances)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    IconButton(onClick = { onEditAssetCost(asset) }, modifier = Modifier.size(38.dp)) { Icon(Icons.Rounded.Edit, "Ajustar custo de ${asset.symbol}", modifier = Modifier.size(18.dp)) }
                }
            }
            if (wallet.hiddenAssetCount > 0) Text("Tokens sem preço disponível ficam ocultos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (wallet.assets.isEmpty() && !wallet.syncing) Text("Nenhum saldo com cotação reconhecida.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            Row(Modifier.fillMaxWidth()) { Text("Valor", Modifier.weight(1f)); Text(hiddenOr(formatMoney(wallet.currentValueUsd, settings), settings.hideBalances), fontWeight = FontWeight.SemiBold) }
            wallet.pnlUsd?.let { pnl -> Row(Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text(if (wallet.basisIsExact) "P/L pelo custo informado" else "P/L de referência"); Text(if (wallet.basisIsExact) "Custo informado" else "Valor da primeira consulta", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(hiddenOr(formatMoney(pnl, settings), settings.hideBalances), color = if (pnl >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error) } }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { TextButton(onEditCost) { Icon(Icons.Rounded.Edit, null); Spacer(Modifier.size(5.dp)); Text("Ajustar custo") }; Spacer(Modifier.weight(1f)); if (wallet.lastSyncAt > 0) Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(wallet.lastSyncAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun TrackWalletDialog(portfolios: List<PortfolioView>, settings: AppSettings, onDismiss: () -> Unit, onCreate: (TrackedWalletDraft, (Result<Long>) -> Unit) -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }; var address by rememberSaveable { mutableStateOf("") }; var cost by rememberSaveable { mutableStateOf("") }
    var network by rememberSaveable { mutableStateOf(WalletNetwork.SOLANA) }; var selectedId by remember(portfolios) { mutableLongStateOf(portfolios.firstOrNull()?.id ?: 0L) }
    var error by remember { mutableStateOf<String?>(null) }; var saving by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Adicionar endereço público") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(WalletNetwork.entries) { item -> FilterChip(network == item, { network = item }, { Text(networkLabel(item)) }, leadingIcon = { AssetAvatar(networkSymbol(item), size = 22.dp) }) } }
            Text("Use apenas o endereço público da carteira.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(label, { label = it.take(32) }, label = { Text("Nome do endereço") }, placeholder = { Text("Ex.: Phantom principal") }, singleLine = true)
            OutlinedTextField(address, { address = it.trim().take(90) }, label = { Text("Endereço ${networkLabel(network)}") }, singleLine = true, textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(portfolios) { p -> FilterChip(selectedId == p.id, { selectedId = p.id }, { Text(p.name) }) } }
            OutlinedTextField(cost, { cost = decimalOnly(it) }, label = { Text("Custo opcional (${settings.displayCurrency.name})") }, supportingText = { Text("Informe quanto pagou para calcular o P/L.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !saving && selectedId > 0 && address.isNotBlank(), onClick = {
        val entered = cost.replace(',', '.').toDoubleOrNull(); val usd = entered?.let { if (settings.displayCurrency == DisplayCurrency.BRL) it / settings.brlPerUsd else it }
        saving = true; error = null
        onCreate(TrackedWalletDraft(selectedId, label, address, network.name, usd)) { result -> saving = false; result.onSuccess { onDismiss() }.onFailure { error = it.message ?: "Não foi possível adicionar." } }
    }) { Text(if (saving) "Adicionando…" else "Adicionar") } }, dismissButton = { TextButton(onDismiss) { Text("Cancelar") } })
}

@Composable private fun CostBasisDialog(wallet: TrackedWalletView, settings: AppSettings, onDismiss: () -> Unit, onSave: (Double?) -> Unit) {
    val initial = wallet.basisUsd?.let { if (settings.displayCurrency == DisplayCurrency.BRL) it * settings.brlPerUsd else it }
    var value by rememberSaveable(wallet.id) { mutableStateOf(initial?.let { String.format(Locale.ROOT, "%.2f", it).replace('.', ',') } ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Custo de ${wallet.label}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { Text("Use o total realmente pago pelos ativos que ainda estão neste endereço.", color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedTextField(value, { value = decimalOnly(it) }, label = { Text("Custo (${settings.displayCurrency.name})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) } }, confirmButton = { TextButton({ val entered = value.replace(',', '.').toDoubleOrNull(); onSave(entered?.let { if (settings.displayCurrency == DisplayCurrency.BRL) it / settings.brlPerUsd else it }) }, enabled = value.isNotBlank()) { Text("Salvar") } }, dismissButton = { TextButton(onDismiss) { Text("Cancelar") } })
}

@Composable private fun AssetCostBasisDialog(asset: WalletAssetView, settings: AppSettings, onDismiss: () -> Unit, onSave: (Double?) -> Unit) {
    val initial = asset.costBasisUsd?.let { if (settings.displayCurrency == DisplayCurrency.BRL) it * settings.brlPerUsd else it }
    var value by rememberSaveable(asset.mint) { mutableStateOf(initial?.let { String.format(Locale.ROOT, "%.2f", it).replace('.', ',') } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custo de ${asset.symbol}") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { Text("Informe quanto foi pago pelos ${asset.quantity.toReadable()} ${asset.symbol} que permanecem neste endereço.", color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedTextField(value, { value = decimalOnly(it) }, label = { Text("Custo (${settings.displayCurrency.name})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) } },
        confirmButton = { TextButton({ val entered = value.replace(',', '.').toDoubleOrNull(); onSave(entered?.let { if (settings.displayCurrency == DisplayCurrency.BRL) it / settings.brlPerUsd else it }) }) { Text("Salvar") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } },
    )
}

@Composable private fun CreatePortfolioDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) { var name by rememberSaveable { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Novo portfólio") }, text = { OutlinedTextField(name, { name = it.take(32) }, label = { Text("Nome") }, singleLine = true) }, confirmButton = { TextButton({ onCreate(name) }, enabled = name.isNotBlank()) { Text("Criar") } }, dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }) }
private fun networkLabel(network: WalletNetwork) = when (network) { WalletNetwork.SOLANA -> "Solana"; WalletNetwork.BITCOIN -> "Bitcoin"; WalletNetwork.ETHEREUM -> "Ethereum" }
private fun networkSymbol(network: WalletNetwork) = when (network) { WalletNetwork.SOLANA -> "SOL"; WalletNetwork.BITCOIN -> "BTC"; WalletNetwork.ETHEREUM -> "ETH" }
private fun decimalOnly(value: String) = value.filter { it.isDigit() || it == ',' || it == '.' }.replace('.', ',').let { text -> val index = text.indexOf(','); if (index < 0) text else text.take(index + 1) + text.drop(index + 1).replace(",", "") }
private fun Double.toReadable(): String = if (this >= 1) String.format(Locale.ROOT, "%.4f", this).trimEnd('0').trimEnd('.') else String.format(Locale.ROOT, "%.8f", this).trimEnd('0').trimEnd('.')
