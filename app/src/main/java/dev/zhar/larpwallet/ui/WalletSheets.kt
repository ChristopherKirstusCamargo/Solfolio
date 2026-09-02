package dev.zhar.larpwallet.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zhar.larpwallet.model.TokenAsset
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
import java.util.Locale

@Composable
fun WalletSheetContent(
    route: WalletSheet,
    state: WalletState,
    viewModel: WalletViewModel,
    onDismiss: () -> Unit,
) {
    when (route) {
        WalletSheet.EditTotal -> EditTotalSheet(state, viewModel, onDismiss)
        is WalletSheet.EditAsset -> AssetEditorSheet(route.asset, viewModel, onDismiss)
        WalletSheet.Receive -> ReceiveSheet(state, viewModel, onDismiss)
        WalletSheet.Send -> SendSheet(state, viewModel, onDismiss)
        WalletSheet.Swap -> SwapSheet(state, viewModel, onDismiss)
        WalletSheet.AddCollectible -> AddCollectibleSheet(viewModel, onDismiss)
        WalletSheet.EditProfile -> EditProfileSheet(state, viewModel, onDismiss)
        WalletSheet.ExchangeRate -> ExchangeRateSheet(state, viewModel, onDismiss)
        WalletSheet.ResetDemo -> ResetSheet(viewModel, onDismiss)
        WalletSheet.About -> AboutSheet(onDismiss)
    }
}

@Composable
private fun SheetFrame(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = SoftWhite, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(5.dp))
                Text(subtitle, color = MutedText, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = "Fechar", tint = SoftWhite)
            }
        }
        Spacer(Modifier.height(22.dp))
        content()
    }
}

@Composable
private fun EditTotalSheet(state: WalletState, viewModel: WalletViewModel, onDismiss: () -> Unit) {
    val initial = if (state.useUsd) state.totalBrl / state.usdBrlRate else state.totalBrl
    var amount by remember { mutableStateOf(String.format(Locale.US, "%.2f", initial)) }
    val parsed = amount.toFlexibleDouble()
    val symbol = if (state.useUsd) "US$" else "R$"

    SheetFrame(
        title = "Definir saldo total",
        subtitle = "O valor será distribuído proporcionalmente entre os ativos atuais.",
        onDismiss = onDismiss,
    ) {
        AppTextField(
            value = amount,
            onValueChange = { amount = it.filterNumberInput() },
            label = "Saldo em ${if (state.useUsd) "USD" else "BRL"}",
            prefix = { Text(symbol, color = MutedText) },
            keyboardType = KeyboardType.Decimal,
        )
        Spacer(Modifier.height(12.dp))
        SafetyNote("Ajustar o total não movimenta dinheiro. É apenas uma transformação dos dados fictícios locais.")
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Aplicar saldo",
            enabled = parsed != null && parsed >= 0.0,
            onClick = {
                val value = parsed ?: return@PrimaryButton
                viewModel.setPortfolioTotal(if (state.useUsd) value * state.usdBrlRate else value)
                onDismiss()
            },
        )
    }
}

