package de.tradebuddy.presentation

import de.tradebuddy.data.PortfolioRepository
import de.tradebuddy.domain.model.AssetCategory
import de.tradebuddy.domain.model.DEFAULT_PORTFOLIO_GROUP_COLOR_HEX
import de.tradebuddy.domain.model.PerformanceRange
import de.tradebuddy.domain.model.PortfolioAllocationSlice
import de.tradebuddy.domain.model.PortfolioGroup
import de.tradebuddy.domain.model.PortfolioHistoryPoint
import de.tradebuddy.domain.model.PortfolioPosition
import de.tradebuddy.domain.model.PortfolioPositionMetrics
import de.tradebuddy.domain.model.PortfolioSnapshot
import de.tradebuddy.domain.model.PortfolioSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PortfolioViewModel(
    private val repository: PortfolioRepository
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var snapshot: PortfolioSnapshot = PortfolioSnapshot(
        groups = emptyList(),
        positions = emptyList(),
        history = emptyList(),
        updatedAt = Instant.now()
    )

    private val _state = MutableStateFlow(PortfolioUiState())
    val state: StateFlow<PortfolioUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadSnapshot() }
                .onSuccess { loaded ->
                    snapshot = loaded
                    rebuildState()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Portfolio konnte nicht geladen werden"
                        )
                    }
                }
        }
    }

    fun setRange(range: PerformanceRange) {
        _state.update { it.copy(selectedRange = range) }
        rebuildState()
    }

    fun setSelectedGroup(groupId: String?) {
        _state.update { it.copy(selectedGroupId = groupId) }
        rebuildState()
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        rebuildState()
    }

    fun saveGroup(groupId: String?, name: String, colorHex: String) {
        scope.launch {
            val existing = snapshot.groups.firstOrNull { it.id == groupId }
            val now = Instant.now()
            val group = PortfolioGroup(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                colorHex = colorHex.ifBlank { DEFAULT_PORTFOLIO_GROUP_COLOR_HEX },
                createdAt = existing?.createdAt ?: now
            )
            repository.saveGroup(group)
            refresh()
        }
    }

    fun deleteGroup(groupId: String) {
        scope.launch {
            repository.deleteGroup(groupId)
            refresh()
        }
    }

    fun savePosition(
        positionId: String?,
        groupId: String,
        name: String,
        symbol: String,
        category: AssetCategory,
        quantity: Double,
        averagePrice: Double,
        currentPrice: Double,
        currency: String,
        tags: Set<String>
    ) {
        scope.launch {
            val existing = snapshot.positions.firstOrNull { it.id == positionId }
            val now = Instant.now()
            val position = PortfolioPosition(
                id = existing?.id ?: UUID.randomUUID().toString(),
                groupId = groupId,
                name = name.trim(),
                symbol = symbol.trim().uppercase(),
                category = category,
                quantity = quantity.coerceAtLeast(0.0),
                averagePrice = averagePrice.coerceAtLeast(0.0),
                currentPrice = currentPrice.coerceAtLeast(0.0),
                currency = currency.trim().uppercase().ifBlank { "USD" },
                tags = tags.map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            repository.savePosition(position)
            refresh()
        }
    }

    fun deletePosition(positionId: String) {
        scope.launch {
            repository.deletePosition(positionId)
            refresh()
        }
    }

    fun resetPortfolio() {
        scope.launch {
            repository.reset()
            refresh()
        }
    }

    override fun close() {
        scope.cancel()
    }

    private fun rebuildState() {
        _state.update { current ->
            val allMetrics = snapshot.positions.map { it.toMetrics() }
            val groupFilter = current.selectedGroupId
            val query = current.searchQuery.trim().lowercase()

            val filteredMetrics = allMetrics
                .asSequence()
                .filter { metrics ->
                    groupFilter == null || metrics.position.groupId == groupFilter
                }
                .filter { metrics ->
                    query.isBlank() ||
                        metrics.position.name.lowercase().contains(query) ||
                        metrics.position.symbol.lowercase().contains(query) ||
                        metrics.position.tags.any { tag -> tag.lowercase().contains(query) }
                }
                .sortedByDescending { it.marketValue }
                .toList()

            current.copy(
                isLoading = false,
                errorMessage = null,
                groups = snapshot.groups.sortedBy { it.name.lowercase() },
                positions = filteredMetrics,
                summary = allMetrics.summary(),
                allocationByCategory = allMetrics.allocationByCategory(),
                allocationByGroup = allMetrics.allocationByGroup(snapshot.groups),
                history = snapshot.history.forRange(current.selectedRange)
            )
        }
    }
}

private fun PortfolioPosition.toMetrics(): PortfolioPositionMetrics {
    val marketValue = quantity * currentPrice
    val costBasis = quantity * averagePrice
    val profitLoss = marketValue - costBasis
    val profitLossPercent = if (costBasis > 0.0) (profitLoss / costBasis) * 100.0 else 0.0
    return PortfolioPositionMetrics(
        position = this,
        marketValue = marketValue,
        costBasis = costBasis,
        profitLoss = profitLoss,
        profitLossPercent = profitLossPercent
    )
}

private fun List<PortfolioPositionMetrics>.summary(): PortfolioSummary {
    val totalValue = sumOf { it.marketValue }
    val totalCost = sumOf { it.costBasis }
    val pnl = totalValue - totalCost
    val pnlPercent = if (totalCost > 0.0) (pnl / totalCost) * 100.0 else 0.0
    return PortfolioSummary(
        totalValue = totalValue,
        totalCostBasis = totalCost,
        profitLoss = pnl,
        profitLossPercent = pnlPercent
    )
}

private fun List<PortfolioPositionMetrics>.allocationByCategory(): List<PortfolioAllocationSlice> {
    val total = sumOf { it.marketValue }.takeIf { it > 0.0 } ?: return emptyList()
    return groupBy { it.position.category }
        .map { (category, entries) ->
            val value = entries.sumOf { it.marketValue }
            PortfolioAllocationSlice(
                label = category.label,
                value = value,
                percentage = (value / total) * 100.0
            )
        }
        .sortedByDescending { it.value }
}

private fun List<PortfolioPositionMetrics>.allocationByGroup(
    groups: List<PortfolioGroup>
): List<PortfolioAllocationSlice> {
    val total = sumOf { it.marketValue }.takeIf { it > 0.0 } ?: return emptyList()
    val groupNames = groups.associateBy({ it.id }, { it.name })
    return groupBy { it.position.groupId }
        .map { (groupId, entries) ->
            val value = entries.sumOf { it.marketValue }
            PortfolioAllocationSlice(
                label = groupNames[groupId] ?: "Ungrouped",
                value = value,
                percentage = (value / total) * 100.0
            )
        }
        .sortedByDescending { it.value }
}

private fun List<PortfolioHistoryPoint>.forRange(range: PerformanceRange): List<PortfolioHistoryPoint> {
    if (isEmpty()) return emptyList()
    if (range == PerformanceRange.All) return this
    val today = LocalDate.now(ZoneOffset.UTC)
    val cutoff = when (range) {
        PerformanceRange.Today -> today.minusDays(1)
        PerformanceRange.SevenDays -> today.minusDays(7)
        PerformanceRange.ThirtyDays -> today.minusDays(30)
        PerformanceRange.All -> today.minusDays(30)
    }
    val filtered = filter { it.date >= cutoff }
    return if (filtered.isNotEmpty()) filtered else this
}
