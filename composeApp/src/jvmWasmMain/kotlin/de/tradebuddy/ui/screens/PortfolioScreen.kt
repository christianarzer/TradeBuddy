package de.tradebuddy.ui.screens

import de.tradebuddy.domain.model.AssetCategory
import de.tradebuddy.domain.model.AppDisplayCurrency
import de.tradebuddy.domain.model.PerformanceRange
import de.tradebuddy.domain.model.PortfolioAllocationSlice
import de.tradebuddy.domain.model.PortfolioGroup
import de.tradebuddy.domain.model.PortfolioPositionMetrics
import de.tradebuddy.presentation.PortfolioUiState
import de.tradebuddy.presentation.PortfolioViewModel
import de.tradebuddy.ui.charts.SnowBarChart
import de.tradebuddy.ui.charts.SnowBarEntry
import de.tradebuddy.ui.charts.SnowLineChart
import de.tradebuddy.ui.charts.SnowLineSeries
import de.tradebuddy.ui.components.SnowTabItem
import de.tradebuddy.ui.components.SnowTabs
import de.tradebuddy.ui.icons.SnowIcons
import de.tradebuddy.ui.theme.AppIconSize
import de.tradebuddy.ui.theme.AppPalette
import de.tradebuddy.ui.theme.AppSpacing
import de.tradebuddy.ui.theme.appBlockCardColors
import de.tradebuddy.ui.theme.appFlatCardElevation
import de.tradebuddy.ui.theme.extended
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.datepicker_cancel
import trade_buddy.composeapp.generated.resources.portfolio_add_group
import trade_buddy.composeapp.generated.resources.portfolio_add_position
import trade_buddy.composeapp.generated.resources.portfolio_all_groups
import trade_buddy.composeapp.generated.resources.portfolio_allocation_category
import trade_buddy.composeapp.generated.resources.portfolio_allocation_group
import trade_buddy.composeapp.generated.resources.portfolio_average_price
import trade_buddy.composeapp.generated.resources.portfolio_category
import trade_buddy.composeapp.generated.resources.portfolio_category_bond
import trade_buddy.composeapp.generated.resources.portfolio_category_cash
import trade_buddy.composeapp.generated.resources.portfolio_category_commodity
import trade_buddy.composeapp.generated.resources.portfolio_category_crypto
import trade_buddy.composeapp.generated.resources.portfolio_category_equity
import trade_buddy.composeapp.generated.resources.portfolio_category_etf
import trade_buddy.composeapp.generated.resources.portfolio_category_forex
import trade_buddy.composeapp.generated.resources.portfolio_category_other
import trade_buddy.composeapp.generated.resources.portfolio_current_price
import trade_buddy.composeapp.generated.resources.portfolio_currency
import trade_buddy.composeapp.generated.resources.portfolio_delete
import trade_buddy.composeapp.generated.resources.portfolio_edit
import trade_buddy.composeapp.generated.resources.portfolio_empty
import trade_buddy.composeapp.generated.resources.portfolio_group
import trade_buddy.composeapp.generated.resources.portfolio_groups
import trade_buddy.composeapp.generated.resources.portfolio_header_amount
import trade_buddy.composeapp.generated.resources.portfolio_header_asset
import trade_buddy.composeapp.generated.resources.portfolio_header_price
import trade_buddy.composeapp.generated.resources.portfolio_header_pl
import trade_buddy.composeapp.generated.resources.portfolio_header_qty
import trade_buddy.composeapp.generated.resources.portfolio_metric_best_performer
import trade_buddy.composeapp.generated.resources.portfolio_metric_cost
import trade_buddy.composeapp.generated.resources.portfolio_metric_group_count
import trade_buddy.composeapp.generated.resources.portfolio_name
import trade_buddy.composeapp.generated.resources.portfolio_no_history
import trade_buddy.composeapp.generated.resources.portfolio_no_groups
import trade_buddy.composeapp.generated.resources.portfolio_performance
import trade_buddy.composeapp.generated.resources.portfolio_positions
import trade_buddy.composeapp.generated.resources.portfolio_profit_loss
import trade_buddy.composeapp.generated.resources.portfolio_quantity
import trade_buddy.composeapp.generated.resources.portfolio_range_30d
import trade_buddy.composeapp.generated.resources.portfolio_range_7d
import trade_buddy.composeapp.generated.resources.portfolio_range_all
import trade_buddy.composeapp.generated.resources.portfolio_range_today
import trade_buddy.composeapp.generated.resources.portfolio_refresh
import trade_buddy.composeapp.generated.resources.portfolio_search
import trade_buddy.composeapp.generated.resources.portfolio_summary_total
import trade_buddy.composeapp.generated.resources.portfolio_symbol
import trade_buddy.composeapp.generated.resources.portfolio_table_top
import trade_buddy.composeapp.generated.resources.portfolio_title
import trade_buddy.composeapp.generated.resources.portfolio_title_subtitle
import trade_buddy.composeapp.generated.resources.portfolio_total_value
import trade_buddy.composeapp.generated.resources.currency_eur
import trade_buddy.composeapp.generated.resources.currency_usd

