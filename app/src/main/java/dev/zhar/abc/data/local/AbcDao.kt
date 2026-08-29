package dev.zhar.abc.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AbcDao {
    @Query("SELECT * FROM portfolios ORDER BY createdAt ASC") suspend fun exportPortfolios(): List<PortfolioEntity>
    @Query("SELECT * FROM assets ORDER BY symbol ASC") suspend fun exportAssets(): List<AssetEntity>
    @Query("SELECT * FROM ledger_entries ORDER BY timestamp ASC, id ASC") suspend fun exportLedger(): List<LedgerEntryEntity>
    @Query("SELECT * FROM tracked_wallets ORDER BY createdAt ASC") suspend fun exportTrackedWallets(): List<TrackedWalletEntity>
    @Query("SELECT * FROM tracked_wallet_assets ORDER BY walletId, symbol") suspend fun exportTrackedWalletAssets(): List<TrackedWalletAssetEntity>
    @Query("SELECT * FROM portfolio_daily_snapshots ORDER BY dayEpochUtc") suspend fun exportDailySnapshots(): List<PortfolioDailySnapshotEntity>
    @Query("SELECT * FROM portfolios ORDER BY createdAt ASC")
    fun observePortfolios(): Flow<List<PortfolioEntity>>

    @Query("SELECT * FROM assets ORDER BY isCustom ASC, symbol ASC")
    fun observeAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM ledger_entries ORDER BY timestamp ASC, id ASC")
    fun observeLedger(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM tracked_wallets ORDER BY createdAt ASC")
    fun observeTrackedWallets(): Flow<List<TrackedWalletEntity>>

    @Query("SELECT * FROM tracked_wallet_assets ORDER BY walletId ASC, symbol ASC")
    fun observeTrackedWalletAssets(): Flow<List<TrackedWalletAssetEntity>>

    @Query("SELECT * FROM portfolio_daily_snapshots WHERE portfolioId = :portfolioId ORDER BY dayEpochUtc ASC")
    fun observePortfolioHistory(portfolioId: Long): Flow<List<PortfolioDailySnapshotEntity>>

    @Query("SELECT * FROM tracked_wallet_assets WHERE walletId = :walletId")
    suspend fun getTrackedWalletAssets(walletId: Long): List<TrackedWalletAssetEntity>

    @Insert
    suspend fun insertPortfolio(portfolio: PortfolioEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePortfolios(rows: List<PortfolioEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreAssets(rows: List<AssetEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreLedger(rows: List<LedgerEntryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreWallets(rows: List<TrackedWalletEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreWalletAssets(rows: List<TrackedWalletAssetEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreSnapshots(rows: List<PortfolioDailySnapshotEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Insert
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long

    @Insert suspend fun insertTrackedWallet(wallet: TrackedWalletEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTrackedWalletAssets(assets: List<TrackedWalletAssetEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDailySnapshot(snapshot: PortfolioDailySnapshotEntity)

    @Delete
    suspend fun deleteLedgerEntry(entry: LedgerEntryEntity)

    @Query("DELETE FROM portfolios WHERE id = :portfolioId")
    suspend fun deletePortfolio(portfolioId: Long)

    @Query("DELETE FROM portfolio_daily_snapshots WHERE portfolioId = :portfolioId")
    suspend fun deletePortfolioHistory(portfolioId: Long)

    @Transaction
    suspend fun deletePortfolioAndHistory(portfolioId: Long) {
        deletePortfolioHistory(portfolioId)
        deletePortfolio(portfolioId)
    }

    @Query("DELETE FROM tracked_wallets WHERE id = :walletId") suspend fun deleteTrackedWallet(walletId: Long)
    @Query("UPDATE tracked_wallets SET costBasisUsd = :costBasisUsd WHERE id = :walletId") suspend fun updateTrackedWalletCost(walletId: Long, costBasisUsd: Double?)
    @Query("UPDATE tracked_wallet_assets SET costBasisUsd = :costBasisUsd WHERE walletId = :walletId AND mint = :mint")
    suspend fun updateTrackedWalletAssetCost(walletId: Long, mint: String, costBasisUsd: Double?)
    @Query("DELETE FROM tracked_wallet_assets WHERE walletId = :walletId") suspend fun deleteTrackedWalletAssets(walletId: Long)
    @Query("UPDATE tracked_wallets SET lastSyncAt = :syncedAt, baselineValueUsd = CASE WHEN baselineValueUsd IS NULL AND :currentValueUsd > 0 THEN :currentValueUsd ELSE baselineValueUsd END WHERE id = :walletId")
    suspend fun markWalletSynced(walletId: Long, syncedAt: Long, currentValueUsd: Double)
    @Query("SELECT COUNT(*) FROM ledger_entries WHERE symbol = :symbol") suspend fun ledgerCountForAsset(symbol: String): Int
    @Query("SELECT COALESCE(SUM(CASE WHEN kind = 'BUY' THEN quantity WHEN kind = 'SELL' THEN -quantity ELSE 0 END), 0) FROM ledger_entries WHERE portfolioId = :portfolioId AND symbol = :symbol")
    suspend fun availableManualQuantity(portfolioId: Long, symbol: String): Double
    @Query("DELETE FROM assets WHERE symbol = :symbol AND isCustom = 1") suspend fun deleteCustomAsset(symbol: String): Int
    @Query("DELETE FROM portfolio_daily_snapshots WHERE capturedAt < :before") suspend fun prunePortfolioHistory(before: Long)

    @Transaction
    suspend fun replaceTrackedWalletAssets(walletId: Long, assets: List<TrackedWalletAssetEntity>) {
        deleteTrackedWalletAssets(walletId)
        if (assets.isNotEmpty()) insertTrackedWalletAssets(assets)
    }

    @Query("DELETE FROM portfolios") suspend fun clearPortfoliosForRestore()
    @Query("DELETE FROM assets") suspend fun clearAssetsForRestore()
    @Query("DELETE FROM portfolio_daily_snapshots") suspend fun clearSnapshotsForRestore()

    @Transaction
    suspend fun replaceAllPermanentData(
        portfolios: List<PortfolioEntity>,
        assets: List<AssetEntity>,
        ledger: List<LedgerEntryEntity>,
        wallets: List<TrackedWalletEntity>,
        walletAssets: List<TrackedWalletAssetEntity>,
        snapshots: List<PortfolioDailySnapshotEntity>,
    ) {
        clearPortfoliosForRestore()
        clearAssetsForRestore()
        clearSnapshotsForRestore()
        restorePortfolios(portfolios)
        restoreAssets(assets)
        restoreLedger(ledger)
        restoreWallets(wallets)
        restoreWalletAssets(walletAssets)
        restoreSnapshots(snapshots)
    }

    @Query("SELECT COUNT(*) FROM portfolios")
    suspend fun portfolioCount(): Int

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun assetCount(): Int
}
