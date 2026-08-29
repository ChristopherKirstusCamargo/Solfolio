package dev.zhar.abc.data.market

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

data class HistoricalCandle(
    val timestamp: Long,
    val lowUsd: Double,
    val highUsd: Double,
    val openUsd: Double,
    val closeUsd: Double,
)

class HistoricalPriceService(private val client: OkHttpClient) {
    private data class CachedHistory(val loadedAt: Long, val candles: List<HistoricalCandle>)
    private val cache = ConcurrentHashMap<String, CachedHistory>()

    suspend fun fetch(productId: String, days: Int): List<HistoricalCandle> = withContext(Dispatchers.IO) {
        val normalizedProduct = productId.trim().uppercase()
        require(PRODUCT.matches(normalizedProduct)) { "Ativo inválido." }
        val safeDays = days.coerceIn(7, 90)
        val key = "$normalizedProduct:$safeDays"
        cache[key]?.takeIf { System.currentTimeMillis() - it.loadedAt < CACHE_TTL_MS }?.candles?.let { return@withContext it }

        val end = Instant.now()
        val start = end.minusSeconds(safeDays * 86_400L)
        val url = "https://api.exchange.coinbase.com/products/$normalizedProduct/candles".toHttpUrl().newBuilder()
            .addQueryParameter("granularity", "86400")
            .addQueryParameter("start", start.toString())
            .addQueryParameter("end", end.toString())
            .build()
        val request = Request.Builder().url(url).header("Accept", "application/json").header("User-Agent", "Solfolio/0.6.2").build()
        val candles = client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "Histórico indisponível agora." }
            val rows = JSONArray(response.body?.string().orEmpty())
            buildList {
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONArray(index) ?: continue
                    if (row.length() < 5) continue
                    val timestamp = row.optLong(0)
                    val low = row.optDouble(1)
                    val high = row.optDouble(2)
                    val open = row.optDouble(3)
                    val close = row.optDouble(4)
                    if (timestamp > 0 && low > 0 && high > 0 && open > 0 && close > 0) {
                        add(HistoricalCandle(timestamp, low, high, open, close))
                    }
                }
            }.sortedBy { it.timestamp }
        }
        require(candles.size >= 2) { "Não há histórico suficiente para este ativo." }
        cache[key] = CachedHistory(System.currentTimeMillis(), candles)
        candles
    }

    companion object {
        private const val CACHE_TTL_MS = 30 * 60 * 1_000L
        private val PRODUCT = Regex("^[A-Z0-9]{2,12}-USD$")
    }
}