@Composable
private fun AssetEditorSheet(asset: TokenAsset?, viewModel: WalletViewModel, onDismiss: () -> Unit) {
    var symbol by remember(asset?.id) { mutableStateOf(asset?.symbol.orEmpty()) }
    var name by remember(asset?.id) { mutableStateOf(asset?.name.orEmpty()) }
    var quantity by remember(asset?.id) { mutableStateOf(asset?.quantity?.toPlainInput().orEmpty()) }
    var price by remember(asset?.id) { mutableStateOf(asset?.priceBrl?.toPlainInput().orEmpty()) }
    var change by remember(asset?.id) { mutableStateOf(asset?.change24h?.toPlainInput().orEmpty()) }
    var selectedColor by remember(asset?.id) { mutableStateOf(asset?.colorHex ?: "#9C72FF") }
    val colors = listOf("#9C72FF", "#F7931A", "#2775CA", "#67D8C3", "#FF6B8A", "#F3C969")
    val valid = symbol.isNotBlank() && quantity.toFlexibleDouble() != null && price.toFlexibleDouble() != null

    SheetFrame(
        title = if (asset == null) "Adicionar ativo" else "Editar ${asset.symbol}",
        subtitle = "Você controla todos os números exibidos neste cenário.",
        onDismiss = onDismiss,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(
                value = symbol,
                onValueChange = { symbol = it.uppercase(Locale.ROOT).filter { char -> char.isLetterOrDigit() }.take(10) },
                label = "Símbolo",
                modifier = Modifier.weight(0.38f),
                capitalization = KeyboardCapitalization.Characters,
            )
            AppTextField(
                value = name,
                onValueChange = { name = it.take(32) },
                label = "Nome",
                modifier = Modifier.weight(0.62f),
                capitalization = KeyboardCapitalization.Words,
            )
        }
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = quantity,
            onValueChange = { quantity = it.filterNumberInput() },
            label = "Quantidade",
            keyboardType = KeyboardType.Decimal,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(
                value = price,
                onValueChange = { price = it.filterNumberInput() },
                label = "Preço em BRL",
                prefix = { Text("R$", color = MutedText) },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Decimal,
            )
            AppTextField(
                value = change,
                onValueChange = { change = it.filterSignedNumberInput() },
                label = "24h",
                suffix = { Text("%", color = MutedText) },
                modifier = Modifier.weight(0.72f),
                keyboardType = KeyboardType.Decimal,
            )
        }
        Spacer(Modifier.height(17.dp))
        Text("Cor do ativo", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            colors.forEach { colorHex ->
                val selected = colorHex == selectedColor
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(colorFromHex(colorHex), CircleShape)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.15f),
                            shape = CircleShape,
                        )
                        .clickable { selectedColor = colorHex },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            text = if (asset == null) "Adicionar ao cenário" else "Salvar alterações",
            enabled = valid,
            onClick = {
                viewModel.upsertAsset(
                    existingId = asset?.id,
                    symbol = symbol,
                    name = name,
                    quantity = quantity.toFlexibleDouble() ?: 0.0,
                    priceBrl = price.toFlexibleDouble() ?: 0.0,
                    change24h = change.toFlexibleDouble() ?: 0.0,
                    colorHex = selectedColor,
                )
                onDismiss()
            },
        )
    }
}

@Composable
private fun ReceiveSheet(state: WalletState, viewModel: WalletViewModel, onDismiss: () -> Unit) {
    var symbol by remember { mutableStateOf(state.assets.firstOrNull()?.symbol.orEmpty()) }
    var amount by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val fakeAddress = "SIMULACAO-NAO-E-ENDERECO-REAL-${symbol.ifBlank { "TOKEN" }}-LARP"

    SheetFrame(
        title = "Receber",
        subtitle = "Crie uma entrada fictícia para o ativo escolhido.",
        onDismiss = onDismiss,
    ) {
        if (state.assets.isEmpty()) {
            SafetyNote("Adicione pelo menos um ativo antes de simular um recebimento.")
            return@SheetFrame
        }
        AssetSelector(assets = state.assets, selectedSymbol = symbol, onSelected = { symbol = it })
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, RoundedCornerShape(20.dp))
                .border(1.dp, Hairline, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                FakeQrMark()
                Spacer(Modifier.height(13.dp))
                Text("ENDEREÇO CENOGRÁFICO", color = PurpleBright, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(7.dp))
                Text(fakeAddress, color = SoftWhite, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(7.dp))
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(fakeAddress))
                        Toast.makeText(context, "Texto cenográfico copiado", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Copiar texto")
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        AppTextField(
            value = amount,
            onValueChange = { amount = it.filterNumberInput() },
            label = "Quantidade a simular",
            keyboardType = KeyboardType.Decimal,
        )
        Spacer(Modifier.height(12.dp))
        SafetyNote("Este endereço é deliberadamente inválido. Não envie cripto para ele.")
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Simular recebimento",
            enabled = (amount.toFlexibleDouble() ?: 0.0) > 0.0,
            onClick = {
                viewModel.receive(symbol, amount.toFlexibleDouble() ?: 0.0)
                onDismiss()
            },
        )
    }
}

