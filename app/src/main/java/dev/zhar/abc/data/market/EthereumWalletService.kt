package dev.zhar.abc.data.market

import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class EthereumWalletService(private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()) {

    suspend fun fetchHoldings(address: String): List<SolanaWalletHolding> = withContext(Dispatchers.IO) {
        val normalized = address.trim()
        require(isValidAddress(normalized)) { "Endereço público Ethereum inválido." }
        val calls = JSONArray().put(
            JSONObject().put("jsonrpc", "2.0").put("id", "native").put("method", "eth_getBalance")
                .put("params", JSONArray().put(normalized).put("latest")),
        )
        KNOWN_ERC20.forEach { token ->
            val data = "0x70a08231" + normalized.removePrefix("0x").lowercase().padStart(64, '0')
            calls.put(
                JSONObject().put("jsonrpc", "2.0").put("id", token.symbol).put("method", "eth_call")
                    .put("params", JSONArray().put(JSONObject().put("to", token.contract).put("data", data)).put("latest")),
            )
        }
        val request = Request.Builder().url(RPC_URL).post(calls.toString().toRequestBody(JSON)).build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "A rede Ethereum respondeu com HTTP ${response.code}." }
            val rows = JSONArray(response.body?.string().orEmpty())
            val results = buildMap {
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    if (!row.has("error")) put(row.getString("id"), row.optString("result"))
                }
            }
            buildList {
                hexAmount(results["native"], 18)?.takeIf { it > 0.0 }?.let {
                    add(SolanaWalletHolding("native-eth", "ETH", "Ethereum", it))
                }
                KNOWN_ERC20.forEach { token ->
                    hexAmount(results[token.symbol], token.decimals)?.takeIf { it > 0.0 }?.let {
                        add(SolanaWalletHolding(token.contract.lowercase(), token.symbol, token.name, it))
                    }
                }
            }
        }
    }

    private fun hexAmount(value: String?, decimals: Int): Double? = runCatching {
        val raw = BigDecimal(BigInteger(value.orEmpty().removePrefix("0x").ifBlank { "0" }, 16))
        raw.divide(BigDecimal.TEN.pow(decimals)).toDouble()
    }.getOrNull()

    companion object {
        private const val RPC_URL = "https://ethereum-rpc.publicnode.com"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val ADDRESS = Regex("^0x[0-9a-fA-F]{40}$")
        private data class Erc20(val symbol: String, val name: String, val contract: String, val decimals: Int)
        private val KNOWN_ERC20 = listOf(
            Erc20("USDT", "Tether USD", "0xdAC17F958D2ee523a2206206994597C13D831ec7", 6),
            Erc20("USDC", "USD Coin", "0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", 6),
        )
        fun isValidAddress(value: String): Boolean = ADDRESS.matches(value.trim())
    }
}
