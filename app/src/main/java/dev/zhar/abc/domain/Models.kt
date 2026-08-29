package dev.zhar.abc.domain

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
}

enum class DisplayCurrency {
    BRL,
    USD,
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
    val interactionFeedback: Boolean = true,
    val proEntitled: Boolean = false,
    val colorPalette: ColorPalette = ColorPalette.SOLANA,
    val priceRefreshSpeed: PriceRefreshSpeed = PriceRefreshSpeed.MEDIUM,
    val lastDestination: String = "HOME",
    val brlPerUsd: Double = 5.50,
    val fxUpdatedAt: Long = 0L,
)

data class ProStatus(
    val owned: Boolean = false,
    val pending: Boolean = false,
    val available: Boolean = false,
    val formattedPrice: String = "R$ 16,90",
    val message: String? = null,
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
