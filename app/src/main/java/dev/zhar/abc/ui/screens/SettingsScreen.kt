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
    biometricAvailable: Boolean,
    onThemeChange: (ThemePreference) -> Unit,
    onPaletteChange: (ColorPalette) -> Unit,
    onPriceRefreshSpeedChange: (PriceRefreshSpeed) -> Unit,
    onCurrencyChange: (DisplayCurrency) -> Unit,
    onHideBalancesChange: (Boolean) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onSecureScreenChange: (Boolean) -> Unit,
    onLockTimeoutChange: (LockTimeout) -> Unit,
    onCreateBackup: (CharArray, (Result<ByteArray>) -> Unit) -> Unit,
    onRestoreBackup: (ByteArray, CharArray, (Result<BackupSummary>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    var exportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var importBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showCreatePassword by remember { mutableStateOf(false) }
    var showRestorePassword by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    if (page != SettingsPage.MAIN) {
        when (page) {
            SettingsPage.DONATE -> DonateScreen(modifier = modifier, onBack = { page = SettingsPage.MAIN })
            SettingsPage.PALETTE -> PaletteSettingsScreen(settings, onPaletteChange, modifier) { page = SettingsPage.MAIN }
            SettingsPage.PRICES -> PriceSettingsScreen(settings, onPriceRefreshSpeedChange, modifier) { page = SettingsPage.MAIN }
            SettingsPage.CURRENCY -> CurrencySettingsScreen(settings, onCurrencyChange, modifier) { page = SettingsPage.MAIN }
            SettingsPage.MAIN -> Unit
        }
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
        item { SettingsLinkCard(Icons.Rounded.Palette, "Paleta de cores", paletteLabel(settings.colorPalette)) { page = SettingsPage.PALETTE } }
        item { SectionTitle("Moeda") }
        item { SettingsLinkCard(Icons.Rounded.CurrencyExchange, "Moeda de exibição", settings.displayCurrency.currencyCode) { page = SettingsPage.CURRENCY } }
        item { SectionTitle("Cotações") }
        item { SettingsLinkCard(Icons.Rounded.Speed, "Atualização dos preços", priceSpeedLabel(settings.priceRefreshSpeed)) { page = SettingsPage.PRICES } }
        item { SectionTitle("Privacidade") }
        item { SettingSwitch(Icons.Rounded.RemoveRedEye, "Ocultar saldos", "Esconde valores e percentuais.", settings.hideBalances, onHideBalancesChange) }
        item { SettingSwitch(Icons.Rounded.Fingerprint, "Bloqueio biométrico", if (biometricAvailable) "Pede sua biometria ao abrir." else "Biometria forte não configurada.", settings.biometricLock, onBiometricChange, biometricAvailable) }
        if (settings.biometricLock) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bloquear depois de", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LockTimeout.entries.forEach { timeout -> FilterChip(settings.lockTimeout == timeout, { onLockTimeoutChange(timeout) }, { Text(lockTimeoutLabel(timeout)) }) }
                    }
                }
            }
        }
        item { SettingSwitch(Icons.Rounded.Screenshot, "Proteger tela", "Impede capturas e oculta a prévia em aplicativos recentes.", settings.secureScreen, onSecureScreenChange) }
        item { SectionTitle("Apoiar") }
        item {
            Card(onClick = { page = SettingsPage.DONATE }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
                        FilledTonalButton(onClick = { showCreatePassword = true }, modifier = Modifier.weight(1f)) { Text("Criar") }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) }, modifier = Modifier.weight(1f)) { Text("Restaurar") }
                    }
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

private enum class SettingsPage { MAIN, PALETTE, PRICES, CURRENCY, DONATE }

@Composable
private fun SettingsLinkCard(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Rounded.ChevronRight, null)
        }
    }
}

@Composable
private fun SettingsPageHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Voltar") }
        Column { Text(title, style = MaterialTheme.typography.headlineMedium); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun PaletteSettingsScreen(settings: AppSettings, onChange: (ColorPalette) -> Unit, modifier: Modifier, onBack: () -> Unit) {
    LazyColumn(modifier, contentPadding = PaddingValues(SolfolioLayout.screenHorizontal, 14.dp, SolfolioLayout.screenHorizontal, SolfolioLayout.screenBottom), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SettingsPageHeader("Paleta de cores", "Escolha a identidade visual.", onBack) }
        items(ColorPalette.entries) { palette ->
            Card(onClick = { onChange(palette) }, colors = CardDefaults.cardColors(containerColor = if (settings.colorPalette == palette) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).background(paletteColor(palette), CircleShape)); Spacer(Modifier.width(12.dp)); Text(paletteLabel(palette), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (settings.colorPalette == palette) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun PriceSettingsScreen(settings: AppSettings, onChange: (PriceRefreshSpeed) -> Unit, modifier: Modifier, onBack: () -> Unit) {
    LazyColumn(modifier, contentPadding = PaddingValues(SolfolioLayout.screenHorizontal, 14.dp, SolfolioLayout.screenHorizontal, SolfolioLayout.screenBottom), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SettingsPageHeader("Atualização dos preços", "Controle a frequência visual.", onBack) }
        item { Text("Os preços vêm do mercado. Intervalos curtos deixam a tela mais ativa e podem gastar mais bateria.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(PriceRefreshSpeed.entries) { speed ->
            Card(onClick = { onChange(speed) }, colors = CardDefaults.cardColors(containerColor = if (settings.priceRefreshSpeed == speed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Text(priceSpeedLabel(speed), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (settings.priceRefreshSpeed == speed) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun CurrencySettingsScreen(settings: AppSettings, onChange: (DisplayCurrency) -> Unit, modifier: Modifier, onBack: () -> Unit) {
    LazyColumn(modifier, contentPadding = PaddingValues(SolfolioLayout.screenHorizontal, 14.dp, SolfolioLayout.screenHorizontal, SolfolioLayout.screenBottom), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SettingsPageHeader("Moeda de exibição", "Escolha entre ${DisplayCurrency.entries.size} moedas fiat.", onBack) }
        item { Text(if (settings.fxUpdatedAt > 0) "Câmbio atualizado automaticamente." else "Usando taxas offline até a primeira atualização.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(DisplayCurrency.entries) { currency ->
            Card(onClick = { onChange(currency) }, colors = CardDefaults.cardColors(containerColor = if (settings.displayCurrency == currency) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(currency.currencyCode, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (settings.displayCurrency == currency) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
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

private fun lockTimeoutLabel(value: LockTimeout) = when (value) {
    LockTimeout.INSTANT -> "Imediatamente"; LockTimeout.ONE_MINUTE -> "1 minuto"; LockTimeout.FIVE_MINUTES -> "5 minutos"; LockTimeout.TEN_MINUTES -> "10 minutos"
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