private val LocalPortfolioDisplayCurrency = compositionLocalOf { AppDisplayCurrency.Eur }
private const val USD_TO_EUR_RATE = 0.92

@Composable
fun PortfolioScreen(
    state: PortfolioUiState,
    viewModel: PortfolioViewModel,
    displayCurrency: AppDisplayCurrency
) {
    CompositionLocalProvider(LocalPortfolioDisplayCurrency provides displayCurrency) {
    var editingGroup by remember { mutableStateOf<PortfolioGroup?>(null) }
    var showGroupDialog by rememberSaveable { mutableStateOf(false) }
    var editingPosition by remember { mutableStateOf<PortfolioPositionMetrics?>(null) }
    var showPositionDialog by rememberSaveable { mutableStateOf(false) }

    val bestPosition = state.positions.maxByOrNull { it.profitLossPercent }
    val topPositions = state.positions.take(6)
    val history = state.history
    val latestValue = history.lastOrNull()?.totalValue
    val previousValue = history.dropLast(1).lastOrNull()?.totalValue
    val periodDeltaPercent = if (latestValue != null && previousValue != null && previousValue != 0.0) {
        ((latestValue - previousValue) / previousValue) * 100.0
    } else {
        0.0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
    ) {
        item {
            PortfolioHeaderCard(
                onRefresh = viewModel::refresh,
                onAddGroup = {
                    editingGroup = null
                    showGroupDialog = true
                },
                onAddPosition = {
                    editingPosition = null
                    showPositionDialog = true
                },
                hasGroups = state.groups.isNotEmpty()
            )
        }

        item {
            MetricsGrid(
                totalValue = state.summary.totalValue,
                totalDeltaPercent = state.summary.profitLossPercent,
                totalProfitLoss = state.summary.profitLoss,
                totalCost = state.summary.totalCostBasis,
                positionCount = state.positions.size,
                groupCount = state.groups.size,
                bestPosition = bestPosition
            )
        }

        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 980.dp
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                        PerformanceCard(
                            historyValues = history.map { it.totalValue },
                            selectedRange = state.selectedRange,
                            onRangeSelected = viewModel::setRange,
                            latestValue = latestValue,
                            periodDeltaPercent = periodDeltaPercent
                        )
                        AllocationCard(
                            title = stringResource(Res.string.portfolio_allocation_category),
                            values = state.allocationByCategory
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)
                    ) {
                        Box(modifier = Modifier.weight(2f)) {
                            PerformanceCard(
                                historyValues = history.map { it.totalValue },
                                selectedRange = state.selectedRange,
                                onRangeSelected = viewModel::setRange,
                                latestValue = latestValue,
                                periodDeltaPercent = periodDeltaPercent
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            AllocationCard(
                                title = stringResource(Res.string.portfolio_allocation_category),
                                values = state.allocationByCategory
                            )
                        }
                    }
                }
            }
        }

        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 980.dp
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                        TopPositionsCard(
                            positions = topPositions,
                            onEdit = { metrics ->
                                editingPosition = metrics
                                showPositionDialog = true
                            },
                            onDelete = { positionId -> viewModel.deletePosition(positionId) }
                        )
                        GroupsCard(
                            groups = state.groups,
                            selectedGroupId = state.selectedGroupId,
                            onSelectGroup = viewModel::setSelectedGroup,
                            onAddGroup = {
                                editingGroup = null
                                showGroupDialog = true
                            },
                            onEditGroup = { group ->
                                editingGroup = group
                                showGroupDialog = true
                            },
                            onDeleteGroup = { group -> viewModel.deleteGroup(group.id) }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)
                    ) {
                        Box(modifier = Modifier.weight(2f)) {
                            TopPositionsCard(
                                positions = topPositions,
                                onEdit = { metrics ->
                                    editingPosition = metrics
                                    showPositionDialog = true
                                },
                                onDelete = { positionId -> viewModel.deletePosition(positionId) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            GroupsCard(
                                groups = state.groups,
                                selectedGroupId = state.selectedGroupId,
                                onSelectGroup = viewModel::setSelectedGroup,
                                onAddGroup = {
                                    editingGroup = null
                                    showGroupDialog = true
                                },
                                onEditGroup = { group ->
                                    editingGroup = group
                                    showGroupDialog = true
                                },
                                onDeleteGroup = { group -> viewModel.deleteGroup(group.id) }
                            )
                        }
                    }
                }
            }
        }

        item {
            PositionsCard(
                state = state,
                onSearchChange = viewModel::setSearchQuery,
                onAddPosition = {
                    editingPosition = null
                    showPositionDialog = true
                },
                onEdit = { metrics ->
                    editingPosition = metrics
                    showPositionDialog = true
                },
                onDelete = { metrics -> viewModel.deletePosition(metrics.position.id) }
            )
        }

        item {
            AllocationCard(
                title = stringResource(Res.string.portfolio_allocation_group),
                values = state.allocationByGroup
            )
        }
    }

    if (showGroupDialog) {
        GroupEditorDialog(
            initialGroup = editingGroup,
            onDismiss = {
                showGroupDialog = false
                editingGroup = null
            },
            onSave = { name, colorHex ->
                viewModel.saveGroup(editingGroup?.id, name, colorHex)
                showGroupDialog = false
                editingGroup = null
            }
        )
    }

    if (showPositionDialog) {
        PositionEditorDialog(
            groups = state.groups,
            initial = editingPosition,
            defaultCurrency = displayCurrency,
            onDismiss = {
                showPositionDialog = false
                editingPosition = null
            },
            onSave = { form ->
                viewModel.savePosition(
                    positionId = editingPosition?.position?.id,
                    groupId = form.groupId,
                    name = form.name,
                    symbol = form.symbol,
                    category = form.category,
                    quantity = form.quantity,
                    averagePrice = form.averagePrice,
                    currentPrice = form.currentPrice,
                    currency = form.currency,
                    tags = form.tags
                )
                showPositionDialog = false
                editingPosition = null
            }
        )
    }
    }
}

