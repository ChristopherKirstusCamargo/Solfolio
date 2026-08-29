package dev.zhar.abc.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.zhar.abc.domain.*
import dev.zhar.abc.ui.components.FeedBadge
import dev.zhar.abc.data.backup.BackupSummary
import dev.zhar.abc.ui.theme.SolfolioLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettings,
    feedStatus: FeedStatus,
    proStatus: ProStatus,
    biometricAvailable: Boolean,
    onThemeChange: (ThemePreference) -> Unit,
    onPaletteChange: (ColorPalette) -> Unit,
    onPriceRefreshSpeedChange: (PriceRefreshSpeed) -> Unit,
    onCurrencyChange: (DisplayCurrency) -> Unit,
    onHideBalancesChange: (Boolean) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onSecureScreenChange: (Boolean) -> Unit,
    onInteractionFeedbackChange: (Boolean) -> Unit,
    onPurchasePro: () -> Unit,
    onCreateBackup: (CharArray, (Result<ByteArray>) -> Unit) -> Unit,
    onRestoreBackup: (ByteArray, CharArray, (Result<BackupSummary>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDonate by rememberSaveable { mutableStateOf(false) }
    var exportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var importBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showCreatePassword by remember { mutableStateOf(false) }
    var showRestorePassword by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    if (showDonate) {
        DonateScreen(modifier = modifier, onBack = { showDonate = false })
        return
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes = exportBytes
        if (uri != null && bytes != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Não foi possível abrir o arquivo.") } }
                .onSuccess { backupMessage = "Backup criado com sucesso." }
                .onFailure { backupMessage = it.message ?: "Não foi possível salvar o backup." }
            exportBytes = null
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBackupBytes() } ?: error("Não foi possível abrir o arquivo.") } }
                .onSuccess { importBytes = it; showRestorePassword = true }
                .onFailure { backupMessage = it.message ?: "Não foi possível ler o backup." }
        }
    }
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
        item { Column { Text("Ajustes", style = MaterialTheme.typography.headlineMedium); Text("Aparência, moeda e privacidade.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Solfolio PRO", style = MaterialTheme.typography.titleLarge)
                            Text("Análise e backup.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (proStatus.owned) "PRO ativado neste aparelho" else "Compra única · ${proStatus.formattedPrice}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (!proStatus.owned && proStatus.available) {
                        FilledTonalButton(onClick = onPurchasePro, enabled = proStatus.available && !proStatus.pending, modifier = Modifier.fillMaxWidth()) {
                            Text(if (proStatus.pending) "Pagamento pendente" else "Desbloquear PRO")
                        }
                    } else if (!proStatus.owned) {
                        Text("Compra em preparação", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { SectionTitle("Aparência") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ThemePreference.entries.forEach { theme ->
                        FilterChip(
                            selected = settings.theme == theme,
                            onClick = { onThemeChange(theme) },
                            label = { Text(themeLabel(theme)) },
                            leadingIcon = { Icon(if (theme == ThemePreference.AMOLED) Icons.Rounded.DarkMode else Icons.Rounded.Contrast, null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
            }
        }
        item { Text("Paleta de cores", style = MaterialTheme.typography.titleMedium) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(ColorPalette.entries) { palette ->
                    FilterChip(selected = settings.colorPalette == palette, onClick = { onPaletteChange(palette) }, label = { Text(paletteLabel(palette)) }, leadingIcon = { Box(Modifier.size(18.dp).background(paletteColor(palette), CircleShape)) })
                }
            }
        }
        item { Text("Escolha as cores do aplicativo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SectionTitle("Moeda") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.CurrencyExchange, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.size(11.dp)); Column { Text("Moeda de exibição", style = MaterialTheme.typography.titleMedium); Text("Veja os valores em real ou dólar.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { DisplayCurrency.entries.forEach { currency -> FilterChip(settings.displayCurrency == currency, { onCurrencyChange(currency) }, { Text(currency.name) }, Modifier.weight(1f)) } }
                    Text("USD/BRL: ${String.format(Locale.ROOT, "%.4f", settings.brlPerUsd)}" + if (settings.fxUpdatedAt > 0) " · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(settings.fxUpdatedAt))}" else " · valor temporário", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SectionTitle("Cotações") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Speed, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(11.dp))
                        Column { Text("Atualização dos preços", style = MaterialTheme.typography.titleMedium); Text("Escolha com que frequência a tela muda.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PriceRefreshSpeed.entries) { speed ->
                            FilterChip(
                                selected = settings.priceRefreshSpeed == speed,
                                onClick = { onPriceRefreshSpeedChange(speed) },
                                label = { Text(priceSpeedLabel(speed)) },
                            )
                        }
                    }
                    Text("Define a frequência visual dos preços reais.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SectionTitle("Privacidade") }
        item { SettingSwitch(Icons.Rounded.RemoveRedEye, "Ocultar saldos", "Esconde valores e percentuais.", settings.hideBalances, onHideBalancesChange) }
        item { SettingSwitch(Icons.Rounded.Fingerprint, "Bloqueio biométrico", if (biometricAvailable) "Pede sua biometria ao abrir." else "Biometria forte não configurada.", settings.biometricLock, onBiometricChange, biometricAvailable) }
        item { SettingSwitch(Icons.Rounded.Screenshot, "Proteger tela", "Impede capturas e oculta a prévia em aplicativos recentes.", settings.secureScreen, onSecureScreenChange) }
        item { SettingSwitch(Icons.Rounded.TouchApp, "Som e resposta tátil", "Confirma os toques na navegação.", settings.interactionFeedback, onInteractionFeedbackChange) }
        item { SectionTitle("Apoiar") }
        item {
            Card(onClick = { showDonate = true }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VolunteerActivism, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Apoiar o Solfolio", style = MaterialTheme.typography.titleMedium)
                        Text("Doação opcional em Solana.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, null)
                }
            }
        }
        item { SectionTitle("Dados e conexão") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DataRow(Icons.Rounded.Security, "Somente leitura", "Usa apenas endereços públicos.")
                    DataRow(Icons.Rounded.Public, "Consulta online", "Atualiza preços e saldos.")
                    FeedBadge(feedStatus)
                }
            }
        }
        item { SectionTitle("Backup") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DataRow(Icons.Rounded.Lock, "Backup protegido", "Salve ou restaure seus dados.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { showCreatePassword = true }, enabled = proStatus.owned, modifier = Modifier.weight(1f)) { Text("Criar") }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) }, enabled = proStatus.owned, modifier = Modifier.weight(1f)) { Text("Restaurar") }
                    }
                    if (!proStatus.owned) Text("Disponível no PRO.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    backupMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }

    if (showCreatePassword) BackupPasswordDialog(
        title = "Proteger backup",
        message = "Crie uma senha com pelo menos 8 caracteres. Ela será necessária para restaurar o arquivo.",
        confirmLabel = "Criar backup",
        requireConfirmation = true,
        onDismiss = { showCreatePassword = false },
    ) { password ->
        showCreatePassword = false
        onCreateBackup(password) { result ->
            result.onSuccess { bytes ->
                exportBytes = bytes
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                exportLauncher.launch("solfolio-$date.solfolio")
            }.onFailure { backupMessage = it.message ?: "Não foi possível criar o backup." }
        }
    }
    if (showRestorePassword && importBytes != null) BackupPasswordDialog(
        title = "Restaurar backup",
        message = "A restauração substituirá os dados atuais somente depois que o arquivo for validado.",
        confirmLabel = "Validar e restaurar",
        requireConfirmation = false,
        onDismiss = { showRestorePassword = false; importBytes = null },
    ) { password ->
        importBytes?.let { bytes ->
            showRestorePassword = false
            onRestoreBackup(bytes, password) { result ->
                result.onSuccess { backupMessage = "${it.portfolios} portfólio(s) e ${it.operations} operação(ões) restaurados." }
                    .onFailure { backupMessage = it.message ?: "Senha incorreta ou backup inválido." }
                importBytes = null
            }
        }
    }
}

@Composable
private fun DonateScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var message by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(SolfolioLayout.screenHorizontal, 14.dp, SolfolioLayout.screenHorizontal, SolfolioLayout.screenBottom),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Voltar") }
                Column(Modifier.weight(1f)) {
                    Text("Apoiar o projeto", style = MaterialTheme.typography.headlineMedium)
                    Text("Escolha a moeda e a rede.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Rounded.VolunteerActivism, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    Text("Obrigado por apoiar o Solfolio", style = MaterialTheme.typography.titleLarge)
                    Text("A doação não altera funções do aplicativo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            DonationAddressCard(
                title = "Solana",
                network = "Rede Solana · somente SOL",
                address = SOLANA_DONATION_WALLET,
                onCopy = { clipboard.setText(AnnotatedString(SOLANA_DONATION_WALLET)); message = "Endereço Solana copiado." },
                onOpen = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("solana:$SOLANA_DONATION_WALLET?label=Solfolio&message=Apoiar%20o%20projeto")))
                    }.onFailure { message = "Nenhuma carteira Solana compatível foi encontrada." }
                },
            )
        }
        item {
            DonationAddressCard(
                title = "USDT",
                network = "Rede Ethereum · token ERC-20",
                address = USDT_DONATION_WALLET,
                onCopy = { clipboard.setText(AnnotatedString(USDT_DONATION_WALLET)); message = "Endereço USDT copiado." },
            )
        }
        item { message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) } }
        item { Text("Confirme sempre a moeda e a rede antes de enviar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun DonationAddressCard(
    title: String,
    network: String,
    address: String,
    onCopy: () -> Unit,
    onOpen: (() -> Unit)? = null,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Paid, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(network, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                Text("${address.take(8)}…${address.takeLast(8)}", Modifier.fillMaxWidth().padding(11.dp), style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copiar")
                }
                if (onOpen != null) {
                    Button(onClick = onOpen, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.OpenInNew, null); Spacer(Modifier.width(6.dp)); Text("Abrir")
                    }
                }
            }
        }
    }
}

@Composable private fun BackupPasswordDialog(
    title: String,
    message: String,
    confirmLabel: String,
    requireConfirmation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = password.length >= 8 && (!requireConfirmation || password == confirmation)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(password, { password = it.take(128) }, label = { Text("Senha") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            if (requireConfirmation) OutlinedTextField(confirmation, { confirmation = it.take(128) }, label = { Text("Confirmar senha") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            if (password.isNotEmpty() && password.length < 8) Text("Use pelo menos 8 caracteres.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(onClick = { onConfirm(password.toCharArray()); password = ""; confirmation = "" }, enabled = valid) { Text(confirmLabel) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } },
    )
}

@Composable private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 6.dp))
@Composable private fun SettingSwitch(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.clickable(enabled) { onCheckedChange(!checked) }) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.size(12.dp)); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked, onCheckedChange, enabled = enabled) }
    }
}
@Composable private fun DataRow(icon: ImageVector, title: String, text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.Top) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.size(11.dp)); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; trailing?.invoke() }
}
private fun themeLabel(theme: ThemePreference) = when (theme) { ThemePreference.SYSTEM -> "Seguir o sistema"; ThemePreference.LIGHT -> "Claro"; ThemePreference.DARK -> "Escuro"; ThemePreference.AMOLED -> "Preto AMOLED" }
private fun paletteLabel(value: ColorPalette) = when (value) { ColorPalette.SOLANA -> "Solana"; ColorPalette.VIOLET -> "Violeta"; ColorPalette.OCEAN -> "Oceano"; ColorPalette.FOREST -> "Floresta"; ColorPalette.SUNSET -> "Pôr do sol" }
private fun paletteColor(value: ColorPalette) = when (value) { ColorPalette.SOLANA -> Color(0xFF14F195); ColorPalette.VIOLET -> Color(0xFFD0A8FF); ColorPalette.OCEAN -> Color(0xFF56C5FF); ColorPalette.FOREST -> Color(0xFF63D59A); ColorPalette.SUNSET -> Color(0xFFFF916F) }
private fun priceSpeedLabel(value: PriceRefreshSpeed) = when (value) {
    PriceRefreshSpeed.DISABLED -> "Padrão · 60 s"
    PriceRefreshSpeed.VERY_LOW -> "Muito baixa · 45 s"
    PriceRefreshSpeed.LOW -> "Baixa · 30 s"
    PriceRefreshSpeed.MEDIUM -> "Média · 15 s"
    PriceRefreshSpeed.HIGH -> "Alta · 5 s"
    PriceRefreshSpeed.INSTANT -> "Instantânea · <1 s"
}

private fun InputStream.readBackupBytes(): ByteArray {
    val maximum = 26 * 1024 * 1024
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= maximum) { "O arquivo excede o limite de 26 MB." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private const val SOLANA_DONATION_WALLET = "3VgkUFUkcfH8eXcCroytKkfpsbSDnTDXPR7z7RMGMnj1"
private const val USDT_DONATION_WALLET = "0xCBc650f00015B1AdEA77bE360F430b6886053A37"