@Composable
private fun SendSheet(state: WalletState, viewModel: WalletViewModel, onDismiss: () -> Unit) {
    var symbol by remember { mutableStateOf(state.assets.firstOrNull()?.symbol.orEmpty()) }
    var amount by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val selected = state.assets.firstOrNull { it.symbol == symbol }

    SheetFrame(
        title = "Enviar",
        subtitle = "Reduza o saldo e registre uma saída fictícia.",
        onDismiss = onDismiss,
    ) {
        if (state.assets.isEmpty()) {
            SafetyNote("Adicione pelo menos um ativo antes de simular um envio.")
            return@SheetFrame
        }
        AssetSelector(assets = state.assets, selectedSymbol = symbol, onSelected = {
            symbol = it
            error = null
        })
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Disponível: ${formatQuantity(selected?.quantity ?: 0.0)} $symbol",
            color = MutedText,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = destination,
            onValueChange = { destination = it.take(64) },
            label = "Destino fictício (opcional)",
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = amount,
            onValueChange = {
                amount = it.filterNumberInput()
                error = null
            },
            label = "Quantidade",
            keyboardType = KeyboardType.Decimal,
            isError = error != null,
            supportingText = error?.let { message -> { Text(message, color = Negative) } },
        )
        Spacer(Modifier.height(12.dp))
        SafetyNote("Nenhuma transação é assinada ou transmitida para uma rede.")
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Confirmar envio simulado",
            enabled = (amount.toFlexibleDouble() ?: 0.0) > 0.0,
            onClick = {
                val ok = viewModel.send(symbol, amount.toFlexibleDouble() ?: 0.0, destination)
                if (ok) onDismiss() else error = "Quantidade maior que o saldo disponível."
            },
        )
    }
}

@Composable
private fun SwapSheet(state: WalletState, viewModel: WalletViewModel, onDismiss: () -> Unit) {
    var from by remember { mutableStateOf(state.assets.firstOrNull()?.symbol.orEmpty()) }
    var to by remember { mutableStateOf(state.assets.drop(1).firstOrNull()?.symbol ?: state.assets.firstOrNull()?.symbol.orEmpty()) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val fromAsset = state.assets.firstOrNull { it.symbol == from }
    val toAsset = state.assets.firstOrNull { it.symbol == to }
    val receiveAmount = amount.toFlexibleDouble()?.let { qty ->
        if (fromAsset != null && toAsset != null && toAsset.priceBrl > 0) qty * fromAsset.priceBrl / toAsset.priceBrl else null
    }

    SheetFrame(
        title = "Trocar ativos",
        subtitle = "Conversão matemática usando os preços definidos por você.",
        onDismiss = onDismiss,
    ) {
        if (state.assets.size < 2) {
            SafetyNote("Adicione pelo menos dois ativos para criar uma troca simulada.")
            return@SheetFrame
        }
        Text("Você envia", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        AssetSelector(assets = state.assets, selectedSymbol = from, onSelected = {
            from = it
            if (to == it) to = state.assets.first { asset -> asset.symbol != it }.symbol
            error = null
        })
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = amount,
            onValueChange = {
                amount = it.filterNumberInput()
                error = null
            },
            label = "Quantidade de $from",
            keyboardType = KeyboardType.Decimal,
            isError = error != null,
            supportingText = error?.let { message -> { Text(message, color = Negative) } },
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .size(42.dp)
                .align(Alignment.CenterHorizontally)
                .background(Purple.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = PurpleBright)
        }
        Spacer(Modifier.height(14.dp))
        Text("Você recebe", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        AssetSelector(assets = state.assets.filterNot { it.symbol == from }, selectedSymbol = to, onSelected = {
            to = it
            error = null
        })
        Spacer(Modifier.height(12.dp))
        Surface(color = CardSurface, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Recebimento estimado", color = MutedText, modifier = Modifier.weight(1f))
                Text(
                    text = receiveAmount?.let { "${formatQuantity(it)} $to" } ?: "—",
                    color = SoftWhite,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Fazer troca simulada",
            enabled = (amount.toFlexibleDouble() ?: 0.0) > 0.0 && from != to,
            onClick = {
                val ok = viewModel.swap(from, to, amount.toFlexibleDouble() ?: 0.0)
                if (ok) onDismiss() else error = "Verifique a quantidade e o saldo disponível."
            },
        )
    }
}

@Composable
private fun AddCollectibleSheet(viewModel: WalletViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var collection by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#9C72FF") }
    val colors = listOf("#9C72FF", "#4AD9FF", "#67D8C3", "#FF6B8A", "#F3C969")

    SheetFrame(
        title = "Novo colecionável",
        subtitle = "Adicione um item visual fictício à sua coleção local.",
        onDismiss = onDismiss,
    ) {
        AppTextField(
            value = name,
            onValueChange = { name = it.take(36) },
            label = "Nome do item",
            capitalization = KeyboardCapitalization.Words,
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = collection,
            onValueChange = { collection = it.take(36) },
            label = "Coleção",
            capitalization = KeyboardCapitalization.Words,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colorFromHex(color), CircleShape)
                        .border(if (selectedColor == color) 3.dp else 1.dp, Color.White.copy(alpha = if (selectedColor == color) 1f else 0.18f), CircleShape)
                        .clickable { selectedColor = color },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selectedColor == color) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            text = "Adicionar item fictício",
            enabled = name.isNotBlank(),
            onClick = {
                viewModel.addCollectible(name, collection, selectedColor)
                onDismiss()
            },
        )
    }
}

@Composable
private fun EditProfileSheet(state: WalletState, viewModel: WalletViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(state.accountName) }
    SheetFrame(
        title = "Nome da conta",
        subtitle = "Esse nome aparece apenas no aplicativo.",
        onDismiss = onDismiss,
    ) {
        AppTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = "Nome",
            capitalization = KeyboardCapitalization.Sentences,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Salvar nome",
            enabled = name.isNotBlank(),
            onClick = {
                viewModel.setAccountName(name)
                onDismiss()
            },
        )
    }
}

