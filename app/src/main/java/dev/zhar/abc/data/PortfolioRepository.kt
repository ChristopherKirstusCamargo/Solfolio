package dev.zhar.abc.data

import dev.zhar.abc.data.local.AbcDao
import dev.zhar.abc.data.local.AssetEntity
import dev.zhar.abc.data.local.LedgerEntryEntity
import dev.zhar.abc.data.local.PortfolioEntity
import dev.zhar.abc.data.local.PortfolioDailySnapshotEntity
import dev.zhar.abc.data.local.TrackedWalletAssetEntity
import dev.zhar.abc.data.local.TrackedWalletEntity
import dev.zhar.abc.domain.AssetCatalog
import dev.zhar.abc.domain.AssetDefinition
import dev.zhar.abc.domain.LedgerEntry
import dev.zhar.abc.domain.PortfolioHistoryPoint
import dev.zhar.abc.domain.TransactionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

data class LedgerEntryDraft(
    val portfolioId: Long,
    val asset: AssetDefinition,
    val isCustomAsset: Boolean,
    val kind: TransactionKind,
    val quantity: Double,
    val unitPriceUsd: Double,
    val feeUsd: Double,
    val originalAmount: Double,
    val originalCurrency: String,
    val brlPerUsdAtEntry: Double,
    val timestamp: Long,
    val note: String,
)

data class TrackedWalletDraft(val portfolioId: Long, val label: String, val address: String, val network: String, val costBasisUsd: Double?)
data class TrackedWalletAssetDraft(val mint: String, val symbol: String, val name: String, val quantity: Double)

class PortfolioRepository(private val dao: AbcDao) {
    val portfolios: Flow<List<PortfolioEntity>> = dao.observePortfolios()
    val assets: Flow<List<AssetEntity>> = dao.observeAssets()
    val ledgerEntities: Flow<List<LedgerEntryEntity>> = dao.observeLedger()
    val trackedWallets: Flow<List<TrackedWalletEntity>> = dao.observeTrackedWallets()
    val trackedWalletAssets: Flow<List<TrackedWalletAssetEntity>> = dao.observeTrackedWalletAssets()

    fun portfolioHistory(portfolioId: Long?): Flow<List<PortfolioHistoryPoint>> =
        dao.observePortfolioHistory(portfolioId ?: 0L).map { rows ->
            rows.map { PortfolioHistoryPoint(it.capturedAt, it.totalValueUsd) }
        }

    val ledger: Flow<List<LedgerEntry>> = combine(ledgerEntities, assets) { entries, assets ->
        val names = assets.associate { it.symbol to it.name }
        entries.map { entry ->
            LedgerEntry(
                id = entry.id,
                portfolioId = entry.portfolioId,
                symbol = entry.symbol,
                assetName = names[entry.symbol] ?: entry.symbol,
                kind = runCatching { TransactionKind.valueOf(entry.kind) }
                    .getOrDefault(TransactionKind.BUY),
                quantity = entry.quantity,
                unitPriceUsd = entry.unitPriceUsd,
                feeUsd = entry.feeUsd,
                timestamp = entry.timestamp,
            )
        }
    }

    suspend fun seedDefaults() {
        dao.insertAssets(
            AssetCatalog.defaults.map {
                AssetEntity(
                    symbol = it.symbol,
                    name = it.name,
                    productId = it.productId,
                    isCustom = false,
                )
            },
        )
        if (dao.portfolioCount() == 0) {
            dao.insertPortfolio(PortfolioEntity(name = "Principal"))
        }
    }

    suspend fun createPortfolio(name: String): Long = dao.insertPortfolio(
        PortfolioEntity(name = name.trim().ifBlank { "Nova carteira" }),
    )

    suspend fun deletePortfolio(id: Long) = dao.deletePortfolioAndHistory(id)

