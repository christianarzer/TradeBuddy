package de.tradebuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.tradebuddy.data.DefaultCityDataSource
import de.tradebuddy.domain.model.BacktestEdgeGateDecision
import de.tradebuddy.domain.model.BacktestEdgeValidation
import de.tradebuddy.domain.model.BacktestEdgeValidationStatus
import de.tradebuddy.domain.model.BacktestExchange
import de.tradebuddy.domain.model.BacktestHistoryRun
import de.tradebuddy.domain.model.BacktestMarginMode
import de.tradebuddy.domain.model.BacktestOptimizationMode
import de.tradebuddy.domain.model.BacktestOptimizationRun
import de.tradebuddy.domain.model.BacktestOrderType
import de.tradebuddy.domain.model.BacktestPositionSizingMode
import de.tradebuddy.domain.model.BacktestResult
import de.tradebuddy.domain.model.BacktestStrategyTemplate
import de.tradebuddy.domain.model.BacktestTimeframe
import de.tradebuddy.domain.model.BacktestTrade
import de.tradebuddy.domain.model.BacktestTradeSide
import de.tradebuddy.domain.util.key
import de.tradebuddy.presentation.BacktestEdgeGatePreset
import de.tradebuddy.presentation.BacktestOptimizationObjective
import de.tradebuddy.presentation.BacktestTradesSort
import de.tradebuddy.presentation.BacktestCustomPreset
import de.tradebuddy.presentation.BacktestingUiState
import de.tradebuddy.presentation.BacktestingViewModel
import de.tradebuddy.ui.charts.SnowBarChart
import de.tradebuddy.ui.charts.SnowBarEntry
import de.tradebuddy.ui.charts.SnowCandleEntry
import de.tradebuddy.ui.charts.SnowChartAnnotation
import de.tradebuddy.ui.charts.SnowChartLegend
import de.tradebuddy.ui.charts.SnowInteractiveCandleChart
import de.tradebuddy.ui.charts.SnowLineChart
import de.tradebuddy.ui.charts.SnowLineChartWithMarkers
import de.tradebuddy.ui.charts.SnowChartMarker
import de.tradebuddy.ui.charts.SnowChartSegment
import de.tradebuddy.ui.charts.SnowLineSeries
import de.tradebuddy.ui.components.rememberCopyTextToClipboard
import de.tradebuddy.ui.components.shared.SnowToolbar
import de.tradebuddy.ui.components.shared.SnowToolbarIconButton
import de.tradebuddy.ui.components.shared.SnowToolbarPopupPanel
import de.tradebuddy.ui.icons.SnowIcons
import de.tradebuddy.ui.theme.AppIconSize
import de.tradebuddy.ui.theme.AppSpacing
import de.tradebuddy.ui.theme.extended
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.backtesting_allow_short
import trade_buddy.composeapp.generated.resources.backtesting_allow_hedging
import trade_buddy.composeapp.generated.resources.backtesting_analysis_in_sample
import trade_buddy.composeapp.generated.resources.backtesting_analysis_iterations
import trade_buddy.composeapp.generated.resources.backtesting_analysis_median
import trade_buddy.composeapp.generated.resources.backtesting_analysis_out_sample
import trade_buddy.composeapp.generated.resources.backtesting_analysis_p05
import trade_buddy.composeapp.generated.resources.backtesting_analysis_p95
import trade_buddy.composeapp.generated.resources.backtesting_analysis_splits
import trade_buddy.composeapp.generated.resources.backtesting_analysis_stability
import trade_buddy.composeapp.generated.resources.backtesting_analysis_title
import trade_buddy.composeapp.generated.resources.backtesting_annual_volatility
import trade_buddy.composeapp.generated.resources.backtesting_astro_aspect_scope
import trade_buddy.composeapp.generated.resources.backtesting_astro_aspect_scope_all
import trade_buddy.composeapp.generated.resources.backtesting_astro_aspect_scope_moon_only
import trade_buddy.composeapp.generated.resources.backtesting_astro_city
import trade_buddy.composeapp.generated.resources.backtesting_astro_include_aspects
import trade_buddy.composeapp.generated.resources.backtesting_astro_include_moon_events
import trade_buddy.composeapp.generated.resources.backtesting_astro_include_moon_phases
import trade_buddy.composeapp.generated.resources.backtesting_astro_include_sun_events
import trade_buddy.composeapp.generated.resources.backtesting_astro_signal_window_bars
import trade_buddy.composeapp.generated.resources.backtesting_astro_signal_window_bars_help
import trade_buddy.composeapp.generated.resources.backtesting_atr_period
import trade_buddy.composeapp.generated.resources.backtesting_atr_risk_percent
import trade_buddy.composeapp.generated.resources.backtesting_average_trade
import trade_buddy.composeapp.generated.resources.backtesting_breakout_lookback
import trade_buddy.composeapp.generated.resources.backtesting_cagr
import trade_buddy.composeapp.generated.resources.backtesting_cancel
import trade_buddy.composeapp.generated.resources.backtesting_chart_drawdown
import trade_buddy.composeapp.generated.resources.backtesting_chart_equity
import trade_buddy.composeapp.generated.resources.backtesting_chart_labels_all
import trade_buddy.composeapp.generated.resources.backtesting_chart_labels_mode
import trade_buddy.composeapp.generated.resources.backtesting_chart_labels_selected
import trade_buddy.composeapp.generated.resources.backtesting_chart_trade_pnl
import trade_buddy.composeapp.generated.resources.backtesting_compare_title
import trade_buddy.composeapp.generated.resources.backtesting_custom_strategy_json
import trade_buddy.composeapp.generated.resources.backtesting_data_notes
import trade_buddy.composeapp.generated.resources.backtesting_daily_loss_limit
import trade_buddy.composeapp.generated.resources.backtesting_date_end
import trade_buddy.composeapp.generated.resources.backtesting_date_start
import trade_buddy.composeapp.generated.resources.backtesting_delete
import trade_buddy.composeapp.generated.resources.backtesting_expectancy
import trade_buddy.composeapp.generated.resources.backtesting_execution_title
import trade_buddy.composeapp.generated.resources.backtesting_exchange
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_cpcv
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_notes
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_oos_return
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_oos_sharpe
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_positive_paths
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_samples
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_score
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_spa_pvalue
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_status
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_status_failed
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_status_passed
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_status_unavailable
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_status_warning
import trade_buddy.composeapp.generated.resources.backtesting_edge_lab_title
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_enabled
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_require_passed_status
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_min_edge_score
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_max_spa_pvalue
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_min_positive_paths
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_min_trades
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_min_oos_sharpe
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_title
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_status
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_passed
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_failed
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_preset
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_preset_aggressive
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_preset_custom
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_preset_moderate
import trade_buddy.composeapp.generated.resources.backtesting_edge_gate_preset_strict
import trade_buddy.composeapp.generated.resources.backtesting_exposure_time
import trade_buddy.composeapp.generated.resources.backtesting_export_json
import trade_buddy.composeapp.generated.resources.backtesting_export_trades
import trade_buddy.composeapp.generated.resources.backtesting_fee_bps
import trade_buddy.composeapp.generated.resources.backtesting_fill_ratio_percent
import trade_buddy.composeapp.generated.resources.backtesting_filter_sort
import trade_buddy.composeapp.generated.resources.backtesting_force_refresh
import trade_buddy.composeapp.generated.resources.backtesting_funding_bps_per_day
import trade_buddy.composeapp.generated.resources.backtesting_header_duration
import trade_buddy.composeapp.generated.resources.backtesting_header_entry
import trade_buddy.composeapp.generated.resources.backtesting_header_exit
import trade_buddy.composeapp.generated.resources.backtesting_header_pnl
import trade_buddy.composeapp.generated.resources.backtesting_header_reason
import trade_buddy.composeapp.generated.resources.backtesting_header_side
import trade_buddy.composeapp.generated.resources.backtesting_history_empty
import trade_buddy.composeapp.generated.resources.backtesting_history_open
import trade_buddy.composeapp.generated.resources.backtesting_history_reload
import trade_buddy.composeapp.generated.resources.backtesting_history_title
import trade_buddy.composeapp.generated.resources.backtesting_history_toggle_compare
import trade_buddy.composeapp.generated.resources.backtesting_initial_balance
import trade_buddy.composeapp.generated.resources.backtesting_kill_switch_drawdown
import trade_buddy.composeapp.generated.resources.backtesting_leverage
import trade_buddy.composeapp.generated.resources.backtesting_legend_long_entry
import trade_buddy.composeapp.generated.resources.backtesting_legend_long_exit
import trade_buddy.composeapp.generated.resources.backtesting_legend_short_entry
import trade_buddy.composeapp.generated.resources.backtesting_legend_short_exit
import trade_buddy.composeapp.generated.resources.backtesting_limit_offset_bps
import trade_buddy.composeapp.generated.resources.backtesting_long_only
import trade_buddy.composeapp.generated.resources.backtesting_maker_fee_bps
import trade_buddy.composeapp.generated.resources.backtesting_market_data_title
import trade_buddy.composeapp.generated.resources.backtesting_margin_cash
import trade_buddy.composeapp.generated.resources.backtesting_margin_cross
import trade_buddy.composeapp.generated.resources.backtesting_margin_isolated
import trade_buddy.composeapp.generated.resources.backtesting_margin_mode
import trade_buddy.composeapp.generated.resources.backtesting_max_drawdown
import trade_buddy.composeapp.generated.resources.backtesting_max_volume_participation
import trade_buddy.composeapp.generated.resources.backtesting_monthly_returns
import trade_buddy.composeapp.generated.resources.backtesting_no_results
import trade_buddy.composeapp.generated.resources.backtesting_monte_carlo
import trade_buddy.composeapp.generated.resources.backtesting_order_limit
import trade_buddy.composeapp.generated.resources.backtesting_order_market
import trade_buddy.composeapp.generated.resources.backtesting_order_type
import trade_buddy.composeapp.generated.resources.backtesting_optimization_iterations
import trade_buddy.composeapp.generated.resources.backtesting_optimization_mode
import trade_buddy.composeapp.generated.resources.backtesting_optimization_objective
import trade_buddy.composeapp.generated.resources.backtesting_optimization_range_title
import trade_buddy.composeapp.generated.resources.backtesting_optimization_score
import trade_buddy.composeapp.generated.resources.backtesting_optimization_sma_fast
import trade_buddy.composeapp.generated.resources.backtesting_optimization_sma_slow
import trade_buddy.composeapp.generated.resources.backtesting_optimization_rsi_period
import trade_buddy.composeapp.generated.resources.backtesting_optimization_breakout
import trade_buddy.composeapp.generated.resources.backtesting_optimization_min
import trade_buddy.composeapp.generated.resources.backtesting_optimization_max
import trade_buddy.composeapp.generated.resources.backtesting_optimization_step
import trade_buddy.composeapp.generated.resources.backtesting_optimization_top_k
import trade_buddy.composeapp.generated.resources.backtesting_optimization_title
import trade_buddy.composeapp.generated.resources.backtesting_optimize
import trade_buddy.composeapp.generated.resources.backtesting_optimization_heatmap_title
import trade_buddy.composeapp.generated.resources.backtesting_optimization_matrix_title
import trade_buddy.composeapp.generated.resources.backtesting_optimization_filter_profitable
import trade_buddy.composeapp.generated.resources.backtesting_optimization_filter_min_trades
import trade_buddy.composeapp.generated.resources.backtesting_optimization_sort_by
import trade_buddy.composeapp.generated.resources.backtesting_optimization_sort_score
import trade_buddy.composeapp.generated.resources.backtesting_optimization_sort_return
import trade_buddy.composeapp.generated.resources.backtesting_optimization_sort_sharpe
import trade_buddy.composeapp.generated.resources.backtesting_optimization_sort_drawdown
import trade_buddy.composeapp.generated.resources.backtesting_position_size_fixed
import trade_buddy.composeapp.generated.resources.backtesting_position_size_mode
import trade_buddy.composeapp.generated.resources.backtesting_position_size_percent
import trade_buddy.composeapp.generated.resources.backtesting_profit_factor
import trade_buddy.composeapp.generated.resources.backtesting_result_subtitle
import trade_buddy.composeapp.generated.resources.backtesting_results_title
import trade_buddy.composeapp.generated.resources.backtesting_rsi_overbought
import trade_buddy.composeapp.generated.resources.backtesting_rsi_oversold
import trade_buddy.composeapp.generated.resources.backtesting_rsi_period
import trade_buddy.composeapp.generated.resources.backtesting_run
import trade_buddy.composeapp.generated.resources.backtesting_run_title
import trade_buddy.composeapp.generated.resources.backtesting_sharpe
import trade_buddy.composeapp.generated.resources.backtesting_show_signal_preview
import trade_buddy.composeapp.generated.resources.backtesting_sizing_fixed
import trade_buddy.composeapp.generated.resources.backtesting_sizing_atr_risk
import trade_buddy.composeapp.generated.resources.backtesting_sizing_percent
import trade_buddy.composeapp.generated.resources.backtesting_sizing_volatility_target
import trade_buddy.composeapp.generated.resources.backtesting_slippage_bps
import trade_buddy.composeapp.generated.resources.backtesting_sma_fast
import trade_buddy.composeapp.generated.resources.backtesting_sma_slow
import trade_buddy.composeapp.generated.resources.backtesting_sortino
import trade_buddy.composeapp.generated.resources.backtesting_sort_entry_newest
import trade_buddy.composeapp.generated.resources.backtesting_sort_entry_oldest
import trade_buddy.composeapp.generated.resources.backtesting_sort_pnl_asc
import trade_buddy.composeapp.generated.resources.backtesting_sort_pnl_desc
import trade_buddy.composeapp.generated.resources.backtesting_strategy_title
import trade_buddy.composeapp.generated.resources.backtesting_strategy_type
import trade_buddy.composeapp.generated.resources.backtesting_subtitle
import trade_buddy.composeapp.generated.resources.backtesting_section_info_content_description
import trade_buddy.composeapp.generated.resources.backtesting_section_info_strategy
import trade_buddy.composeapp.generated.resources.backtesting_section_info_market_data
import trade_buddy.composeapp.generated.resources.backtesting_section_info_execution
import trade_buddy.composeapp.generated.resources.backtesting_section_info_run
import trade_buddy.composeapp.generated.resources.backtesting_section_info_results
import trade_buddy.composeapp.generated.resources.backtesting_section_info_history
import trade_buddy.composeapp.generated.resources.backtesting_symbol
import trade_buddy.composeapp.generated.resources.backtesting_symbol_loading
import trade_buddy.composeapp.generated.resources.backtesting_symbol_refresh
import trade_buddy.composeapp.generated.resources.backtesting_symbol_search
import trade_buddy.composeapp.generated.resources.backtesting_symbol_no_match
import trade_buddy.composeapp.generated.resources.backtesting_stop_loss
import trade_buddy.composeapp.generated.resources.backtesting_taker_fee_bps
import trade_buddy.composeapp.generated.resources.backtesting_take_profit
import trade_buddy.composeapp.generated.resources.backtesting_target_volatility_percent
import trade_buddy.composeapp.generated.resources.backtesting_timeframe
import trade_buddy.composeapp.generated.resources.backtesting_trade_label_compact
import trade_buddy.composeapp.generated.resources.backtesting_title
import trade_buddy.composeapp.generated.resources.backtesting_total_return
import trade_buddy.composeapp.generated.resources.backtesting_trades_count
import trade_buddy.composeapp.generated.resources.backtesting_trades_empty
import trade_buddy.composeapp.generated.resources.backtesting_trades_search
import trade_buddy.composeapp.generated.resources.backtesting_trades_title
import trade_buddy.composeapp.generated.resources.backtesting_trailing_stop
import trade_buddy.composeapp.generated.resources.backtesting_walk_forward
import trade_buddy.composeapp.generated.resources.backtesting_walk_forward_fold
import trade_buddy.composeapp.generated.resources.backtesting_warmup_bars
import trade_buddy.composeapp.generated.resources.backtesting_win_rate
import trade_buddy.composeapp.generated.resources.backtesting_chart_price_signals
import trade_buddy.composeapp.generated.resources.backtesting_custom_preset_breakout
import trade_buddy.composeapp.generated.resources.backtesting_custom_preset_mean_reversion
import trade_buddy.composeapp.generated.resources.backtesting_custom_preset_trend
import trade_buddy.composeapp.generated.resources.backtesting_custom_presets_label
import trade_buddy.composeapp.generated.resources.backtesting_custom_json_editor
import trade_buddy.composeapp.generated.resources.backtesting_custom_json_hint
import trade_buddy.composeapp.generated.resources.backtesting_export_optimization
import trade_buddy.composeapp.generated.resources.backtesting_selected_trade
import trade_buddy.composeapp.generated.resources.backtesting_selected_trade_none
import trade_buddy.composeapp.generated.resources.backtesting_selected_trade_focus
import trade_buddy.composeapp.generated.resources.backtesting_selected_trade_timing
import trade_buddy.composeapp.generated.resources.datepicker_cancel
import trade_buddy.composeapp.generated.resources.datepicker_ok

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun BacktestingScreen(
    state: BacktestingUiState,
    viewModel: BacktestingViewModel
) {
    val ext = MaterialTheme.extended
    val copyToClipboard = rememberCopyTextToClipboard()
    var showStrategyMenu by rememberSaveable { mutableStateOf(false) }
    var showExchangeMenu by rememberSaveable { mutableStateOf(false) }
    var showTimeframeMenu by rememberSaveable { mutableStateOf(false) }
    var showSymbolMenu by rememberSaveable { mutableStateOf(false) }
    var symbolSearchQuery by rememberSaveable(state.exchange) { mutableStateOf("") }
    var showSizingMenu by rememberSaveable { mutableStateOf(false) }
    var showOrderMenu by rememberSaveable { mutableStateOf(false) }
    var showMarginMenu by rememberSaveable { mutableStateOf(false) }
    var showOptimizationModeMenu by rememberSaveable { mutableStateOf(false) }
    var showOptimizationObjectiveMenu by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var optimizationSort by rememberSaveable { mutableStateOf(OptimizationRunSort.Score) }
    var optimizationOnlyProfitable by rememberSaveable { mutableStateOf(false) }
    var optimizationMinTradesInput by rememberSaveable { mutableStateOf("0") }
    var showAllTradeLabels by rememberSaveable { mutableStateOf(false) }
    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }

    val result = state.result
    val visibleTrades = remember(result, state.tradesSearchQuery, state.tradesSort) {
        filterAndSortTrades(result?.trades.orEmpty(), state.tradesSearchQuery, state.tradesSort)
    }
    val comparedRuns = remember(state.history, state.comparisonRunIds) {
        state.history.filter { it.id in state.comparisonRunIds }.take(2)
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
    ) {
        Text(text = stringResource(Res.string.backtesting_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(Res.string.backtesting_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SnowToolbar {
            OutlinedButton(onClick = { viewModel.runBacktest(false) }, enabled = !state.isRunning) {
                Icon(SnowIcons.ChartLine, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.backtesting_run))
            }
            OutlinedButton(onClick = { viewModel.runBacktest(true) }, enabled = !state.isRunning) {
                Icon(SnowIcons.Refresh, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.backtesting_force_refresh))
            }
            OutlinedButton(onClick = { viewModel.runOptimization(false) }, enabled = !state.isRunning) {
                Icon(SnowIcons.Sliders, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.backtesting_optimize))
            }
            OutlinedButton(onClick = { viewModel.runWalkForward(false) }, enabled = !state.isRunning) {
                Icon(SnowIcons.ChartLine, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.backtesting_walk_forward))
            }
            OutlinedButton(onClick = viewModel::cancelBacktest, enabled = state.isRunning) {
                Icon(SnowIcons.Close, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.backtesting_cancel))
            }
            Spacer(Modifier.weight(1f))
            SnowToolbarIconButton(
                icon = SnowIcons.Refresh,
                onClick = viewModel::loadHistory,
                contentDescription = stringResource(Res.string.backtesting_history_reload)
            )
        }

        if (state.isRunning) {
            LinearProgressIndicator(
                progress = { state.runProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = state.runStatus.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = ext.sidebarTextMuted
            )
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                    .padding(AppSpacing.s)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
        ) {
            BacktestingSection(
                title = stringResource(Res.string.backtesting_strategy_title),
                infoText = stringResource(Res.string.backtesting_section_info_strategy)
            ) {
                BacktestSelectChip(
                    label = stringResource(Res.string.backtesting_strategy_type),
                    value = state.selectedStrategy.label,
                    expanded = showStrategyMenu,
                    onExpandedChange = { showStrategyMenu = it }
                ) {
                    BacktestStrategyTemplate.entries.forEach { template ->
                        FilterChip(
                            selected = template == state.selectedStrategy,
                            onClick = {
                                viewModel.setStrategy(template)
                                showStrategyMenu = false
                            },
                            label = { Text(template.label) }
                        )
                    }
                }
                FilterChip(
                    selected = state.showSignalPreview,
                    onClick = { viewModel.setShowSignalPreview(!state.showSignalPreview) },
                    label = { Text(stringResource(Res.string.backtesting_show_signal_preview)) }
                )
                StrategyInputs(state = state, viewModel = viewModel)
            }

            BacktestingSection(
                title = stringResource(Res.string.backtesting_market_data_title),
                infoText = stringResource(Res.string.backtesting_section_info_market_data)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BacktestSelectChip(
                        label = stringResource(Res.string.backtesting_exchange),
                        value = state.exchange.label,
                        expanded = showExchangeMenu,
                        onExpandedChange = { showExchangeMenu = it }
                    ) {
                        BacktestExchange.entries.forEach { exchange ->
                            FilterChip(
                                selected = exchange == state.exchange,
                                onClick = {
                                    viewModel.setExchange(exchange)
                                    showExchangeMenu = false
                                },
                                label = { Text(exchange.label) }
                            )
                        }
                    }
                    BacktestSelectChip(
                        label = stringResource(Res.string.backtesting_timeframe),
                        value = state.timeframe.label,
                        expanded = showTimeframeMenu,
                        onExpandedChange = { showTimeframeMenu = it }
                    ) {
                        BacktestTimeframe.entries.forEach { timeframe ->
                            FilterChip(
                                selected = timeframe == state.timeframe,
                                onClick = {
                                    viewModel.setTimeframe(timeframe)
                                    showTimeframeMenu = false
                                },
                                label = { Text(timeframe.label) }
                            )
                        }
                    }
                }
                val normalizedCurrentSymbol = state.symbolInput.trim().uppercase()
                val symbolOptions = remember(state.availableSymbols, normalizedCurrentSymbol) {
                    if (normalizedCurrentSymbol.isBlank() || state.availableSymbols.any { it == normalizedCurrentSymbol }) {
                        state.availableSymbols
                    } else {
                        listOf(normalizedCurrentSymbol) + state.availableSymbols
                    }
                }
                val filteredSymbols = remember(symbolOptions, symbolSearchQuery) {
                    val query = symbolSearchQuery.trim().uppercase()
                    if (query.isEmpty()) {
                        symbolOptions
                    } else {
                        symbolOptions.filter { it.contains(query) }
                    }
                }
                BacktestSelectChip(
                    label = stringResource(Res.string.backtesting_symbol),
                    value = normalizedCurrentSymbol.ifBlank { "-" },
                    expanded = showSymbolMenu,
                    onExpandedChange = { showSymbolMenu = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = symbolSearchQuery,
                            onValueChange = { symbolSearchQuery = it.uppercase() },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(stringResource(Res.string.backtesting_symbol_search)) }
                        )
                        AssistChip(
                            onClick = viewModel::refreshSymbols,
                            enabled = !state.isLoadingSymbols,
                            label = { Text(stringResource(Res.string.backtesting_symbol_refresh)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = SnowIcons.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppIconSize.xs)
                                )
                            }
                        )
                    }
                    if (state.isLoadingSymbols) {
                        Text(
                            text = stringResource(Res.string.backtesting_symbol_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extended.sidebarTextMuted
                        )
                    }
                    state.symbolsErrorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredSymbols.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.backtesting_symbol_no_match),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.extended.sidebarTextMuted
                            )
                        } else {
                            filteredSymbols.forEach { symbol ->
                                FilterChip(
                                    selected = symbol == normalizedCurrentSymbol,
                                    onClick = {
                                        viewModel.setSymbol(symbol)
                                        showSymbolMenu = false
                                    },
                                    label = { Text(symbol) }
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartPicker = true }) {
                        Text("${stringResource(Res.string.backtesting_date_start)}: ${state.startDate.formatDate()}")
                    }
                    OutlinedButton(onClick = { showEndPicker = true }) {
                        Text("${stringResource(Res.string.backtesting_date_end)}: ${state.endDate.formatDate()}")
                    }
                }
                OutlinedTextField(
                    value = state.warmupBarsInput,
                    onValueChange = { viewModel.setWarmupBars(sanitizePositiveIntegerInput(it)) },
                    modifier = Modifier.widthIn(min = 190.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(Res.string.backtesting_warmup_bars)) }
                )
            }

            BacktestingSection(
                title = stringResource(Res.string.backtesting_execution_title),
                infoText = stringResource(Res.string.backtesting_section_info_execution)
            ) {
                OutlinedTextField(
                    value = state.initialBalanceInput,
                    onValueChange = { viewModel.setInitialBalance(sanitizePositiveDecimalInput(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(stringResource(Res.string.backtesting_initial_balance)) }
                )
                BacktestSelectChip(
                    label = stringResource(Res.string.backtesting_position_size_mode),
                    value = when (state.sizingMode) {
                        BacktestPositionSizingMode.FixedAmount -> stringResource(Res.string.backtesting_sizing_fixed)
                        BacktestPositionSizingMode.PercentOfEquity -> stringResource(Res.string.backtesting_sizing_percent)
                        BacktestPositionSizingMode.AtrRisk -> stringResource(Res.string.backtesting_sizing_atr_risk)
                        BacktestPositionSizingMode.VolatilityTarget -> stringResource(Res.string.backtesting_sizing_volatility_target)
                    },
                    expanded = showSizingMenu,
                    onExpandedChange = { showSizingMenu = it }
                ) {
                    FilterChip(
                        selected = state.sizingMode == BacktestPositionSizingMode.FixedAmount,
                        onClick = {
                            viewModel.setSizingMode(BacktestPositionSizingMode.FixedAmount)
                            showSizingMenu = false
                        },
                        label = { Text(stringResource(Res.string.backtesting_sizing_fixed)) }
                    )
                    FilterChip(
                        selected = state.sizingMode == BacktestPositionSizingMode.PercentOfEquity,
                        onClick = {
                            viewModel.setSizingMode(BacktestPositionSizingMode.PercentOfEquity)
                            showSizingMenu = false
                        },
                        label = { Text(stringResource(Res.string.backtesting_sizing_percent)) }
                    )
                    FilterChip(
                        selected = state.sizingMode == BacktestPositionSizingMode.AtrRisk,
                        onClick = {
                            viewModel.setSizingMode(BacktestPositionSizingMode.AtrRisk)
                            showSizingMenu = false
                        },
                        label = { Text(stringResource(Res.string.backtesting_sizing_atr_risk)) }
                    )
                    FilterChip(
                        selected = state.sizingMode == BacktestPositionSizingMode.VolatilityTarget,
                        onClick = {
                            viewModel.setSizingMode(BacktestPositionSizingMode.VolatilityTarget)
                            showSizingMenu = false
                        },
                        label = { Text(stringResource(Res.string.backtesting_sizing_volatility_target)) }
                    )
                }
                val positionInputLabel = when (state.sizingMode) {
                    BacktestPositionSizingMode.FixedAmount -> stringResource(Res.string.backtesting_position_size_fixed)
                    BacktestPositionSizingMode.PercentOfEquity -> stringResource(Res.string.backtesting_position_size_percent)
                    BacktestPositionSizingMode.AtrRisk -> stringResource(Res.string.backtesting_atr_risk_percent)
                    BacktestPositionSizingMode.VolatilityTarget -> stringResource(Res.string.backtesting_target_volatility_percent)
                }
                OutlinedTextField(
                    value = when (state.sizingMode) {
                        BacktestPositionSizingMode.FixedAmount -> state.fixedAmountInput
                        BacktestPositionSizingMode.PercentOfEquity -> state.percentEquityInput
                        BacktestPositionSizingMode.AtrRisk -> state.atrRiskPercentInput
                        BacktestPositionSizingMode.VolatilityTarget -> state.targetVolatilityPercentInput
                    },
                    onValueChange = {
                        when (state.sizingMode) {
                            BacktestPositionSizingMode.FixedAmount -> viewModel.setFixedAmount(sanitizePositiveDecimalInput(it))
                            BacktestPositionSizingMode.PercentOfEquity -> viewModel.setPercentOfEquity(sanitizePositiveDecimalInput(it))
                            BacktestPositionSizingMode.AtrRisk -> viewModel.setAtrRiskPercent(sanitizePositiveDecimalInput(it))
                            BacktestPositionSizingMode.VolatilityTarget -> viewModel.setTargetVolatilityPercent(sanitizePositiveDecimalInput(it))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(positionInputLabel) }
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.makerFeeBpsInput,
                        onValueChange = { viewModel.setMakerFeeBps(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_maker_fee_bps)) }
                    )
                    OutlinedTextField(
                        value = state.takerFeeBpsInput,
                        onValueChange = { viewModel.setTakerFeeBps(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_taker_fee_bps)) }
                    )
                    OutlinedTextField(
                        value = state.slippageBpsInput,
                        onValueChange = { viewModel.setSlippageBps(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_slippage_bps)) }
                    )
                }
                BacktestSelectChip(
                    label = stringResource(Res.string.backtesting_order_type),
                    value = when (state.orderType) {
                        BacktestOrderType.Market -> stringResource(Res.string.backtesting_order_market)
                        BacktestOrderType.Limit -> stringResource(Res.string.backtesting_order_limit)
                    },
                    expanded = showOrderMenu,
                    onExpandedChange = { showOrderMenu = it }
                ) {
                    FilterChip(
                        selected = state.orderType == BacktestOrderType.Market,
                        onClick = {
                            viewModel.setOrderType(BacktestOrderType.Market)
                            showOrderMenu = false
                        },
                        label = { Text(stringResource(Res.string.backtesting_order_market)) }
                    )
                    FilterChip(
                        selected = state.orderType == BacktestOrderType.Limit,
                        onClick = {
                            viewModel.setOrderType(BacktestOrderType.Limit)
                            showOrderMenu = false
                        },
                        label = { Text(stringResource(Res.string.backtesting_order_limit)) }
                    )
                }
                if (state.orderType == BacktestOrderType.Limit) {
                    OutlinedTextField(
                        value = state.limitOffsetBpsInput,
                        onValueChange = { viewModel.setLimitOffsetBps(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 190.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_limit_offset_bps)) }
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.fillRatioPercentInput,
                        onValueChange = { viewModel.setFillRatioPercent(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_fill_ratio_percent)) }
                    )
                    OutlinedTextField(
                        value = state.maxVolumeParticipationInput,
                        onValueChange = { viewModel.setMaxVolumeParticipation(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_max_volume_participation)) }
                    )
                    OutlinedTextField(
                        value = state.leverageInput,
                        onValueChange = { viewModel.setLeverage(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_leverage)) }
                    )
                    OutlinedTextField(
                        value = state.fundingBpsPerDayInput,
                        onValueChange = { viewModel.setFundingBpsPerDay(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_funding_bps_per_day)) }
                    )
                }
                BacktestSelectChip(
                    label = stringResource(Res.string.backtesting_margin_mode),
                    value = when (state.marginMode) {
                        BacktestMarginMode.Cash -> stringResource(Res.string.backtesting_margin_cash)
                        BacktestMarginMode.Isolated -> stringResource(Res.string.backtesting_margin_isolated)
                        BacktestMarginMode.Cross -> stringResource(Res.string.backtesting_margin_cross)
                    },
                    expanded = showMarginMenu,
                    onExpandedChange = { showMarginMenu = it }
                ) {
                    FilterChip(
                        selected = state.marginMode == BacktestMarginMode.Cash,
                        onClick = {
                            viewModel.setMarginMode(BacktestMarginMode.Cash)
                            showMarginMenu = false
                        },
                        label = { Text(stringResource(Res.string.backtesting_margin_cash)) }
                    )
                    FilterChip(
                        selected = state.marginMode == BacktestMarginMode.Isolated,
                        onClick = {
                            viewModel.setMarginMode(BacktestMarginMode.Isolated)
                            showMarginMenu = false
                        },
                        label = { Text(stringResource(Res.string.backtesting_margin_isolated)) }
                    )
                    FilterChip(
                        selected = state.marginMode == BacktestMarginMode.Cross,
                        onClick = {
                            viewModel.setMarginMode(BacktestMarginMode.Cross)
                            showMarginMenu = false
                        },
                        label = { Text(stringResource(Res.string.backtesting_margin_cross)) }
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.stopLossInput,
                        onValueChange = { viewModel.setStopLoss(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_stop_loss)) }
                    )
                    OutlinedTextField(
                        value = state.takeProfitInput,
                        onValueChange = { viewModel.setTakeProfit(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_take_profit)) }
                    )
                    OutlinedTextField(
                        value = state.trailingStopInput,
                        onValueChange = { viewModel.setTrailingStop(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_trailing_stop)) }
                    )
                    OutlinedTextField(
                        value = state.killSwitchDrawdownInput,
                        onValueChange = { viewModel.setKillSwitchDrawdown(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_kill_switch_drawdown)) }
                    )
                    OutlinedTextField(
                        value = state.dailyLossLimitInput,
                        onValueChange = { viewModel.setDailyLossLimit(sanitizePositiveDecimalInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(stringResource(Res.string.backtesting_daily_loss_limit)) }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (state.allowShort) stringResource(Res.string.backtesting_allow_short) else stringResource(Res.string.backtesting_long_only),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = state.allowShort, onCheckedChange = viewModel::setAllowShort)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.backtesting_allow_hedging),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = state.allowHedging, onCheckedChange = viewModel::setAllowHedging)
                }
            }

            BacktestingSection(
                title = stringResource(Res.string.backtesting_run_title),
                infoText = stringResource(Res.string.backtesting_section_info_run)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BacktestSelectChip(
                        label = stringResource(Res.string.backtesting_optimization_mode),
                        value = state.optimizationMode.label,
                        expanded = showOptimizationModeMenu,
                        onExpandedChange = { showOptimizationModeMenu = it }
                    ) {
                        BacktestOptimizationMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.optimizationMode == mode,
                                onClick = {
                                    viewModel.setOptimizationMode(mode)
                                    showOptimizationModeMenu = false
                                },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                    BacktestSelectChip(
                        label = stringResource(Res.string.backtesting_optimization_objective),
                        value = objectiveLabel(state.optimizationObjective),
                        expanded = showOptimizationObjectiveMenu,
                        onExpandedChange = { showOptimizationObjectiveMenu = it }
                    ) {
                        BacktestOptimizationObjective.entries.forEach { objective ->
                            FilterChip(
                                selected = state.optimizationObjective == objective,
                                onClick = {
                                    viewModel.setOptimizationObjective(objective)
                                    showOptimizationObjectiveMenu = false
                                },
                                label = { Text(objectiveLabel(objective)) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.optimizationIterationsInput,
                        onValueChange = { viewModel.setOptimizationIterations(sanitizePositiveIntegerInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(stringResource(Res.string.backtesting_optimization_iterations)) }
                    )
                    OutlinedTextField(
                        value = state.optimizationTopKInput,
                        onValueChange = { viewModel.setOptimizationTopK(sanitizePositiveIntegerInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(stringResource(Res.string.backtesting_optimization_top_k)) }
                    )
                    OutlinedTextField(
                        value = state.walkForwardSplitsInput,
                        onValueChange = { viewModel.setWalkForwardSplits(sanitizePositiveIntegerInput(it)) },
                        modifier = Modifier.widthIn(min = 170.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(stringResource(Res.string.backtesting_analysis_splits)) }
                    )
                }
                Text(
                    text = stringResource(Res.string.backtesting_optimization_range_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.sidebarTextMuted
                )
                OptimizationRangeRow(
                    label = stringResource(Res.string.backtesting_optimization_sma_fast),
                    minValue = state.optimizationSmaFastMinInput,
                    maxValue = state.optimizationSmaFastMaxInput,
                    stepValue = state.optimizationSmaFastStepInput,
                    onMinChange = { viewModel.setOptimizationSmaFastMin(sanitizePositiveIntegerInput(it)) },
                    onMaxChange = { viewModel.setOptimizationSmaFastMax(sanitizePositiveIntegerInput(it)) },
                    onStepChange = { viewModel.setOptimizationSmaFastStep(sanitizePositiveIntegerInput(it)) }
                )
                OptimizationRangeRow(
                    label = stringResource(Res.string.backtesting_optimization_sma_slow),
                    minValue = state.optimizationSmaSlowMinInput,
                    maxValue = state.optimizationSmaSlowMaxInput,
                    stepValue = state.optimizationSmaSlowStepInput,
                    onMinChange = { viewModel.setOptimizationSmaSlowMin(sanitizePositiveIntegerInput(it)) },
                    onMaxChange = { viewModel.setOptimizationSmaSlowMax(sanitizePositiveIntegerInput(it)) },
                    onStepChange = { viewModel.setOptimizationSmaSlowStep(sanitizePositiveIntegerInput(it)) }
                )
                OptimizationRangeRow(
                    label = stringResource(Res.string.backtesting_optimization_rsi_period),
                    minValue = state.optimizationRsiPeriodMinInput,
                    maxValue = state.optimizationRsiPeriodMaxInput,
                    stepValue = state.optimizationRsiPeriodStepInput,
                    onMinChange = { viewModel.setOptimizationRsiMin(sanitizePositiveIntegerInput(it)) },
                    onMaxChange = { viewModel.setOptimizationRsiMax(sanitizePositiveIntegerInput(it)) },
                    onStepChange = { viewModel.setOptimizationRsiStep(sanitizePositiveIntegerInput(it)) }
                )
                OptimizationRangeRow(
                    label = stringResource(Res.string.backtesting_optimization_breakout),
                    minValue = state.optimizationBreakoutMinInput,
                    maxValue = state.optimizationBreakoutMaxInput,
                    stepValue = state.optimizationBreakoutStepInput,
                    onMinChange = { viewModel.setOptimizationBreakoutMin(sanitizePositiveIntegerInput(it)) },
                    onMaxChange = { viewModel.setOptimizationBreakoutMax(sanitizePositiveIntegerInput(it)) },
                    onStepChange = { viewModel.setOptimizationBreakoutStep(sanitizePositiveIntegerInput(it)) }
                )
                Text(
                    text = stringResource(Res.string.backtesting_edge_gate_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.sidebarTextMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.backtesting_edge_gate_enabled),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = state.edgeGateEnabled, onCheckedChange = viewModel::setEdgeGateEnabled)
                }
                if (state.edgeGateEnabled) {
                    Text(
                        text = stringResource(Res.string.backtesting_edge_gate_preset),
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.sidebarTextMuted
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            BacktestEdgeGatePreset.Strict,
                            BacktestEdgeGatePreset.Moderate,
                            BacktestEdgeGatePreset.Aggressive
                        ).forEach { preset ->
                            FilterChip(
                                selected = state.edgeGatePreset == preset,
                                onClick = { viewModel.setEdgeGatePreset(preset) },
                                label = { Text(edgeGatePresetLabel(preset)) }
                            )
                        }
                        if (state.edgeGatePreset == BacktestEdgeGatePreset.Custom) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(edgeGatePresetLabel(BacktestEdgeGatePreset.Custom)) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.backtesting_edge_gate_require_passed_status),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = state.edgeGateRequirePassedStatus,
                            onCheckedChange = viewModel::setEdgeGateRequirePassedStatus
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.edgeGateMinEdgeScoreInput,
                            onValueChange = { viewModel.setEdgeGateMinEdgeScore(sanitizePositiveDecimalInput(it)) },
                            modifier = Modifier.widthIn(min = 170.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text(stringResource(Res.string.backtesting_edge_gate_min_edge_score)) }
                        )
                        OutlinedTextField(
                            value = state.edgeGateMaxSpaPValueInput,
                            onValueChange = { viewModel.setEdgeGateMaxSpaPValue(sanitizePositiveDecimalInput(it)) },
                            modifier = Modifier.widthIn(min = 170.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text(stringResource(Res.string.backtesting_edge_gate_max_spa_pvalue)) }
                        )
                        OutlinedTextField(
                            value = state.edgeGateMinPositivePathsInput,
                            onValueChange = { viewModel.setEdgeGateMinPositivePaths(sanitizePositiveDecimalInput(it)) },
                            modifier = Modifier.widthIn(min = 170.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text(stringResource(Res.string.backtesting_edge_gate_min_positive_paths)) }
                        )
                        OutlinedTextField(
                            value = state.edgeGateMinTradesInput,
                            onValueChange = { viewModel.setEdgeGateMinTrades(sanitizePositiveIntegerInput(it)) },
                            modifier = Modifier.widthIn(min = 170.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text(stringResource(Res.string.backtesting_edge_gate_min_trades)) }
                        )
                        OutlinedTextField(
                            value = state.edgeGateMinOosSharpeInput,
                            onValueChange = { viewModel.setEdgeGateMinOosSharpe(sanitizePositiveDecimalInput(it)) },
                            modifier = Modifier.widthIn(min = 170.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text(stringResource(Res.string.backtesting_edge_gate_min_oos_sharpe)) }
                        )
                    }
                }
                state.result?.edgeGateDecision?.let { gate ->
                    val passedLabel = if (gate.passed) {
                        stringResource(Res.string.backtesting_edge_gate_passed)
                    } else {
                        stringResource(Res.string.backtesting_edge_gate_failed)
                    }
                    val passedColor = if (gate.passed) ext.positive else ext.negative
                    Text(
                        text = "${stringResource(Res.string.backtesting_edge_gate_status)}: $passedLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = passedColor
                    )
                }
                Text(
                    text = state.runStatus ?: stringResource(Res.string.backtesting_no_results),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.sidebarTextMuted
                )
                if (state.dataNotes.isNotEmpty()) {
                    Text(text = stringResource(Res.string.backtesting_data_notes), style = MaterialTheme.typography.labelSmall)
                    state.dataNotes.forEach { note ->
                        Text(note, style = MaterialTheme.typography.bodySmall, color = ext.sidebarTextMuted)
                    }
                }
                state.optimizationResult?.let { optimization ->
                    Text(
                        text = stringResource(Res.string.backtesting_optimization_title),
                        style = MaterialTheme.typography.labelMedium
                    )
                    optimization.bestRuns.forEachIndexed { index, run ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.sidebarTextMuted,
                                modifier = Modifier.width(26.dp)
                            )
                            Text(
                                text = run.parameterLabel,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${stringResource(Res.string.backtesting_optimization_score)} ${run.score.pretty()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.sidebarTextMuted
                            )
                        }
                    }
                    OptimizationHeatmap(optimization.allRuns)
                    OptimizationRunsMatrix(
                        runs = optimization.allRuns,
                        sort = optimizationSort,
                        onlyProfitable = optimizationOnlyProfitable,
                        minTradesInput = optimizationMinTradesInput,
                        onSortChange = { optimizationSort = it },
                        onOnlyProfitableChange = { optimizationOnlyProfitable = it },
                        onMinTradesInputChange = { optimizationMinTradesInput = sanitizePositiveIntegerInput(it) }
                    )
                }
                state.walkForwardResult?.let { walkForward ->
                    Text(
                        text = stringResource(Res.string.backtesting_walk_forward),
                        style = MaterialTheme.typography.labelMedium
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnalysisMetricChip(
                            stringResource(Res.string.backtesting_analysis_splits),
                            walkForward.splits.toString()
                        )
                        AnalysisMetricChip(
                            stringResource(Res.string.backtesting_total_return),
                            walkForward.averageOutSampleReturnPercent.prettyPercent()
                        )
                        AnalysisMetricChip(
                            stringResource(Res.string.backtesting_sharpe),
                            walkForward.averageOutSampleSharpe.pretty()
                        )
                        AnalysisMetricChip(
                            stringResource(Res.string.backtesting_max_drawdown),
                            walkForward.averageOutSampleDrawdownPercent.prettyPercent()
                        )
                    }
                    walkForward.folds.forEach { fold ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.extended.toolbarSurface)
                                .border(1.dp, MaterialTheme.extended.shellDivider, MaterialTheme.shapes.small)
                                .padding(AppSpacing.xs),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.backtesting_walk_forward_fold, fold.foldIndex),
                                style = MaterialTheme.typography.labelSmall
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AnalysisMetricChip(
                                    stringResource(Res.string.backtesting_total_return),
                                    fold.outSampleMetrics.totalReturnPercent.prettyPercent()
                                )
                                AnalysisMetricChip(
                                    stringResource(Res.string.backtesting_sharpe),
                                    fold.outSampleMetrics.sharpeRatio.pretty()
                                )
                                AnalysisMetricChip(
                                    stringResource(Res.string.backtesting_max_drawdown),
                                    fold.outSampleMetrics.maxDrawdownPercent.prettyPercent()
                                )
                                fold.edgeScore?.let { edgeScore ->
                                    AnalysisMetricChip(
                                        stringResource(Res.string.backtesting_edge_lab_score),
                                        edgeScore.pretty()
                                    )
                                }
                                fold.edgeGatePassed?.let { passed ->
                                    AnalysisMetricChip(
                                        stringResource(Res.string.backtesting_edge_gate_status),
                                        if (passed) {
                                            stringResource(Res.string.backtesting_edge_gate_passed)
                                        } else {
                                            stringResource(Res.string.backtesting_edge_gate_failed)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            BacktestingSection(
                title = stringResource(Res.string.backtesting_results_title),
                infoText = stringResource(Res.string.backtesting_section_info_results)
            ) {
                if (result == null) {
                    Text(stringResource(Res.string.backtesting_no_results), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        text = stringResource(Res.string.backtesting_result_subtitle, result.candlesCount, result.runId),
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.sidebarTextMuted
                    )
                    MetricsGrid(result)
                    EdgeLabSection(result.edgeValidation)
                    EdgeGateSection(result.edgeGateDecision)
                    ResultCharts(
                        result = result,
                        selectedTradeId = state.selectedTradeId,
                        showAllTradeLabels = showAllTradeLabels,
                        onShowAllTradeLabelsChange = { showAllTradeLabels = it }
                    )
                    AnalysisSection(result)
                    TradesSection(
                        state = state,
                        trades = visibleTrades,
                        selectedTradeId = state.selectedTradeId,
                        onQueryChange = viewModel::setTradesSearchQuery,
                        onSelectTrade = viewModel::selectTrade,
                        onSortChange = viewModel::setTradesSort,
                        sortMenuExpanded = showSortMenu,
                        onSortMenuExpandedChange = { showSortMenu = it }
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { copyToClipboard(viewModel.exportTradesCsv()) }) {
                            Icon(SnowIcons.Copy, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.backtesting_export_trades))
                        }
                        OutlinedButton(onClick = { copyToClipboard(viewModel.exportSummaryJson()) }) {
                            Icon(SnowIcons.Copy, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.backtesting_export_json))
                        }
                        OutlinedButton(
                            onClick = { copyToClipboard(viewModel.exportOptimizationCsv()) },
                            enabled = state.optimizationResult != null
                        ) {
                            Icon(SnowIcons.Copy, contentDescription = null, modifier = Modifier.size(AppIconSize.xs))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.backtesting_export_optimization))
                        }
                    }
                }
            }

            BacktestingSection(
                title = stringResource(Res.string.backtesting_history_title),
                infoText = stringResource(Res.string.backtesting_section_info_history)
            ) {
                if (state.history.isEmpty()) {
                    Text(stringResource(Res.string.backtesting_history_empty), style = MaterialTheme.typography.bodySmall)
                } else {
                    state.history.forEach { run ->
                        HistoryRow(
                            run = run,
                            isSelected = state.selectedHistoryRunId == run.id,
                            isCompared = run.id in state.comparisonRunIds,
                            onOpen = { viewModel.openHistoryRun(run.id) },
                            onToggleCompare = { viewModel.toggleComparisonRun(run.id) },
                            onDelete = { viewModel.deleteHistoryRun(run.id) }
                        )
                    }
                }
                if (comparedRuns.size == 2) {
                    CompareRuns(comparedRuns[0], comparedRuns[1])
                }
            }
        }
    }

    if (showStartPicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        viewModel.setStartDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showStartPicker = false
                }) { Text(stringResource(Res.string.datepicker_ok)) }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text(stringResource(Res.string.datepicker_cancel)) } }
        ) { DatePicker(state = picker) }
    }

    if (showEndPicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.endDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        viewModel.setEndDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showEndPicker = false
                }) { Text(stringResource(Res.string.datepicker_ok)) }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text(stringResource(Res.string.datepicker_cancel)) } }
        ) { DatePicker(state = picker) }
    }
}

