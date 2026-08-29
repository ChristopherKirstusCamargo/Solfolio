package dev.zhar.abc.data.backup

import dev.zhar.abc.data.local.*
import dev.zhar.abc.data.SettingsStore
import dev.zhar.abc.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

data class BackupSummary(
    val portfolios: Int,
    val operations: Int,
    val wallets: Int,
    val createdAt: Long,
)

class BackupManager(private val dao: AbcDao, private val settingsStore: SettingsStore) {
    suspend fun create(password: CharArray): ByteArray = withContext(Dispatchers.IO) {
        val settings = settingsStore.settings.first()
        val root = JSONObject()
            .put("format", "solfolio-backup")
            .put("schema", 1)
            .put("createdAt", System.currentTimeMillis())
            .put("settings", JSONObject().put("theme", settings.theme.name).put("currency", settings.displayCurrency.name).put("hideBalances", settings.hideBalances).put("palette", settings.colorPalette.name).put("refresh", settings.priceRefreshSpeed.name).put("secureScreen", settings.secureScreen).put("interactionFeedback", settings.interactionFeedback))
            .put("portfolios", JSONArray().apply { dao.exportPortfolios().forEach { put(it.toJson()) } })
            .put("assets", JSONArray().apply { dao.exportAssets().forEach { put(it.toJson()) } })
            .put("ledger", JSONArray().apply { dao.exportLedger().forEach { put(it.toJson()) } })
            .put("wallets", JSONArray().apply { dao.exportTrackedWallets().forEach { put(it.toJson()) } })
            .put("walletAssets", JSONArray().apply { dao.exportTrackedWalletAssets().forEach { put(it.toJson()) } })
            .put("snapshots", JSONArray().apply { dao.exportDailySnapshots().forEach { put(it.toJson()) } })
        val plaintext = root.toString().encodeToByteArray()
        try {
            EncryptedBackupCodec.encrypt(plaintext, password)
        } finally {
            plaintext.fill(0)
        }
    }

    suspend fun inspect(bytes: ByteArray, password: CharArray): BackupSummary = withContext(Dispatchers.Default) {
        val root = decode(bytes, password)
        BackupSummary(root.array("portfolios", 100).length(), root.array("ledger", 100_000).length(), root.array("wallets", 500).length(), root.getLong("createdAt"))
    }

    suspend fun restore(bytes: ByteArray, password: CharArray): BackupSummary = withContext(Dispatchers.IO) {
        val root = decode(bytes, password)
        val portfolios = root.array("portfolios", 100).objects().map { it.portfolio() }
        val assets = root.array("assets", 2_000).objects().map { it.asset() }
        val ledger = root.array("ledger", 100_000).objects().map { it.ledger() }
        val wallets = root.array("wallets", 500).objects().map { it.wallet() }
        val walletAssets = root.array("walletAssets", 20_000).objects().map { it.walletAsset() }
        val snapshots = root.array("snapshots", 100_000).objects().map { it.snapshot() }
        require(portfolios.isNotEmpty()) { "O backup não contém portfólios." }
        val portfolioIds = portfolios.map { it.id }.toSet()
        val symbols = assets.map { it.symbol }.toSet()
        val walletIds = wallets.map { it.id }.toSet()
        require(portfolioIds.size == portfolios.size && walletIds.size == wallets.size && symbols.size == assets.size) { "O backup contém identificadores duplicados." }
        require(ledger.all { it.portfolioId in portfolioIds && it.symbol in symbols }) { "Há operações com vínculos inválidos." }
        require(ledger.all { it.quantity > 0.0 && it.unitPriceUsd > 0.0 && it.feeUsd >= 0.0 }) { "Há operações com valores inválidos." }
        require(wallets.all { it.portfolioId in portfolioIds }) { "Há endereços com portfólio inválido." }
        require(walletAssets.all { it.walletId in walletIds }) { "Há ativos com endereço inválido." }
        require(walletAssets.all { it.quantity >= 0.0 && (it.costBasisUsd ?: 0.0) >= 0.0 }) { "Há saldos ou custos inválidos." }
        require(snapshots.all { it.portfolioId == 0L || it.portfolioId in portfolioIds }) { "Há histórico com portfólio inválido." }
        dao.replaceAllPermanentData(portfolios, assets, ledger, wallets, walletAssets, snapshots)
        root.optJSONObject("settings")?.let { saved ->
            val current = settingsStore.settings.first()
            settingsStore.restoreUserPreferences(current.copy(
                theme = saved.enum("theme", current.theme),
                displayCurrency = saved.enum("currency", current.displayCurrency),
                hideBalances = saved.optBoolean("hideBalances", current.hideBalances),
                colorPalette = saved.enum("palette", current.colorPalette),
                priceRefreshSpeed = saved.enum("refresh", current.priceRefreshSpeed),
                secureScreen = saved.optBoolean("secureScreen", current.secureScreen),
                interactionFeedback = saved.optBoolean("interactionFeedback", current.interactionFeedback),
            ))
        }
        BackupSummary(portfolios.size, ledger.size, wallets.size, root.getLong("createdAt"))
    }