@Composable
private fun ExchangeRateSheet(state: WalletState, viewModel: WalletViewModel, onDismiss: () -> Unit) {
    var rate by remember { mutableStateOf(state.usdBrlRate.toPlainInput()) }
    SheetFrame(
        title = "Cotação USD/BRL",
        subtitle = "Como o app é offline, a cotação também é definida manualmente.",
        onDismiss = onDismiss,
    ) {
        AppTextField(
            value = rate,
            onValueChange = { rate = it.filterNumberInput() },
            label = "Um dólar vale",
            prefix = { Text("R$", color = MutedText) },
            keyboardType = KeyboardType.Decimal,
        )
        Spacer(Modifier.height(12.dp))
        SafetyNote("Essa cotação serve apenas para converter a exibição entre BRL e USD.")
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Salvar cotação",
            enabled = (rate.toFlexibleDouble() ?: 0.0) > 0.0,
            onClick = {
                viewModel.setExchangeRate(rate.toFlexibleDouble() ?: state.usdBrlRate)
                onDismiss()
            },
        )
    }
}

@Composable
private fun ResetSheet(viewModel: WalletViewModel, onDismiss: () -> Unit) {
    SheetFrame(
        title = "Restaurar demonstração?",
        subtitle = "Todos os ativos, valores, atividades e itens criados por você serão substituídos.",
        onDismiss = onDismiss,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Negative.copy(alpha = 0.12f), CircleShape)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = Negative, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.resetDemo()
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Negative, contentColor = Color(0xFF26050B)),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Restaurar agora", modifier = Modifier.padding(vertical = 5.dp), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Manter meus dados", color = SoftWhite)
        }
    }
}

@Composable
private fun AboutSheet(onDismiss: () -> Unit) {
    SheetFrame(
        title = "Sobre o LARP Wallet",
        subtitle = "Uma carteira cenográfica para protótipos, histórias e demonstrações.",
        onDismiss = onDismiss,
    ) {
        AboutPoint(Icons.Outlined.Lock, "Sem blockchain", "O aplicativo não se conecta a redes, não possui seed e não assina transações.")
        Spacer(Modifier.height(14.dp))
        AboutPoint(Icons.Outlined.Info, "Valores inventados", "Preços, saldos, variações e atividades são definidos localmente por você.")
        Spacer(Modifier.height(14.dp))
        AboutPoint(Icons.Outlined.DeleteOutline, "Dados no aparelho", "Ao limpar os dados ou desinstalar o app, o cenário é apagado.")
        Spacer(Modifier.height(18.dp))
        SafetyNote("Não use capturas deste aplicativo para alegar posse de fundos reais. O aviso de simulação faz parte permanente da interface.")
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Entendi", onClick = onDismiss)
    }
}