private fun sanitizePositiveDecimalInput(rawInput: String): String {
    val normalized = rawInput.replace(',', '.')
    val builder = StringBuilder()
    var hasDot = false
    normalized.forEach {
        when {
            it.isDigit() -> builder.append(it)
            it == '.' && !hasDot -> {
                hasDot = true
                if (builder.isEmpty()) builder.append('0')
                builder.append('.')
            }
        }
    }
    return builder.toString()
}

private fun sanitizePositiveIntegerInput(rawInput: String): String = rawInput.filter { it.isDigit() }

@Composable
private fun objectiveLabel(objective: BacktestOptimizationObjective): String = when (objective) {
    BacktestOptimizationObjective.Balanced -> "Balanced"
    BacktestOptimizationObjective.TotalReturn -> "Total Return"
    BacktestOptimizationObjective.Sharpe -> "Sharpe"
    BacktestOptimizationObjective.ProfitFactor -> "Profit Factor"
    BacktestOptimizationObjective.Calmar -> "Calmar"
}

@Composable
private fun edgeGatePresetLabel(preset: BacktestEdgeGatePreset): String = when (preset) {
    BacktestEdgeGatePreset.Strict -> stringResource(Res.string.backtesting_edge_gate_preset_strict)
    BacktestEdgeGatePreset.Moderate -> stringResource(Res.string.backtesting_edge_gate_preset_moderate)
    BacktestEdgeGatePreset.Aggressive -> stringResource(Res.string.backtesting_edge_gate_preset_aggressive)
    BacktestEdgeGatePreset.Custom -> stringResource(Res.string.backtesting_edge_gate_preset_custom)
}