    suspend fun createTrackedWallet(draft: TrackedWalletDraft): Long = dao.insertTrackedWallet(
        TrackedWalletEntity(portfolioId = draft.portfolioId, label = draft.label.trim().ifBlank { "Endereço ${draft.network.lowercase().replaceFirstChar { it.uppercase() }}" }, address = draft.address.trim(), network = draft.network, costBasisUsd = draft.costBasisUsd?.takeIf { it >= 0.0 }),
    )
    suspend fun deleteTrackedWallet(id: Long) = dao.deleteTrackedWallet(id)
    suspend fun updateTrackedWalletCost(id: Long, costBasisUsd: Double?) = dao.updateTrackedWalletCost(id, costBasisUsd?.takeIf { it >= 0.0 })
    suspend fun updateTrackedWalletAssetCost(walletId: Long, mint: String, costBasisUsd: Double?) =
        dao.updateTrackedWalletAssetCost(walletId, mint, costBasisUsd?.takeIf { it >= 0.0 })
    suspend fun updateTrackedWalletAssets(walletId: Long, assets: List<TrackedWalletAssetDraft>, currentValueUsd: Double) {
        val syncedAt = System.currentTimeMillis()
        val previousCosts = dao.getTrackedWalletAssets(walletId).associate { it.mint to it.costBasisUsd }
        dao.replaceTrackedWalletAssets(walletId, assets.filter { it.quantity > 0.0 }.map {
            TrackedWalletAssetEntity(
                walletId = walletId,
                mint = it.mint,
                symbol = it.symbol,
                name = it.name,
                quantity = it.quantity,
                costBasisUsd = previousCosts[it.mint],
                updatedAt = syncedAt,
            )
        })
        dao.markWalletSynced(walletId, syncedAt, currentValueUsd)
    }
    suspend fun deleteCustomAsset(symbol: String) {
        val normalized = symbol.trim().uppercase()
        require(dao.ledgerCountForAsset(normalized) == 0) { "Esse ativo possui lançamentos. Exclua o portfólio ou mantenha o ativo para preservar o histórico." }
        require(dao.deleteCustomAsset(normalized) > 0) { "Somente ativos personalizados podem ser removidos." }
    }

    suspend fun deleteManualPosition(portfolioId: Long, symbol: String) {
        require(dao.deleteManualPosition(portfolioId, symbol.trim().uppercase()) > 0) { "Nenhum lançamento encontrado para remover." }
    }

    suspend fun addLedgerEntry(draft: LedgerEntryDraft): Long {
        val symbol = draft.asset.symbol.trim().uppercase()
        if (draft.kind == TransactionKind.SELL) {
            val available = dao.availableManualQuantity(draft.portfolioId, symbol)
            require(draft.quantity <= available + 1e-10) {
                "A venda excede a quantidade registrada (${available.coerceAtLeast(0.0)} $symbol)."
            }
        }
        if (draft.isCustomAsset) {
            dao.insertAsset(
                AssetEntity(
                    symbol = symbol,
                    name = draft.asset.name.trim().ifBlank { symbol },
                    productId = draft.asset.productId.trim().uppercase()
                        .ifBlank { "$symbol-USD" },
                    isCustom = true,
                ),
            )
        }
        return dao.insertLedgerEntry(
            LedgerEntryEntity(
                portfolioId = draft.portfolioId,
                symbol = symbol,
                kind = draft.kind.name,
                quantity = draft.quantity,
                unitPriceUsd = draft.unitPriceUsd,
                feeUsd = draft.feeUsd,
                originalAmount = draft.originalAmount,
                originalCurrency = draft.originalCurrency,
                brlPerUsdAtEntry = draft.brlPerUsdAtEntry,
                timestamp = draft.timestamp,
                note = draft.note.trim(),
            ),
        )
    }

    suspend fun recordDailySnapshot(portfolioId: Long?, snapshot: dev.zhar.abc.domain.PortfolioSnapshot) {
        if (snapshot.holdings.isEmpty()) return
        val now = System.currentTimeMillis()
        val day = now / TimeUnit.DAYS.toMillis(1)
        dao.upsertDailySnapshot(
            PortfolioDailySnapshotEntity(
                portfolioId = portfolioId ?: 0L,
                dayEpochUtc = day,
                totalValueUsd = snapshot.totalValueUsd,
                remainingCostUsd = snapshot.remainingCostUsd,
                realizedPnlUsd = snapshot.realizedPnlUsd,
                unrealizedPnlUsd = snapshot.unrealizedPnlUsd,
                capturedAt = now,
            ),
        )
        dao.prunePortfolioHistory(now - TimeUnit.DAYS.toMillis(370))
    }
}
