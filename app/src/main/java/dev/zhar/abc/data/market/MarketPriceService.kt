package dev.zhar.abc.data.market

import android.content.Context
import dev.zhar.abc.domain.AssetQuote
import dev.zhar.abc.domain.FeedStatus
import dev.zhar.abc.domain.PriceRefreshSpeed
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.json.JSONArray

class MarketPriceService(context: Context, baseClient: OkHttpClient = OkHttpClient()) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = baseClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val cache = context.applicationContext.getSharedPreferences("market_quote_cache", Context.MODE_PRIVATE)
    private val _quotes = MutableStateFlow(loadCachedQuotes())
    val quotes: StateFlow<Map<String, AssetQuote>> = _quotes.asStateFlow()

    private val _status = MutableStateFlow(FeedStatus.OFFLINE)
    val status: StateFlow<FeedStatus> = _status.asStateFlow()

    private var products: List<String> = listOf("BTC-USD", "ETH-USD", "SOL-USD")
    private var webSocket: WebSocket? = null
    private var shouldRun = false
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null
    private var restRefreshJob: Job? = null
    @Volatile private var publishIntervalMs = PriceRefreshSpeed.MEDIUM.intervalMs
    @Volatile private var lastPublishedAt = 0L
    @Volatile private var lastCachedAt = 0L
    private val latestQuotes = mutableMapOf<String, AssetQuote>()

    init {
        latestQuotes.putAll(_quotes.value)
    }

    fun setRefreshSpeed(value: PriceRefreshSpeed) {
        publishIntervalMs = value.intervalMs
    }

    @Synchronized
    fun updateProducts(newProducts: List<String>) {
        val sanitized = newProducts
            .map { it.trim().uppercase() }
            .filter { it.endsWith("-USD") }
            .distinct()
        if (sanitized.isEmpty() || sanitized == products) return
        products = sanitized
        if (shouldRun) openSocket(restart = true)
    }

    @Synchronized
    fun start() {
        if (shouldRun && webSocket != null) return
        shouldRun = true
        reconnectAttempt = 0
        openSocket(restart = false)
        if (restRefreshJob?.isActive != true) {
            restRefreshJob = scope.launch {
                while (shouldRun) {
                    refreshRestSnapshot()
                    delay(60_000L)
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        shouldRun = false
        reconnectJob?.cancel()
        reconnectJob = null
        restRefreshJob?.cancel()
        restRefreshJob = null
        webSocket?.close(1000, "App em segundo plano")
        webSocket = null
        _status.value = FeedStatus.OFFLINE
    }

    @Synchronized
    private fun openSocket(restart: Boolean) {
        if (!shouldRun) return
        reconnectJob?.cancel()
        if (restart) webSocket?.close(1000, "Atualizando ativos")
        _status.value = if (reconnectAttempt == 0) FeedStatus.CONNECTING else FeedStatus.RECONNECTING
        val request = Request.Builder()
            .url("wss://advanced-trade-ws.coinbase.com")
            .build()
        webSocket = client.newWebSocket(request, listener)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempt = 0
            val productArray = products.joinToString(",") { "\"$it\"" }
            webSocket.send(
                "{\"type\":\"subscribe\",\"channel\":\"ticker\",\"product_ids\":[$productArray]}",
            )
            webSocket.send("{\"type\":\"subscribe\",\"channel\":\"heartbeats\"}")
            _status.value = FeedStatus.LIVE
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            runCatching { parseTicker(text) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (shouldRun) scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (shouldRun) scheduleReconnect()
        }
    }

    private fun parseTicker(message: String) {
        val root = JSONObject(message)
        if (root.optString("channel") != "ticker") return
        val events = root.optJSONArray("events") ?: return
        val now = System.currentTimeMillis()
        val updated = synchronized(latestQuotes) {
            if (latestQuotes.isEmpty()) latestQuotes.putAll(_quotes.value)
            latestQuotes
        }

        for (eventIndex in 0 until events.length()) {
            val tickers = events.optJSONObject(eventIndex)?.optJSONArray("tickers") ?: continue
            for (tickerIndex in 0 until tickers.length()) {
                val ticker = tickers.optJSONObject(tickerIndex) ?: continue
                val productId = ticker.optString("product_id")
                val symbol = productId.substringBefore("-").uppercase()
                val price = ticker.optString("price").toDoubleOrNull() ?: continue
                val change24h = ticker.optString("price_percent_chg_24_h").toDoubleOrNull()
                val previous = updated[symbol]
                val history = (previous?.recentPrices.orEmpty() + price).takeLast(42)
                updated[symbol] = AssetQuote(
                    symbol = symbol,
                    priceUsd = price,
                    change24hPercent = change24h ?: previous?.change24hPercent,
                    updatedAt = now,
                    recentPrices = history,
                )
            }
        }
        if (updated.isNotEmpty() && (lastPublishedAt == 0L || now - lastPublishedAt >= publishIntervalMs)) {
            _quotes.value = synchronized(latestQuotes) { latestQuotes.toMap() }
            lastPublishedAt = now
            if (lastCachedAt == 0L || now - lastCachedAt >= 30_000L) {
                lastCachedAt = now
                saveCachedQuotes(_quotes.value)
            }
        }
    }

    /** One inexpensive request fills assets that are not present in the ticker stream. */
    private fun refreshRestSnapshot() {
        runCatching {
            val response = client.newCall(
                Request.Builder()
                    .url("https://api.coinbase.com/v2/exchange-rates?currency=USD")
                    .header("User-Agent", "Solfolio/0.7.0")
                    .build(),
            ).execute()
            response.use {
                if (!it.isSuccessful) return@runCatching
                val rates = JSONObject(it.body?.string().orEmpty())
                    .optJSONObject("data")
                    ?.optJSONObject("rates")
                    ?: return@runCatching
                val now = System.currentTimeMillis()
                synchronized(latestQuotes) {
                    products.forEach { product ->
                        val symbol = product.substringBefore('-')
                        val unitsPerUsd = rates.optString(symbol).toDoubleOrNull() ?: return@forEach
                        if (!unitsPerUsd.isFinite() || unitsPerUsd <= 0.0) return@forEach
                        val price = 1.0 / unitsPerUsd
                        val previous = latestQuotes[symbol]
                        latestQuotes[symbol] = AssetQuote(
                            symbol = symbol,
                            priceUsd = price,
                            change24hPercent = previous?.change24hPercent,
                            updatedAt = now,
                            recentPrices = (previous?.recentPrices.orEmpty() + price).takeLast(42),
                        )
                    }
                    listOf("USDC", "USDT").forEach { symbol ->
                        latestQuotes.putIfAbsent(symbol, AssetQuote(symbol, 1.0, 0.0, now))
                    }
                    _quotes.value = latestQuotes.toMap()
                }
                lastPublishedAt = now
                saveCachedQuotes(_quotes.value)
            }
        }
    }

    private fun loadCachedQuotes(): Map<String, AssetQuote> = runCatching {
        val rows = JSONArray(cache.getString("quotes", "[]"))
        buildMap {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val symbol = row.optString("symbol").uppercase()
                val price = row.optDouble("price").takeIf { it > 0.0 } ?: continue
                val historyJson = row.optJSONArray("history") ?: JSONArray()
                val history = buildList {
                    for (historyIndex in 0 until historyJson.length()) {
                        historyJson.optDouble(historyIndex).takeIf { it > 0.0 }?.let(::add)
                    }
                }
                put(
                    symbol,
                    AssetQuote(
                        symbol = symbol,
                        priceUsd = price,
                        change24hPercent = row.optDouble("change").takeUnless { it.isNaN() },
                        updatedAt = row.optLong("updatedAt"),
                        recentPrices = history,
                    ),
                )
            }
        }
    }.getOrDefault(emptyMap())

    private fun saveCachedQuotes(quotes: Map<String, AssetQuote>) {
        scope.launch {
            val rows = JSONArray()
            quotes.values.sortedByDescending { it.updatedAt }.take(80).forEach { quote ->
                rows.put(
                    JSONObject().apply {
                        put("symbol", quote.symbol)
                        put("price", quote.priceUsd)
                        quote.change24hPercent?.let { put("change", it) }
                        put("updatedAt", quote.updatedAt)
                        put("history", JSONArray(quote.recentPrices))
                    },
                )
            }
            cache.edit().putString("quotes", rows.toString()).apply()
        }
    }

    @Synchronized
    private fun scheduleReconnect() {
        if (!shouldRun || reconnectJob?.isActive == true) return
        webSocket = null
        reconnectAttempt += 1
        _status.value = FeedStatus.RECONNECTING
        val delayMs = (1_500L * reconnectAttempt.coerceAtMost(10)).coerceAtMost(15_000L)
        reconnectJob = scope.launch {
            delay(delayMs)
            openSocket(restart = false)
        }
    }
}