@Composable
private fun OptimizationRangeRow(
    label: String,
    minValue: String,
    maxValue: String,
    stepValue: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onStepChange: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.extended.sidebarTextMuted,
            modifier = Modifier.widthIn(min = 110.dp)
        )
        OutlinedTextField(
            value = minValue,
            onValueChange = onMinChange,
            modifier = Modifier.widthIn(min = 90.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(Res.string.backtesting_optimization_min)) }
        )
        OutlinedTextField(
            value = maxValue,
            onValueChange = onMaxChange,
            modifier = Modifier.widthIn(min = 90.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(Res.string.backtesting_optimization_max)) }
        )
        OutlinedTextField(
            value = stepValue,
            onValueChange = onStepChange,
            modifier = Modifier.widthIn(min = 90.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(Res.string.backtesting_optimization_step)) }
        )
    }
}

private fun LocalDate.formatDate(): String = format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY))

private fun Instant.formatDateTime(): String =
    atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMANY))

private fun Double.pretty(): String {
    val rounded = round(this * 100.0) / 100.0
    return rounded.toString().removeSuffix(".0")
}

private fun Double.prettyPercent(): String = "${pretty()}%"

private fun Double.prettySigned(): String {
    val raw = abs(this).pretty()
    return when {
        this > 0.0 -> "+$raw"
        this < 0.0 -> "-$raw"
        else -> raw
    }
}

