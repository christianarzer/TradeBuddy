package de.tradebuddy.presentation

import de.tradebuddy.data.DefaultCityDataSource
import de.tradebuddy.domain.model.BacktestExchange
import de.tradebuddy.domain.model.BacktestHistoryRun
import de.tradebuddy.domain.model.BacktestMarginMode
import de.tradebuddy.domain.model.BacktestOptimizationMode
import de.tradebuddy.domain.model.BacktestOptimizationResult
import de.tradebuddy.domain.model.BacktestOrderType
import de.tradebuddy.domain.model.BacktestPositionSizingMode
import de.tradebuddy.domain.model.BacktestResult
import de.tradebuddy.domain.model.BacktestStrategyTemplate
import de.tradebuddy.domain.model.BacktestTimeframe
import de.tradebuddy.domain.model.BacktestWalkForwardBatchResult
import de.tradebuddy.domain.util.key
import java.time.LocalDate
import java.time.ZoneOffset

enum class BacktestTradesSort {
    EntryNewest,
    EntryOldest,
    PnlDesc,
    PnlAsc
}

enum class BacktestOptimizationObjective {
    Balanced,
    TotalReturn,
    Sharpe,
    ProfitFactor,
    Calmar
}

data class BacktestingUiState(
    val isLoadingHistory: Boolean = true,
    val isRunning: Boolean = false,
    val runProgress: Float = 0f,
    val runStatus: String? = null,
    val errorMessage: String? = null,
    val selectedStrategy: BacktestStrategyTemplate = BacktestStrategyTemplate.SmaCrossover,
    val showSignalPreview: Boolean = false,
    val smaFastInput: String = "20",
    val smaSlowInput: String = "50",
    val rsiPeriodInput: String = "14",
    val rsiOversoldInput: String = "30",
    val rsiOverboughtInput: String = "70",
    val breakoutLookbackInput: String = "20",
    val atrPeriodInput: String = "14",
    val astroCityKey: String = DefaultCityDataSource.cities().firstOrNull { it.label == "Frankfurt" }?.key()
        ?: DefaultCityDataSource.cities().firstOrNull()?.key().orEmpty(),
    val astroSignalWindowBarsInput: String = "1",
    val astroIncludeSunEvents: Boolean = true,
    val astroIncludeMoonEvents: Boolean = true,
    val astroIncludeMoonPhases: Boolean = true,
    val astroIncludeAspects: Boolean = true,
    val astroAspectsMoonOnly: Boolean = true,
    val customStrategyJson: String = """
        {
          "entryLong": {
            "all": [
              { "left": { "indicator": "sma", "period": 20 }, "op": "CROSS_UP", "right": { "indicator": "sma", "period": 50 } },
              { "left": { "indicator": "rsi", "period": 14 }, "op": "LT", "right": { "value": 65.0 } }
            ]
          },
          "exitLong": {
            "any": [
              { "left": { "indicator": "rsi", "period": 14 }, "op": "GT", "right": { "value": 74.0 } },
              { "left": { "indicator": "sma", "period": 20 }, "op": "CROSS_DOWN", "right": { "indicator": "sma", "period": 50 } }
            ]
          },
          "entryShort": {
            "all": [
              { "left": { "indicator": "sma", "period": 20 }, "op": "CROSS_DOWN", "right": { "indicator": "sma", "period": 50 } }
            ]
          },
          "exitShort": {
            "any": [
              { "left": { "indicator": "sma", "period": 20 }, "op": "CROSS_UP", "right": { "indicator": "sma", "period": 50 } }
            ]
          }
        }
    """.trimIndent(),
    val exchange: BacktestExchange = BacktestExchange.BinanceSpot,
    val symbolInput: String = "BTCUSDT",
    val availableSymbols: List<String> = emptyList(),
    val isLoadingSymbols: Boolean = false,
    val symbolsErrorMessage: String? = null,
    val timeframe: BacktestTimeframe = BacktestTimeframe.H1,
    val startDate: LocalDate = LocalDate.now(ZoneOffset.UTC).minusDays(90),
    val endDate: LocalDate = LocalDate.now(ZoneOffset.UTC),
    val warmupBarsInput: String = "200",
    val initialBalanceInput: String = "10000",
    val sizingMode: BacktestPositionSizingMode = BacktestPositionSizingMode.PercentOfEquity,
    val fixedAmountInput: String = "1000",
    val percentEquityInput: String = "20",
    val atrRiskPercentInput: String = "1",
    val targetVolatilityPercentInput: String = "15",
    val feeBpsInput: String = "10",
    val makerFeeBpsInput: String = "6",
    val takerFeeBpsInput: String = "10",
    val slippageBpsInput: String = "5",
    val orderType: BacktestOrderType = BacktestOrderType.Market,
    val limitOffsetBpsInput: String = "2",
    val fillRatioPercentInput: String = "100",
    val maxVolumeParticipationInput: String = "25",
    val allowShort: Boolean = false,
    val allowHedging: Boolean = false,
    val leverageInput: String = "1",
    val marginMode: BacktestMarginMode = BacktestMarginMode.Cash,
    val fundingBpsPerDayInput: String = "0",
    val stopLossInput: String = "",
    val takeProfitInput: String = "",
    val trailingStopInput: String = "",
    val killSwitchDrawdownInput: String = "",
    val dailyLossLimitInput: String = "",
    val optimizationMode: BacktestOptimizationMode = BacktestOptimizationMode.Grid,
    val optimizationObjective: BacktestOptimizationObjective = BacktestOptimizationObjective.Balanced,
    val optimizationIterationsInput: String = "24",
    val optimizationTopKInput: String = "6",
    val optimizationSmaFastMinInput: String = "6",
    val optimizationSmaFastMaxInput: String = "36",
    val optimizationSmaFastStepInput: String = "2",
    val optimizationSmaSlowMinInput: String = "28",
    val optimizationSmaSlowMaxInput: String = "140",
    val optimizationSmaSlowStepInput: String = "4",
    val optimizationRsiPeriodMinInput: String = "7",
    val optimizationRsiPeriodMaxInput: String = "28",
    val optimizationRsiPeriodStepInput: String = "3",
    val optimizationBreakoutMinInput: String = "10",
    val optimizationBreakoutMaxInput: String = "90",
    val optimizationBreakoutStepInput: String = "5",
    val optimizationResult: BacktestOptimizationResult? = null,
    val walkForwardSplitsInput: String = "4",
    val walkForwardResult: BacktestWalkForwardBatchResult? = null,
    val dataNotes: List<String> = emptyList(),
    val result: BacktestResult? = null,
    val history: List<BacktestHistoryRun> = emptyList(),
    val selectedHistoryRunId: String? = null,
    val comparisonRunIds: Set<String> = emptySet(),
    val selectedTradeId: String? = null,
    val tradesSearchQuery: String = "",
    val tradesSort: BacktestTradesSort = BacktestTradesSort.EntryNewest
)