@Composable
private fun MetricsGrid(
    totalValue: Double,
    totalDeltaPercent: Double,
    totalProfitLoss: Double,
    totalCost: Double,
    positionCount: Int,
    groupCount: Int,
    bestPosition: PortfolioPositionMetrics?
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 880.dp
        val rowContent: @Composable RowScope.() -> Unit = {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.portfolio_summary_total),
                value = totalValue.asMoney(),
                trend = totalDeltaPercent.asSignedPercent(),
                trendPositive = totalDeltaPercent >= 0.0
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.portfolio_profit_loss),
                value = totalProfitLoss.asSignedMoney(),
                subtitle = stringResource(Res.string.portfolio_metric_cost, totalCost.asMoney())
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.portfolio_positions),
                value = positionCount.toString(),
                subtitle = stringResource(Res.string.portfolio_metric_group_count, groupCount)
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.portfolio_metric_best_performer),
                value = bestPosition?.position?.symbol ?: "-",
                trend = bestPosition?.profitLossPercent?.asSignedPercent(),
                trendPositive = (bestPosition?.profitLossPercent ?: 0.0) >= 0.0
            )
        }

        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                MetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(Res.string.portfolio_summary_total),
                    value = totalValue.asMoney(),
                    trend = totalDeltaPercent.asSignedPercent(),
                    trendPositive = totalDeltaPercent >= 0.0
                )
                MetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(Res.string.portfolio_profit_loss),
                    value = totalProfitLoss.asSignedMoney(),
                    subtitle = stringResource(Res.string.portfolio_metric_cost, totalCost.asMoney())
                )
                MetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(Res.string.portfolio_positions),
                    value = positionCount.toString(),
                    subtitle = stringResource(Res.string.portfolio_metric_group_count, groupCount)
                )
                MetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(Res.string.portfolio_metric_best_performer),
                    value = bestPosition?.position?.symbol ?: "-",
                    trend = bestPosition?.profitLossPercent?.asSignedPercent(),
                    trendPositive = (bestPosition?.profitLossPercent ?: 0.0) >= 0.0
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)
            ) {
                rowContent()
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String? = null,
    trend: String? = null,
    trendPositive: Boolean = true
) {
    val ext = MaterialTheme.extended
    ElevatedCard(
        modifier = modifier,
        colors = appBlockCardColors(),
        elevation = appFlatCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.s),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            when {
                trend != null -> {
                    Text(
                        text = trend,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (trendPositive) ext.positive else ext.negative
                    )
                }

                subtitle != null -> {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceCard(
    historyValues: List<Double>,
    selectedRange: PerformanceRange,
    onRangeSelected: (PerformanceRange) -> Unit,
    latestValue: Double?,
    periodDeltaPercent: Double
) {
    val ext = MaterialTheme.extended
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = appBlockCardColors(),
        elevation = appFlatCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = SnowIcons.ChartLine,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(Res.string.portfolio_performance),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = AppSpacing.xs)
                )
                Spacer(Modifier.weight(1f))
                if (latestValue != null) {
                    Text(
                        text = latestValue.asMoney(),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SnowTabs(
                    items = PerformanceRange.entries.map { range ->
                        SnowTabItem(id = range.name, label = rangeLabel(range))
                    },
                    selectedId = selectedRange.name,
                    onSelect = { id ->
                        PerformanceRange.entries.firstOrNull { it.name == id }?.let(onRangeSelected)
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = periodDeltaPercent.asSignedPercent(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (periodDeltaPercent >= 0.0) ext.positive else ext.negative
                )
            }

            if (historyValues.isEmpty()) {
                Text(
                    stringResource(Res.string.portfolio_no_history),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                PortfolioLineChart(
                    values = historyValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
        }
    }
}

@Composable
private fun AllocationCard(
    title: String,
    values: List<PortfolioAllocationSlice>
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = appBlockCardColors(),
        elevation = appFlatCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = SnowIcons.ChartPie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = AppSpacing.xs)
                )
            }

            if (values.isEmpty()) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SnowBarChart(
                    entries = values.take(6).mapIndexed { index, slice ->
                        SnowBarEntry(
                            label = slice.label,
                            value = slice.percentage,
                            color = allocationColor(index)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
            AllocationList(values)
        }
    }
}

@Composable
private fun AllocationLegendRow(
    slice: PortfolioAllocationSlice,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(color)
        Text(
            text = slice.label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = slice.percentage.asPercent(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun PortfolioHeaderCard(
    onRefresh: () -> Unit,
    onAddGroup: () -> Unit,
    onAddPosition: () -> Unit,
    hasGroups: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = appBlockCardColors(),
        elevation = appFlatCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.portfolio_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(Res.string.portfolio_title_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = SnowIcons.Refresh,
                        contentDescription = stringResource(Res.string.portfolio_refresh)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                FilledTonalButton(onClick = onAddGroup) {
                    Icon(SnowIcons.Plus, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                    Text(
                        stringResource(Res.string.portfolio_add_group),
                        modifier = Modifier.padding(start = AppSpacing.xxs)
                    )
                }
                FilledTonalButton(
                    enabled = hasGroups,
                    onClick = onAddPosition
                ) {
                    Icon(SnowIcons.Plus, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                    Text(
                        stringResource(Res.string.portfolio_add_position),
                        modifier = Modifier.padding(start = AppSpacing.xxs)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopPositionsCard(
    positions: List<PortfolioPositionMetrics>,
    onEdit: (PortfolioPositionMetrics) -> Unit,
    onDelete: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = appBlockCardColors(),
        elevation = appFlatCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
        ) {
            Text(
                text = stringResource(Res.string.portfolio_table_top),
                style = MaterialTheme.typography.titleMedium
            )
            if (positions.isEmpty()) {
                Text(
                    stringResource(Res.string.portfolio_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                PositionTableHeader()
                positions.forEach { metrics ->
                    PositionTableRow(
                        metrics = metrics,
                        onEdit = { onEdit(metrics) },
                        onDelete = { onDelete(metrics.position.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PositionHeaderCell(text = stringResource(Res.string.portfolio_header_asset), weight = 2.4f)
        PositionHeaderCell(text = stringResource(Res.string.portfolio_header_price), weight = 1.0f)
        PositionHeaderCell(text = stringResource(Res.string.portfolio_header_qty), weight = 0.8f)
        PositionHeaderCell(text = stringResource(Res.string.portfolio_header_amount), weight = 1.2f)
        PositionHeaderCell(text = stringResource(Res.string.portfolio_header_pl), weight = 1.0f)
        Spacer(Modifier.width(58.dp))
    }
    HorizontalDivider(color = MaterialTheme.extended.shellDivider)
}

@Composable
private fun RowScope.PositionHeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun PositionTableRow(
    metrics: PortfolioPositionMetrics,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = MaterialTheme.extended
    val pnlColor = if (metrics.profitLoss >= 0.0) ext.positive else ext.negative
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2.4f)) {
            Text(
                text = "${metrics.position.symbol} ${metrics.position.name}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = categoryLabel(metrics.position.category),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = metrics.position.currentPrice.asMoney(metrics.position.currency),
            modifier = Modifier.weight(1.0f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = metrics.position.quantity.asQuantity(),
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = metrics.marketValue.asMoney(metrics.position.currency),
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = metrics.profitLossPercent.asSignedPercent(),
            modifier = Modifier.weight(1.0f),
            style = MaterialTheme.typography.bodySmall,
            color = pnlColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
            Icon(SnowIcons.Edit, contentDescription = stringResource(Res.string.portfolio_edit), modifier = Modifier.size(AppIconSize.xs))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
            Icon(SnowIcons.Delete, contentDescription = stringResource(Res.string.portfolio_delete), modifier = Modifier.size(AppIconSize.xs))
        }
    }
    HorizontalDivider(color = MaterialTheme.extended.shellDivider.copy(alpha = 0.7f))
}

@Composable
private fun GroupsCard(
    groups: List<PortfolioGroup>,
    selectedGroupId: String?,
    onSelectGroup: (String?) -> Unit,
    onAddGroup: () -> Unit,
    onEditGroup: (PortfolioGroup) -> Unit,
    onDeleteGroup: (PortfolioGroup) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = appBlockCardColors(),
        elevation = appFlatCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.portfolio_groups),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onAddGroup, modifier = Modifier.size(28.dp)) {
                    Icon(SnowIcons.Plus, contentDescription = stringResource(Res.string.portfolio_add_group), modifier = Modifier.size(AppIconSize.xs))
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                item {
                    AssistChip(
                        onClick = { onSelectGroup(null) },
                        label = { Text(stringResource(Res.string.portfolio_all_groups)) },
                        leadingIcon = if (selectedGroupId == null) {
                            { Dot(MaterialTheme.colorScheme.primary) }
                        } else {
                            null
                        }
                    )
                }
                items(groups) { group ->
                    AssistChip(
                        onClick = { onSelectGroup(group.id) },
                        label = { Text(group.name) },
                        leadingIcon = {
                            Dot(group.colorHex.toColorOrDefault())
                        }
                    )
                }
            }

            if (groups.isEmpty()) {
                Text(
                    text = stringResource(Res.string.portfolio_no_groups),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                groups.forEach { group ->
                    GroupRow(
                        group = group,
                        onEdit = { onEditGroup(group) },
                        onDelete = { onDeleteGroup(group) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionsCard(
    state: PortfolioUiState,
    onSearchChange: (String) -> Unit,
    onAddPosition: () -> Unit,
    onEdit: (PortfolioPositionMetrics) -> Unit,
    onDelete: (PortfolioPositionMetrics) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = appBlockCardColors(),
        elevation = appFlatCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.portfolio_positions),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onAddPosition,
                    enabled = state.groups.isNotEmpty()
                ) {
                    Icon(SnowIcons.Plus, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                    Text(
                        stringResource(Res.string.portfolio_add_position),
                        modifier = Modifier.padding(start = AppSpacing.xxs)
                    )
                }
            }

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(SnowIcons.Search, contentDescription = null)
                },
                label = { Text(stringResource(Res.string.portfolio_search)) }
            )

            if (state.positions.isEmpty()) {
                Text(
                    stringResource(Res.string.portfolio_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.positions.forEach { metrics ->
                    PositionRow(
                        metrics = metrics,
                        groupName = state.groups.firstOrNull { it.id == metrics.position.groupId }?.name.orEmpty(),
                        onEdit = { onEdit(metrics) },
                        onDelete = { onDelete(metrics) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor
        )
    }
}

@Composable
private fun AllocationList(values: List<PortfolioAllocationSlice>) {
    if (values.isEmpty()) {
        Text(
            text = "-",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        values.forEach { slice ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${slice.value.asMoney()} (${slice.percentage.asPercent()})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { (slice.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: PortfolioGroup,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(group.colorHex.toColorOrDefault())
        Text(
            text = group.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = AppSpacing.xs)
        )
        IconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
            Icon(SnowIcons.Edit, contentDescription = stringResource(Res.string.portfolio_edit), modifier = Modifier.size(AppIconSize.xs))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
            Icon(SnowIcons.Delete, contentDescription = stringResource(Res.string.portfolio_delete), modifier = Modifier.size(AppIconSize.xs))
        }
    }
}

@Composable
private fun PositionRow(
    metrics: PortfolioPositionMetrics,
    groupName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val pnlColor = if (metrics.profitLoss >= 0.0) MaterialTheme.extended.positive else MaterialTheme.extended.negative
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = appBlockCardColors(),
        elevation = appFlatCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.s),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${metrics.position.symbol} ${metrics.position.name}",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${metrics.position.category.label} | $groupName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(SnowIcons.Edit, contentDescription = stringResource(Res.string.portfolio_edit), modifier = Modifier.size(AppIconSize.xs))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(SnowIcons.Delete, contentDescription = stringResource(Res.string.portfolio_delete), modifier = Modifier.size(AppIconSize.xs))
                }
            }

            SummaryRow(
                label = stringResource(Res.string.portfolio_total_value),
                value = metrics.marketValue.asMoney(metrics.position.currency)
            )
            SummaryRow(
                label = stringResource(Res.string.portfolio_profit_loss),
                value = "${metrics.profitLoss.asSignedMoney(metrics.position.currency)} (${metrics.profitLossPercent.asSignedPercent()})",
                valueColor = pnlColor
            )
            SummaryRow(
                label = stringResource(Res.string.portfolio_quantity),
                value = metrics.position.quantity.asQuantity()
            )
            SummaryRow(
                label = stringResource(Res.string.portfolio_average_price),
                value = metrics.position.averagePrice.asMoney(metrics.position.currency)
            )
            SummaryRow(
                label = stringResource(Res.string.portfolio_current_price),
                value = metrics.position.currentPrice.asMoney(metrics.position.currency)
            )
        }
    }
}

@Composable
private fun PortfolioLineChart(
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    if (values.size <= 1) return
    val ext = MaterialTheme.extended
    SnowLineChart(
        series = listOf(
            SnowLineSeries(
                name = "portfolio",
                values = values,
                color = ext.info
            )
        ),
        modifier = modifier,
        showAxes = false
    )
}

@Composable
private fun GroupEditorDialog(
    initialGroup: PortfolioGroup?,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String) -> Unit
) {
    var name by rememberSaveable(initialGroup?.id) { mutableStateOf(initialGroup?.name.orEmpty()) }
    var color by rememberSaveable(initialGroup?.id) {
        mutableStateOf(initialGroup?.colorHex ?: AppPalette.DefaultPortfolioHex)
    }
    val palette = remember { AppPalette.PortfolioPaletteHex }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.portfolio_group)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.portfolio_name)) }
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    items(palette) { swatch ->
                        Box(
                            modifier = Modifier
                                .size(if (swatch == color) 28.dp else 24.dp)
                                .clip(CircleShape)
                                .background(swatch.toColorOrDefault())
                                .clickable { color = swatch }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name, color) }
            ) {
                Text(stringResource(Res.string.portfolio_add_group))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.datepicker_cancel))
            }
        }
    )
}

private data class PositionEditorForm(
    val groupId: String,
    val name: String,
    val symbol: String,
    val category: AssetCategory,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double,
    val currency: String,
    val tags: Set<String>
)

@Composable
private fun PositionEditorDialog(
    groups: List<PortfolioGroup>,
    initial: PortfolioPositionMetrics?,
    defaultCurrency: AppDisplayCurrency,
    onDismiss: () -> Unit,
    onSave: (PositionEditorForm) -> Unit
) {
    var selectedGroupId by rememberSaveable(initial?.position?.id) {
        mutableStateOf(initial?.position?.groupId ?: groups.firstOrNull()?.id.orEmpty())
    }
    var name by rememberSaveable(initial?.position?.id) { mutableStateOf(initial?.position?.name.orEmpty()) }
    var symbol by rememberSaveable(initial?.position?.id) { mutableStateOf(initial?.position?.symbol.orEmpty()) }
    var quantityText by rememberSaveable(initial?.position?.id) { mutableStateOf(initial?.position?.quantity?.toPlainText() ?: "0") }
    var averageText by rememberSaveable(initial?.position?.id) { mutableStateOf(initial?.position?.averagePrice?.toPlainText() ?: "0") }
    var currentText by rememberSaveable(initial?.position?.id) { mutableStateOf(initial?.position?.currentPrice?.toPlainText() ?: "0") }
    var currency by rememberSaveable(initial?.position?.id) {
        mutableStateOf(
            when (initial?.position?.currency?.uppercase()) {
                "EUR" -> "EUR"
                "USD" -> "USD"
                else -> defaultCurrency.toCurrencyCode()
            }
        )
    }
    var tags by rememberSaveable(initial?.position?.id) { mutableStateOf(initial?.position?.tags?.joinToString(", ").orEmpty()) }
    var category by rememberSaveable(initial?.position?.id) { mutableStateOf(initial?.position?.category ?: AssetCategory.Equity) }

    val quantity = quantityText.toDoubleOrNull()
    val averagePrice = averageText.toDoubleOrNull()
    val currentPrice = currentText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.portfolio_add_position)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                if (groups.isEmpty()) {
                    Text(
                        stringResource(Res.string.portfolio_empty),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        items(groups) { group ->
                            AssistChip(
                                onClick = { selectedGroupId = group.id },
                                label = { Text(group.name) },
                                leadingIcon = if (selectedGroupId == group.id) {
                                    { Dot(group.colorHex.toColorOrDefault()) }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.portfolio_name)) }
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.portfolio_symbol)) }
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    items(AssetCategory.entries) { candidate ->
                        AssistChip(
                            onClick = { category = candidate },
                            label = { Text(categoryLabel(candidate)) },
                            leadingIcon = if (candidate == category) {
                                { Dot(MaterialTheme.colorScheme.primary) }
                            } else {
                                null
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = sanitizePositiveDecimalInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(stringResource(Res.string.portfolio_quantity)) }
                )
                OutlinedTextField(
                    value = averageText,
                    onValueChange = { averageText = sanitizePositiveDecimalInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(stringResource(Res.string.portfolio_average_price)) }
                )
                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = sanitizePositiveDecimalInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(stringResource(Res.string.portfolio_current_price)) }
                )
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                    Text(
                        text = stringResource(Res.string.portfolio_currency),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        listOf("EUR", "USD").forEach { candidate ->
                            AssistChip(
                                onClick = { currency = candidate },
                                label = { Text(currencyDisplayLabel(candidate)) },
                                leadingIcon = if (currency == candidate) {
                                    { Dot(MaterialTheme.colorScheme.primary) }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.portfolio_category)) }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = groups.isNotEmpty() &&
                    selectedGroupId.isNotBlank() &&
                    name.isNotBlank() &&
                    symbol.isNotBlank() &&
                    quantity != null &&
                    averagePrice != null &&
                    currentPrice != null,
                onClick = {
                    onSave(
                        PositionEditorForm(
                            groupId = selectedGroupId,
                            name = name,
                            symbol = symbol,
                            category = category,
                            quantity = quantity ?: 0.0,
                            averagePrice = averagePrice ?: 0.0,
                            currentPrice = currentPrice ?: 0.0,
                            currency = currency.uppercase(),
                            tags = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                        )
                    )
                }
            ) {
                Text(stringResource(Res.string.portfolio_add_position))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.datepicker_cancel))
            }
        }
    )
}

private fun allocationColor(index: Int): Color {
    val palette = AppPalette.PortfolioPalette
    return palette[index % palette.size]
}

@Composable
private fun rangeLabel(range: PerformanceRange): String = when (range) {
    PerformanceRange.Today -> stringResource(Res.string.portfolio_range_today)
    PerformanceRange.SevenDays -> stringResource(Res.string.portfolio_range_7d)
    PerformanceRange.ThirtyDays -> stringResource(Res.string.portfolio_range_30d)
    PerformanceRange.All -> stringResource(Res.string.portfolio_range_all)
}

@Composable
private fun categoryLabel(category: AssetCategory): String = when (category) {
    AssetCategory.Equity -> stringResource(Res.string.portfolio_category_equity)
    AssetCategory.Etf -> stringResource(Res.string.portfolio_category_etf)
    AssetCategory.Crypto -> stringResource(Res.string.portfolio_category_crypto)
    AssetCategory.Bond -> stringResource(Res.string.portfolio_category_bond)
    AssetCategory.Commodity -> stringResource(Res.string.portfolio_category_commodity)
    AssetCategory.Forex -> stringResource(Res.string.portfolio_category_forex)
    AssetCategory.Cash -> stringResource(Res.string.portfolio_category_cash)
    AssetCategory.Other -> stringResource(Res.string.portfolio_category_other)
}

private fun String.toColorOrDefault(): Color = runCatching {
    val normalized = removePrefix("#")
    val value = normalized.toLong(16)
    when (normalized.length) {
        6 -> Color((0xFF000000 or value).toInt())
        8 -> Color(value.toInt())
        else -> AppPalette.Indigo
    }
}.getOrDefault(AppPalette.Indigo)

@Composable
private fun Double.asMoney(sourceCurrencyCode: String = "USD"): String {
    val targetCurrency = LocalPortfolioDisplayCurrency.current
    val sourceCurrency = sourceCurrencyCode.toDisplayCurrencyOrNull()
    val convertedValue = if (sourceCurrency != null) {
        convertCurrencyAmount(this, sourceCurrency, targetCurrency)
    } else {
        this
    }
    return "${targetCurrency.toCurrencyCode()} ${convertedValue.toPlainText()}"
}

@Composable
private fun Double.asSignedMoney(sourceCurrencyCode: String = "USD"): String {
    val targetCurrency = LocalPortfolioDisplayCurrency.current
    val sourceCurrency = sourceCurrencyCode.toDisplayCurrencyOrNull()
    val convertedValue = if (sourceCurrency != null) {
        convertCurrencyAmount(this, sourceCurrency, targetCurrency)
    } else {
        this
    }
    val absValue = abs(convertedValue).toPlainText()
    val prefix = if (this >= 0.0) "+" else "-"
    return "$prefix${targetCurrency.toCurrencyCode()} $absValue"
}

private fun Double.asPercent(): String = "${toPlainText()}%"

private fun Double.asSignedPercent(): String {
    val absValue = abs(this).toPlainText()
    val prefix = if (this >= 0.0) "+" else "-"
    return "$prefix$absValue%"
}

private fun Double.asQuantity(): String = toPlainText()

private fun Double.toPlainText(): String {
    val rounded = (this * 100.0).roundToInt() / 100.0
    val raw = rounded.toString()
    return if (raw.endsWith(".0")) raw.dropLast(2) else raw
}

private fun String.toDisplayCurrencyOrNull(): AppDisplayCurrency? = when (uppercase()) {
    "EUR" -> AppDisplayCurrency.Eur
    "USD" -> AppDisplayCurrency.Usd
    else -> null
}

private fun AppDisplayCurrency.toCurrencyCode(): String = when (this) {
    AppDisplayCurrency.Eur -> "EUR"
    AppDisplayCurrency.Usd -> "USD"
}

private fun convertCurrencyAmount(
    amount: Double,
    sourceCurrency: AppDisplayCurrency,
    targetCurrency: AppDisplayCurrency
): Double {
    if (sourceCurrency == targetCurrency) return amount
    return when {
        sourceCurrency == AppDisplayCurrency.Usd && targetCurrency == AppDisplayCurrency.Eur -> amount * USD_TO_EUR_RATE
        sourceCurrency == AppDisplayCurrency.Eur && targetCurrency == AppDisplayCurrency.Usd -> amount / USD_TO_EUR_RATE
        else -> amount
    }
}

@Composable
private fun currencyDisplayLabel(currencyCode: String): String = when (currencyCode.uppercase()) {
    "EUR" -> stringResource(Res.string.currency_eur)
    "USD" -> stringResource(Res.string.currency_usd)
    else -> currencyCode
}

private fun sanitizePositiveDecimalInput(rawInput: String): String {
    val normalized = rawInput.replace(',', '.')
    val builder = StringBuilder()
    var hasDot = false
    normalized.forEachIndexed { index, ch ->
        when {
            ch.isDigit() -> builder.append(ch)
            ch == '.' && !hasDot -> {
                hasDot = true
                if (builder.isEmpty()) builder.append('0')
                builder.append('.')
            }
            ch == '-' && index == 0 -> Unit
            else -> Unit
        }
    }
    return builder.toString()
}