private fun filterAndSortTrades(
    trades: List<BacktestTrade>,
    query: String,
    sort: BacktestTradesSort
): List<BacktestTrade> {
    val filtered = if (query.isBlank()) {
        trades
    } else {
        val needle = query.lowercase().trim()
        trades.filter {
            buildString {
                append(it.side.name)
                append(' ')
                append(it.exitReason.name)
                append(' ')
                append(it.entryPrice)
                append(' ')
                append(it.exitPrice)
            }.lowercase().contains(needle)
        }
    }
    return when (sort) {
        BacktestTradesSort.EntryNewest -> filtered.sortedByDescending { it.entryTime }
        BacktestTradesSort.EntryOldest -> filtered.sortedBy { it.entryTime }
        BacktestTradesSort.PnlDesc -> filtered.sortedByDescending { it.pnl }
        BacktestTradesSort.PnlAsc -> filtered.sortedBy { it.pnl }
    }
}

@Composable
private fun StrategyInputs(state: BacktestingUiState, viewModel: BacktestingViewModel) {
    when (state.selectedStrategy) {
        BacktestStrategyTemplate.SmaCrossover -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.smaFastInput,
                    onValueChange = { viewModel.setSmaFast(sanitizePositiveIntegerInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_sma_fast)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = state.smaSlowInput,
                    onValueChange = { viewModel.setSmaSlow(sanitizePositiveIntegerInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_sma_slow)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        BacktestStrategyTemplate.RsiMeanReversion -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.rsiPeriodInput,
                    onValueChange = { viewModel.setRsiPeriod(sanitizePositiveIntegerInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_rsi_period)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = state.rsiOversoldInput,
                    onValueChange = { viewModel.setRsiOversold(sanitizePositiveDecimalInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_rsi_oversold)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = state.rsiOverboughtInput,
                    onValueChange = { viewModel.setRsiOverbought(sanitizePositiveDecimalInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_rsi_overbought)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }

        BacktestStrategyTemplate.Breakout -> {
            OutlinedTextField(
                value = state.breakoutLookbackInput,
                onValueChange = { viewModel.setBreakoutLookback(sanitizePositiveIntegerInput(it)) },
                label = { Text(stringResource(Res.string.backtesting_breakout_lookback)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        BacktestStrategyTemplate.SmaRsiConfluence -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.smaFastInput,
                    onValueChange = { viewModel.setSmaFast(sanitizePositiveIntegerInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_sma_fast)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = state.smaSlowInput,
                    onValueChange = { viewModel.setSmaSlow(sanitizePositiveIntegerInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_sma_slow)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = state.rsiPeriodInput,
                    onValueChange = { viewModel.setRsiPeriod(sanitizePositiveIntegerInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_rsi_period)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = state.rsiOversoldInput,
                    onValueChange = { viewModel.setRsiOversold(sanitizePositiveDecimalInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_rsi_oversold)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = state.rsiOverboughtInput,
                    onValueChange = { viewModel.setRsiOverbought(sanitizePositiveDecimalInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_rsi_overbought)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }

        BacktestStrategyTemplate.AtrVolatilityBreakout -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.breakoutLookbackInput,
                    onValueChange = { viewModel.setBreakoutLookback(sanitizePositiveIntegerInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_breakout_lookback)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = state.atrPeriodInput,
                    onValueChange = { viewModel.setAtrPeriod(sanitizePositiveIntegerInput(it)) },
                    label = { Text(stringResource(Res.string.backtesting_atr_period)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        BacktestStrategyTemplate.SunMoonAstroFlow -> {
            val cities = remember {
                DefaultCityDataSource.cities().sortedWith(compareBy({ it.country }, { it.label }))
            }
            var showAstroCityMenu by rememberSaveable { mutableStateOf(false) }
            val selectedCityLabel = remember(state.astroCityKey, cities) {
                cities.firstOrNull { it.key() == state.astroCityKey }?.let { "${it.label} (${it.zoneId})" }
                    ?: cities.firstOrNull()?.let { "${it.label} (${it.zoneId})" }
                    ?: "-"
            }
            BacktestSelectChip(
                label = stringResource(Res.string.backtesting_astro_city),
                value = selectedCityLabel,
                expanded = showAstroCityMenu,
                onExpandedChange = { showAstroCityMenu = it }
            ) {
                cities.forEach { city ->
                    FilterChip(
                        selected = city.key() == state.astroCityKey,
                        onClick = {
                            viewModel.setAstroCityKey(city.key())
                            showAstroCityMenu = false
                        },
                        label = { Text("${city.label} (${city.zoneId})") }
                    )
                }
            }
            OutlinedTextField(
                value = state.astroSignalWindowBarsInput,
                onValueChange = { viewModel.setAstroSignalWindowBars(sanitizePositiveIntegerInput(it)) },
                label = { Text(stringResource(Res.string.backtesting_astro_signal_window_bars)) },
                supportingText = {
                    Text(
                        text = stringResource(Res.string.backtesting_astro_signal_window_bars_help),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.astroIncludeSunEvents,
                    onClick = { viewModel.setAstroIncludeSunEvents(!state.astroIncludeSunEvents) },
                    label = { Text(stringResource(Res.string.backtesting_astro_include_sun_events)) }
                )
                FilterChip(
                    selected = state.astroIncludeMoonEvents,
                    onClick = { viewModel.setAstroIncludeMoonEvents(!state.astroIncludeMoonEvents) },
                    label = { Text(stringResource(Res.string.backtesting_astro_include_moon_events)) }
                )
                FilterChip(
                    selected = state.astroIncludeMoonPhases,
                    onClick = { viewModel.setAstroIncludeMoonPhases(!state.astroIncludeMoonPhases) },
                    label = { Text(stringResource(Res.string.backtesting_astro_include_moon_phases)) }
                )
                FilterChip(
                    selected = state.astroIncludeAspects,
                    onClick = { viewModel.setAstroIncludeAspects(!state.astroIncludeAspects) },
                    label = { Text(stringResource(Res.string.backtesting_astro_include_aspects)) }
                )
            }
            if (state.astroIncludeAspects) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.backtesting_astro_aspect_scope),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.extended.sidebarTextMuted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    FilterChip(
                        selected = state.astroAspectsMoonOnly,
                        onClick = { viewModel.setAstroAspectsMoonOnly(true) },
                        label = { Text(stringResource(Res.string.backtesting_astro_aspect_scope_moon_only)) }
                    )
                    FilterChip(
                        selected = !state.astroAspectsMoonOnly,
                        onClick = { viewModel.setAstroAspectsMoonOnly(false) },
                        label = { Text(stringResource(Res.string.backtesting_astro_aspect_scope_all)) }
                    )
                }
            }
        }

        BacktestStrategyTemplate.Custom -> {
            var showJsonEditor by rememberSaveable { mutableStateOf(false) }
            Text(
                text = stringResource(Res.string.backtesting_custom_presets_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.extended.sidebarTextMuted
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = { viewModel.applyCustomStrategyPreset(BacktestCustomPreset.TrendFollowing) },
                    label = { Text(stringResource(Res.string.backtesting_custom_preset_trend)) }
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.applyCustomStrategyPreset(BacktestCustomPreset.MeanReversion) },
                    label = { Text(stringResource(Res.string.backtesting_custom_preset_mean_reversion)) }
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.applyCustomStrategyPreset(BacktestCustomPreset.Breakout) },
                    label = { Text(stringResource(Res.string.backtesting_custom_preset_breakout)) }
                )
            }
            FilterChip(
                selected = showJsonEditor,
                onClick = { showJsonEditor = !showJsonEditor },
                label = { Text(stringResource(Res.string.backtesting_custom_json_editor)) }
            )
            if (showJsonEditor) {
                OutlinedTextField(
                    value = state.customStrategyJson,
                    onValueChange = viewModel::setCustomStrategyJson,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    label = { Text(stringResource(Res.string.backtesting_custom_strategy_json)) }
                )
            } else {
                Text(
                    text = stringResource(Res.string.backtesting_custom_json_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extended.sidebarTextMuted
                )
            }
        }
    }
    if (
        state.selectedStrategy != BacktestStrategyTemplate.AtrVolatilityBreakout &&
        state.selectedStrategy != BacktestStrategyTemplate.SunMoonAstroFlow
    ) {
        OutlinedTextField(
            value = state.atrPeriodInput,
            onValueChange = { viewModel.setAtrPeriod(sanitizePositiveIntegerInput(it)) },
            label = { Text(stringResource(Res.string.backtesting_atr_period)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun MetricsGrid(result: BacktestResult) {
    val metrics = result.metrics
    val rows = listOf(
        stringResource(Res.string.backtesting_win_rate) to metrics.winRatePercent.prettyPercent(),
        stringResource(Res.string.backtesting_total_return) to metrics.totalReturnPercent.prettyPercent(),
        stringResource(Res.string.backtesting_cagr) to (metrics.cagrPercent?.prettyPercent() ?: "-"),
        stringResource(Res.string.backtesting_max_drawdown) to metrics.maxDrawdownPercent.prettyPercent(),
        stringResource(Res.string.backtesting_sharpe) to metrics.sharpeRatio.pretty(),
        stringResource(Res.string.backtesting_sortino) to metrics.sortinoRatio.pretty(),
        stringResource(Res.string.backtesting_profit_factor) to metrics.profitFactor.pretty(),
        stringResource(Res.string.backtesting_expectancy) to metrics.expectancyPercent.prettyPercent(),
        stringResource(Res.string.backtesting_annual_volatility) to metrics.annualVolatilityPercent.prettyPercent(),
        stringResource(Res.string.backtesting_trades_count) to metrics.tradeCount.toString(),
        stringResource(Res.string.backtesting_average_trade) to metrics.averageTradePercent.prettyPercent(),
        stringResource(Res.string.backtesting_exposure_time) to metrics.exposureTimePercent.prettyPercent()
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { (label, value) ->
            Column(
                modifier = Modifier
                    .widthIn(min = 170.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.extended.toolbarSurface)
                    .border(1.dp, MaterialTheme.extended.shellDivider, MaterialTheme.shapes.small)
                    .padding(AppSpacing.s)
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extended.sidebarTextMuted)
                Text(value, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun EdgeLabSection(validation: BacktestEdgeValidation?) {
    val edge = validation ?: return
    val ext = MaterialTheme.extended
    val statusLabel = when (edge.status) {
        BacktestEdgeValidationStatus.Passed -> stringResource(Res.string.backtesting_edge_lab_status_passed)
        BacktestEdgeValidationStatus.Warning -> stringResource(Res.string.backtesting_edge_lab_status_warning)
        BacktestEdgeValidationStatus.Failed -> stringResource(Res.string.backtesting_edge_lab_status_failed)
        BacktestEdgeValidationStatus.Unavailable -> stringResource(Res.string.backtesting_edge_lab_status_unavailable)
    }
    val statusColor = when (edge.status) {
        BacktestEdgeValidationStatus.Passed -> ext.positive
        BacktestEdgeValidationStatus.Warning -> ext.warning
        BacktestEdgeValidationStatus.Failed -> ext.negative
        BacktestEdgeValidationStatus.Unavailable -> ext.sidebarTextMuted
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(stringResource(Res.string.backtesting_edge_lab_title), style = MaterialTheme.typography.labelMedium)
    Text(
        text = "${stringResource(Res.string.backtesting_edge_lab_status)}: $statusLabel",
        style = MaterialTheme.typography.bodySmall,
        color = statusColor
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnalysisMetricChip(stringResource(Res.string.backtesting_edge_lab_score), edge.edgeScore.pretty())
        AnalysisMetricChip(stringResource(Res.string.backtesting_edge_lab_samples), edge.sampleCount.toString())
        AnalysisMetricChip(
            stringResource(Res.string.backtesting_edge_lab_cpcv),
            "${edge.cpcvSplits}/${edge.cpcvPaths}"
        )
        AnalysisMetricChip(
            stringResource(Res.string.backtesting_edge_lab_oos_sharpe),
            edge.oosSharpe?.pretty() ?: "-"
        )
        AnalysisMetricChip(
            stringResource(Res.string.backtesting_edge_lab_oos_return),
            edge.oosReturnPercent?.prettyPercent() ?: "-"
        )
        AnalysisMetricChip(
            stringResource(Res.string.backtesting_edge_lab_positive_paths),
            edge.oosPositivePathPercent?.prettyPercent() ?: "-"
        )
        AnalysisMetricChip(
            stringResource(Res.string.backtesting_edge_lab_spa_pvalue),
            edge.spaPValue?.pretty() ?: "-"
        )
    }
    if (edge.notes.isNotEmpty()) {
        Text(
            text = stringResource(Res.string.backtesting_edge_lab_notes),
            style = MaterialTheme.typography.labelSmall,
            color = ext.sidebarTextMuted
        )
        edge.notes.take(4).forEach { note ->
            Text(
                text = "- $note",
                style = MaterialTheme.typography.bodySmall,
                color = ext.sidebarTextMuted
            )
        }
    }
}

@Composable
private fun EdgeGateSection(decision: BacktestEdgeGateDecision?) {
    val gate = decision ?: return
    val ext = MaterialTheme.extended
    val statusLabel = if (gate.passed) {
        stringResource(Res.string.backtesting_edge_gate_passed)
    } else {
        stringResource(Res.string.backtesting_edge_gate_failed)
    }
    val statusColor = if (gate.passed) ext.positive else ext.negative

    Spacer(modifier = Modifier.height(4.dp))
    Text(stringResource(Res.string.backtesting_edge_gate_title), style = MaterialTheme.typography.labelMedium)
    Text(
        text = "${stringResource(Res.string.backtesting_edge_gate_status)}: $statusLabel",
        style = MaterialTheme.typography.bodySmall,
        color = statusColor
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnalysisMetricChip(stringResource(Res.string.backtesting_edge_gate_min_edge_score), gate.minEdgeScore.pretty())
        AnalysisMetricChip(stringResource(Res.string.backtesting_edge_gate_max_spa_pvalue), gate.maxSpaPValue.pretty())
        AnalysisMetricChip(stringResource(Res.string.backtesting_edge_gate_min_positive_paths), gate.minPositivePathsPercent.prettyPercent())
        AnalysisMetricChip(stringResource(Res.string.backtesting_edge_gate_min_trades), gate.minTrades.toString())
        AnalysisMetricChip(stringResource(Res.string.backtesting_edge_gate_min_oos_sharpe), gate.minOosSharpe.pretty())
    }
    gate.reasons.take(4).forEach { reason ->
        Text(
            text = "- $reason",
            style = MaterialTheme.typography.bodySmall,
            color = ext.sidebarTextMuted
        )
    }
}

@Composable
private fun ResultCharts(
    result: BacktestResult,
    selectedTradeId: String?,
    showAllTradeLabels: Boolean,
    onShowAllTradeLabelsChange: (Boolean) -> Unit
) {
    val ext = MaterialTheme.extended
    val hasCandles = result.candleSeries.isNotEmpty()
    val prices = if (hasCandles) result.candleSeries.map { it.close } else result.priceCurve.map { it.price }
    val priceTimes = if (hasCandles) {
        result.candleSeries.map { it.closeTime.toEpochMilli() }
    } else {
        result.priceCurve.map { it.time.toEpochMilli() }
    }
    if (prices.isNotEmpty()) {
        val selectedTrade = result.trades.firstOrNull { it.id == selectedTradeId }
        val selectedEntryIndex = selectedTrade?.let { nearestIndexByTime(priceTimes, it.entryTime.toEpochMilli()) }
        val selectedExitIndex = selectedTrade?.let { nearestIndexByTime(priceTimes, it.exitTime.toEpochMilli()) }
        val selectedStart = if (selectedEntryIndex != null && selectedExitIndex != null) minOf(selectedEntryIndex, selectedExitIndex) else null
        val selectedEnd = if (selectedEntryIndex != null && selectedExitIndex != null) maxOf(selectedEntryIndex, selectedExitIndex) else null
        val windowPadding = 90
        val visibleStart = selectedStart?.let { (it - windowPadding).coerceAtLeast(0) } ?: 0
        val visibleEnd = selectedEnd?.let { (it + windowPadding).coerceAtMost(prices.lastIndex) } ?: prices.lastIndex
        val markers = buildList {
            result.trades.forEach { trade ->
                val isSelected = trade.id == selectedTradeId
                val entryIndex = nearestIndexByTime(priceTimes, trade.entryTime.toEpochMilli())
                if (entryIndex != null) {
                    add(
                        SnowChartMarker(
                            index = entryIndex,
                            value = trade.entryPrice,
                            color = (if (trade.side == BacktestTradeSide.Long) ext.positive else ext.warning).let { color ->
                                if (selectedTrade != null && !isSelected) color.copy(alpha = 0.45f) else color
                            },
                            isBuy = trade.side == BacktestTradeSide.Long,
                            sizeScale = if (isSelected) 1.6f else 1f
                        )
                    )
                }
                val exitIndex = nearestIndexByTime(priceTimes, trade.exitTime.toEpochMilli())
                if (exitIndex != null) {
                    add(
                        SnowChartMarker(
                            index = exitIndex,
                            value = trade.exitPrice,
                            color = (if (trade.side == BacktestTradeSide.Long) ext.negative else ext.info).let { color ->
                                if (selectedTrade != null && !isSelected) color.copy(alpha = 0.45f) else color
                            },
                            isBuy = trade.side != BacktestTradeSide.Long,
                            sizeScale = if (isSelected) 1.6f else 1f
                        )
                    )
                }
            }
        }
        val tradeSegments = buildList {
            result.trades.forEach { trade ->
                val isSelected = trade.id == selectedTradeId
                val entryIndex = nearestIndexByTime(priceTimes, trade.entryTime.toEpochMilli()) ?: return@forEach
                val exitIndex = nearestIndexByTime(priceTimes, trade.exitTime.toEpochMilli()) ?: return@forEach
                add(
                    SnowChartSegment(
                        startIndex = entryIndex,
                        startValue = trade.entryPrice,
                        endIndex = exitIndex,
                        endValue = trade.exitPrice,
                        color = (if (trade.pnl >= 0.0) ext.positive else ext.negative).let { color ->
                            if (selectedTrade != null && !isSelected) color.copy(alpha = 0.35f) else color
                        },
                        strokeWidth = if (isSelected) 3.4f else 2.0f
                    )
                )
            }
        }
        val visiblePrices = prices.subList(visibleStart, visibleEnd + 1)
        val visibleMarkers = markers
            .filter { it.index in visibleStart..visibleEnd }
            .map { it.copy(index = it.index - visibleStart) }
        val visibleSegments = tradeSegments
            .filter { it.startIndex in visibleStart..visibleEnd || it.endIndex in visibleStart..visibleEnd }
            .map { segment ->
                val safeStart = segment.startIndex.coerceIn(visibleStart, visibleEnd) - visibleStart
                val safeEnd = segment.endIndex.coerceIn(visibleStart, visibleEnd) - visibleStart
                segment.copy(startIndex = safeStart, endIndex = safeEnd)
            }
        Text(stringResource(Res.string.backtesting_selected_trade), style = MaterialTheme.typography.labelMedium)
        if (selectedTrade == null) {
            Text(
                text = stringResource(Res.string.backtesting_selected_trade_none),
                style = MaterialTheme.typography.bodySmall,
                color = ext.sidebarTextMuted
            )
        } else {
            Text(
                text = stringResource(
                    Res.string.backtesting_selected_trade_focus,
                    selectedTrade.entryPrice.pretty(),
                    selectedTrade.exitPrice.pretty(),
                    selectedTrade.pnl.prettySigned()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = ext.sidebarTextMuted
            )
            Text(
                text = stringResource(
                    Res.string.backtesting_selected_trade_timing,
                    selectedTrade.entryTime.formatDateTime(),
                    selectedTrade.exitTime.formatDateTime(),
                    selectedTrade.durationMinutes
                ),
                style = MaterialTheme.typography.bodySmall,
                color = ext.sidebarTextMuted
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalysisMetricChip("MAE", selectedTrade.maePercent.prettyPercent())
                AnalysisMetricChip("MFE", selectedTrade.mfePercent.prettyPercent())
                AnalysisMetricChip(stringResource(Res.string.backtesting_header_duration), "${selectedTrade.durationMinutes}m")
                AnalysisMetricChip(stringResource(Res.string.backtesting_header_reason), selectedTrade.exitReason.name)
            }
        }
        Text(stringResource(Res.string.backtesting_chart_price_signals), style = MaterialTheme.typography.labelMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.backtesting_chart_labels_mode),
                style = MaterialTheme.typography.labelSmall,
                color = ext.sidebarTextMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
            FilterChip(
                selected = !showAllTradeLabels,
                onClick = { onShowAllTradeLabelsChange(false) },
                label = { Text(stringResource(Res.string.backtesting_chart_labels_selected)) }
            )
            FilterChip(
                selected = showAllTradeLabels,
                onClick = { onShowAllTradeLabelsChange(true) },
                label = { Text(stringResource(Res.string.backtesting_chart_labels_all)) }
            )
        }
        if (hasCandles) {
            val fullCandles = result.candleSeries.map { candle ->
                SnowCandleEntry(
                    open = candle.open,
                    high = candle.high,
                    low = candle.low,
                    close = candle.close,
                    timeLabel = candle.closeTime.formatDateTime()
                )
            }
            val annotationItems = buildList {
                val tradesForLabels = if (showAllTradeLabels) {
                    result.trades
                } else {
                    listOfNotNull(selectedTrade)
                }
                tradesForLabels.forEachIndexed { index, trade ->
                    val entryIndex = nearestIndexByTime(priceTimes, trade.entryTime.toEpochMilli())
                    val exitIndex = nearestIndexByTime(priceTimes, trade.exitTime.toEpochMilli())
                    if (showAllTradeLabels) {
                        if (entryIndex != null && exitIndex != null) {
                            add(
                                SnowChartAnnotation(
                                    index = ((entryIndex + exitIndex) / 2).coerceIn(0, fullCandles.lastIndex),
                                    text = stringResource(
                                        Res.string.backtesting_trade_label_compact,
                                        index + 1,
                                        "${trade.pnlPercent.prettySigned()}%",
                                        trade.durationMinutes
                                    ),
                                    color = if (trade.pnl >= 0.0) ext.positive else ext.negative
                                )
                            )
                        }
                    } else {
                        if (entryIndex != null) {
                            add(
                                SnowChartAnnotation(
                                    index = entryIndex,
                                    text = "Entry ${trade.entryPrice.pretty()} ${trade.entryTime.formatDateTime()}",
                                    color = ext.positive
                                )
                            )
                        }
                        if (exitIndex != null) {
                            add(
                                SnowChartAnnotation(
                                    index = exitIndex,
                                    text = "Exit ${trade.exitPrice.pretty()} ${trade.exitTime.formatDateTime()}",
                                    color = if (trade.pnl >= 0.0) ext.positive else ext.negative
                                )
                            )
                        }
                        if (entryIndex != null && exitIndex != null) {
                            add(
                                SnowChartAnnotation(
                                    index = ((entryIndex + exitIndex) / 2).coerceIn(0, fullCandles.lastIndex),
                                    text = "${stringResource(Res.string.backtesting_header_duration)} ${trade.durationMinutes}m",
                                    color = ext.info
                                )
                            )
                        }
                    }
                }
            }
            SnowInteractiveCandleChart(
                candles = fullCandles,
                markers = markers,
                segments = tradeSegments,
                annotations = annotationItems,
                modifier = Modifier.fillMaxWidth(),
                upColor = ext.positive,
                downColor = ext.negative
            )
        } else {
            SnowLineChartWithMarkers(
                values = visiblePrices,
                lineColor = MaterialTheme.colorScheme.primary,
                markers = visibleMarkers,
                segments = visibleSegments,
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
        }
        SnowChartLegend(
            items = listOf(
                stringResource(Res.string.backtesting_legend_long_entry) to ext.positive,
                stringResource(Res.string.backtesting_legend_long_exit) to ext.negative,
                stringResource(Res.string.backtesting_legend_short_entry) to ext.warning,
                stringResource(Res.string.backtesting_legend_short_exit) to ext.info
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    Text(stringResource(Res.string.backtesting_chart_equity), style = MaterialTheme.typography.labelMedium)
    SnowLineChart(
        series = listOf(
            SnowLineSeries("equity", result.equityCurve.map { it.equity }, MaterialTheme.colorScheme.primary)
        ),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
    Text(stringResource(Res.string.backtesting_chart_drawdown), style = MaterialTheme.typography.labelMedium)
    SnowLineChart(
        series = listOf(
            SnowLineSeries("drawdown", result.drawdownCurve.map { it.drawdownPercent }, ext.negative)
        ),
        modifier = Modifier.fillMaxWidth().height(180.dp)
    )
    Text(stringResource(Res.string.backtesting_chart_trade_pnl), style = MaterialTheme.typography.labelMedium)
    SnowBarChart(
        entries = result.trades.takeLast(20).mapIndexed { index, trade ->
            SnowBarEntry(index.toString(), trade.pnl, if (trade.pnl >= 0.0) ext.positive else ext.negative)
        },
        modifier = Modifier.fillMaxWidth().height(180.dp)
    )
}

@Composable
private fun AnalysisSection(result: BacktestResult) {
    val analysis = result.analysis ?: return
    val ext = MaterialTheme.extended
    Text(stringResource(Res.string.backtesting_analysis_title), style = MaterialTheme.typography.titleSmall)

    if (analysis.monthlyReturns.isNotEmpty()) {
        Text(stringResource(Res.string.backtesting_monthly_returns), style = MaterialTheme.typography.labelMedium)
        SnowBarChart(
            entries = analysis.monthlyReturns.takeLast(12).map {
                SnowBarEntry(
                    label = "${it.month.toString().padStart(2, '0')}/${it.year}",
                    value = it.returnPercent,
                    color = if (it.returnPercent >= 0.0) ext.positive else ext.negative
                )
            },
            modifier = Modifier.fillMaxWidth().height(170.dp)
        )
    }

    analysis.walkForward?.let { wf ->
        Text(stringResource(Res.string.backtesting_walk_forward), style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalysisMetricChip(stringResource(Res.string.backtesting_analysis_splits), wf.splitCount.toString())
            AnalysisMetricChip(stringResource(Res.string.backtesting_analysis_in_sample), wf.inSampleAverageReturnPercent.prettyPercent())
            AnalysisMetricChip(stringResource(Res.string.backtesting_analysis_out_sample), wf.outOfSampleAverageReturnPercent.prettyPercent())
            AnalysisMetricChip(stringResource(Res.string.backtesting_analysis_stability), wf.stabilityScorePercent.prettyPercent())
        }
    }

    analysis.monteCarlo?.let { mc ->
        Text(stringResource(Res.string.backtesting_monte_carlo), style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalysisMetricChip(stringResource(Res.string.backtesting_analysis_iterations), mc.iterations.toString())
            AnalysisMetricChip(stringResource(Res.string.backtesting_analysis_median), mc.medianReturnPercent.prettyPercent())
            AnalysisMetricChip(stringResource(Res.string.backtesting_analysis_p05), mc.p05ReturnPercent.prettyPercent())
            AnalysisMetricChip(stringResource(Res.string.backtesting_analysis_p95), mc.p95ReturnPercent.prettyPercent())
        }
    }
}

private fun nearestIndexByTime(sortedTimes: List<Long>, targetTime: Long): Int? {
    if (sortedTimes.isEmpty()) return null
    var low = 0
    var high = sortedTimes.lastIndex
    while (low <= high) {
        val mid = (low + high) ushr 1
        val value = sortedTimes[mid]
        when {
            value < targetTime -> low = mid + 1
            value > targetTime -> high = mid - 1
            else -> return mid
        }
    }
    val upper = low.coerceAtMost(sortedTimes.lastIndex)
    val lower = (low - 1).coerceAtLeast(0)
    val upperDelta = abs(sortedTimes[upper] - targetTime)
    val lowerDelta = abs(sortedTimes[lower] - targetTime)
    return if (upperDelta < lowerDelta) upper else lower
}

@Composable
private fun AnalysisMetricChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.extended.toolbarSurface)
            .border(1.dp, MaterialTheme.extended.shellDivider, MaterialTheme.shapes.small)
            .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extended.sidebarTextMuted)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun OptimizationHeatmap(runs: List<BacktestOptimizationRun>) {
    val ext = MaterialTheme.extended
    val points = runs
        .filter { it.smaFastPeriod != null && it.smaSlowPeriod != null }
        .groupBy { it.smaFastPeriod!! to it.smaSlowPeriod!! }
        .mapNotNull { (_, grouped) -> grouped.maxByOrNull { it.score } }
    if (points.isEmpty()) return

    val xValues = points.mapNotNull { it.smaFastPeriod }.distinct().sorted()
    val yValues = points.mapNotNull { it.smaSlowPeriod }.distinct().sorted()
    if (xValues.isEmpty() || yValues.isEmpty()) return
    val minScore = points.minOf { it.score }
    val maxScore = points.maxOf { it.score }
    val scoreRange = (maxScore - minScore).takeIf { it > 0.0 } ?: 1.0
    val lookup = points.associateBy { (it.smaFastPeriod ?: 0) to (it.smaSlowPeriod ?: 0) }

    Text(
        text = stringResource(Res.string.backtesting_optimization_heatmap_title),
        style = MaterialTheme.typography.labelMedium
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(MaterialTheme.shapes.small)
            .background(ext.toolbarSurface)
            .border(1.dp, ext.shellDivider, MaterialTheme.shapes.small)
            .padding(4.dp)
    ) {
        val cols = xValues.size.coerceAtLeast(1)
        val rows = yValues.size.coerceAtLeast(1)
        val cellWidth = size.width / cols.toFloat()
        val cellHeight = size.height / rows.toFloat()
        val emptyColor = ext.shellDivider.copy(alpha = 0.22f)
        for (row in yValues.indices) {
            for (col in xValues.indices) {
                val fast = xValues[col]
                val slow = yValues[yValues.lastIndex - row]
                val item = lookup[fast to slow]
                val color = if (item == null) {
                    emptyColor
                } else {
                    val normalized = ((item.score - minScore) / scoreRange).toFloat().coerceIn(0f, 1f)
                    lerp(ext.negative, ext.positive, normalized)
                }
                drawRect(
                    color = color.copy(alpha = 0.82f),
                    topLeft = androidx.compose.ui.geometry.Offset(col * cellWidth, row * cellHeight),
                    size = androidx.compose.ui.geometry.Size(cellWidth - 1f, cellHeight - 1f)
                )
            }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnalysisMetricChip(
            stringResource(Res.string.backtesting_optimization_sma_fast),
            "${xValues.first()}..${xValues.last()}"
        )
        AnalysisMetricChip(
            stringResource(Res.string.backtesting_optimization_sma_slow),
            "${yValues.first()}..${yValues.last()}"
        )
        AnalysisMetricChip(
            stringResource(Res.string.backtesting_optimization_score),
            "${minScore.pretty()}..${maxScore.pretty()}"
        )
    }
}

private enum class OptimizationRunSort {
    Score,
    Return,
    Sharpe,
    Drawdown
}

@Composable
private fun OptimizationRunsMatrix(
    runs: List<BacktestOptimizationRun>,
    sort: OptimizationRunSort,
    onlyProfitable: Boolean,
    minTradesInput: String,
    onSortChange: (OptimizationRunSort) -> Unit,
    onOnlyProfitableChange: (Boolean) -> Unit,
    onMinTradesInputChange: (String) -> Unit
) {
    val minTrades = minTradesInput.toIntOrNull() ?: 0
    val filtered = runs
        .asSequence()
        .filter { !onlyProfitable || it.metrics.totalReturnPercent > 0.0 }
        .filter { it.metrics.tradeCount >= minTrades }
        .let { sequence ->
            when (sort) {
                OptimizationRunSort.Score -> sequence.sortedByDescending { it.score }
                OptimizationRunSort.Return -> sequence.sortedByDescending { it.metrics.totalReturnPercent }
                OptimizationRunSort.Sharpe -> sequence.sortedByDescending { it.metrics.sharpeRatio }
                OptimizationRunSort.Drawdown -> sequence.sortedBy { it.metrics.maxDrawdownPercent }
            }
        }
        .take(60)
        .toList()
    if (runs.isEmpty()) return

    Text(
        text = stringResource(Res.string.backtesting_optimization_matrix_title),
        style = MaterialTheme.typography.labelMedium
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = onlyProfitable,
            onClick = { onOnlyProfitableChange(!onlyProfitable) },
            label = { Text(stringResource(Res.string.backtesting_optimization_filter_profitable)) }
        )
        OutlinedTextField(
            value = minTradesInput,
            onValueChange = onMinTradesInputChange,
            modifier = Modifier.widthIn(min = 110.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(Res.string.backtesting_optimization_filter_min_trades)) }
        )
        Text(
            text = stringResource(Res.string.backtesting_optimization_sort_by),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.extended.sidebarTextMuted,
            modifier = Modifier.padding(top = 8.dp)
        )
        FilterChip(
            selected = sort == OptimizationRunSort.Score,
            onClick = { onSortChange(OptimizationRunSort.Score) },
            label = { Text(stringResource(Res.string.backtesting_optimization_sort_score)) }
        )
        FilterChip(
            selected = sort == OptimizationRunSort.Return,
            onClick = { onSortChange(OptimizationRunSort.Return) },
            label = { Text(stringResource(Res.string.backtesting_optimization_sort_return)) }
        )
        FilterChip(
            selected = sort == OptimizationRunSort.Sharpe,
            onClick = { onSortChange(OptimizationRunSort.Sharpe) },
            label = { Text(stringResource(Res.string.backtesting_optimization_sort_sharpe)) }
        )
        FilterChip(
            selected = sort == OptimizationRunSort.Drawdown,
            onClick = { onSortChange(OptimizationRunSort.Drawdown) },
            label = { Text(stringResource(Res.string.backtesting_optimization_sort_drawdown)) }
        )
    }
    val ext = MaterialTheme.extended
    if (filtered.isEmpty()) {
        Text(
            text = stringResource(Res.string.backtesting_trades_empty),
            style = MaterialTheme.typography.bodySmall,
            color = ext.sidebarTextMuted
        )
        return
    }
    filtered.forEachIndexed { index, run ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(ext.toolbarSurface)
                .border(1.dp, ext.shellDivider, MaterialTheme.shapes.small)
                .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = ext.sidebarTextMuted,
                modifier = Modifier.width(26.dp)
            )
            Text(
                text = run.parameterLabel,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${run.metrics.totalReturnPercent.prettyPercent()} / S ${run.metrics.sharpeRatio.pretty()} / DD ${run.metrics.maxDrawdownPercent.prettyPercent()}",
                style = MaterialTheme.typography.labelSmall,
                color = ext.sidebarTextMuted
            )
        }
    }
}

@Composable
private fun TradesSection(
    state: BacktestingUiState,
    trades: List<BacktestTrade>,
    selectedTradeId: String?,
    onQueryChange: (String) -> Unit,
    onSelectTrade: (String?) -> Unit,
    onSortChange: (BacktestTradesSort) -> Unit,
    sortMenuExpanded: Boolean,
    onSortMenuExpandedChange: (Boolean) -> Unit
) {
    Text(stringResource(Res.string.backtesting_trades_title), style = MaterialTheme.typography.titleSmall)
    SnowToolbar {
        OutlinedTextField(
            value = state.tradesSearchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(stringResource(Res.string.backtesting_trades_search)) }
        )
        Box {
            SnowToolbarIconButton(
                icon = SnowIcons.Sort,
                onClick = { onSortMenuExpandedChange(true) },
                contentDescription = stringResource(Res.string.backtesting_filter_sort)
            )
            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { onSortMenuExpandedChange(false) }) {
                SnowToolbarPopupPanel {
                    listOf(
                        BacktestTradesSort.EntryNewest to stringResource(Res.string.backtesting_sort_entry_newest),
                        BacktestTradesSort.EntryOldest to stringResource(Res.string.backtesting_sort_entry_oldest),
                        BacktestTradesSort.PnlDesc to stringResource(Res.string.backtesting_sort_pnl_desc),
                        BacktestTradesSort.PnlAsc to stringResource(Res.string.backtesting_sort_pnl_asc)
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = state.tradesSort == mode,
                            onClick = {
                                onSortChange(mode)
                                onSortMenuExpandedChange(false)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    }

    if (trades.isEmpty()) {
        Text(stringResource(Res.string.backtesting_trades_empty), style = MaterialTheme.typography.bodySmall)
        return
    }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        HeaderCell(stringResource(Res.string.backtesting_header_side), 1.0f)
        HeaderCell(stringResource(Res.string.backtesting_header_entry), 1.8f)
        HeaderCell(stringResource(Res.string.backtesting_header_exit), 1.8f)
        HeaderCell(stringResource(Res.string.backtesting_header_pnl), 1.5f)
        HeaderCell(stringResource(Res.string.backtesting_header_duration), 1.0f)
        HeaderCell(stringResource(Res.string.backtesting_header_reason), 1.2f)
    }
    trades.take(200).forEach { trade ->
        val selected = selectedTradeId == trade.id
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(if (selected) MaterialTheme.extended.toolbarSurface else Color.Transparent)
                .clickable { onSelectTrade(trade.id) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ValueCell(if (trade.side == BacktestTradeSide.Long) "Long" else "Short", 1.0f)
            ValueCell("${trade.entryTime.formatDateTime()} @ ${trade.entryPrice.pretty()}", 1.8f)
            ValueCell("${trade.exitTime.formatDateTime()} @ ${trade.exitPrice.pretty()}", 1.8f)
            ValueCell("${trade.pnl.prettySigned()} (${trade.pnlPercent.prettyPercent()})", 1.5f)
            ValueCell("${trade.durationMinutes}m", 1.0f)
            ValueCell(trade.exitReason.name, 1.2f)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.extended.shellDivider.copy(alpha = 0.7f)))
    }
}

@Composable
private fun HistoryRow(
    run: BacktestHistoryRun,
    isSelected: Boolean,
    isCompared: Boolean,
    onOpen: () -> Unit,
    onToggleCompare: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.extended.toolbarSurface)
            .border(1.dp, MaterialTheme.extended.shellDivider, MaterialTheme.shapes.small)
            .padding(AppSpacing.s),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(run.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "${run.createdAt.formatDateTime()} | ${run.result.metrics.totalReturnPercent.prettyPercent()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extended.sidebarTextMuted
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = isSelected, onClick = onOpen, label = { Text(stringResource(Res.string.backtesting_history_open)) })
            FilterChip(selected = isCompared, onClick = onToggleCompare, label = { Text(stringResource(Res.string.backtesting_history_toggle_compare)) })
            AssistChip(onClick = onDelete, label = { Text(stringResource(Res.string.backtesting_delete)) })
        }
    }
}

@Composable
private fun CompareRuns(left: BacktestHistoryRun, right: BacktestHistoryRun) {
    Text(stringResource(Res.string.backtesting_compare_title), style = MaterialTheme.typography.titleSmall)
    CompareRow("Run", left.title, right.title)
    CompareRow(stringResource(Res.string.backtesting_win_rate), left.result.metrics.winRatePercent.prettyPercent(), right.result.metrics.winRatePercent.prettyPercent())
    CompareRow(stringResource(Res.string.backtesting_total_return), left.result.metrics.totalReturnPercent.prettyPercent(), right.result.metrics.totalReturnPercent.prettyPercent())
    CompareRow(stringResource(Res.string.backtesting_max_drawdown), left.result.metrics.maxDrawdownPercent.prettyPercent(), right.result.metrics.maxDrawdownPercent.prettyPercent())
    CompareRow(stringResource(Res.string.backtesting_sharpe), left.result.metrics.sharpeRatio.pretty(), right.result.metrics.sharpeRatio.pretty())
}

@Composable
private fun CompareRow(label: String, left: String, right: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extended.sidebarTextMuted)
        Text(left, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(right, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BacktestingSection(
    title: String,
    infoText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var showInfoPopup by rememberSaveable(title) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            if (!infoText.isNullOrBlank()) {
                Box {
                    SnowToolbarIconButton(
                        icon = SnowIcons.Info,
                        onClick = { showInfoPopup = true },
                        contentDescription = stringResource(Res.string.backtesting_section_info_content_description)
                    )
                    DropdownMenu(
                        expanded = showInfoPopup,
                        onDismissRequest = { showInfoPopup = false }
                    ) {
                        SnowToolbarPopupPanel(modifier = Modifier.widthIn(min = 320.dp, max = 560.dp)) {
                            Text(
                                text = infoText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.extended.sidebarTextMuted,
                                modifier = Modifier
                                    .heightIn(max = 420.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.extended.shellDivider.copy(alpha = 0.7f))
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            content()
        }
    }
}

@Composable
private fun BacktestSelectChip(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box {
        AssistChip(
            onClick = { onExpandedChange(true) },
            label = { Text("$label: $value", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { Icon(SnowIcons.CaretDown, contentDescription = null, modifier = Modifier.size(AppIconSize.xs)) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            SnowToolbarPopupPanel(modifier = Modifier.widthIn(min = 220.dp, max = 340.dp), content = content)
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.weight(weight).padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.extended.sidebarTextMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RowScope.ValueCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.weight(weight).padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
