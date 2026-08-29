package dev.zhar.abc.data.market

import dev.zhar.abc.data.SettingsStore
import dev.zhar.abc.domain.AppSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
        val rate = fetchLatestBrlPerUsd() ?: return
        if (rate in 2.0..10.0) settingsStore.updateFxRate(rate)
    }

    private suspend fun fetchLatestBrlPerUsd(): Double? = withContext(Dispatchers.IO) {
        runCatching {
            val formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy")
            val end = LocalDate.now().format(formatter)
            val start = LocalDate.now().minusDays(10).format(formatter)
            val query = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/" +
                "CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)" +
                "?@dataInicial='$start'&@dataFinalCotacao='$end'" +
                "&%24orderby=dataHoraCotacao%20desc&%24top=1&%24format=json"
            val request = Request.Builder().url(query).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val item = JSONObject(body).optJSONArray("value")?.optJSONObject(0) ?: return@use null
                val buy = item.optDouble("cotacaoCompra", Double.NaN)
                val sell = item.optDouble("cotacaoVenda", Double.NaN)
                if (buy.isNaN() || sell.isNaN()) null else (buy + sell) / 2.0
            }
        }.getOrNull()
    }
}
