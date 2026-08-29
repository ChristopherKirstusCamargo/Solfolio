package dev.zhar.abc.domain

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
}

enum class DisplayCurrency(val currencyCode: String, val fallbackPerUsd: Double) {
    BRL("BRL", 5.50), USD("USD", 1.0), EUR("EUR", .92), GBP("GBP", .79),
    JPY("JPY", 150.0), CAD("CAD", 1.36), AUD("AUD", 1.52), CHF("CHF", .88),
    CNY("CNY", 7.20), HKD("HKD", 7.82), SGD("SGD", 1.34), NZD("NZD", 1.65),
    MXN("MXN", 17.0), ARS("ARS", 1_050.0), CLP("CLP", 960.0), COP("COP", 4_100.0),
    PEN("PEN", 3.75), UYU("UYU", 42.0), INR("INR", 86.0), KRW("KRW", 1_450.0),
    TRY("TRY", 36.0), ZAR("ZAR", 18.5), SEK("SEK", 10.5), NOK("NOK", 11.0),
    DKK("DKK", 6.86), PLN("PLN", 4.0),
}

enum class ColorPalette {
    SOLANA,
    VIOLET,
    OCEAN,
    FOREST,
    SUNSET,
}

enum class PriceRefreshSpeed(val intervalMs: Long) {
    DISABLED(60_000L),
    VERY_LOW(45_000L),
    LOW(30_000L),
    MEDIUM(15_000L),
    HIGH(5_000L),
    INSTANT(500L),
}

enum class LockTimeout(val timeoutMs: Long) {
    INSTANT(0L), ONE_MINUTE(60_000L), FIVE_MINUTES(300_000L), TEN_MINUTES(600_000L),
}

enum class TransactionKind {
    BUY,
    SELL,
}

enum class FeedStatus {
    OFFLINE,
    CONNECTING,
    LIVE,
    RECONNECTING,
}

enum class WalletNetwork {
    SOLANA,
    BITCOIN,
    ETHEREUM,
}

data class AppSettings(
    val theme: ThemePreference = ThemePreference.AMOLED,
    val displayCurrency: DisplayCurrency = DisplayCurrency.BRL,
    val hideBalances: Boolean = false,
    val biometricLock: Boolean = false,
    val secureScreen: Boolean = false,
    val colorPalette: ColorPalette = ColorPalette.SOLANA,
    val priceRefreshSpeed: PriceRefreshSpeed = PriceRefreshSpeed.MEDIUM,
    val lockTimeout: LockTimeout = LockTimeout.ONE_MINUTE,
    val lastDestination: String = "HOME",
    val brlPerUsd: Double = 5.50,
    val fiatPerUsd: Map<DisplayCurrency, Double> = DisplayCurrency.entries.associateWith { it.fallbackPerUsd },
    val fxUpdatedAt: Long = 0L,
)

data class AssetDefinition(
    val symbol: String,
    val name: String,
    val productId: String,
)