@Composable
private fun AboutPoint(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Purple.copy(alpha = 0.13f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = PurpleBright, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SoftWhite, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(body, color = MutedText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AssetSelector(
    assets: List<TokenAsset>,
    selectedSymbol: String,
    onSelected: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(assets, key = { it.id }) { asset ->
            FilterChip(
                selected = selectedSymbol == asset.symbol,
                onClick = { onSelected(asset.symbol) },
                label = { Text(asset.symbol, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(colorFromHex(asset.colorHex), CircleShape),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = CardSurface,
                    labelColor = MutedText,
                    selectedContainerColor = Purple.copy(alpha = 0.18f),
                    selectedLabelColor = PurpleBright,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedSymbol == asset.symbol,
                    borderColor = Hairline,
                    selectedBorderColor = Purple.copy(alpha = 0.48f),
                ),
            )
        }
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    prefix: (@Composable (() -> Unit))? = null,
    suffix: (@Composable (() -> Unit))? = null,
    isError: Boolean = false,
    supportingText: (@Composable (() -> Unit))? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = ImeAction.Next,
        ),
        shape = RoundedCornerShape(17.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SoftWhite,
            unfocusedTextColor = SoftWhite,
            focusedBorderColor = Purple,
            unfocusedBorderColor = Hairline,
            focusedLabelColor = PurpleBright,
            unfocusedLabelColor = MutedText,
            cursorColor = PurpleBright,
            focusedContainerColor = Color(0xFF121016),
            unfocusedContainerColor = Color(0xFF121016),
        ),
    )
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PurpleBright,
            contentColor = Color(0xFF17101F),
            disabledContainerColor = Hairline,
            disabledContentColor = MutedText,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 5.dp))
    }
}

@Composable
private fun SafetyNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Purple.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
            .border(1.dp, Purple.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = PurpleBright, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(9.dp))
        Text(text, color = MutedText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FakeQrMark() {
    val cells = listOf(
        "1111111010101111111",
        "1000001011101000001",
        "1011101010101011101",
        "1011101001101011101",
        "1011101011101011101",
        "1000001000101000001",
        "1111111010101111111",
        "0000000011000000000",
        "1010111110111010101",
        "0111000011000101110",
        "1100111010111010011",
        "0011000101000101100",
        "1111111011101110101",
        "1000001001001010110",
        "1011101010111110011",
        "1011101001100011100",
        "1011101010111010111",
        "1000001011000101000",
        "1111111010111010111",
    )
    Column(
        modifier = Modifier
            .size(156.dp)
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        cells.forEach { row ->
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (cell == '1') Color(0xFF17121F) else Color.Transparent),
                    )
                }
            }
        }
    }
}

private fun String.toFlexibleDouble(): Double? = trim().replace(" ", "").let { raw ->
    when {
        raw.isBlank() || raw == "-" -> null
        raw.contains(',') && raw.contains('.') -> raw.replace(".", "").replace(',', '.').toDoubleOrNull()
        raw.contains(',') -> raw.replace(',', '.').toDoubleOrNull()
        else -> raw.toDoubleOrNull()
    }
}

private fun String.filterNumberInput(): String {
    var separatorFound = false
    return filter { char ->
        when {
            char.isDigit() -> true
            (char == ',' || char == '.') && !separatorFound -> {
                separatorFound = true
                true
            }
            else -> false
        }
    }.take(20)
}

private fun String.filterSignedNumberInput(): String {
    var separatorFound = false
    return filterIndexed { index, char ->
        when {
            char.isDigit() -> true
            char == '-' && index == 0 -> true
            (char == ',' || char == '.') && !separatorFound -> {
                separatorFound = true
                true
            }
            else -> false
        }
    }.take(12)
}

private fun Double.toPlainInput(): String = if (this % 1.0 == 0.0) {
    toLong().toString()
} else {
    java.math.BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
}
