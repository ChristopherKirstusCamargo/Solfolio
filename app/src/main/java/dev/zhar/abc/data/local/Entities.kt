package dev.zhar.abc.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "portfolios")
data class PortfolioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val productId: String,
    val isCustom: Boolean = false,
)

@Entity(
    tableName = "ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = PortfolioEntity::class,
            parentColumns = ["id"],
            childColumns = ["portfolioId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["symbol"],
            childColumns = ["symbol"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("portfolioId"), Index("symbol")],
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val portfolioId: Long,
    val symbol: String,
    val kind: String,
    val quantity: Double,
    val unitPriceUsd: Double,
    val feeUsd: Double = 0.0,
    val originalAmount: Double,
    val originalCurrency: String,
    val brlPerUsdAtEntry: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
)

@Entity(
    tableName = "tracked_wallets",
    foreignKeys = [ForeignKey(entity = PortfolioEntity::class, parentColumns = ["id"], childColumns = ["portfolioId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index("portfolioId"),
        Index(value = ["portfolioId", "network", "address"], unique = true),
    ],
)
data class TrackedWalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val portfolioId: Long,
    val label: String,
    val address: String,
    val network: String = "SOLANA",
    val costBasisUsd: Double? = null,
    val baselineValueUsd: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = 0L,
)

@Entity(
    tableName = "tracked_wallet_assets",
    primaryKeys = ["walletId", "mint"],
    foreignKeys = [ForeignKey(entity = TrackedWalletEntity::class, parentColumns = ["id"], childColumns = ["walletId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("walletId"), Index("symbol")],
)
data class TrackedWalletAssetEntity(
    val walletId: Long,
    val mint: String,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val costBasisUsd: Double? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Permanent, bounded daily history derived from the user's local portfolio. */
@Entity(
    tableName = "portfolio_daily_snapshots",
    indices = [Index(value = ["portfolioId", "dayEpochUtc"], unique = true), Index("capturedAt")],
)
data class PortfolioDailySnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Zero represents the combined view; positive values represent one portfolio. */
    val portfolioId: Long,
    val dayEpochUtc: Long,
    val totalValueUsd: Double,
    val remainingCostUsd: Double,
    val realizedPnlUsd: Double,
    val unrealizedPnlUsd: Double,
    val capturedAt: Long = System.currentTimeMillis(),
)
