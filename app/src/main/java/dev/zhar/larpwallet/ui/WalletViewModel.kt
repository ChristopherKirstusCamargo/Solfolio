package dev.zhar.larpwallet.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import dev.zhar.larpwallet.data.WalletRepository
import dev.zhar.larpwallet.model.Collectible
import dev.zhar.larpwallet.model.SimTransaction
import dev.zhar.larpwallet.model.TokenAsset
import dev.zhar.larpwallet.model.TransactionKind
import dev.zhar.larpwallet.model.WalletState
import dev.zhar.larpwallet.model.defaultWalletState
import java.util.Locale

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WalletRepository(application)

    var state by mutableStateOf(repository.load())
        private set

    fun setAccountName(name: String) = update { it.copy(accountName = name.trim().ifBlank { it.accountName }) }

    fun setBalanceVisibility(hidden: Boolean) = update { it.copy(hideBalances = hidden) }

    fun setUseUsd(enabled: Boolean) = update { it.copy(useUsd = enabled) }

    fun setExchangeRate(rate: Double) = update { it.copy(usdBrlRate = rate.coerceAtLeast(0.01)) }

    fun setHaptics(enabled: Boolean) = update { it.copy(haptics = enabled) }

    fun setPortfolioTotal(targetBrl: Double) {
        val safeTarget = targetBrl.coerceAtLeast(0.0)
        update { current ->
            val currentTotal = current.totalBrl
            val assets = when {
                current.assets.isEmpty() -> listOf(
                    TokenAsset(
                        symbol = "BRL",
                        name = "Saldo demonstrativo",
                        quantity = safeTarget,
                        priceBrl = 1.0,
                        change24h = 0.0,
                        colorHex = "#A98CFF",
                    ),
                )
                currentTotal <= 0.0 -> current.assets.mapIndexed { index, asset ->
                    if (index == 0) asset.copy(quantity = safeTarget / asset.priceBrl.coerceAtLeast(0.01)) else asset.copy(quantity = 0.0)
                }
                else -> {
                    val multiplier = safeTarget / currentTotal
                    current.assets.map { it.copy(quantity = it.quantity * multiplier) }
                }
            }
            current.copy(
                assets = assets,
                transactions = listOf(
                    SimTransaction(
                        kind = TransactionKind.ADJUSTED,
                        title = "Saldo ajustado",
                        subtitle = "Edição manual do cenário",
                        symbol = "BRL",
                        amount = safeTarget,
                        valueBrl = safeTarget,
                    ),
                ) + current.transactions,
            )
        }
    }

    fun upsertAsset(
        existingId: String?,
        symbol: String,
        name: String,
        quantity: Double,
        priceBrl: Double,
        change24h: Double,
        colorHex: String,
    ) {
        val normalizedSymbol = symbol.trim().uppercase(Locale.ROOT).take(10)
        if (normalizedSymbol.isBlank()) return
        update { current ->
            val item = TokenAsset(
                id = existingId ?: java.util.UUID.randomUUID().toString(),
                symbol = normalizedSymbol,
                name = name.trim().ifBlank { normalizedSymbol },
                quantity = quantity.coerceAtLeast(0.0),
                priceBrl = priceBrl.coerceAtLeast(0.0),
                change24h = change24h.coerceIn(-999.0, 999.0),
                colorHex = colorHex,
            )
            val assets = current.assets.toMutableList()
            val index = assets.indexOfFirst { it.id == existingId }
            if (index >= 0) assets[index] = item else assets.add(item)
            current.copy(
                assets = assets,
                transactions = listOf(
                    SimTransaction(
                        kind = TransactionKind.ADJUSTED,
                        title = if (index >= 0) "Ativo editado" else "Ativo adicionado",
                        subtitle = "Valor fictício definido manualmente",
                        symbol = item.symbol,
                        amount = item.quantity,
                        valueBrl = item.valueBrl,
                    ),
                ) + current.transactions,
            )
        }
    }

    fun removeAsset(id: String) = update { current ->
        current.copy(assets = current.assets.filterNot { it.id == id })
    }

    fun receive(symbol: String, amount: Double) {
        if (amount <= 0.0) return
        update { current ->
            val asset = current.assets.firstOrNull { it.symbol == symbol } ?: return@update current
            current.copy(
                assets = current.assets.map { if (it.id == asset.id) it.copy(quantity = it.quantity + amount) else it },
                transactions = listOf(
                    SimTransaction(
                        kind = TransactionKind.RECEIVED,
                        title = "Recebido",
                        subtitle = "Transferência fictícia",
                        symbol = asset.symbol,
                        amount = amount,
                        valueBrl = amount * asset.priceBrl,
                    ),
                ) + current.transactions,
            )
        }
    }

    fun send(symbol: String, amount: Double, destination: String): Boolean {
        val asset = state.assets.firstOrNull { it.symbol == symbol } ?: return false
        if (amount <= 0.0 || amount > asset.quantity) return false
        update { current ->
            current.copy(
                assets = current.assets.map { if (it.id == asset.id) it.copy(quantity = it.quantity - amount) else it },
                transactions = listOf(
                    SimTransaction(
                        kind = TransactionKind.SENT,
                        title = "Enviado",
                        subtitle = destination.trim().ifBlank { "Destino fictício" }.take(28),
                        symbol = asset.symbol,
                        amount = -amount,
                        valueBrl = -(amount * asset.priceBrl),
                    ),
                ) + current.transactions,
            )
        }
        return true
    }

    fun swap(fromSymbol: String, toSymbol: String, amount: Double): Boolean {
        if (fromSymbol == toSymbol || amount <= 0.0) return false
        val from = state.assets.firstOrNull { it.symbol == fromSymbol } ?: return false
        val to = state.assets.firstOrNull { it.symbol == toSymbol } ?: return false
        if (amount > from.quantity || from.priceBrl <= 0.0 || to.priceBrl <= 0.0) return false
        val received = amount * from.priceBrl / to.priceBrl
        update { current ->
            current.copy(
                assets = current.assets.map { asset ->
                    when (asset.id) {
                        from.id -> asset.copy(quantity = asset.quantity - amount)
                        to.id -> asset.copy(quantity = asset.quantity + received)
                        else -> asset
                    }
                },
                transactions = listOf(
                    SimTransaction(
                        kind = TransactionKind.SWAPPED,
                        title = "Troca simulada",
                        subtitle = "${from.symbol} → ${to.symbol}",
                        symbol = to.symbol,
                        amount = received,
                        valueBrl = amount * from.priceBrl,
                    ),
                ) + current.transactions,
            )
        }
        return true
    }

    fun addCollectible(name: String, collection: String, accentHex: String) {
        if (name.isBlank()) return
        update { current ->
            current.copy(
                collectibles = current.collectibles + Collectible(
                    name = name.trim(),
                    collection = collection.trim().ifBlank { "Coleção simulada" },
                    accentHex = accentHex,
                ),
            )
        }
    }

    fun removeCollectible(id: String) = update { current ->
        current.copy(collectibles = current.collectibles.filterNot { it.id == id })
    }

    fun resetDemo() {
        state = defaultWalletState()
        repository.save(state)
    }

    private inline fun update(transform: (WalletState) -> WalletState) {
        state = transform(state)
        repository.save(state)
    }
}
