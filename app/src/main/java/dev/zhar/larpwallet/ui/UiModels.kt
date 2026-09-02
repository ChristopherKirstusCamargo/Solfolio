package dev.zhar.larpwallet.ui

import dev.zhar.larpwallet.model.TokenAsset

enum class MainTab(val label: String) {
    WALLET("Carteira"),
    COLLECTIBLES("Coleções"),
    ACTIVITY("Atividade"),
    SETTINGS("Ajustes"),
}

sealed interface WalletSheet {
    data object EditTotal : WalletSheet
    data class EditAsset(val asset: TokenAsset?) : WalletSheet
    data object Receive : WalletSheet
    data object Send : WalletSheet
    data object Swap : WalletSheet
    data object AddCollectible : WalletSheet
    data object EditProfile : WalletSheet
    data object ExchangeRate : WalletSheet
    data object ResetDemo : WalletSheet
    data object About : WalletSheet
}

