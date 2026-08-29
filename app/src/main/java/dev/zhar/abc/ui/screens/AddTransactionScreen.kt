package dev.zhar.abc.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.zhar.abc.data.LedgerEntryDraft
import dev.zhar.abc.domain.AppSettings
import dev.zhar.abc.domain.AssetDefinition
import dev.zhar.abc.domain.AssetQuote
import dev.zhar.abc.domain.DisplayCurrency
import dev.zhar.abc.domain.TransactionKind
import dev.zhar.abc.ui.PortfolioView
import dev.zhar.abc.ui.components.AssetAvatar
import dev.zhar.abc.ui.theme.SolfolioLayout
import dev.zhar.abc.util.formatMoneyDirect
import dev.zhar.abc.util.ratePerUsd
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun AddTransactionScreen(
    portfolios: List<PortfolioView>,
    assets: List<AssetDefinition>,
    quotes: Map<String, AssetQuote>,
    settings: AppSettings,
    customAssetSymbols: Set<String>,
    initialPortfolioId: Long?,
    onSave: (LedgerEntryDraft, (Result<Long>) -> Unit) -> Unit,
    onDeleteCustomAsset: (String, (Result<Unit>) -> Unit) -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var portfolioId by rememberSaveable { mutableStateOf<Long?>(initialPortfolioId) }
    var portfolioMenu by remember { mutableStateOf(false) }
    var selectedAsset by remember { mutableStateOf<AssetDefinition?>(null) }
    var customAsset by remember { mutableStateOf<AssetDefinition?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showAssetManager by remember { mutableStateOf(false) }
    var moreAssetsMenu by remember { mutableStateOf(false) }
    var kind by rememberSaveable { mutableStateOf(TransactionKind.BUY) }
    var entryCurrency by rememberSaveable { mutableStateOf(settings.displayCurrency) }
    var currencyMenu by remember { mutableStateOf(false) }
    var quantityText by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var feeText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(portfolios, initialPortfolioId) {
        if (portfolioId == null || portfolios.none { it.id == portfolioId }) {
            portfolioId = initialPortfolioId?.takeIf { id -> portfolios.any { it.id == id } }
                ?: portfolios.firstOrNull()?.id
        }
    }
    LaunchedEffect(assets) {
        if (selectedAsset == null && customAsset == null) {
            selectedAsset = assets.firstOrNull { it.symbol == "BTC" } ?: assets.firstOrNull()
        }
    }

    val activeAsset = customAsset ?: selectedAsset
    val activeQuote = activeAsset?.let { quotes[it.symbol] }
    val selectedPortfolioName = portfolios.firstOrNull { it.id == portfolioId }?.name ?: "Escolher"

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    start = SolfolioLayout.screenHorizontal,
                    end = SolfolioLayout.screenHorizontal,
                    top = SolfolioLayout.screenTop,
                    bottom = SolfolioLayout.screenBottom,
                ),
            ),
        verticalArrangement = Arrangement.spacedBy(SolfolioLayout.cardPadding),
    ) {
        Column {
            Text("Novo lançamento", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Adicione compras e vendas para acompanhar seu resultado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text("Portfólio", style = MaterialTheme.typography.titleMedium)
        Column {
            OutlinedButton(onClick = { portfolioMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedPortfolioName, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ExpandMore, contentDescription = null)
            }
            DropdownMenu(expanded = portfolioMenu, onDismissRequest = { portfolioMenu = false }) {
                portfolios.forEach { portfolio ->
                    DropdownMenuItem(
                        text = { Text(portfolio.name) },
                        trailingIcon = if (portfolio.id == portfolioId) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        onClick = {
                            portfolioId = portfolio.id
                            portfolioMenu = false
                        },
                    )
                }
            }
        }

        Text("Ativo", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            assets.take(8).forEach { asset ->
                FilterChip(
                    selected = customAsset == null && selectedAsset?.symbol == asset.symbol,
                    onClick = {
                        customAsset = null
                        selectedAsset = asset
                    },
                    label = { Text(asset.symbol) },
                    leadingIcon = {
                        AssetAvatar(symbol = asset.symbol, size = 24.dp)
                    },
                )
            }
            if (assets.size > 8) {
                Box {
                    FilterChip(
                        selected = assets.drop(8).any { it.symbol == selectedAsset?.symbol },
                        onClick = { moreAssetsMenu = true },
                        label = { Text(selectedAsset?.takeIf { selected -> assets.drop(8).any { it.symbol == selected.symbol } }?.symbol ?: "Mais") },
                        leadingIcon = { Icon(Icons.Rounded.MoreHoriz, contentDescription = null) },
                    )
                    DropdownMenu(expanded = moreAssetsMenu, onDismissRequest = { moreAssetsMenu = false }) {
                        assets.drop(8).forEach { asset ->
                            DropdownMenuItem(
                                text = { Text("${asset.name} · ${asset.symbol}") },
                                leadingIcon = { AssetAvatar(asset.symbol, size = 26.dp) },
                                onClick = {
                                    customAsset = null
                                    selectedAsset = asset
                                    moreAssetsMenu = false
                                },
                            )
                        }
                    }
                }
            }
            customAsset?.let { asset ->
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(asset.symbol) },
                    leadingIcon = { AssetAvatar(symbol = asset.symbol, size = 24.dp) },
                )
            }
            FilterChip(
                selected = false,
                onClick = { showCustomDialog = true },
                label = { Text("Outro") },
                leadingIcon = { Icon(Icons.Rounded.AddCircleOutline, contentDescription = null) },
            )
            if (customAssetSymbols.isNotEmpty()) {
                FilterChip(
                    selected = false,
                    onClick = { showAssetManager = true },
                    label = { Text("Gerenciar") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                )
            }
        }

        activeAsset?.let { asset ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssetAvatar(asset.symbol)
                    Spacer(Modifier.size(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(asset.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${asset.symbol} · preço de mercado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = activeQuote?.let {
                            formatMoneyDirect(
                                it.priceUsd * ratePerUsd(settings, entryCurrency),
                                entryCurrency,
                            )
                        } ?: "Sem cotação",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (activeQuote != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text("Operação", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransactionKind.entries.forEach { option ->
                FilterChip(
                    selected = kind == option,
                    onClick = { kind = option },
                    label = { Text(if (option == TransactionKind.BUY) "Compra" else "Venda") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (option == TransactionKind.BUY) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                    ),
                )
            }
        }

        Box {
            OutlinedButton(onClick = { currencyMenu = true }) {
                Text("Moeda: ${entryCurrency.currencyCode}"); Spacer(Modifier.size(6.dp)); Icon(Icons.Rounded.ExpandMore, null)
            }
            DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                DisplayCurrency.entries.forEach { currency -> DropdownMenuItem(text = { Text(currency.currencyCode) }, trailingIcon = if (currency == entryCurrency) ({ Icon(Icons.Rounded.Check, null) }) else null, onClick = { entryCurrency = currency; currencyMenu = false }) }
            }
        }

        OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = sanitizeDecimal(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Quantidade") },
            placeholder = { Text("0,00") },
            suffix = { Text(activeAsset?.symbol.orEmpty()) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = sanitizeDecimal(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (kind == TransactionKind.BUY) "Total pago (opcional)" else "Total recebido (opcional)") },
            placeholder = { Text("Deixe vazio se não souber") },
            prefix = { Text("${entryCurrency.currencyCode} ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        if (activeQuote != null) {
            TextButton(
                onClick = {
                    val quantity = parseDecimal(quantityText) ?: 0.0
                    if (quantity > 0.0) {
                        val total = quantity * activeQuote.priceUsd *
                            ratePerUsd(settings, entryCurrency)
                        amountText = decimalForInput(total)
                    } else {
                        error = "Informe primeiro a quantidade."
                    }
                },
            ) {
                Icon(Icons.Rounded.AutoGraph, contentDescription = null)
                Spacer(Modifier.size(7.dp))
                Text("Usar preço ao vivo")
            }
        }
        OutlinedTextField(
            value = feeText,
            onValueChange = { feeText = sanitizeDecimal(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Taxa (opcional)") },
            prefix = { Text("${entryCurrency.currencyCode} ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(120) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Observação (opcional)") },
            placeholder = { Text("Ex.: compra mensal") },
            minLines = 2,
            maxLines = 3,
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                error = null
                val asset = activeAsset
                val portfolio = portfolioId
                val quantity = parseDecimal(quantityText)
                val amount = parseDecimal(amountText)?.takeIf { it > 0.0 } ?: 0.0
                val fee = parseDecimal(feeText) ?: 0.0
                when {
                    portfolio == null -> error = "Escolha um portfólio."
                    asset == null -> error = "Escolha um ativo."
                    quantity == null || quantity <= 0.0 -> error = "Informe uma quantidade válida."
                    else -> {
                        val rate = ratePerUsd(settings, entryCurrency).takeIf { it > 0.0 } ?: 1.0
                        val amountUsd = amount / rate
                        val feeUsd = fee / rate
                        val draft = LedgerEntryDraft(
                            portfolioId = portfolio,
                            asset = asset,
                            isCustomAsset = customAsset != null,
                            kind = kind,
                            quantity = quantity,
                            unitPriceUsd = if (amountUsd > 0.0) amountUsd / quantity else 0.0,
                            feeUsd = feeUsd,
                            originalAmount = amount,
                            originalCurrency = entryCurrency.name,
                            brlPerUsdAtEntry = settings.brlPerUsd,
                            timestamp = System.currentTimeMillis(),
                            note = note,
                        )
                        saving = true
                        onSave(draft) { result ->
                            saving = false
                            result.onSuccess { onSaved() }
                                .onFailure { error = it.message ?: "Não foi possível salvar." }
                        }
                    }
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (saving) "Salvando…" else "Salvar lançamento")
        }
    }

    if (showCustomDialog) {
        CustomAssetDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = {
                customAsset = it
                selectedAsset = null
                showCustomDialog = false
            },
        )
    }
    if (showAssetManager) {
        CustomAssetsManagerDialog(
            assets = assets.filter { it.symbol in customAssetSymbols },
            onDismiss = { showAssetManager = false },
            onDelete = onDeleteCustomAsset,
        )
    }
}

@Composable
private fun CustomAssetsManagerDialog(
    assets: List<AssetDefinition>,
    onDismiss: () -> Unit,
    onDelete: (String, (Result<Unit>) -> Unit) -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ativos personalizados") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                assets.forEach { asset ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(asset.symbol, style = MaterialTheme.typography.titleMedium); Text(asset.name, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton(onClick = {
                                error = null
                                onDelete(asset.symbol) { result -> result.onFailure { error = it.message ?: "Não foi possível remover." } }
                            }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remover ${asset.symbol}") }
                        }
                    }
                }
                if (assets.isEmpty()) Text("Nenhum ativo personalizado.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun CustomAssetDialog(
    onDismiss: () -> Unit,
    onConfirm: (AssetDefinition) -> Unit,
) {
    var symbol by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar outro ativo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "O app tentará receber a cotação pública do par TICKER-USD. Se o par não existir, o último preço registrado será usado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase().filter(Char::isLetterOrDigit).take(10) },
                    label = { Text("Ticker") },
                    placeholder = { Text("Ex.: BNB") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(32) },
                    label = { Text("Nome") },
                    placeholder = { Text("Ex.: BNB") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = symbol.isNotBlank(),
                onClick = {
                    onConfirm(
                        AssetDefinition(
                            symbol = symbol,
                            name = name.ifBlank { symbol },
                            productId = "$symbol-USD",
                        ),
                    )
                },
            ) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun sanitizeDecimal(value: String): String = value
    .filter { it.isDigit() || it == ',' || it == '.' }
    .replace('.', ',')
    .let { filtered ->
        val firstComma = filtered.indexOf(',')
        if (firstComma < 0) filtered else {
            filtered.substring(0, firstComma + 1) + filtered.substring(firstComma + 1).replace(",", "")
        }
    }

private fun parseDecimal(value: String): Double? = value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

private fun decimalForInput(value: Double): String {
    val symbols = DecimalFormatSymbols(Locale("pt", "BR"))
    return DecimalFormat("0.########", symbols).format(value)
}