object AssetCatalog {
    val defaults = listOf(
        AssetDefinition("BTC", "Bitcoin", "BTC-USD"),
        AssetDefinition("ETH", "Ethereum", "ETH-USD"),
        AssetDefinition("SOL", "Solana", "SOL-USD"),
        AssetDefinition("USDT", "Tether", "USDT-USD"),
        AssetDefinition("USDC", "USD Coin", "USDC-USD"),
        AssetDefinition("XRP", "XRP", "XRP-USD"),
        AssetDefinition("ADA", "Cardano", "ADA-USD"),
        AssetDefinition("DOGE", "Dogecoin", "DOGE-USD"),
        AssetDefinition("AVAX", "Avalanche", "AVAX-USD"),
        AssetDefinition("LINK", "Chainlink", "LINK-USD"),
        AssetDefinition("LTC", "Litecoin", "LTC-USD"),
        AssetDefinition("SHIB", "Shiba Inu", "SHIB-USD"),
        AssetDefinition("POL", "Polygon", "POL-USD"),
        AssetDefinition("DOT", "Polkadot", "DOT-USD"),
        AssetDefinition("BCH", "Bitcoin Cash", "BCH-USD"),
        AssetDefinition("UNI", "Uniswap", "UNI-USD"),
        AssetDefinition("XLM", "Stellar", "XLM-USD"),
        AssetDefinition("ATOM", "Cosmos", "ATOM-USD"),
        AssetDefinition("NEAR", "NEAR Protocol", "NEAR-USD"),
        AssetDefinition("AAVE", "Aave", "AAVE-USD"),
        AssetDefinition("SUI", "Sui", "SUI-USD"),
        AssetDefinition("HBAR", "Hedera", "HBAR-USD"),
        AssetDefinition("ICP", "Internet Computer", "ICP-USD"),
        AssetDefinition("ETC", "Ethereum Classic", "ETC-USD"),
        AssetDefinition("FIL", "Filecoin", "FIL-USD"),
        AssetDefinition("ARB", "Arbitrum", "ARB-USD"),
        AssetDefinition("OP", "Optimism", "OP-USD"),
        AssetDefinition("INJ", "Injective", "INJ-USD"),
        AssetDefinition("APT", "Aptos", "APT-USD"),
        AssetDefinition("ALGO", "Algorand", "ALGO-USD"),
        AssetDefinition("MKR", "Maker", "MKR-USD"),
        AssetDefinition("GRT", "The Graph", "GRT-USD"),
        AssetDefinition("LDO", "Lido DAO", "LDO-USD"),
        AssetDefinition("PEPE", "Pepe", "PEPE-USD"),
        AssetDefinition("BONK", "Bonk", "BONK-USD"),
        AssetDefinition("WIF", "dogwifhat", "WIF-USD"),
        AssetDefinition("SEI", "Sei", "SEI-USD"),
        AssetDefinition("RENDER", "Render", "RENDER-USD"),
        AssetDefinition("IMX", "Immutable", "IMX-USD"),
        AssetDefinition("MANA", "Decentraland", "MANA-USD"),
    )
}

data class AssetQuote(
    val symbol: String,
    val priceUsd: Double,
    val change24hPercent: Double? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val recentPrices: List<Double> = emptyList(),
)

data class LedgerEntry(
    val id: Long,
    val portfolioId: Long,
    val symbol: String,
    val assetName: String,
    val kind: TransactionKind,
    val quantity: Double,
    val unitPriceUsd: Double,
    val feeUsd: Double,
    val timestamp: Long,
)

data class Holding(
    val sourceKey: String,
    val sourceLabel: String,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val averageCostUsd: Double,
    val remainingCostUsd: Double,
    val currentPriceUsd: Double,
    val currentValueUsd: Double,
    val unrealizedPnlUsd: Double,
    val realizedPnlUsd: Double,
    val change24hPercent: Double?,
    val allocationPercent: Double,
    val recentPrices: List<Double>,
    val hasLivePrice: Boolean,
    val basisIsExact: Boolean,
)

data class PortfolioSnapshot(
    val holdings: List<Holding> = emptyList(),
    val totalValueUsd: Double = 0.0,
    val remainingCostUsd: Double = 0.0,
    val unrealizedPnlUsd: Double = 0.0,
    val realizedPnlUsd: Double = 0.0,
    val unrealizedPnlPercent: Double = 0.0,
    val change24hPercent: Double? = null,
    val healthScore: Int = 0,
    val healthLabel: String = "Sem dados",
    val largestAllocationPercent: Double = 0.0,
    val missingLivePrices: Int = 0,
    val riskScore: Int = 0,
    val riskLabel: String = "Sem dados",
    val performanceScore: Int = 50,
    val performanceLabel: String = "Sem histórico",
    val expectedLowUsd: Double = 0.0,
    val expectedHighUsd: Double = 0.0,
    val trackedWalletCount: Int = 0,
    val trackedValueUsd: Double = 0.0,
    val pnlIsEstimated: Boolean = false,
    val pnlCoveragePercent: Double = 0.0,
    val dataCoveragePercent: Double = 0.0,
    val sourceCount: Int = 0,
    val best24hSymbol: String? = null,
    val worst24hSymbol: String? = null,
)

data class PortfolioHistoryPoint(
    val capturedAt: Long,
    val totalValueUsd: Double,
)

data class TrackedPosition(
    val walletId: Long,
    val sourceLabel: String,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val basisUsd: Double,
    val basisIsExact: Boolean,
    val fallbackPriceUsd: Double? = null,
)
