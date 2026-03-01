package de.tradebuddy.presentation

import de.tradebuddy.domain.model.PerformanceRange
import de.tradebuddy.domain.model.PortfolioAllocationSlice
import de.tradebuddy.domain.model.PortfolioGroup
import de.tradebuddy.domain.model.PortfolioHistoryPoint
import de.tradebuddy.domain.model.PortfolioPositionMetrics
import de.tradebuddy.domain.model.PortfolioSummary

data class PortfolioUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val groups: List<PortfolioGroup> = emptyList(),
    val positions: List<PortfolioPositionMetrics> = emptyList(),
    val summary: PortfolioSummary = PortfolioSummary(
        totalValue = 0.0,
        totalCostBasis = 0.0,
        profitLoss = 0.0,
        profitLossPercent = 0.0
    ),
    val allocationByCategory: List<PortfolioAllocationSlice> = emptyList(),
    val allocationByGroup: List<PortfolioAllocationSlice> = emptyList(),
    val history: List<PortfolioHistoryPoint> = emptyList(),
    val selectedRange: PerformanceRange = PerformanceRange.ThirtyDays,
    val selectedGroupId: String? = null,
    val searchQuery: String = ""
)

