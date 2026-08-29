package dev.zhar.abc

import android.app.Application
import dev.zhar.abc.data.PortfolioRepository
import dev.zhar.abc.data.SettingsStore
import dev.zhar.abc.data.local.AbcDatabase
import dev.zhar.abc.data.market.FxRateService
import dev.zhar.abc.data.market.BitcoinWalletService
import dev.zhar.abc.data.market.EthereumWalletService
import dev.zhar.abc.data.market.MarketPriceService
import dev.zhar.abc.data.market.SolanaWalletService
import dev.zhar.abc.data.market.HistoricalPriceService
import dev.zhar.abc.data.backup.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AbcApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AbcDatabase.get(this) }
    val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }
    val settingsStore by lazy { SettingsStore(this) }
    val repository by lazy { PortfolioRepository(database.dao()) }
    val marketPriceService by lazy { MarketPriceService(this, httpClient) }
    val solanaWalletService by lazy { SolanaWalletService(httpClient) }
    val bitcoinWalletService by lazy { BitcoinWalletService(httpClient) }
    val ethereumWalletService by lazy { EthereumWalletService(httpClient) }
    val historicalPriceService by lazy { HistoricalPriceService(httpClient) }
    val fxRateService by lazy { FxRateService(settingsStore, httpClient) }
    val backupManager by lazy { BackupManager(database.dao(), settingsStore) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { repository.seedDefaults() }
    }
}
