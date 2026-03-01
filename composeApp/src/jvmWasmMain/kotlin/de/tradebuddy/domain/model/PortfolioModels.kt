package de.tradebuddy.domain.model

import java.time.Instant
import java.time.LocalDate

const val DEFAULT_PORTFOLIO_GROUP_COLOR_HEX: String = "#ADADFB"

enum class AssetCategory(val label: String) {
    Equity("Equity"),
    Etf("ETF"),
    Crypto("Crypto"),
    Bond("Bond"),
    Commodity("Commodity"),
    Forex("Forex"),
    Cash("Cash"),
    Other("Other")
}

enum class PerformanceRange(val label: String) {
    Today("Today"),
    SevenDays("7D"),
    ThirtyDays("30D"),
    All("All")
}

data class PortfolioGroup(
    val id: String,
    val name: String,
    val colorHex: String,
    val createdAt: Instant
)

data class PortfolioPosition(
    val id: String,
    val groupId: String,
    val name: String,
    val symbol: String,
    val category: AssetCategory,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double,
    val currency: String,
    val tags: Set<String> = emptySet(),
    val createdAt: Instant,
    val updatedAt: Instant
)

data class PortfolioHistoryPoint(
    val date: LocalDate,
    val totalValue: Double
)

data class PortfolioSnapshot(
    val groups: List<PortfolioGroup>,
    val positions: List<PortfolioPosition>,
    val history: List<PortfolioHistoryPoint>,
    val updatedAt: Instant
)

data class PortfolioPositionMetrics(
    val position: PortfolioPosition,
    val marketValue: Double,
    val costBasis: Double,
    val profitLoss: Double,
    val profitLossPercent: Double
)

data class PortfolioSummary(
    val totalValue: Double,
    val totalCostBasis: Double,
    val profitLoss: Double,
    val profitLossPercent: Double
)

data class PortfolioAllocationSlice(
    val label: String,
    val value: Double,
    val percentage: Double
)
