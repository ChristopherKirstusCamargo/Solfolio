package dev.zhar.abc.data.market

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class SolanaWalletHolding(val mint: String, val symbol: String, val name: String, val quantity: Double)

class SolanaWalletService(
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build(),
    private val rpcUrls: List<String> = listOf("https://solana-rpc.publicnode.com", "https://api.mainnet-beta.solana.com"),
) {

    suspend fun fetchHoldings(address: String): List<SolanaWalletHolding> = withContext(Dispatchers.IO) {
        require(isValidAddress(address)) { "Endereço público Solana inválido." }
        val totals = linkedMapOf<String, Double>()
        val lamports = rpc("getBalance", JSONArray().put(address).put(JSONObject().put("commitment", "confirmed")))
            .getJSONObject("result").getLong("value")
        if (lamports > 0L) totals[SOL_MINT] = lamports / 1_000_000_000.0
        listOf(TOKEN_PROGRAM, TOKEN_2022_PROGRAM).forEach { programId ->
            val params = JSONArray().put(address).put(JSONObject().put("programId", programId))
                .put(JSONObject().put("encoding", "jsonParsed").put("commitment", "confirmed"))
            val accounts = rpc("getTokenAccountsByOwner", params).getJSONObject("result").getJSONArray("value")
            for (index in 0 until accounts.length()) {
                val info = accounts.getJSONObject(index).getJSONObject("account").getJSONObject("data")
                    .getJSONObject("parsed").getJSONObject("info")
                val mint = info.getString("mint")
                val quantity = info.getJSONObject("tokenAmount").optString("uiAmountString").toDoubleOrNull() ?: 0.0
                if (quantity > 0.0) totals[mint] = (totals[mint] ?: 0.0) + quantity
            }
        }
        totals.map { (mint, quantity) ->
            val metadata = KNOWN_TOKENS[mint] ?: TokenMetadata("SPL-${mint.take(4).uppercase()}", "Token ${shortAddress(mint)}")
            SolanaWalletHolding(mint, metadata.symbol, metadata.name, quantity)
        }
    }

    private fun rpc(method: String, params: JSONArray): JSONObject {
        val payload = JSONObject().put("jsonrpc", "2.0").put("id", method).put("method", method).put("params", params).toString()
        var lastFailure: Throwable? = null
        rpcUrls.distinct().forEach { url ->
            val result = runCatching {
                val request = Request.Builder().url(url).post(payload.toRequestBody(JSON_MEDIA_TYPE)).header("Accept", "application/json").build()
                client.newCall(request).execute().use { response ->
                    require(response.isSuccessful) { "HTTP ${response.code}" }
                    val root = JSONObject(response.body?.string().orEmpty())
                    val error = root.optJSONObject("error")
                    require(error == null) { error?.optString("message").orEmpty().ifBlank { "Falha no RPC" } }
                    root
                }
            }
            result.onSuccess { return it }.onFailure { lastFailure = it }
        }
        throw IllegalStateException("Não foi possível consultar a rede Solana.", lastFailure)
    }

    companion object {
        private const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        private const val TOKEN_2022_PROGRAM = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"
        private const val SOL_MINT = "native-sol"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val BASE58 = Regex("^[1-9A-HJ-NP-Za-km-z]{32,44}$")
        private data class TokenMetadata(val symbol: String, val name: String)
        private val KNOWN_TOKENS = mapOf(
            SOL_MINT to TokenMetadata("SOL", "Solana"),
            "So11111111111111111111111111111111111111112" to TokenMetadata("SOL", "Wrapped Solana"),
            "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v" to TokenMetadata("USDC", "USD Coin"),
            "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB" to TokenMetadata("USDT", "Tether USD"),
        )
        fun isValidAddress(value: String): Boolean = BASE58.matches(value.trim())
        fun shortAddress(value: String): String = if (value.length <= 12) value else "${value.take(5)}…${value.takeLast(5)}"
    }
}
