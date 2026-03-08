package de.tradebuddy.domain.model

import java.time.Instant

enum class BacktestExchange(val id: String, val label: String) {
    BinanceSpot(id = "binance_spot", label = "Binance Spot"),
    BybitSpot(id = "bybit_spot", label = "Bybit Spot"),
    OkxSpot(id = "okx_spot", label = "OKX Spot")
}

enum class BacktestTimeframe(
    val id: String,
    val label: String,
    val binanceInterval: String,
    val approxMinutes: Int
) {
    M1(id = "1m", label = "1m", binanceInterval = "1m", approxMinutes = 1),
    M5(id = "5m", label = "5m", binanceInterval = "5m", approxMinutes = 5),
    M15(id = "15m", label = "15m", binanceInterval = "15m", approxMinutes = 15),
    H1(id = "1h", label = "1h", binanceInterval = "1h", approxMinutes = 60),
    H4(id = "4h", label = "4h", binanceInterval = "4h", approxMinutes = 240),
    D1(id = "1d", label = "1d", binanceInterval = "1d", approxMinutes = 1_440);
}

enum class BacktestStrategyTemplate(val id: String, val label: String) {
    SmaCrossover(id = "sma_crossover", label = "SMA Crossover"),
    RsiMeanReversion(id = "rsi_mean_reversion", label = "RSI Mean Reversion"),
    Breakout(id = "breakout", label = "Breakout"),
    SmaRsiConfluence(id = "sma_rsi_confluence", label = "SMA + RSI Confluence"),
    AtrVolatilityBreakout(id = "atr_volatility_breakout", label = "ATR Volatility Breakout"),
    SunMoonAstroFlow(id = "sun_moon_astro_flow", label = "Sun/Moon/Astro Flow"),
    Custom(id = "custom", label = "Custom Strategy")
}

enum class BacktestPositionSizingMode(val id: String, val label: String) {
    FixedAmount(id = "fixed", label = "Fixbetrag"),
    PercentOfEquity(id = "percent", label = "% vom Kapital"),
    AtrRisk(id = "atr_risk", label = "ATR-Risiko"),
    VolatilityTarget(id = "vol_target", label = "Volatilitäts-Ziel")
}

enum class BacktestOrderType(val id: String, val label: String) {
    Market(id = "market", label = "Market"),
    Limit(id = "limit", label = "Limit")
}

enum class BacktestMarginMode(val id: String, val label: String) {
    Cash(id = "cash", label = "Cash"),
    Isolated(id = "isolated", label = "Isolated"),
    Cross(id = "cross", label = "Cross")
}

enum class BacktestOptimizationMode(val id: String, val label: String) {
    Grid(id = "grid", label = "Grid Search"),
    Random(id = "random", label = "Random Search")
}

enum class BacktestTradeSide {
    Long,
    Short
}

enum class BacktestExitReason {
    Signal,
    StopLoss,
    TakeProfit,
    TrailingStop,
    DailyLossLimit,
    KillSwitch,
    EndOfTest
}

enum class BacktestSignal {
    Buy,
    Sell,
    Short,
    Cover
}

enum class BacktestEdgeValidationStatus {
    Passed,
    Warning,
    Failed,
    Unavailable
}

