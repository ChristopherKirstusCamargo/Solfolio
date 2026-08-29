package dev.zhar.abc.data.market

import dev.zhar.abc.data.SettingsStore
import dev.zhar.abc.domain.AppSettings
import dev.zhar.abc.domain.DisplayCurrency
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class FxRateService(
    private val settingsStore: SettingsStore,
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build(),
) {

    suspend fun refreshIfNeeded(settings: AppSettings) {
        val sixHours = TimeUnit.HOURS.toMillis(6)
        if (System.currentTimeMillis() - settings.fxUpdatedAt < sixHours) return
        val rates = fetchLatestRates()
        if (rates.size >= 10) settingsStore.updateFxRates(rates)
    }

    private suspend fun fetchLatestRates(): Map<DisplayCurrency, Double> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("https://api.coinbase.com/v2/exchange-rates?currency=USD")
                .header("Accept", "application/json").header("User-Agent", "Solfolio/0.7.0").get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyMap()
                val rates = JSONObject(response.body?.string().orEmpty()).optJSONObject("data")?.optJSONObject("rates") ?: return@use emptyMap()
                buildMap {
                    DisplayCurrency.entries.forEach { currency ->
                        val rate = if (currency == DisplayCurrency.USD) 1.0 else rates.optString(currency.currencyCode).toDoubleOrNull()
                        if (rate != null && rate.isFinite() && rate > 0.0) put(currency, rate)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }
}
