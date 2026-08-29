package dev.zhar.abc.ui

import android.view.SoundEffectConstants
import android.view.WindowManager
import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zhar.abc.ui.screens.AddTransactionScreen
import dev.zhar.abc.ui.screens.AnalysisScreen
import dev.zhar.abc.ui.screens.DashboardScreen
import dev.zhar.abc.ui.screens.PortfoliosScreen
import dev.zhar.abc.ui.screens.MarketScreen
import dev.zhar.abc.ui.screens.SettingsScreen
import dev.zhar.abc.ui.theme.AbcTheme

private enum class AppDestination(
    val label: String,
    val icon: ImageVector,
    val showInNavigation: Boolean = true,
) {
    HOME("Início", Icons.Rounded.Home),
    PORTFOLIOS("Carteiras", Icons.Rounded.AccountBalanceWallet),
    MARKET("Mercado", Icons.AutoMirrored.Rounded.ShowChart),
    ANALYSIS("Análise", Icons.Rounded.Analytics),
    SETTINGS("Ajustes", Icons.Rounded.Settings),
    ADD("Novo", Icons.Rounded.AddCircle, false),
}

@Composable
fun AbcAppRoot(
    viewModel: AbcViewModel,
    biometricAvailable: Boolean,
    authenticate: ((Boolean) -> Unit) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var promptRunning by remember { mutableStateOf(false) }
    var backgroundedAt by rememberSaveable { mutableLongStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val rootView = LocalView.current

    DisposableEffect(rootView, state.settings.secureScreen) {
        val window = (rootView.context as? Activity)?.window
        if (state.settings.secureScreen) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { }
    }

    DisposableEffect(lifecycleOwner, state.settings.biometricLock) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (
                        state.settings.biometricLock &&
                        backgroundedAt > 0L &&
                        System.currentTimeMillis() - backgroundedAt > 15_000L
                    ) {
                        unlocked = false
                    }
                    viewModel.startLiveUpdates()
                }

                Lifecycle.Event.ON_STOP -> {
                    backgroundedAt = System.currentTimeMillis()
                    viewModel.stopLiveUpdates()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.settings.biometricLock, unlocked, biometricAvailable) {
        if (state.settings.biometricLock && biometricAvailable && !unlocked && !promptRunning) {
            promptRunning = true
            authenticate { success ->
                unlocked = success
                promptRunning = false
            }
        }
    }

    AbcTheme(settings = state.settings) {
        when {
            state.settings.biometricLock && !unlocked -> BiometricLockScreen(
                biometricAvailable = biometricAvailable,
                promptRunning = promptRunning,
                onUnlock = {
                    if (!promptRunning) {
                        promptRunning = true
                        authenticate { success ->
                            unlocked = success
                            promptRunning = false
                        }
                    }
                },
            )

            !state.isReady -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            else -> AbcApp(
                state = state,
                viewModel = viewModel,
                biometricAvailable = biometricAvailable,
            )
        }
    }
}

