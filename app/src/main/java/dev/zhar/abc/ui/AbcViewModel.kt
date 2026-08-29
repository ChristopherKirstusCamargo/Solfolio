package dev.zhar.abc.ui

import android.app.Application
import android.app.Activity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.zhar.abc.AbcApplication
import dev.zhar.abc.data.*
import dev.zhar.abc.data.local.*
import dev.zhar.abc.data.market.*
import dev.zhar.abc.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class PortfolioView(val id: Long, val name: String, val snapshot: PortfolioSnapshot, val entryCount: Int, val trackedWalletCount: Int)
data class WalletAssetView(val mint: String, val symbol: String, val name: String, val quantity: Double, val valueUsd: Double, val hasPrice: Boolean, val costBasisUsd: Double?)
data class TrackedWalletView(
    val id: Long,
    val portfolioId: Long,
    val portfolioName: String,
    val label: String,
    val address: String,
    val network: WalletNetwork,
    val assets: List<WalletAssetView>,
    val hiddenAssetCount: Int,
    val currentValueUsd: Double,
    val basisUsd: Double?,
    val basisIsExact: Boolean,
    val pnlUsd: Double?,
    val lastSyncAt: Long,
    val syncing: Boolean,
    val error: String?,
)
data class AbcUiState(
    val portfolios: List<PortfolioView> = emptyList(),
    val trackedWallets: List<TrackedWalletView> = emptyList(),
    val selectedPortfolioId: Long? = null,
    val selectedPortfolioName: String = "Todos os portfólios",
    val snapshot: PortfolioSnapshot = PortfolioSnapshot(),
    val assets: List<AssetDefinition> = emptyList(),
    val customAssetSymbols: Set<String> = emptySet(),
    val quotes: Map<String, AssetQuote> = emptyMap(),
    val settings: AppSettings = AppSettings(),
    val feedStatus: FeedStatus = FeedStatus.OFFLINE,
    val history: List<PortfolioHistoryPoint> = emptyList(),
    val proStatus: ProStatus = ProStatus(),
    val isReady: Boolean = false,
)
private data class WalletData(val wallets: List<TrackedWalletEntity>, val assets: List<TrackedWalletAssetEntity>)
private data class CoreData(val portfolios: List<PortfolioEntity>, val ledger: List<LedgerEntry>, val assets: List<AssetEntity>, val quotes: Map<String, AssetQuote>, val walletData: WalletData)
private data class SelectionData(val portfolioId: Long?, val settings: AppSettings, val history: List<PortfolioHistoryPoint>)

class AbcViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AbcApplication
    private val repository = app.repository
    private val settingsStore = app.settingsStore
    private val market = app.marketPriceService
    private val fxRateService = app.fxRateService
    private val backupManager = app.backupManager
    private val selectedPortfolioId = MutableStateFlow<Long?>(null)
    private val syncingWallets = MutableStateFlow<Set<Long>>(emptySet())
    private val walletErrors = MutableStateFlow<Map<Long, String>>(emptyMap())
    private var snapshotJob: Job? = null
    private val walletSyncSlots = Semaphore(3)
    private val supportedMarketSymbols = AssetCatalog.defaults.map { it.symbol }.toSet()
    private val walletData = combine(repository.trackedWallets, repository.trackedWalletAssets) { wallets, assets -> WalletData(wallets, assets) }
    private val coreData = combine(repository.portfolios, repository.ledger, repository.assets, market.quotes, walletData) { p, l, a, q, w -> CoreData(p, l, a, q, w) }
    private val selectedHistory = selectedPortfolioId.flatMapLatest(repository::portfolioHistory)
    private val selectionData = combine(selectedPortfolioId, settingsStore.settings, selectedHistory) { id, settings, history ->
        SelectionData(id, settings, history)
    }

    val uiState: StateFlow<AbcUiState> = combine(
        coreData,
        selectionData,
        market.status,
        combine(syncingWallets, walletErrors) { syncing, errors -> syncing to errors },
        app.proBillingManager.status,
    ) { core, selection, status, walletStatus, proStatus ->
        val selectedId = selection.portfolioId
        val settings = selection.settings
        val history = selection.history
        val names = core.portfolios.associate { it.id to it.name }
        val entriesByPortfolio = core.ledger.groupBy { it.portfolioId }
        val walletsByPortfolio = core.walletData.wallets.groupBy { it.portfolioId }
        val walletAssetsByWallet = core.walletData.assets.groupBy { it.walletId }
        val walletViews = core.walletData.wallets.map { wallet ->
            walletView(wallet, walletAssetsByWallet[wallet.id].orEmpty(), core.quotes, names, walletStatus.first, walletStatus.second)
        }
        val portfolioViews = core.portfolios.map { portfolio ->
            val entries = entriesByPortfolio[portfolio.id].orEmpty()
            val wallets = walletsByPortfolio[portfolio.id].orEmpty()
            val positions = trackedPositions(wallets, walletAssetsByWallet, core.quotes)
            PortfolioView(portfolio.id, portfolio.name, PortfolioCalculator.calculate(entries, core.quotes, positions, names), entries.size, wallets.size)
        }
        val selectedSnapshot = portfolioViews.firstOrNull { it.id == selectedId }?.snapshot
            ?: PortfolioCalculator.calculate(
                core.ledger,
                core.quotes,
                trackedPositions(core.walletData.wallets, walletAssetsByWallet, core.quotes),
                names,
            )
        AbcUiState(
            portfolios = portfolioViews,
            trackedWallets = walletViews,
            selectedPortfolioId = selectedId,
            selectedPortfolioName = portfolioViews.firstOrNull { it.id == selectedId }?.name ?: "Todos os portfólios",
            snapshot = selectedSnapshot,
            assets = core.assets.map { AssetDefinition(it.symbol, it.name, it.productId) },
            customAssetSymbols = core.assets.filter { it.isCustom }.map { it.symbol }.toSet(),
            quotes = core.quotes,
            settings = settings,
            feedStatus = status,
            history = history,
            proStatus = proStatus.copy(owned = proStatus.owned || settings.proEntitled),
            // Room has emitted the persisted state. Defaults can continue seeding without
            // trapping the whole interface behind an indefinite loading screen.
            isReady = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AbcUiState())

    init {
        viewModelScope.launch {
            combine(repository.assets, repository.trackedWalletAssets) { assets, walletAssets ->
                (assets.map { it.productId } + walletAssets.map { "${it.symbol}-USD" }.filterNot { it.startsWith("SPL-") }).distinct()
            }.collect(market::updateProducts)
        }
        viewModelScope.launch {
            settingsStore.settings
                .map { it.priceRefreshSpeed }
                .distinctUntilChanged()
                .collect(market::setRefreshSpeed)
        }
    }

    private fun trackedPositions(wallets: List<TrackedWalletEntity>, assetsByWallet: Map<Long, List<TrackedWalletAssetEntity>>, quotes: Map<String, AssetQuote>): List<TrackedPosition> = wallets.flatMap { wallet ->
        val walletRows = assetsByWallet[wallet.id].orEmpty().filter { it.symbol.uppercase() in supportedMarketSymbols }
        val liveRows = walletRows.map { row -> row to priceFor(row.symbol, quotes) }
        val knownValue = liveRows.sumOf { (row, price) -> row.quantity * price }
        val missingRows = liveRows.filter { it.second <= 0.0 }
        val fallbackValue = ((wallet.baselineValueUsd ?: 0.0) - knownValue).coerceAtLeast(0.0)
        val fallbackRow = missingRows.singleOrNull()
        val pricedRows = liveRows.mapNotNull { (row, livePrice) ->
            val fallbackPrice = if (row == fallbackRow && row.quantity > 0.0) fallbackValue / row.quantity else 0.0
            val price = livePrice.takeIf { it > 0.0 } ?: fallbackPrice.takeIf { it > 0.0 } ?: return@mapNotNull null
            Triple(row, row.quantity * price, fallbackPrice.takeIf { livePrice <= 0.0 && it > 0.0 })
        }
        val total = pricedRows.sumOf { it.second }
        val basis = wallet.costBasisUsd ?: wallet.baselineValueUsd ?: total
        val explicitCosts = pricedRows.sumOf { (asset, _, _) -> asset.costBasisUsd ?: 0.0 }
        val unassignedValue = pricedRows.filter { (asset, _, _) -> asset.costBasisUsd == null }.sumOf { it.second }
        val remainingBasis = (basis - explicitCosts).coerceAtLeast(0.0)
        pricedRows.map { (asset, value, fallbackPrice) ->
            val inferredShare = if (unassignedValue > 0.0) value / unassignedValue else 0.0
            val assetBasis = asset.costBasisUsd ?: (remainingBasis * inferredShare)
            TrackedPosition(wallet.id, wallet.label, asset.symbol, asset.name, asset.quantity, assetBasis, asset.costBasisUsd != null || wallet.costBasisUsd != null, fallbackPrice)
        }
    }

    private fun walletView(
        wallet: TrackedWalletEntity,
        walletAssets: List<TrackedWalletAssetEntity>,
        quotes: Map<String, AssetQuote>,
        names: Map<Long, String>,
        syncing: Set<Long>,
        errors: Map<Long, String>,
    ): TrackedWalletView {
        val mapped = walletAssets.map { row ->
            val price = priceFor(row.symbol, quotes)
            WalletAssetView(row.mint, row.symbol, friendlyName(row.symbol, row.name), row.quantity, row.quantity * price, price > 0.0, row.costBasisUsd)
        }
        val visible = mapped.filter { it.hasPrice }.sortedByDescending { it.valueUsd }
        val value = visible.sumOf { it.valueUsd }
        val basis = wallet.costBasisUsd ?: wallet.baselineValueUsd
        val network = runCatching { WalletNetwork.valueOf(wallet.network) }.getOrDefault(WalletNetwork.SOLANA)
        return TrackedWalletView(
            wallet.id, wallet.portfolioId, names[wallet.portfolioId] ?: "Portfólio", wallet.label, wallet.address, network,
            visible, mapped.count { !it.hasPrice }, value, basis, wallet.costBasisUsd != null, basis?.let { value - it },
            wallet.lastSyncAt, wallet.id in syncing, errors[wallet.id],
        )
    }

    private fun friendlyName(symbol: String, fallback: String) = when (symbol.uppercase()) {
        "BTC" -> "Bitcoin"; "ETH" -> "Ethereum"; "SOL" -> "Solana"; "USDC" -> "USD Coin"; "USDT" -> "Tether USD"
        else -> fallback.takeIf { !it.startsWith("Token ") } ?: "Token não identificado"
    }

    private fun priceFor(symbol: String, quotes: Map<String, AssetQuote>): Double = quotes[symbol.uppercase()]?.priceUsd
        ?: if (symbol.uppercase() in setOf("USDC", "USDT")) 1.0 else 0.0

    fun startLiveUpdates() {
        market.start()
        app.proBillingManager.start()
        viewModelScope.launch { fxRateService.refreshIfNeeded(settingsStore.settings.first()) }
        viewModelScope.launch {
            delay(750)
            val now = System.currentTimeMillis()
            uiState.value.trackedWallets.filter { !it.syncing && now - it.lastSyncAt > 60_000 }.forEach { refreshTrackedWallet(it.id) }
        }
        if (snapshotJob?.isActive != true) {
            snapshotJob = viewModelScope.launch {
                delay(5_000)
                while (isActive) {
                    persistCurrentSnapshots()
                    delay(60 * 60 * 1_000L)
                }
            }
        }
    }
    fun stopLiveUpdates() {
        market.stop()
        app.proBillingManager.stop()
        snapshotJob?.cancel()
        snapshotJob = null
    }

    private suspend fun persistCurrentSnapshots() {
        val state = uiState.value
        if (!state.isReady) return
        repository.recordDailySnapshot(null, PortfolioCalculator.calculate(
            entries = repository.ledger.first(),
            quotes = state.quotes,
            trackedPositions = trackedPositions(
                repository.trackedWallets.first(),
                repository.trackedWalletAssets.first().groupBy { it.walletId },
                state.quotes,
            ),
            portfolioNames = state.portfolios.associate { it.id to it.name },
        ))
        state.portfolios.forEach { repository.recordDailySnapshot(it.id, it.snapshot) }
    }
    fun selectPortfolio(id: Long?) { selectedPortfolioId.value = id }
    fun createPortfolio(name: String, onCreated: (Long) -> Unit = {}) = viewModelScope.launch { onCreated(repository.createPortfolio(name)) }
    fun deletePortfolio(id: Long) = viewModelScope.launch { repository.deletePortfolio(id); if (selectedPortfolioId.value == id) selectedPortfolioId.value = null }

    fun createTrackedWallet(draft: TrackedWalletDraft, onResult: (Result<Long>) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            require(draft.portfolioId > 0) { "Escolha um portfólio." }
            val network = WalletNetwork.valueOf(draft.network)
            require(isValidAddress(network, draft.address)) { "Endereço público ${networkLabel(network)} inválido." }
            repository.createTrackedWallet(draft)
        }
        result.onSuccess { syncTrackedWallet(it, draft.address, WalletNetwork.valueOf(draft.network)) }
        onResult(result)
    }

    fun refreshTrackedWallet(id: Long) {
        uiState.value.trackedWallets.firstOrNull { it.id == id }?.let { syncTrackedWallet(id, it.address, it.network) }
    }

    private fun syncTrackedWallet(id: Long, address: String, network: WalletNetwork) = viewModelScope.launch {
        if (id in syncingWallets.value) return@launch
        syncingWallets.value = syncingWallets.value + id
        walletErrors.value = walletErrors.value - id
        try {
            walletSyncSlots.withPermit {
                runCatching {
                    when (network) {
                        WalletNetwork.SOLANA -> app.solanaWalletService.fetchHoldings(address)
                        WalletNetwork.BITCOIN -> app.bitcoinWalletService.fetchHoldings(address)
                        WalletNetwork.ETHEREUM -> app.ethereumWalletService.fetchHoldings(address)
                    }
                }.onSuccess { holdings ->
                    val quotes = uiState.value.quotes
                    val value = holdings.sumOf { it.quantity * priceFor(it.symbol, quotes) }
                    repository.updateTrackedWalletAssets(id, holdings.map { TrackedWalletAssetDraft(it.mint, it.symbol, it.name, it.quantity) }, value)
                }.onFailure { walletErrors.value = walletErrors.value + (id to (it.message ?: "Falha ao consultar ${networkLabel(network)}.")) }
            }
        } finally {
            syncingWallets.value = syncingWallets.value - id
        }
    }

    private fun isValidAddress(network: WalletNetwork, address: String) = when (network) {
        WalletNetwork.SOLANA -> SolanaWalletService.isValidAddress(address)
        WalletNetwork.BITCOIN -> BitcoinWalletService.isValidAddress(address)
        WalletNetwork.ETHEREUM -> EthereumWalletService.isValidAddress(address)
    }
    private fun networkLabel(network: WalletNetwork) = when (network) { WalletNetwork.SOLANA -> "Solana"; WalletNetwork.BITCOIN -> "Bitcoin"; WalletNetwork.ETHEREUM -> "Ethereum" }

    fun refreshAllTrackedWallets() = uiState.value.trackedWallets.forEach { refreshTrackedWallet(it.id) }
    fun loadMarketHistory(productId: String, days: Int, onResult: (Result<List<HistoricalCandle>>) -> Unit) = viewModelScope.launch {
        onResult(runCatching { app.historicalPriceService.fetch(productId, days) })
    }
    fun deleteTrackedWallet(id: Long) = viewModelScope.launch { repository.deleteTrackedWallet(id) }
    fun updateTrackedWalletCost(id: Long, costUsd: Double?, onResult: (Result<Unit>) -> Unit) = viewModelScope.launch { onResult(runCatching { repository.updateTrackedWalletCost(id, costUsd) }) }
    fun updateTrackedWalletAssetCost(walletId: Long, mint: String, costUsd: Double?, onResult: (Result<Unit>) -> Unit) = viewModelScope.launch {
        onResult(runCatching { repository.updateTrackedWalletAssetCost(walletId, mint, costUsd) })
    }
    fun deleteCustomAsset(symbol: String, onResult: (Result<Unit>) -> Unit) = viewModelScope.launch { onResult(runCatching { repository.deleteCustomAsset(symbol) }) }
    fun saveEntry(draft: LedgerEntryDraft, onResult: (Result<Long>) -> Unit) = viewModelScope.launch {
        onResult(runCatching {
            require(draft.portfolioId > 0) { "Escolha uma carteira." }
            require(draft.quantity > 0) { "A quantidade precisa ser maior que zero." }
            require(draft.unitPriceUsd > 0) { "Informe um valor válido." }
            require(draft.asset.symbol.isNotBlank()) { "Informe o ativo." }
            repository.addLedgerEntry(draft)
        })
    }
    fun setTheme(v: ThemePreference) = viewModelScope.launch { settingsStore.setTheme(v) }
    fun setColorPalette(v: ColorPalette) = viewModelScope.launch { settingsStore.setColorPalette(v) }
    fun setPriceRefreshSpeed(v: PriceRefreshSpeed) = viewModelScope.launch { settingsStore.setPriceRefreshSpeed(v) }
    fun setLastDestination(value: String) = viewModelScope.launch { settingsStore.setLastDestination(value) }
    fun setDisplayCurrency(v: DisplayCurrency) = viewModelScope.launch { settingsStore.setDisplayCurrency(v) }
    fun setHideBalances(v: Boolean) = viewModelScope.launch { settingsStore.setHideBalances(v) }
    fun setBiometricLock(v: Boolean) = viewModelScope.launch { settingsStore.setBiometricLock(v) }
    fun setSecureScreen(v: Boolean) = viewModelScope.launch { settingsStore.setSecureScreen(v) }
    fun setInteractionFeedback(v: Boolean) = viewModelScope.launch { settingsStore.setInteractionFeedback(v) }
    fun purchasePro(activity: Activity) = app.proBillingManager.purchase(activity)
    fun createBackup(password: CharArray, onResult: (Result<ByteArray>) -> Unit) = viewModelScope.launch {
        try {
            onResult(runCatching { backupManager.create(password) })
        } finally {
            password.fill('\u0000')
        }
    }
    fun restoreBackup(bytes: ByteArray, password: CharArray, onResult: (Result<dev.zhar.abc.data.backup.BackupSummary>) -> Unit) = viewModelScope.launch {
        try {
            onResult(runCatching { backupManager.restore(bytes, password) })
        } finally {
            password.fill('\u0000')
        }
    }

    class Factory(private val application: AbcApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AbcViewModel(application) as T
    }
}