    private fun decode(bytes: ByteArray, password: CharArray): JSONObject {
        val plaintext = EncryptedBackupCodec.decrypt(bytes, password)
        val root = try {
            JSONObject(plaintext.toString(Charsets.UTF_8))
        } finally {
            plaintext.fill(0)
        }
        require(root.optString("format") == "solfolio-backup" && root.optInt("schema") == 1) { "Versão de backup incompatível." }
        return root
    }
}

private fun JSONObject.array(name: String, max: Int): JSONArray = getJSONArray(name).also { require(it.length() <= max) { "O backup contém dados demais em $name." } }
private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
private fun JSONObject.requireFinite(name: String): Double = getDouble(name).also { require(it.isFinite()) { "Valor inválido em $name." } }
private fun JSONObject.nullableDouble(name: String): Double? = if (isNull(name)) null else requireFinite(name)
private inline fun <reified T : Enum<T>> JSONObject.enum(name: String, fallback: T): T = runCatching { enumValueOf<T>(getString(name)) }.getOrDefault(fallback)

private fun PortfolioEntity.toJson() = JSONObject().put("id", id).put("name", name).put("createdAt", createdAt)
private fun AssetEntity.toJson() = JSONObject().put("symbol", symbol).put("name", name).put("productId", productId).put("custom", isCustom)
private fun LedgerEntryEntity.toJson() = JSONObject().put("id", id).put("portfolioId", portfolioId).put("symbol", symbol).put("kind", kind).put("quantity", quantity).put("unitPriceUsd", unitPriceUsd).put("feeUsd", feeUsd).put("originalAmount", originalAmount).put("originalCurrency", originalCurrency).put("brlPerUsdAtEntry", brlPerUsdAtEntry).put("timestamp", timestamp).put("note", note)
private fun TrackedWalletEntity.toJson() = JSONObject().put("id", id).put("portfolioId", portfolioId).put("label", label).put("address", address).put("network", network).put("costBasisUsd", costBasisUsd ?: JSONObject.NULL).put("baselineValueUsd", baselineValueUsd ?: JSONObject.NULL).put("createdAt", createdAt).put("lastSyncAt", lastSyncAt)
private fun TrackedWalletAssetEntity.toJson() = JSONObject().put("walletId", walletId).put("mint", mint).put("symbol", symbol).put("name", name).put("quantity", quantity).put("costBasisUsd", costBasisUsd ?: JSONObject.NULL).put("updatedAt", updatedAt)
private fun PortfolioDailySnapshotEntity.toJson() = JSONObject().put("id", id).put("portfolioId", portfolioId).put("dayEpochUtc", dayEpochUtc).put("totalValueUsd", totalValueUsd).put("remainingCostUsd", remainingCostUsd).put("realizedPnlUsd", realizedPnlUsd).put("unrealizedPnlUsd", unrealizedPnlUsd).put("capturedAt", capturedAt)

private fun JSONObject.portfolio() = PortfolioEntity(getLong("id"), getString("name").take(64), getLong("createdAt"))
private fun JSONObject.asset() = AssetEntity(getString("symbol").uppercase().take(12), getString("name").take(64), getString("productId").uppercase().take(32), getBoolean("custom"))
private fun JSONObject.ledger() = LedgerEntryEntity(getLong("id"), getLong("portfolioId"), getString("symbol").uppercase(), getString("kind"), requireFinite("quantity"), requireFinite("unitPriceUsd"), requireFinite("feeUsd"), requireFinite("originalAmount"), getString("originalCurrency"), requireFinite("brlPerUsdAtEntry"), getLong("timestamp"), getString("note").take(120))
private fun JSONObject.wallet() = TrackedWalletEntity(getLong("id"), getLong("portfolioId"), getString("label").take(32), getString("address").take(100), getString("network"), nullableDouble("costBasisUsd"), nullableDouble("baselineValueUsd"), getLong("createdAt"), getLong("lastSyncAt"))
private fun JSONObject.walletAsset() = TrackedWalletAssetEntity(getLong("walletId"), getString("mint").take(100), getString("symbol").uppercase().take(12), getString("name").take(64), requireFinite("quantity"), nullableDouble("costBasisUsd"), getLong("updatedAt"))
private fun JSONObject.snapshot() = PortfolioDailySnapshotEntity(getLong("id"), getLong("portfolioId"), getLong("dayEpochUtc"), requireFinite("totalValueUsd"), requireFinite("remainingCostUsd"), requireFinite("realizedPnlUsd"), requireFinite("unrealizedPnlUsd"), getLong("capturedAt"))