data class OhlcvCandle(
    val openTime: Instant,
    val closeTime: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class BacktestStrategyConfig(
    val template: BacktestStrategyTemplate = BacktestStrategyTemplate.SmaCrossover,
    val smaFastPeriod: Int = 20,
    val smaSlowPeriod: Int = 50,
    val rsiPeriod: Int = 14,
    val rsiOversold: Double = 30.0,
    val rsiOverbought: Double = 70.0,
    val breakoutLookback: Int = 20,
    val atrPeriod: Int = 14,
    val astroCityKey: String = "",
    val astroSignalWindowBars: Int = 1,
    val astroIncludeSunEvents: Boolean = true,
    val astroIncludeMoonEvents: Boolean = true,
    val astroIncludeMoonPhases: Boolean = true,
    val astroIncludeAspects: Boolean = true,
    val astroAspectsMoonOnly: Boolean = true,
    val customStrategyJson: String = "{}",
    val showSignalPreview: Boolean = false
)

data class BacktestExecutionSettings(
    val initialBalance: Double = 10_000.0,
    val sizingMode: BacktestPositionSizingMode = BacktestPositionSizingMode.PercentOfEquity,
    val fixedPositionAmount: Double = 1_000.0,
    val percentOfEquity: Double = 20.0,
    val atrRiskPercent: Double = 1.0,
    val targetVolatilityPercent: Double = 15.0,
    val makerFeeBps: Double = 6.0,
    val takerFeeBps: Double = 10.0,
    val slippageBps: Double = 5.0,
    val orderType: BacktestOrderType = BacktestOrderType.Market,
    val limitOffsetBps: Double = 2.0,
    val fillRatioPercent: Double = 100.0,
    val maxVolumeParticipationPercent: Double = 25.0,
    val allowShort: Boolean = false,
    val allowHedging: Boolean = false,
    val leverage: Double = 1.0,
    val marginMode: BacktestMarginMode = BacktestMarginMode.Cash,
    val fundingBpsPerDay: Double = 0.0,
    val stopLossPercent: Double? = null,
    val takeProfitPercent: Double? = null,
    val trailingStopPercent: Double? = null,
    val killSwitchMaxDrawdownPercent: Double? = null,
    val dailyLossLimitPercent: Double? = null,
    val maxConcurrentPositions: Int = 1
)

data class BacktestDataRequest(
    val exchange: BacktestExchange,
    val symbol: String,
    val timeframe: BacktestTimeframe,
    val from: Instant,
    val to: Instant,
    val warmupBars: Int = 200,
    val forceRefresh: Boolean = false
)

data class BacktestRunRequest(
    val data: BacktestDataRequest,
    val strategy: BacktestStrategyConfig,
    val execution: BacktestExecutionSettings
)

data class StrategySignalPoint(
    val symbol: String,
    val time: Instant,
    val price: Double,
    val signal: BacktestSignal
)

data class BacktestTrade(
    val id: String,
    val symbol: String,
    val side: BacktestTradeSide,
    val entryTime: Instant,
    val exitTime: Instant,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val durationMinutes: Long,
    val maePercent: Double,
    val mfePercent: Double,
    val feesPaid: Double,
    val fundingPaid: Double,
    val exitReason: BacktestExitReason
)

data class BacktestEquityPoint(
    val time: Instant,
    val equity: Double,
    val drawdownPercent: Double = 0.0
)

data class BacktestPricePoint(
    val time: Instant,
    val price: Double
)

data class BacktestMetrics(
    val winRatePercent: Double,
    val totalReturnPercent: Double,
    val cagrPercent: Double?,
    val maxDrawdownPercent: Double,
    val sharpeRatio: Double,
    val sortinoRatio: Double,
    val profitFactor: Double,
    val expectancyPercent: Double,
    val annualVolatilityPercent: Double,
    val tradeCount: Int,
    val averageTradePercent: Double,
    val exposureTimePercent: Double,
    val startEquity: Double,
    val endEquity: Double
)

data class BacktestMonthlyReturn(
    val year: Int,
    val month: Int,
    val returnPercent: Double
)

data class BacktestWalkForwardResult(
    val splitCount: Int,
    val inSampleAverageReturnPercent: Double,
    val outOfSampleAverageReturnPercent: Double,
    val stabilityScorePercent: Double
)

data class BacktestMonteCarloResult(
    val iterations: Int,
    val medianReturnPercent: Double,
    val p05ReturnPercent: Double,
    val p95ReturnPercent: Double
)

data class BacktestAnalysis(
    val monthlyReturns: List<BacktestMonthlyReturn>,
    val walkForward: BacktestWalkForwardResult?,
    val monteCarlo: BacktestMonteCarloResult?
)

data class BacktestEdgeValidation(
    val status: BacktestEdgeValidationStatus,
    val edgeScore: Double,
    val sampleCount: Int,
    val cpcvSplits: Int,
    val cpcvPaths: Int,
    val oosSharpe: Double? = null,
    val oosReturnPercent: Double? = null,
    val oosPositivePathPercent: Double? = null,
    val spaPValue: Double? = null,
    val notes: List<String> = emptyList(),
    val generatedAt: Instant = Instant.now()
)

data class BacktestEdgeGateDecision(
    val enabled: Boolean,
    val passed: Boolean,
    val minEdgeScore: Double,
    val maxSpaPValue: Double,
    val minPositivePathsPercent: Double,
    val minTrades: Int,
    val minOosSharpe: Double,
    val requirePassedStatus: Boolean,
    val reasons: List<String> = emptyList(),
    val evaluatedAt: Instant = Instant.now()
)

data class BacktestOptimizationRun(
    val runId: String,
    val score: Double,
    val parameterLabel: String,
    val metrics: BacktestMetrics,
    val smaFastPeriod: Int? = null,
    val smaSlowPeriod: Int? = null,
    val rsiPeriod: Int? = null,
    val breakoutLookback: Int? = null,
    val stopLossPercent: Double? = null,
    val takeProfitPercent: Double? = null,
    val trailingStopPercent: Double? = null
)

data class BacktestOptimizationResult(
    val mode: BacktestOptimizationMode,
    val iterations: Int,
    val bestRuns: List<BacktestOptimizationRun>,
    val allRuns: List<BacktestOptimizationRun> = emptyList(),
    val startedAt: Instant,
    val finishedAt: Instant
)

data class BacktestWalkForwardFoldResult(
    val foldIndex: Int,
    val inSampleFrom: Instant,
    val inSampleTo: Instant,
    val outOfSampleFrom: Instant,
    val outOfSampleTo: Instant,
    val bestParameterLabel: String,
    val outSampleMetrics: BacktestMetrics,
    val score: Double,
    val edgeGatePassed: Boolean? = null,
    val edgeScore: Double? = null
)

data class BacktestWalkForwardBatchResult(
    val splits: Int,
    val folds: List<BacktestWalkForwardFoldResult>,
    val averageOutSampleReturnPercent: Double,
    val averageOutSampleSharpe: Double,
    val averageOutSampleDrawdownPercent: Double
)

data class BacktestMarketData(
    val candles: List<OhlcvCandle>,
    val notes: List<String> = emptyList(),
    val servedFromCache: Boolean = false
)

data class BacktestResult(
    val runId: String,
    val request: BacktestRunRequest,
    val candlesCount: Int,
    val candleSeries: List<OhlcvCandle> = emptyList(),
    val trades: List<BacktestTrade>,
    val priceCurve: List<BacktestPricePoint> = emptyList(),
    val equityCurve: List<BacktestEquityPoint>,
    val drawdownCurve: List<BacktestEquityPoint>,
    val signalPoints: List<StrategySignalPoint>,
    val metrics: BacktestMetrics,
    val analysis: BacktestAnalysis? = null,
    val edgeValidation: BacktestEdgeValidation? = null,
    val edgeGateDecision: BacktestEdgeGateDecision? = null,
    val dataNotes: List<String>,
    val createdAt: Instant = Instant.now()
)

data class BacktestHistoryRun(
    val id: String,
    val title: String,
    val createdAt: Instant,
    val request: BacktestRunRequest,
    val result: BacktestResult
)

