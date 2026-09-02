package dev.zhar.larpwallet.model

import java.util.UUID

data class TokenAsset(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val name: String,
    val quantity: Double,
    val priceBrl: Double,
    val change24h: Double,
    val colorHex: String,
) {
    val valueBrl: Double get() = quantity * priceBrl
}

enum class TransactionKind(val label: String) {
    RECEIVED("Recebido"),
    SENT("Enviado"),
    SWAPPED("Troca"),
    ADJUSTED("Ajuste"),
}

data class SimTransaction(
    val id: String = UUID.randomUUID().toString(),
    val kind: TransactionKind,
    val title: String,
    val subtitle: String,
    val symbol: String,
    val amount: Double,
    val valueBrl: Double,
    val timestamp: Long = System.currentTimeMillis(),
)

data class Collectible(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val collection: String,
    val accentHex: String,
)

data class WalletState(
    val accountName: String = "Conta de demonstração",
    val assets: List<TokenAsset> = emptyList(),
    val transactions: List<SimTransaction> = emptyList(),
    val collectibles: List<Collectible> = emptyList(),
    val hideBalances: Boolean = false,
    val useUsd: Boolean = false,
    val usdBrlRate: Double = 5.48,
    val haptics: Boolean = true,
) {
    val totalBrl: Double get() = assets.sumOf { it.valueBrl }
    val weightedChange24h: Double
        get() {
            val total = totalBrl
            return if (total <= 0.0) 0.0 else assets.sumOf { it.valueBrl * it.change24h } / total
        }
}

fun defaultWalletState(): WalletState = WalletState(
    assets = listOf(
        TokenAsset(
            symbol = "SOL",
            name = "Solana",
            quantity = 16.8429,
            priceBrl = 934.80,
            change24h = 3.84,
            colorHex = "#9C72FF",
        ),
        TokenAsset(
            symbol = "BTC",
            name = "Bitcoin",
            quantity = 0.0824,
            priceBrl = 617_450.00,
            change24h = 1.36,
            colorHex = "#F7931A",
        ),
        TokenAsset(
            symbol = "USDC",
            name = "USD Coin",
            quantity = 1_240.00,
            priceBrl = 5.48,
            change24h = 0.04,
            colorHex = "#2775CA",
        ),
        TokenAsset(
            symbol = "JUP",
            name = "Jupiter",
            quantity = 420.00,
            priceBrl = 5.92,
            change24h = -2.15,
            colorHex = "#67D8C3",
        ),
    ),
    transactions = listOf(
        SimTransaction(
            kind = TransactionKind.RECEIVED,
            title = "Recebido",
            subtitle = "Cenário demonstrativo",
            symbol = "SOL",
            amount = 3.5,
            valueBrl = 3_271.80,
            timestamp = System.currentTimeMillis() - 2 * 60 * 60 * 1000,
        ),
        SimTransaction(
            kind = TransactionKind.SWAPPED,
            title = "Troca simulada",
            subtitle = "USDC → SOL",
            symbol = "SOL",
            amount = 1.2,
            valueBrl = 1_121.76,
            timestamp = System.currentTimeMillis() - 26 * 60 * 60 * 1000,
        ),
    ),
    collectibles = listOf(
        Collectible(name = "Noite #084", collection = "LARP Originals", accentHex = "#9C72FF"),
        Collectible(name = "Órbita #021", collection = "Demo Shapes", accentHex = "#4AD9FF"),
    ),
)

