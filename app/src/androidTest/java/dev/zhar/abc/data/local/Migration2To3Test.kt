package dev.zhar.abc.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration2To3Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-v2-v3-test.db"

    @Before fun prepare() { context.deleteDatabase(name) }
    @After fun clean() { context.deleteDatabase(name) }

    @Test
    fun preservesV5DataAndAllowsSameAddressInDifferentPortfolios() {
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE portfolios (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE assets (symbol TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, productId TEXT NOT NULL, isCustom INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE ledger_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, portfolioId INTEGER NOT NULL, symbol TEXT NOT NULL, kind TEXT NOT NULL, quantity REAL NOT NULL, unitPriceUsd REAL NOT NULL, feeUsd REAL NOT NULL, originalAmount REAL NOT NULL, originalCurrency TEXT NOT NULL, brlPerUsdAtEntry REAL NOT NULL, timestamp INTEGER NOT NULL, note TEXT NOT NULL, FOREIGN KEY(portfolioId) REFERENCES portfolios(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(symbol) REFERENCES assets(symbol) ON UPDATE CASCADE ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX index_ledger_entries_portfolioId ON ledger_entries(portfolioId)")
            db.execSQL("CREATE INDEX index_ledger_entries_symbol ON ledger_entries(symbol)")
            db.execSQL("CREATE TABLE tracked_wallets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, portfolioId INTEGER NOT NULL, label TEXT NOT NULL, address TEXT NOT NULL, network TEXT NOT NULL, costBasisUsd REAL, baselineValueUsd REAL, createdAt INTEGER NOT NULL, lastSyncAt INTEGER NOT NULL, FOREIGN KEY(portfolioId) REFERENCES portfolios(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX index_tracked_wallets_portfolioId ON tracked_wallets(portfolioId)")
            db.execSQL("CREATE UNIQUE INDEX index_tracked_wallets_address ON tracked_wallets(address)")
            db.execSQL("CREATE TABLE tracked_wallet_assets (walletId INTEGER NOT NULL, mint TEXT NOT NULL, symbol TEXT NOT NULL, name TEXT NOT NULL, quantity REAL NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(walletId, mint), FOREIGN KEY(walletId) REFERENCES tracked_wallets(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX index_tracked_wallet_assets_walletId ON tracked_wallet_assets(walletId)")
            db.execSQL("CREATE INDEX index_tracked_wallet_assets_symbol ON tracked_wallet_assets(symbol)")
            db.execSQL("INSERT INTO portfolios(id,name,createdAt) VALUES(1,'Principal',1),(2,'Longo prazo',2)")
            db.execSQL("INSERT INTO assets(symbol,name,productId,isCustom) VALUES('SOL','Solana','SOL-USD',0)")
            db.execSQL("INSERT INTO ledger_entries(id,portfolioId,symbol,kind,quantity,unitPriceUsd,feeUsd,originalAmount,originalCurrency,brlPerUsdAtEntry,timestamp,note) VALUES(1,1,'SOL','BUY',2,100,1,201,'USD',5.5,1,'V5')")
            db.execSQL("INSERT INTO tracked_wallets(id,portfolioId,label,address,network,costBasisUsd,baselineValueUsd,createdAt,lastSyncAt) VALUES(1,1,'Sol','3VgkUFUkcfH8eXcCroytKkfpsbSDnTDXPR7z7RMGMnj1','SOLANA',100,110,1,2)")
            db.execSQL("INSERT INTO tracked_wallet_assets(walletId,mint,symbol,name,quantity,updatedAt) VALUES(1,'native-sol','SOL','Solana',2,2)")
            db.version = 2
        }

        val database = Room.databaseBuilder(context, AbcDatabase::class.java, name)
            .addMigrations(AbcDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM ledger_entries").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO tracked_wallets(portfolioId,label,address,network,costBasisUsd,baselineValueUsd,createdAt,lastSyncAt) VALUES(2,'Mesmo endereço','3VgkUFUkcfH8eXcCroytKkfpsbSDnTDXPR7z7RMGMnj1','SOLANA',NULL,NULL,3,0)",
        )
        database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM tracked_wallets").use {
            assertTrue(it.moveToFirst())
            assertEquals(2, it.getInt(0))
        }
        database.close()
    }
}
