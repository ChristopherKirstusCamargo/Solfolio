package dev.zhar.abc.data.market

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class BitcoinWalletService(private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()) {

    suspend fun fetchHoldings(address: String): List<SolanaWalletHolding> = withContext(Dispatchers.IO) {
        val normalized = address.trim()
        require(isValidAddress(normalized)) { "Endereço público Bitcoin inválido." }
        val request = Request.Builder().url("https://blockstream.info/api/address/$normalized").get().build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "A rede Bitcoin respondeu com HTTP ${response.code}." }
            val root = JSONObject(response.body?.string().orEmpty())
            fun balance(group: String): Long {
                val stats = root.getJSONObject(group)
                return stats.getLong("funded_txo_sum") - stats.getLong("spent_txo_sum")
            }
            val satoshis = balance("chain_stats") + balance("mempool_stats")
            if (satoshis <= 0L) emptyList() else listOf(SolanaWalletHolding("native-btc", "BTC", "Bitcoin", satoshis / 100_000_000.0))
        }
    }

    companion object {
        private val LEGACY = Regex("^[13][1-9A-HJ-NP-Za-km-z]{25,34}$")
        private val SEGWIT = Regex("^(bc1)[ac-hj-np-z02-9]{11,71}$", RegexOption.IGNORE_CASE)
        fun isValidAddress(value: String): Boolean = value.trim().let { LEGACY.matches(it) || SEGWIT.matches(it) }
    }
}
