package dev.zhar.larpwallet.data

import android.content.Context
import dev.zhar.larpwallet.model.Collectible
import dev.zhar.larpwallet.model.SimTransaction
import dev.zhar.larpwallet.model.TokenAsset
import dev.zhar.larpwallet.model.TransactionKind
import dev.zhar.larpwallet.model.WalletState
import dev.zhar.larpwallet.model.defaultWalletState
import org.json.JSONArray
import org.json.JSONObject

class WalletRepository(context: Context) {
    private val preferences = context.getSharedPreferences("larp_wallet_v1", Context.MODE_PRIVATE)

    fun load(): WalletState {
        val raw = preferences.getString(KEY_STATE, null) ?: return defaultWalletState().also(::save)
        return runCatching { decode(JSONObject(raw)) }.getOrElse { defaultWalletState().also(::save) }
    }

    fun save(state: WalletState) {
        preferences.edit().putString(KEY_STATE, encode(state).toString()).apply()
    }

    private fun encode(state: WalletState): JSONObject = JSONObject().apply {
        put("accountName", state.accountName)
        put("hideBalances", state.hideBalances)
        put("useUsd", state.useUsd)
        put("usdBrlRate", state.usdBrlRate)
        put("haptics", state.haptics)
        put("assets", JSONArray().apply {
            state.assets.forEach { asset ->
                put(JSONObject().apply {
                    put("id", asset.id)
                    put("symbol", asset.symbol)
                    put("name", asset.name)
                    put("quantity", asset.quantity)
                    put("priceBrl", asset.priceBrl)
                    put("change24h", asset.change24h)
                    put("colorHex", asset.colorHex)
                })
            }
        })
        put("transactions", JSONArray().apply {
            state.transactions.forEach { transaction ->
                put(JSONObject().apply {
                    put("id", transaction.id)
                    put("kind", transaction.kind.name)
                    put("title", transaction.title)
                    put("subtitle", transaction.subtitle)
                    put("symbol", transaction.symbol)
                    put("amount", transaction.amount)
                    put("valueBrl", transaction.valueBrl)
                    put("timestamp", transaction.timestamp)
                })
            }
        })
        put("collectibles", JSONArray().apply {
            state.collectibles.forEach { collectible ->
                put(JSONObject().apply {
                    put("id", collectible.id)
                    put("name", collectible.name)
                    put("collection", collectible.collection)
                    put("accentHex", collectible.accentHex)
                })
            }
        })
    }

    private fun decode(root: JSONObject): WalletState {
        val assetsJson = root.optJSONArray("assets") ?: JSONArray()
        val assets = buildList {
            for (index in 0 until assetsJson.length()) {
                val item = assetsJson.getJSONObject(index)
                add(
                    TokenAsset(
                        id = item.optString("id"),
                        symbol = item.optString("symbol"),
                        name = item.optString("name"),
                        quantity = item.optDouble("quantity"),
                        priceBrl = item.optDouble("priceBrl"),
                        change24h = item.optDouble("change24h"),
                        colorHex = item.optString("colorHex", "#9C72FF"),
                    ),
                )
            }
        }

        val transactionsJson = root.optJSONArray("transactions") ?: JSONArray()
        val transactions = buildList {
            for (index in 0 until transactionsJson.length()) {
                val item = transactionsJson.getJSONObject(index)
                add(
                    SimTransaction(
                        id = item.optString("id"),
                        kind = runCatching { TransactionKind.valueOf(item.optString("kind")) }
                            .getOrDefault(TransactionKind.ADJUSTED),
                        title = item.optString("title"),
                        subtitle = item.optString("subtitle"),
                        symbol = item.optString("symbol"),
                        amount = item.optDouble("amount"),
                        valueBrl = item.optDouble("valueBrl"),
                        timestamp = item.optLong("timestamp"),
                    ),
                )
            }
        }

        val collectiblesJson = root.optJSONArray("collectibles") ?: JSONArray()
        val collectibles = buildList {
            for (index in 0 until collectiblesJson.length()) {
                val item = collectiblesJson.getJSONObject(index)
                add(
                    Collectible(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        collection = item.optString("collection"),
                        accentHex = item.optString("accentHex", "#9C72FF"),
                    ),
                )
            }
        }

        return WalletState(
            accountName = root.optString("accountName", "Conta de demonstração"),
            assets = assets,
            transactions = transactions,
            collectibles = collectibles,
            hideBalances = root.optBoolean("hideBalances", false),
            useUsd = root.optBoolean("useUsd", false),
            usdBrlRate = root.optDouble("usdBrlRate", 5.48).coerceAtLeast(0.01),
            haptics = root.optBoolean("haptics", true),
        )
    }

    private companion object {
        const val KEY_STATE = "wallet_state"
    }
}

