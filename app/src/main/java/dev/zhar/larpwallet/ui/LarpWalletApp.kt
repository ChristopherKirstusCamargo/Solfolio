package dev.zhar.larpwallet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.zhar.larpwallet.ui.theme.AppBackground
import dev.zhar.larpwallet.ui.theme.CardSurface
import dev.zhar.larpwallet.ui.theme.MutedText
import dev.zhar.larpwallet.ui.theme.PurpleBright
import dev.zhar.larpwallet.ui.theme.SoftWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LarpWalletApp(viewModel: WalletViewModel) {
    val state = viewModel.state
    var selectedTab by remember { mutableStateOf(MainTab.WALLET) }
    var selectedAssetId by remember { mutableStateOf<String?>(null) }
    var activeSheet by remember { mutableStateOf<WalletSheet?>(null) }
    val selectedAsset = state.assets.firstOrNull { it.id == selectedAssetId }

    BackHandler(enabled = selectedAssetId != null) {
        selectedAssetId = null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (selectedAsset == null) {
                AppNavigationBar(selected = selectedTab, onSelected = { selectedTab = it })
            }
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(bottom = scaffoldPadding.calculateBottomPadding())
                .statusBarsPadding(),
        ) {
            SimulationStrip()
            AnimatedContent(
                targetState = selectedAsset,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "assetNavigation",
                modifier = Modifier.weight(1f),
            ) { asset ->
                if (asset != null) {
                    AssetDetailScreen(
                        asset = asset,
                        state = state,
                        onBack = { selectedAssetId = null },
                        onReceive = { activeSheet = WalletSheet.Receive },
                        onSend = { activeSheet = WalletSheet.Send },
                        onSwap = { activeSheet = WalletSheet.Swap },
                        onEdit = { activeSheet = WalletSheet.EditAsset(asset) },
                        onDelete = {
                            viewModel.removeAsset(asset.id)
                            selectedAssetId = null
                        },
                    )
                } else {
                    Crossfade(
                        targetState = selectedTab,
                        label = "mainNavigation",
                        modifier = Modifier.fillMaxSize(),
                    ) { tab ->
                        when (tab) {
                            MainTab.WALLET -> WalletScreen(
                                state = state,
                                onAssetClick = { selectedAssetId = it.id },
                                onEditTotal = { activeSheet = WalletSheet.EditTotal },
                                onAddAsset = { activeSheet = WalletSheet.EditAsset(null) },
                                onReceive = { activeSheet = WalletSheet.Receive },
                                onSend = { activeSheet = WalletSheet.Send },
                                onSwap = { activeSheet = WalletSheet.Swap },
                                onToggleBalance = { viewModel.setBalanceVisibility(!state.hideBalances) },
                            )
                            MainTab.COLLECTIBLES -> CollectiblesScreen(
                                state = state,
                                onAdd = { activeSheet = WalletSheet.AddCollectible },
                                onDelete = viewModel::removeCollectible,
                            )
                            MainTab.ACTIVITY -> ActivityScreen(state = state)
                            MainTab.SETTINGS -> SettingsScreen(
                                state = state,
                                onToggleCurrency = viewModel::setUseUsd,
                                onToggleHaptics = viewModel::setHaptics,
                                onEditProfile = { activeSheet = WalletSheet.EditProfile },
                                onExchangeRate = { activeSheet = WalletSheet.ExchangeRate },
                                onAbout = { activeSheet = WalletSheet.About },
                                onReset = { activeSheet = WalletSheet.ResetDemo },
                            )
                        }
                    }
                }
            }
        }
    }

    activeSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF17141C),
            contentColor = SoftWhite,
            dragHandle = null,
        ) {
            WalletSheetContent(
                route = sheet,
                state = state,
                viewModel = viewModel,
                onDismiss = { activeSheet = null },
            )
        }
    }
}

@Composable
private fun AppNavigationBar(selected: MainTab, onSelected: (MainTab) -> Unit) {
    NavigationBar(
        containerColor = CardSurface,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets.navigationBars,
    ) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                    )
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelLarge) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF17101F),
                    selectedTextColor = SoftWhite,
                    indicatorColor = PurpleBright,
                    unselectedIconColor = MutedText,
                    unselectedTextColor = MutedText,
                ),
            )
        }
    }
}

private val MainTab.icon: ImageVector
    get() = when (this) {
        MainTab.WALLET -> Icons.Outlined.AccountBalanceWallet
        MainTab.COLLECTIBLES -> Icons.Outlined.Collections
        MainTab.ACTIVITY -> Icons.Outlined.History
        MainTab.SETTINGS -> Icons.Outlined.Settings
    }