@Composable
private fun AbcApp(
    state: AbcUiState,
    viewModel: AbcViewModel,
    biometricAvailable: Boolean,
) {
    var destination by rememberSaveable {
        mutableStateOf(
            runCatching { AppDestination.valueOf(state.settings.lastDestination) }
                .getOrDefault(AppDestination.HOME),
        )
    }
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val screenStateHolder = rememberSaveableStateHolder()
    val selectDestination: (AppDestination) -> Unit = { next ->
        if (destination != next) {
            if (state.settings.interactionFeedback) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            destination = next
            viewModel.setLastDestination(next.name)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppDestination.entries.filter { it.showInNavigation }.forEach { item ->
                    val selected = destination == item
                    val pillWidth by animateDpAsState(if (selected) 58.dp else 40.dp, tween(220), label = "pill-width")
                    val pillElevation by animateDpAsState(if (selected) 4.dp else 0.dp, tween(220), label = "pill-elevation")
                    val iconScale by animateFloatAsState(if (selected) 1.08f else 1f, tween(220), label = "icon-scale")
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectDestination(item) },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .width(pillWidth)
                                    .height(if (item == AppDestination.MARKET) 40.dp else 36.dp)
                                    .shadow(pillElevation, RoundedCornerShape(20.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                        RoundedCornerShape(20.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier
                                        .size(if (item == AppDestination.MARKET) 29.dp else 24.dp)
                                        .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                                )
                            }
                        },
                        label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                    )
                }
            }
        },
    ) { contentPadding ->
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            )
        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                (fadeIn(tween(210)) + slideInHorizontally(tween(210)) { it / 14 }) togetherWith
                    (fadeOut(tween(130)) + slideOutHorizontally(tween(130)) { -it / 24 })
            },
            label = "Navegação",
        ) { currentDestination ->
        screenStateHolder.SaveableStateProvider(currentDestination.name) {
        when (currentDestination) {
            AppDestination.HOME -> DashboardScreen(
                portfolioName = state.selectedPortfolioName,
                portfolios = state.portfolios,
                selectedPortfolioId = state.selectedPortfolioId,
                snapshot = state.snapshot,
                history = state.history,
                settings = state.settings,
                feedStatus = state.feedStatus,
                onSelectPortfolio = viewModel::selectPortfolio,
                onAddEntry = { selectDestination(AppDestination.ADD) },
                modifier = screenModifier,
            )

            AppDestination.PORTFOLIOS -> PortfoliosScreen(
                portfolios = state.portfolios,
                trackedWallets = state.trackedWallets,
                settings = state.settings,
                onSelect = {
                    viewModel.selectPortfolio(it)
                    selectDestination(AppDestination.HOME)
                },
                onCreate = { name ->
                    viewModel.createPortfolio(name) { id -> viewModel.selectPortfolio(id) }
                },
                onDelete = viewModel::deletePortfolio,
                onCreateTracked = viewModel::createTrackedWallet,
                onDeleteTracked = viewModel::deleteTrackedWallet,
                onRefreshTracked = viewModel::refreshTrackedWallet,
                onRefreshAll = viewModel::refreshAllTrackedWallets,
                onUpdateCost = viewModel::updateTrackedWalletCost,
                onUpdateAssetCost = viewModel::updateTrackedWalletAssetCost,
                modifier = screenModifier,
            )

            AppDestination.MARKET -> MarketScreen(
                quotes = state.quotes,
                settings = state.settings,
                feedStatus = state.feedStatus,
                onLoadHistory = viewModel::loadMarketHistory,
                modifier = screenModifier,
            )

            AppDestination.ADD -> AddTransactionScreen(
                portfolios = state.portfolios,
                assets = state.assets,
                quotes = state.quotes,
                settings = state.settings,
                customAssetSymbols = state.customAssetSymbols,
                initialPortfolioId = state.selectedPortfolioId,
                onSave = viewModel::saveEntry,
                onDeleteCustomAsset = viewModel::deleteCustomAsset,
                onSaved = { selectDestination(AppDestination.HOME) },
                modifier = screenModifier,
            )

            AppDestination.ANALYSIS -> AnalysisScreen(
                portfolioName = state.selectedPortfolioName,
                snapshot = state.snapshot,
                history = state.history,
                proOwned = state.proStatus.owned,
                settings = state.settings,
                onAddEntry = { selectDestination(AppDestination.ADD) },
                modifier = screenModifier,
            )

            AppDestination.SETTINGS -> SettingsScreen(
                settings = state.settings,
                feedStatus = state.feedStatus,
                proStatus = state.proStatus,
                biometricAvailable = biometricAvailable,
                onThemeChange = viewModel::setTheme,
                onPaletteChange = viewModel::setColorPalette,
                onPriceRefreshSpeedChange = viewModel::setPriceRefreshSpeed,
                onCurrencyChange = viewModel::setDisplayCurrency,
                onHideBalancesChange = viewModel::setHideBalances,
                onBiometricChange = viewModel::setBiometricLock,
                onSecureScreenChange = viewModel::setSecureScreen,
                onInteractionFeedbackChange = viewModel::setInteractionFeedback,
                onPurchasePro = { (view.context as? Activity)?.let(viewModel::purchasePro) },
                onCreateBackup = viewModel::createBackup,
                onRestoreBackup = viewModel::restoreBackup,
                modifier = screenModifier,
            )
        }
        }
        }
    }
}

@Composable
private fun BiometricLockScreen(
    biometricAvailable: Boolean,
    promptRunning: Boolean,
    onUnlock: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp),
                )
            }
            Spacer(Modifier.size(20.dp))
            Text("Solfolio está bloqueado", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.size(8.dp))
            Text(
                if (biometricAvailable) {
                    "Autentique-se para visualizar seus portfólios."
                } else {
                    "A biometria configurada não está disponível."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.size(22.dp))
            Button(onClick = onUnlock, enabled = biometricAvailable && !promptRunning) {
                Text(if (promptRunning) "Aguardando…" else "Desbloquear")
            }
        }
    }
}
