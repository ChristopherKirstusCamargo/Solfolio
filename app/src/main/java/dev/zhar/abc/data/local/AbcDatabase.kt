package dev.zhar.abc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PortfolioEntity::class, AssetEntity::class, LedgerEntryEntity::class, TrackedWalletEntity::class, TrackedWalletAssetEntity::class, PortfolioDailySnapshotEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AbcDatabase : RoomDatabase() {
    abstract fun dao(): AbcDao

    companion object {
        @Volatile
        private var instance: AbcDatabase? = null

        fun get(context: Context): AbcDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AbcDatabase::class.java,
                "abc_portfolios.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `tracked_wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `portfolioId` INTEGER NOT NULL, `label` TEXT NOT NULL, `address` TEXT NOT NULL, `network` TEXT NOT NULL, `costBasisUsd` REAL, `baselineValueUsd` REAL, `createdAt` INTEGER NOT NULL, `lastSyncAt` INTEGER NOT NULL, FOREIGN KEY(`portfolioId`) REFERENCES `portfolios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracked_wallets_portfolioId` ON `tracked_wallets` (`portfolioId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracked_wallets_address` ON `tracked_wallets` (`address`)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `tracked_wallet_assets` (`walletId` INTEGER NOT NULL, `mint` TEXT NOT NULL, `symbol` TEXT NOT NULL, `name` TEXT NOT NULL, `quantity` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`walletId`, `mint`), FOREIGN KEY(`walletId`) REFERENCES `tracked_wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracked_wallet_assets_walletId` ON `tracked_wallet_assets` (`walletId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracked_wallet_assets_symbol` ON `tracked_wallet_assets` (`symbol`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_tracked_wallets_address`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracked_wallets_portfolioId_network_address` ON `tracked_wallets` (`portfolioId`, `network`, `address`)")
                db.execSQL("ALTER TABLE `tracked_wallet_assets` ADD COLUMN `costBasisUsd` REAL")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `portfolio_daily_snapshots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `portfolioId` INTEGER NOT NULL, `dayEpochUtc` INTEGER NOT NULL, `totalValueUsd` REAL NOT NULL, `remainingCostUsd` REAL NOT NULL, `realizedPnlUsd` REAL NOT NULL, `unrealizedPnlUsd` REAL NOT NULL, `capturedAt` INTEGER NOT NULL)""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_portfolio_daily_snapshots_portfolioId_dayEpochUtc` ON `portfolio_daily_snapshots` (`portfolioId`, `dayEpochUtc`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_daily_snapshots_capturedAt` ON `portfolio_daily_snapshots` (`capturedAt`)")
            }
        }
    }
}
