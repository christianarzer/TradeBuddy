package de.tradebuddy.presentation

import de.tradebuddy.data.BacktestingRepository
import de.tradebuddy.domain.model.BacktestDataRequest
import de.tradebuddy.domain.model.BacktestExchange
import de.tradebuddy.domain.model.BacktestExecutionSettings
import de.tradebuddy.domain.model.BacktestHistoryRun
import de.tradebuddy.domain.model.BacktestMarginMode
import de.tradebuddy.domain.model.BacktestOptimizationMode
import de.tradebuddy.domain.model.BacktestOptimizationResult
import de.tradebuddy.domain.model.BacktestOptimizationRun
import de.tradebuddy.domain.model.BacktestOrderType
import de.tradebuddy.domain.model.BacktestPositionSizingMode
import de.tradebuddy.domain.model.BacktestRunRequest
import de.tradebuddy.domain.model.BacktestStrategyConfig
import de.tradebuddy.domain.model.BacktestStrategyTemplate
import de.tradebuddy.domain.model.BacktestTimeframe
import de.tradebuddy.domain.model.BacktestWalkForwardBatchResult
import de.tradebuddy.domain.model.BacktestWalkForwardFoldResult
import de.tradebuddy.domain.model.OhlcvCandle
import de.tradebuddy.engine.BacktestingEngine
import de.tradebuddy.logging.AppLog
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BacktestingViewModel(
    private val repository: BacktestingRepository,
    private val engine: BacktestingEngine = BacktestingEngine()
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var runJob: Job? = null
    private val json = Json { prettyPrint = true }

    private val _state = MutableStateFlow(BacktestingUiState())
    val state: StateFlow<BacktestingUiState> = _state.asStateFlow()

    init {
        loadHistory()
        loadSymbols(forceRefresh = false)
    }

    fun setTradesSearchQuery(query: String) {
        _state.update { it.copy(tradesSearchQuery = query) }
    }

    fun setTradesSort(sort: BacktestTradesSort) {
        _state.update { it.copy(tradesSort = sort) }
    }

    fun setExchange(exchange: BacktestExchange) {
        val current = _state.value
        if (current.exchange == exchange) return
        _state.update {
            it.copy(
                exchange = exchange,
                symbolsErrorMessage = null
            )
        }
        loadSymbols(exchange = exchange, forceRefresh = false)
    }

    fun setStrategy(strategy: BacktestStrategyTemplate) {
        _state.update { it.copy(selectedStrategy = strategy) }
    }

    fun setShowSignalPreview(enabled: Boolean) {
        _state.update { it.copy(showSignalPreview = enabled) }
    }

    fun setSmaFast(value: String) = _state.update { it.copy(smaFastInput = value) }
    fun setSmaSlow(value: String) = _state.update { it.copy(smaSlowInput = value) }
    fun setRsiPeriod(value: String) = _state.update { it.copy(rsiPeriodInput = value) }
    fun setRsiOversold(value: String) = _state.update { it.copy(rsiOversoldInput = value) }
    fun setRsiOverbought(value: String) = _state.update { it.copy(rsiOverboughtInput = value) }
    fun setBreakoutLookback(value: String) = _state.update { it.copy(breakoutLookbackInput = value) }
    fun setAtrPeriod(value: String) = _state.update { it.copy(atrPeriodInput = value) }
    fun setAstroCityKey(value: String) = _state.update { it.copy(astroCityKey = value) }
    fun setAstroSignalWindowBars(value: String) = _state.update { it.copy(astroSignalWindowBarsInput = value) }
    fun setAstroIncludeSunEvents(value: Boolean) = _state.update { it.copy(astroIncludeSunEvents = value) }
    fun setAstroIncludeMoonEvents(value: Boolean) = _state.update { it.copy(astroIncludeMoonEvents = value) }
    fun setAstroIncludeMoonPhases(value: Boolean) = _state.update { it.copy(astroIncludeMoonPhases = value) }
    fun setAstroIncludeAspects(value: Boolean) = _state.update { it.copy(astroIncludeAspects = value) }
    fun setAstroAspectsMoonOnly(value: Boolean) = _state.update { it.copy(astroAspectsMoonOnly = value) }
    fun setCustomStrategyJson(value: String) = _state.update { it.copy(customStrategyJson = value) }
    fun applyCustomStrategyPreset(preset: BacktestCustomPreset) {
        _state.update { current ->
            current.copy(
                selectedStrategy = BacktestStrategyTemplate.Custom,
                customStrategyJson = preset.json
            )
        }
    }
    fun setSymbol(value: String) = _state.update { it.copy(symbolInput = value.uppercase()) }
    fun refreshSymbols() {
        loadSymbols(forceRefresh = true)
    }
    fun setTimeframe(value: BacktestTimeframe) = _state.update { it.copy(timeframe = value) }
    fun setWarmupBars(value: String) = _state.update { it.copy(warmupBarsInput = value) }
    fun setStartDate(value: LocalDate) = _state.update { current ->
        val adjustedEnd = if (value > current.endDate) value else current.endDate
        current.copy(startDate = value, endDate = adjustedEnd)
    }

    fun setEndDate(value: LocalDate) = _state.update { current ->
        val adjustedStart = if (value < current.startDate) value else current.startDate
        current.copy(startDate = adjustedStart, endDate = value)
    }

    fun setInitialBalance(value: String) = _state.update { it.copy(initialBalanceInput = value) }
    fun setSizingMode(value: BacktestPositionSizingMode) = _state.update { it.copy(sizingMode = value) }
    fun setFixedAmount(value: String) = _state.update { it.copy(fixedAmountInput = value) }
    fun setPercentOfEquity(value: String) = _state.update { it.copy(percentEquityInput = value) }
    fun setAtrRiskPercent(value: String) = _state.update { it.copy(atrRiskPercentInput = value) }
    fun setTargetVolatilityPercent(value: String) = _state.update { it.copy(targetVolatilityPercentInput = value) }
    fun setFeeBps(value: String) = _state.update {
        it.copy(
            feeBpsInput = value,
            makerFeeBpsInput = value,
            takerFeeBpsInput = value
        )
    }
    fun setMakerFeeBps(value: String) = _state.update { it.copy(makerFeeBpsInput = value) }
    fun setTakerFeeBps(value: String) = _state.update { it.copy(takerFeeBpsInput = value) }
    fun setSlippageBps(value: String) = _state.update { it.copy(slippageBpsInput = value) }
    fun setOrderType(value: BacktestOrderType) = _state.update { it.copy(orderType = value) }
    fun setLimitOffsetBps(value: String) = _state.update { it.copy(limitOffsetBpsInput = value) }
    fun setFillRatioPercent(value: String) = _state.update { it.copy(fillRatioPercentInput = value) }
    fun setMaxVolumeParticipation(value: String) = _state.update { it.copy(maxVolumeParticipationInput = value) }
    fun setAllowShort(value: Boolean) = _state.update { it.copy(allowShort = value) }
    fun setAllowHedging(value: Boolean) = _state.update { it.copy(allowHedging = value) }
    fun setLeverage(value: String) = _state.update { it.copy(leverageInput = value) }
    fun setMarginMode(value: BacktestMarginMode) = _state.update { it.copy(marginMode = value) }
    fun setFundingBpsPerDay(value: String) = _state.update { it.copy(fundingBpsPerDayInput = value) }
    fun setStopLoss(value: String) = _state.update { it.copy(stopLossInput = value) }
    fun setTakeProfit(value: String) = _state.update { it.copy(takeProfitInput = value) }
    fun setTrailingStop(value: String) = _state.update { it.copy(trailingStopInput = value) }
    fun setKillSwitchDrawdown(value: String) = _state.update { it.copy(killSwitchDrawdownInput = value) }
    fun setDailyLossLimit(value: String) = _state.update { it.copy(dailyLossLimitInput = value) }
    fun setOptimizationMode(value: BacktestOptimizationMode) = _state.update { it.copy(optimizationMode = value) }
    fun setOptimizationObjective(value: BacktestOptimizationObjective) = _state.update { it.copy(optimizationObjective = value) }
    fun setOptimizationIterations(value: String) = _state.update { it.copy(optimizationIterationsInput = value) }
    fun setOptimizationTopK(value: String) = _state.update { it.copy(optimizationTopKInput = value) }
    fun setOptimizationSmaFastMin(value: String) = _state.update { it.copy(optimizationSmaFastMinInput = value) }
    fun setOptimizationSmaFastMax(value: String) = _state.update { it.copy(optimizationSmaFastMaxInput = value) }
    fun setOptimizationSmaFastStep(value: String) = _state.update { it.copy(optimizationSmaFastStepInput = value) }
    fun setOptimizationSmaSlowMin(value: String) = _state.update { it.copy(optimizationSmaSlowMinInput = value) }
    fun setOptimizationSmaSlowMax(value: String) = _state.update { it.copy(optimizationSmaSlowMaxInput = value) }
    fun setOptimizationSmaSlowStep(value: String) = _state.update { it.copy(optimizationSmaSlowStepInput = value) }
    fun setOptimizationRsiMin(value: String) = _state.update { it.copy(optimizationRsiPeriodMinInput = value) }
    fun setOptimizationRsiMax(value: String) = _state.update { it.copy(optimizationRsiPeriodMaxInput = value) }
    fun setOptimizationRsiStep(value: String) = _state.update { it.copy(optimizationRsiPeriodStepInput = value) }
    fun setOptimizationBreakoutMin(value: String) = _state.update { it.copy(optimizationBreakoutMinInput = value) }
    fun setOptimizationBreakoutMax(value: String) = _state.update { it.copy(optimizationBreakoutMaxInput = value) }
    fun setOptimizationBreakoutStep(value: String) = _state.update { it.copy(optimizationBreakoutStepInput = value) }
    fun setWalkForwardSplits(value: String) = _state.update { it.copy(walkForwardSplitsInput = value) }
    fun selectTrade(tradeId: String?) = _state.update { current ->
        current.copy(selectedTradeId = if (current.selectedTradeId == tradeId) null else tradeId)
    }

    fun runBacktest(forceRefreshData: Boolean = false) {
        val snapshot = _state.value
        val parsed = parseRunRequest(snapshot, forceRefreshData) ?: return
        runJob?.cancel()
        runJob = scope.launch {
            _state.update {
                it.copy(
                    isRunning = true,
                    runProgress = 0.01f,
                    runStatus = "Backtest wird vorbereitet",
                    errorMessage = null,
                    dataNotes = emptyList(),
                    optimizationResult = null,
                    walkForwardResult = null
                )
            }

            runCatching {
                val marketData = repository.loadCandles(parsed.data) { progress, message ->
                    _state.update { current ->
                        if (!current.isRunning) {
                            current
                        } else {
                            current.copy(
                                runProgress = (0.05f + progress.coerceIn(0f, 1f) * 0.45f).coerceIn(0f, 0.55f),
                                runStatus = message
                            )
                        }
                    }
                }
                val result = engine.run(parsed, marketData) { progress ->
                    _state.update { current ->
                        if (!current.isRunning) {
                            current
                        } else {
                            current.copy(
                                runProgress = (0.55f + progress.coerceIn(0f, 1f) * 0.40f).coerceIn(0.55f, 0.95f),
                                runStatus = "Simulation läuft"
                            )
                        }
                    }
                }
                val historyRun = BacktestHistoryRun(
                    id = result.runId,
                    title = "${parsed.data.symbol} - ${parsed.strategy.template.label} - ${parsed.data.timeframe.label}",
                    createdAt = Instant.now(),
                    request = parsed,
                    result = result.copy(dataNotes = result.dataNotes + marketData.notes)
                )
                repository.saveHistoryRun(historyRun)
                val history = repository.loadHistory()
                history to historyRun
            }.onSuccess { (history, latestRun) ->
                _state.update {
                    it.copy(
                        isRunning = false,
                        runProgress = 1f,
                        runStatus = "Fertig",
                        result = latestRun.result,
                        dataNotes = latestRun.result.dataNotes,
                        history = history,
                        selectedHistoryRunId = latestRun.id,
                        selectedTradeId = latestRun.result.trades.firstOrNull()?.id
                    )
                }
                _state.update { it.copy(runProgress = 0f, runStatus = null) }
            }.onFailure { error ->
                if (error is CancellationException) {
                    _state.update {
                        it.copy(
                            isRunning = false,
                            runProgress = 0f,
                            runStatus = null
                        )
                    }
                    return@onFailure
                }
                AppLog.error(
                    tag = "BacktestingViewModel",
                    message = "Backtest fehlgeschlagen",
                    throwable = error
                )
                _state.update {
                    it.copy(
                        isRunning = false,
                        runProgress = 0f,
                        runStatus = null,
                        errorMessage = error.message ?: "Backtest konnte nicht ausgeführt werden"
                    )
                }
            }
        }
    }

    fun cancelBacktest() {
        runJob?.cancel()
        _state.update { it.copy(isRunning = false, runProgress = 0f, runStatus = null) }
    }

    fun runOptimization(forceRefreshData: Boolean = false) {
        val snapshot = _state.value
        val baseRequest = parseRunRequest(snapshot, forceRefreshData) ?: return
        val iterations = snapshot.optimizationIterationsInput.parsePositiveInt(max = 500)
        val topK = snapshot.optimizationTopKInput.parsePositiveInt(max = 20)
        val smaFastMin = snapshot.optimizationSmaFastMinInput.parsePositiveInt(max = 500)
        val smaFastMax = snapshot.optimizationSmaFastMaxInput.parsePositiveInt(max = 500)
        val smaFastStep = snapshot.optimizationSmaFastStepInput.parsePositiveInt(max = 100)
        val smaSlowMin = snapshot.optimizationSmaSlowMinInput.parsePositiveInt(max = 2_000)
        val smaSlowMax = snapshot.optimizationSmaSlowMaxInput.parsePositiveInt(max = 2_000)
        val smaSlowStep = snapshot.optimizationSmaSlowStepInput.parsePositiveInt(max = 500)
        val rsiMin = snapshot.optimizationRsiPeriodMinInput.parsePositiveInt(max = 200)
        val rsiMax = snapshot.optimizationRsiPeriodMaxInput.parsePositiveInt(max = 200)
        val rsiStep = snapshot.optimizationRsiPeriodStepInput.parsePositiveInt(max = 50)
        val breakoutMin = snapshot.optimizationBreakoutMinInput.parsePositiveInt(max = 1_000)
        val breakoutMax = snapshot.optimizationBreakoutMaxInput.parsePositiveInt(max = 1_000)
        val breakoutStep = snapshot.optimizationBreakoutStepInput.parsePositiveInt(max = 200)
        if (iterations == null || topK == null) {
            _state.update { it.copy(errorMessage = "Ungültige Optimierungswerte (Iterationen/Top-K)") }
            return
        }
        if (
            smaFastMin == null || smaFastMax == null || smaFastStep == null ||
            smaSlowMin == null || smaSlowMax == null || smaSlowStep == null ||
            rsiMin == null || rsiMax == null || rsiStep == null ||
            breakoutMin == null || breakoutMax == null || breakoutStep == null
        ) {
            _state.update { it.copy(errorMessage = "Ungültige Optimierungs-Ranges") }
            return
        }

        runJob?.cancel()
        runJob = scope.launch {
            _state.update {
                it.copy(
                    isRunning = true,
                    runProgress = 0.01f,
                    runStatus = "Optimierung wird vorbereitet",
                    errorMessage = null,
                    optimizationResult = null,
                    walkForwardResult = null
                )
            }

            runCatching {
                val marketData = repository.loadCandles(baseRequest.data) { progress, message ->
                    _state.update { current ->
                        if (!current.isRunning) {
                            current
                        } else {
                            current.copy(
                                runProgress = (0.05f + progress.coerceIn(0f, 1f) * 0.35f).coerceIn(0f, 0.40f),
                                runStatus = message
                            )
                        }
                    }
                }

                val candidates = buildOptimizationCandidates(
                    base = baseRequest,
                    state = snapshot,
                    mode = snapshot.optimizationMode,
                    iterations = iterations,
                    smaFastMin = smaFastMin,
                    smaFastMax = smaFastMax,
                    smaFastStep = smaFastStep,
                    smaSlowMin = smaSlowMin,
                    smaSlowMax = smaSlowMax,
                    smaSlowStep = smaSlowStep,
                    rsiMin = rsiMin,
                    rsiMax = rsiMax,
                    rsiStep = rsiStep,
                    breakoutMin = breakoutMin,
                    breakoutMax = breakoutMax,
                    breakoutStep = breakoutStep
                )
                if (candidates.isEmpty()) error("Keine Optimierungskombinationen verfügbar")

                val startedAt = Instant.now()
                val runs = mutableListOf<BacktestOptimizationRun>()
                var bestResult: de.tradebuddy.domain.model.BacktestResult? = null
                var bestScore = Double.NEGATIVE_INFINITY

                candidates.forEachIndexed { index, candidate ->
                    coroutineContext.ensureActive()
                    val candidateResult = engine.run(candidate, marketData)
                    val score = scoreResult(candidateResult, snapshot.optimizationObjective)
                    runs += BacktestOptimizationRun(
                        runId = candidateResult.runId,
                        score = score,
                        parameterLabel = candidate.toParameterLabel(),
                        metrics = candidateResult.metrics,
                        smaFastPeriod = candidate.strategy.smaFastPeriod,
                        smaSlowPeriod = candidate.strategy.smaSlowPeriod,
                        rsiPeriod = candidate.strategy.rsiPeriod,
                        breakoutLookback = candidate.strategy.breakoutLookback,
                        stopLossPercent = candidate.execution.stopLossPercent,
                        takeProfitPercent = candidate.execution.takeProfitPercent,
                        trailingStopPercent = candidate.execution.trailingStopPercent
                    )
                    if (score > bestScore) {
                        bestScore = score
                        bestResult = candidateResult
                    }
                    if (index % 3 == 0) {
                        yield()
                    }
                    val progress = (index + 1).toFloat() / candidates.size.toFloat()
                    _state.update { current ->
                        if (!current.isRunning) {
                            current
                        } else {
                            current.copy(
                                runProgress = (0.40f + progress * 0.55f).coerceIn(0.40f, 0.95f),
                                runStatus = "Optimierung ${index + 1}/${candidates.size}"
                            )
                        }
                    }
                }

                val optimizationResult = BacktestOptimizationResult(
                    mode = snapshot.optimizationMode,
                    iterations = candidates.size,
                    bestRuns = runs.sortedByDescending { it.score }.take(topK),
                    allRuns = runs,
                    startedAt = startedAt,
                    finishedAt = Instant.now()
                )

                val pickedResult = bestResult ?: error("Kein Ergebnis aus Optimierung")
                val bestHistoryRun = BacktestHistoryRun(
                    id = pickedResult.runId,
                    title = "OPT ${baseRequest.data.symbol} - ${baseRequest.strategy.template.label} - ${baseRequest.data.timeframe.label}",
                    createdAt = Instant.now(),
                    request = pickedResult.request,
                    result = pickedResult.copy(dataNotes = pickedResult.dataNotes + marketData.notes)
                )
                repository.saveHistoryRun(bestHistoryRun)
                val history = repository.loadHistory()
                Triple(history, bestHistoryRun, optimizationResult)
            }.onSuccess { (history, bestRun, optimizationResult) ->
                _state.update {
                    it.copy(
                        isRunning = false,
                        runProgress = 1f,
                        runStatus = "Optimierung abgeschlossen",
                        result = bestRun.result,
                        dataNotes = bestRun.result.dataNotes,
                        history = history,
                        selectedHistoryRunId = bestRun.id,
                        optimizationResult = optimizationResult,
                        walkForwardResult = null,
                        selectedTradeId = bestRun.result.trades.firstOrNull()?.id
                    )
                }
                _state.update { it.copy(runProgress = 0f, runStatus = null) }
            }.onFailure { error ->
                if (error is CancellationException) {
                    _state.update { it.copy(isRunning = false, runProgress = 0f, runStatus = null) }
                    return@onFailure
                }
                AppLog.error(
                    tag = "BacktestingViewModel",
                    message = "Optimierung fehlgeschlagen",
                    throwable = error
                )
                _state.update {
                    it.copy(
                        isRunning = false,
                        runProgress = 0f,
                        runStatus = null,
                        errorMessage = error.message ?: "Optimierung konnte nicht ausgeführt werden"
                    )
                }
            }
        }
    }

    fun runWalkForward(forceRefreshData: Boolean = false) {
        val snapshot = _state.value
        val baseRequest = parseRunRequest(snapshot, forceRefreshData) ?: return
        val splits = snapshot.walkForwardSplitsInput.parsePositiveInt(max = 12)
        val iterations = snapshot.optimizationIterationsInput.parsePositiveInt(max = 500)
        val smaFastMin = snapshot.optimizationSmaFastMinInput.parsePositiveInt(max = 500)
        val smaFastMax = snapshot.optimizationSmaFastMaxInput.parsePositiveInt(max = 500)
        val smaFastStep = snapshot.optimizationSmaFastStepInput.parsePositiveInt(max = 100)
        val smaSlowMin = snapshot.optimizationSmaSlowMinInput.parsePositiveInt(max = 2_000)
        val smaSlowMax = snapshot.optimizationSmaSlowMaxInput.parsePositiveInt(max = 2_000)
        val smaSlowStep = snapshot.optimizationSmaSlowStepInput.parsePositiveInt(max = 500)
        val rsiMin = snapshot.optimizationRsiPeriodMinInput.parsePositiveInt(max = 200)
        val rsiMax = snapshot.optimizationRsiPeriodMaxInput.parsePositiveInt(max = 200)
        val rsiStep = snapshot.optimizationRsiPeriodStepInput.parsePositiveInt(max = 50)
        val breakoutMin = snapshot.optimizationBreakoutMinInput.parsePositiveInt(max = 1_000)
        val breakoutMax = snapshot.optimizationBreakoutMaxInput.parsePositiveInt(max = 1_000)
        val breakoutStep = snapshot.optimizationBreakoutStepInput.parsePositiveInt(max = 200)
        if (splits == null || iterations == null) {
            _state.update { it.copy(errorMessage = "Ungültige Walk-Forward-Einstellungen") }
            return
        }
        if (
            smaFastMin == null || smaFastMax == null || smaFastStep == null ||
            smaSlowMin == null || smaSlowMax == null || smaSlowStep == null ||
            rsiMin == null || rsiMax == null || rsiStep == null ||
            breakoutMin == null || breakoutMax == null || breakoutStep == null
        ) {
            _state.update { it.copy(errorMessage = "Ungültige Optimierungs-Ranges") }
            return
        }

        runJob?.cancel()
        runJob = scope.launch {
            _state.update {
                it.copy(
                    isRunning = true,
                    runProgress = 0.01f,
                    runStatus = "Walk-Forward wird vorbereitet",
                    errorMessage = null,
                    optimizationResult = null,
                    walkForwardResult = null
                )
            }

            runCatching {
                val marketData = repository.loadCandles(baseRequest.data) { progress, message ->
                    _state.update { current ->
                        if (!current.isRunning) current else current.copy(
                            runProgress = (0.05f + progress.coerceIn(0f, 1f) * 0.25f).coerceIn(0f, 0.30f),
                            runStatus = message
                        )
                    }
                }
                val sortedCandles = marketData.candles.sortedBy { it.openTime }
                val folds = buildWalkForwardFolds(sortedCandles, splits)
                if (folds.isEmpty()) {
                    error("Zu wenige Kerzen für Walk-Forward (Splits reduzieren oder Zeitraum vergrößern)")
                }

                val foldResults = mutableListOf<BacktestWalkForwardFoldResult>()
                var latestOutResult: de.tradebuddy.domain.model.BacktestResult? = null
                val startedAt = Instant.now()

                for ((foldIndex, fold) in folds.withIndex()) {
                    coroutineContext.ensureActive()
                    val trainRequest = baseRequest.withRange(
                        from = fold.train.first().openTime,
                        to = fold.train.last().closeTime
                    )
                    val candidates = buildOptimizationCandidates(
                        base = trainRequest,
                        state = snapshot,
                        mode = snapshot.optimizationMode,
                        iterations = iterations,
                        smaFastMin = smaFastMin,
                        smaFastMax = smaFastMax,
                        smaFastStep = smaFastStep,
                        smaSlowMin = smaSlowMin,
                        smaSlowMax = smaSlowMax,
                        smaSlowStep = smaSlowStep,
                        rsiMin = rsiMin,
                        rsiMax = rsiMax,
                        rsiStep = rsiStep,
                        breakoutMin = breakoutMin,
                        breakoutMax = breakoutMax,
                        breakoutStep = breakoutStep
                    )
                    if (candidates.isEmpty()) {
                        error("Keine Kandidaten für Fold ${foldIndex + 1}")
                    }

                    val trainData = de.tradebuddy.domain.model.BacktestMarketData(
                        candles = fold.train,
                        notes = marketData.notes,
                        servedFromCache = marketData.servedFromCache
                    )
                    var bestCandidate: BacktestRunRequest? = null
                    var bestScore = Double.NEGATIVE_INFINITY
                    candidates.forEachIndexed { candidateIndex, candidate ->
                        coroutineContext.ensureActive()
                        val trainResult = engine.run(candidate, trainData)
                        val score = scoreResult(trainResult, snapshot.optimizationObjective)
                        if (score > bestScore) {
                            bestScore = score
                            bestCandidate = candidate
                        }
                        if (candidateIndex % 3 == 0) {
                            yield()
                        }
                    }
                    val winner = bestCandidate ?: error("Kein bester Kandidat für Fold ${foldIndex + 1}")

                    val testRequest = winner.withRange(
                        from = fold.test.first().openTime,
                        to = fold.test.last().closeTime
                    )
                    val testData = de.tradebuddy.domain.model.BacktestMarketData(
                        candles = fold.test,
                        notes = marketData.notes,
                        servedFromCache = marketData.servedFromCache
                    )
                    val outResult = engine.run(testRequest, testData)
                    latestOutResult = outResult
                    val outScore = scoreResult(outResult, snapshot.optimizationObjective)
                    foldResults += BacktestWalkForwardFoldResult(
                        foldIndex = foldIndex + 1,
                        inSampleFrom = fold.train.first().openTime,
                        inSampleTo = fold.train.last().closeTime,
                        outOfSampleFrom = fold.test.first().openTime,
                        outOfSampleTo = fold.test.last().closeTime,
                        bestParameterLabel = winner.toParameterLabel(),
                        outSampleMetrics = outResult.metrics,
                        score = outScore
                    )
                    val progress = (foldIndex + 1).toFloat() / folds.size.toFloat()
                    _state.update { current ->
                        if (!current.isRunning) current else current.copy(
                            runProgress = (0.30f + progress * 0.65f).coerceIn(0.30f, 0.95f),
                            runStatus = "Walk-Forward Fold ${foldIndex + 1}/${folds.size}"
                        )
                    }
                }

                val wfResult = BacktestWalkForwardBatchResult(
                    splits = foldResults.size,
                    folds = foldResults,
                    averageOutSampleReturnPercent = foldResults.map { it.outSampleMetrics.totalReturnPercent }.average(),
                    averageOutSampleSharpe = foldResults.map { it.outSampleMetrics.sharpeRatio }.average(),
                    averageOutSampleDrawdownPercent = foldResults.map { it.outSampleMetrics.maxDrawdownPercent }.average()
                )

                val selectedResult = latestOutResult ?: error("Walk-Forward lieferte kein Ergebnis")
                val historyRun = BacktestHistoryRun(
                    id = selectedResult.runId,
                    title = "WF ${baseRequest.data.symbol} - ${baseRequest.strategy.template.label} - ${baseRequest.data.timeframe.label}",
                    createdAt = startedAt,
                    request = selectedResult.request,
                    result = selectedResult
                )
                repository.saveHistoryRun(historyRun)
                val history = repository.loadHistory()
                Triple(history, historyRun, wfResult)
            }.onSuccess { (history, run, wfResult) ->
                _state.update {
                    it.copy(
                        isRunning = false,
                        runProgress = 1f,
                        runStatus = "Walk-Forward abgeschlossen",
                        result = run.result,
                        dataNotes = run.result.dataNotes,
                        history = history,
                        selectedHistoryRunId = run.id,
                        selectedTradeId = run.result.trades.firstOrNull()?.id,
                        walkForwardResult = wfResult
                    )
                }
                _state.update { it.copy(runProgress = 0f, runStatus = null) }
            }.onFailure { error ->
                if (error is CancellationException) {
                    _state.update { it.copy(isRunning = false, runProgress = 0f, runStatus = null) }
                    return@onFailure
                }
                AppLog.error(
                    tag = "BacktestingViewModel",
                    message = "Walk-Forward fehlgeschlagen",
                    throwable = error
                )
                _state.update {
                    it.copy(
                        isRunning = false,
                        runProgress = 0f,
                        runStatus = null,
                        errorMessage = error.message ?: "Walk-Forward konnte nicht ausgeführt werden"
                    )
                }
            }
        }
    }

    fun loadHistory() {
        scope.launch {
            _state.update { it.copy(isLoadingHistory = true) }
            runCatching { repository.loadHistory() }
                .onSuccess { history ->
                    _state.update {
                        val selected = it.selectedHistoryRunId?.takeIf { id -> history.any { run -> run.id == id } }
                        it.copy(
                            isLoadingHistory = false,
                            history = history,
                            selectedHistoryRunId = selected
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingHistory = false,
                            errorMessage = error.message ?: "Historie konnte nicht geladen werden"
                        )
                    }
                }
        }
    }

    private fun loadSymbols(
        exchange: BacktestExchange = _state.value.exchange,
        forceRefresh: Boolean
    ) {
        scope.launch {
            _state.update {
                it.copy(
                    isLoadingSymbols = true,
                    symbolsErrorMessage = null
                )
            }

            runCatching { repository.loadSymbols(exchange = exchange, forceRefresh = forceRefresh) }
                .onSuccess { symbols ->
                    val normalized = symbols
                        .map { it.trim().uppercase() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    _state.update { current ->
                        val nextSymbol = when {
                            current.symbolInput.trim().uppercase() in normalized -> current.symbolInput.trim().uppercase()
                            normalized.isNotEmpty() -> normalized.first()
                            else -> current.symbolInput.trim().uppercase()
                        }
                        current.copy(
                            availableSymbols = normalized,
                            symbolInput = nextSymbol,
                            isLoadingSymbols = false,
                            symbolsErrorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { current ->
                        current.copy(
                            isLoadingSymbols = false,
                            symbolsErrorMessage = error.message ?: "Symbol-Liste konnte nicht geladen werden"
                        )
                    }
                }
        }
    }

    fun openHistoryRun(runId: String) {
        val run = _state.value.history.firstOrNull { it.id == runId } ?: return
        _state.update {
            applyRunRequest(it, run.request).copy(
                selectedHistoryRunId = runId,
                result = run.result,
                dataNotes = run.result.dataNotes,
                selectedTradeId = run.result.trades.firstOrNull()?.id
            )
        }
        loadSymbols(exchange = run.request.data.exchange, forceRefresh = false)
    }

    fun toggleComparisonRun(runId: String) {
        _state.update { current ->
            val next = current.comparisonRunIds.toMutableSet().apply {
                if (!add(runId)) {
                    remove(runId)
                }
                if (size > 2) {
                    val first = firstOrNull()
                    if (first != null) remove(first)
                }
            }
            current.copy(comparisonRunIds = next)
        }
    }

    fun deleteHistoryRun(runId: String) {
        scope.launch {
            repository.deleteHistoryRun(runId)
            val updated = repository.loadHistory()
            _state.update { current ->
                current.copy(
                    history = updated,
                    selectedHistoryRunId = if (current.selectedHistoryRunId == runId) null else current.selectedHistoryRunId,
                    comparisonRunIds = current.comparisonRunIds - runId,
                    result = if (current.selectedHistoryRunId == runId) null else current.result,
                    selectedTradeId = if (current.selectedHistoryRunId == runId) null else current.selectedTradeId
                )
            }
        }
    }

    fun clearHistory() {
        scope.launch {
            repository.clearHistory()
            _state.update {
                it.copy(
                    history = emptyList(),
                    selectedHistoryRunId = null,
                    comparisonRunIds = emptySet(),
                    result = null,
                    selectedTradeId = null,
                    dataNotes = emptyList()
                )
            }
        }
    }

    fun clearErrorMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun exportTradesCsv(): String {
        val result = _state.value.result ?: return ""
        val header = "Symbol,Entry Time,Exit Time,Side,Entry Price,Exit Price,Quantity,PnL,PnL %,MAE %,MFE %,Fees,Funding,Duration (min),Exit Reason"
        val rows = result.trades.joinToString(separator = "\n") { trade ->
            listOf(
                trade.symbol,
                trade.entryTime.toString(),
                trade.exitTime.toString(),
                trade.side.name,
                trade.entryPrice.toString(),
                trade.exitPrice.toString(),
                trade.quantity.toString(),
                trade.pnl.toString(),
                trade.pnlPercent.toString(),
                trade.maePercent.toString(),
                trade.mfePercent.toString(),
                trade.feesPaid.toString(),
                trade.fundingPaid.toString(),
                trade.durationMinutes.toString(),
                trade.exitReason.name
            ).joinToString(",")
        }
        return if (rows.isBlank()) header else "$header\n$rows"
    }

    fun exportSummaryJson(): String {
        val result = _state.value.result ?: return ""
        val payload = SummaryExportPayload(
            runId = result.runId,
            symbol = result.request.data.symbol,
            timeframe = result.request.data.timeframe.label,
            strategy = result.request.strategy.template.label,
            from = result.request.data.from.toString(),
            to = result.request.data.to.toString(),
            metrics = MetricsExportPayload(
                winRatePercent = result.metrics.winRatePercent,
                totalReturnPercent = result.metrics.totalReturnPercent,
                cagrPercent = result.metrics.cagrPercent,
                maxDrawdownPercent = result.metrics.maxDrawdownPercent,
                sharpeRatio = result.metrics.sharpeRatio,
                sortinoRatio = result.metrics.sortinoRatio,
                profitFactor = result.metrics.profitFactor,
                expectancyPercent = result.metrics.expectancyPercent,
                annualVolatilityPercent = result.metrics.annualVolatilityPercent,
                tradeCount = result.metrics.tradeCount,
                averageTradePercent = result.metrics.averageTradePercent,
                exposureTimePercent = result.metrics.exposureTimePercent,
                startEquity = result.metrics.startEquity,
                endEquity = result.metrics.endEquity
            ),
            notes = result.dataNotes
        )
        return json.encodeToString(payload)
    }

    fun exportOptimizationCsv(): String {
        val optimization = _state.value.optimizationResult ?: return ""
        val header = "Rank,Score,Total Return %,Sharpe,Sortino,Profit Factor,Max Drawdown %,Win Rate %,Trades,Parameters"
        val rows = optimization.bestRuns.mapIndexed { index, run ->
            listOf(
                (index + 1).toString(),
                run.score.toString(),
                run.metrics.totalReturnPercent.toString(),
                run.metrics.sharpeRatio.toString(),
                run.metrics.sortinoRatio.toString(),
                run.metrics.profitFactor.toString(),
                run.metrics.maxDrawdownPercent.toString(),
                run.metrics.winRatePercent.toString(),
                run.metrics.tradeCount.toString(),
                run.parameterLabel.replace(',', ';')
            ).joinToString(",")
        }
        return if (rows.isEmpty()) header else "$header\n${rows.joinToString("\n")}"
    }

    override fun close() {
        runJob?.cancel()
        scope.cancel()
    }

    private fun parseRunRequest(
        state: BacktestingUiState,
        forceRefreshData: Boolean
    ): BacktestRunRequest? {
        val symbol = state.symbolInput.trim().uppercase()
        if (symbol.length < 5) {
            _state.update { it.copy(errorMessage = "Symbol ist ungültig") }
            return null
        }
        val initialBalance = state.initialBalanceInput.parsePositiveDouble()
        val fixedAmount = state.fixedAmountInput.parsePositiveDouble()
        val percentEquity = state.percentEquityInput.parsePositiveDouble(max = 100.0)
        val atrRiskPercent = state.atrRiskPercentInput.parsePositiveDouble(max = 100.0)
        val targetVolatilityPercent = state.targetVolatilityPercentInput.parsePositiveDouble(max = 500.0)
        val makerFeeBps = state.makerFeeBpsInput.parseNonNegativeDouble(max = 1_000.0)
        val takerFeeBps = state.takerFeeBpsInput.parseNonNegativeDouble(max = 1_000.0)
        val slippageBps = state.slippageBpsInput.parseNonNegativeDouble(max = 1_000.0)
        val limitOffsetBps = state.limitOffsetBpsInput.parseNonNegativeDouble(max = 1_000.0)
        val fillRatioPercent = state.fillRatioPercentInput.parsePositiveDouble(max = 100.0)
        val maxVolumeParticipation = state.maxVolumeParticipationInput.parsePositiveDouble(max = 100.0)
        val leverage = state.leverageInput.parsePositiveDouble(max = 200.0)
        val fundingBpsPerDay = state.fundingBpsPerDayInput.parseNonNegativeDouble(max = 10_000.0)
        val warmupBars = state.warmupBarsInput.parseNonNegativeInt(max = 20_000)
        val astroSignalWindowBars = state.astroSignalWindowBarsInput.parseNonNegativeInt(max = 96)
        if (
            initialBalance == null ||
            fixedAmount == null ||
            percentEquity == null ||
            atrRiskPercent == null ||
            targetVolatilityPercent == null ||
            makerFeeBps == null ||
            takerFeeBps == null ||
            slippageBps == null ||
            limitOffsetBps == null ||
            fillRatioPercent == null ||
            maxVolumeParticipation == null ||
            leverage == null ||
            fundingBpsPerDay == null ||
            warmupBars == null ||
            astroSignalWindowBars == null
        ) {
            _state.update { it.copy(errorMessage = "Bitte gültige numerische Eingaben prüfen") }
            return null
        }

        val strategy = BacktestStrategyConfig(
            template = state.selectedStrategy,
            smaFastPeriod = state.smaFastInput.parsePositiveInt(max = 500) ?: 20,
            smaSlowPeriod = state.smaSlowInput.parsePositiveInt(max = 500) ?: 50,
            rsiPeriod = state.rsiPeriodInput.parsePositiveInt(max = 200) ?: 14,
            rsiOversold = state.rsiOversoldInput.parsePositiveDouble(max = 100.0) ?: 30.0,
            rsiOverbought = state.rsiOverboughtInput.parsePositiveDouble(max = 100.0) ?: 70.0,
            breakoutLookback = state.breakoutLookbackInput.parsePositiveInt(max = 500) ?: 20,
            atrPeriod = state.atrPeriodInput.parsePositiveInt(max = 500) ?: 14,
            astroCityKey = state.astroCityKey,
            astroSignalWindowBars = astroSignalWindowBars,
            astroIncludeSunEvents = state.astroIncludeSunEvents,
            astroIncludeMoonEvents = state.astroIncludeMoonEvents,
            astroIncludeMoonPhases = state.astroIncludeMoonPhases,
            astroIncludeAspects = state.astroIncludeAspects,
            astroAspectsMoonOnly = state.astroAspectsMoonOnly,
            customStrategyJson = state.customStrategyJson.trim(),
            showSignalPreview = state.showSignalPreview
        )

        val from = state.startDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        val to = state.endDate
            .plusDays(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .minus(Duration.ofMinutes(1))
        if (to <= from) {
            _state.update { it.copy(errorMessage = "Der Zeitraum ist ungültig") }
            return null
        }

        return BacktestRunRequest(
            data = BacktestDataRequest(
                exchange = state.exchange,
                symbol = symbol,
                timeframe = state.timeframe,
                from = from,
                to = to,
                warmupBars = warmupBars,
                forceRefresh = forceRefreshData
            ),
            strategy = strategy,
            execution = BacktestExecutionSettings(
                initialBalance = initialBalance,
                sizingMode = state.sizingMode,
                fixedPositionAmount = fixedAmount,
                percentOfEquity = percentEquity,
                atrRiskPercent = atrRiskPercent,
                targetVolatilityPercent = targetVolatilityPercent,
                makerFeeBps = makerFeeBps,
                takerFeeBps = takerFeeBps,
                slippageBps = slippageBps,
                orderType = state.orderType,
                limitOffsetBps = limitOffsetBps,
                fillRatioPercent = fillRatioPercent,
                maxVolumeParticipationPercent = maxVolumeParticipation,
                allowShort = state.allowShort,
                allowHedging = state.allowHedging,
                leverage = leverage,
                marginMode = state.marginMode,
                fundingBpsPerDay = fundingBpsPerDay,
                stopLossPercent = state.stopLossInput.parseNullablePositiveDouble(max = 100.0),
                takeProfitPercent = state.takeProfitInput.parseNullablePositiveDouble(max = 500.0),
                trailingStopPercent = state.trailingStopInput.parseNullablePositiveDouble(max = 100.0),
                killSwitchMaxDrawdownPercent = state.killSwitchDrawdownInput.parseNullablePositiveDouble(max = 100.0),
                dailyLossLimitPercent = state.dailyLossLimitInput.parseNullablePositiveDouble(max = 100.0)
            )
        )
    }

    private fun applyRunRequest(
        current: BacktestingUiState,
        request: BacktestRunRequest
    ): BacktestingUiState {
        return current.copy(
            selectedStrategy = request.strategy.template,
            showSignalPreview = request.strategy.showSignalPreview,
            smaFastInput = request.strategy.smaFastPeriod.toString(),
            smaSlowInput = request.strategy.smaSlowPeriod.toString(),
            rsiPeriodInput = request.strategy.rsiPeriod.toString(),
            rsiOversoldInput = request.strategy.rsiOversold.trimToString(),
            rsiOverboughtInput = request.strategy.rsiOverbought.trimToString(),
            breakoutLookbackInput = request.strategy.breakoutLookback.toString(),
            atrPeriodInput = request.strategy.atrPeriod.toString(),
            astroCityKey = request.strategy.astroCityKey,
            astroSignalWindowBarsInput = request.strategy.astroSignalWindowBars.toString(),
            astroIncludeSunEvents = request.strategy.astroIncludeSunEvents,
            astroIncludeMoonEvents = request.strategy.astroIncludeMoonEvents,
            astroIncludeMoonPhases = request.strategy.astroIncludeMoonPhases,
            astroIncludeAspects = request.strategy.astroIncludeAspects,
            astroAspectsMoonOnly = request.strategy.astroAspectsMoonOnly,
            customStrategyJson = request.strategy.customStrategyJson,
            exchange = request.data.exchange,
            symbolInput = request.data.symbol,
            timeframe = request.data.timeframe,
            startDate = request.data.from.atZone(ZoneOffset.UTC).toLocalDate(),
            endDate = request.data.to.atZone(ZoneOffset.UTC).toLocalDate(),
            warmupBarsInput = request.data.warmupBars.toString(),
            initialBalanceInput = request.execution.initialBalance.trimToString(),
            sizingMode = request.execution.sizingMode,
            fixedAmountInput = request.execution.fixedPositionAmount.trimToString(),
            percentEquityInput = request.execution.percentOfEquity.trimToString(),
            atrRiskPercentInput = request.execution.atrRiskPercent.trimToString(),
            targetVolatilityPercentInput = request.execution.targetVolatilityPercent.trimToString(),
            feeBpsInput = request.execution.takerFeeBps.trimToString(),
            makerFeeBpsInput = request.execution.makerFeeBps.trimToString(),
            takerFeeBpsInput = request.execution.takerFeeBps.trimToString(),
            slippageBpsInput = request.execution.slippageBps.trimToString(),
            orderType = request.execution.orderType,
            limitOffsetBpsInput = request.execution.limitOffsetBps.trimToString(),
            fillRatioPercentInput = request.execution.fillRatioPercent.trimToString(),
            maxVolumeParticipationInput = request.execution.maxVolumeParticipationPercent.trimToString(),
            allowShort = request.execution.allowShort,
            allowHedging = request.execution.allowHedging,
            leverageInput = request.execution.leverage.trimToString(),
            marginMode = request.execution.marginMode,
            fundingBpsPerDayInput = request.execution.fundingBpsPerDay.trimToString(),
            stopLossInput = request.execution.stopLossPercent?.trimToString().orEmpty(),
            takeProfitInput = request.execution.takeProfitPercent?.trimToString().orEmpty(),
            trailingStopInput = request.execution.trailingStopPercent?.trimToString().orEmpty(),
            killSwitchDrawdownInput = request.execution.killSwitchMaxDrawdownPercent?.trimToString().orEmpty(),
            dailyLossLimitInput = request.execution.dailyLossLimitPercent?.trimToString().orEmpty(),
            errorMessage = null
        )
    }

    private fun buildOptimizationCandidates(
        base: BacktestRunRequest,
        state: BacktestingUiState,
        mode: BacktestOptimizationMode,
        iterations: Int,
        smaFastMin: Int,
        smaFastMax: Int,
        smaFastStep: Int,
        smaSlowMin: Int,
        smaSlowMax: Int,
        smaSlowStep: Int,
        rsiMin: Int,
        rsiMax: Int,
        rsiStep: Int,
        breakoutMin: Int,
        breakoutMax: Int,
        breakoutStep: Int
    ): List<BacktestRunRequest> {
        val candidates = when (base.strategy.template) {
            BacktestStrategyTemplate.SmaCrossover,
            BacktestStrategyTemplate.SmaRsiConfluence -> buildSmaCandidates(
                base = base,
                fastRange = intRange(smaFastMin, smaFastMax, smaFastStep),
                slowRange = intRange(smaSlowMin, smaSlowMax, smaSlowStep)
            )
            BacktestStrategyTemplate.RsiMeanReversion -> buildRsiCandidates(
                base = base,
                rsiPeriodRange = intRange(rsiMin, rsiMax, rsiStep)
            )
            BacktestStrategyTemplate.Breakout,
            BacktestStrategyTemplate.AtrVolatilityBreakout -> buildBreakoutCandidates(
                base = base,
                breakoutRange = intRange(breakoutMin, breakoutMax, breakoutStep),
                atrRange = intRange(rsiMin, rsiMax, rsiStep)
            )
            BacktestStrategyTemplate.SunMoonAstroFlow -> buildAstroCandidates(base)
            BacktestStrategyTemplate.Custom -> buildRiskCandidates(base)
        }
        val withRiskVariations = addRiskVariations(candidates, state)
        return when (mode) {
            BacktestOptimizationMode.Grid -> evenlySample(withRiskVariations, iterations)
            BacktestOptimizationMode.Random -> {
                val random = Random(42)
                withRiskVariations.shuffled(random).take(iterations.coerceAtMost(withRiskVariations.size))
            }
        }
    }

    private fun buildSmaCandidates(
        base: BacktestRunRequest,
        fastRange: IntProgression,
        slowRange: IntProgression
    ): List<BacktestRunRequest> {
        val list = mutableListOf<BacktestRunRequest>()
        for (fast in fastRange) {
            for (slow in slowRange) {
                if (fast >= slow) continue
                list += base.copy(
                    strategy = base.strategy.copy(
                        smaFastPeriod = fast,
                        smaSlowPeriod = slow
                    )
                )
            }
        }
        return list
    }

    private fun buildRsiCandidates(
        base: BacktestRunRequest,
        rsiPeriodRange: IntProgression
    ): List<BacktestRunRequest> {
        val list = mutableListOf<BacktestRunRequest>()
        for (period in rsiPeriodRange) {
            for (oversold in listOf(18.0, 22.0, 26.0, 30.0, 34.0)) {
                for (overbought in listOf(66.0, 70.0, 74.0, 78.0, 82.0)) {
                    if (oversold >= overbought) continue
                    list += base.copy(
                        strategy = base.strategy.copy(
                            rsiPeriod = period,
                            rsiOversold = oversold,
                            rsiOverbought = overbought
                        )
                    )
                }
            }
        }
        return list
    }

    private fun buildBreakoutCandidates(
        base: BacktestRunRequest,
        breakoutRange: IntProgression,
        atrRange: IntProgression
    ): List<BacktestRunRequest> {
        val list = mutableListOf<BacktestRunRequest>()
        for (lookback in breakoutRange) {
            for (atrPeriod in atrRange) {
                list += base.copy(
                    strategy = base.strategy.copy(
                        breakoutLookback = lookback,
                        atrPeriod = atrPeriod
                    )
                )
            }
        }
        return list
    }

    private fun buildAstroCandidates(base: BacktestRunRequest): List<BacktestRunRequest> {
        val list = mutableListOf<BacktestRunRequest>()
        val windows = listOf(0, 1, 2, 3, 4, 6)
        val includeSunOptions = listOf(true, false)
        val includeMoonOptions = listOf(true, false)
        val includePhaseOptions = listOf(true, false)
        val includeAspectsOptions = listOf(true, false)
        val moonOnlyOptions = listOf(true, false)

        for (window in windows) {
            for (includeSun in includeSunOptions) {
                for (includeMoon in includeMoonOptions) {
                    for (includePhases in includePhaseOptions) {
                        for (includeAspects in includeAspectsOptions) {
                            if (!includeSun && !includeMoon && !includePhases && !includeAspects) continue
                            if (includeAspects) {
                                for (moonOnly in moonOnlyOptions) {
                                    list += base.copy(
                                        strategy = base.strategy.copy(
                                            astroSignalWindowBars = window,
                                            astroIncludeSunEvents = includeSun,
                                            astroIncludeMoonEvents = includeMoon,
                                            astroIncludeMoonPhases = includePhases,
                                            astroIncludeAspects = true,
                                            astroAspectsMoonOnly = moonOnly
                                        )
                                    )
                                }
                            } else {
                                list += base.copy(
                                    strategy = base.strategy.copy(
                                        astroSignalWindowBars = window,
                                        astroIncludeSunEvents = includeSun,
                                        astroIncludeMoonEvents = includeMoon,
                                        astroIncludeMoonPhases = includePhases,
                                        astroIncludeAspects = false,
                                        astroAspectsMoonOnly = true
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        return list.distinctBy {
            "${it.strategy.astroSignalWindowBars}_${it.strategy.astroIncludeSunEvents}_${it.strategy.astroIncludeMoonEvents}_${it.strategy.astroIncludeMoonPhases}_${it.strategy.astroIncludeAspects}_${it.strategy.astroAspectsMoonOnly}"
        }
    }

    private fun buildRiskCandidates(base: BacktestRunRequest): List<BacktestRunRequest> {
        val list = mutableListOf<BacktestRunRequest>()
        val stops = listOf(0.8, 1.2, 1.8, 2.5)
        val takes = listOf(1.5, 2.5, 3.5, 5.0)
        val trails = listOf<Double?>(null, 0.8, 1.4, 2.0)
        for (stop in stops) {
            for (take in takes) {
                for (trail in trails) {
                    list += base.copy(
                        execution = base.execution.copy(
                            stopLossPercent = stop,
                            takeProfitPercent = take,
                            trailingStopPercent = trail
                        )
                    )
                }
            }
        }
        return list
    }

    private fun addRiskVariations(
        baseCandidates: List<BacktestRunRequest>,
        state: BacktestingUiState
    ): List<BacktestRunRequest> {
        val stop = state.stopLossInput.parseNullablePositiveDouble(max = 100.0)
        val take = state.takeProfitInput.parseNullablePositiveDouble(max = 500.0)
        val trail = state.trailingStopInput.parseNullablePositiveDouble(max = 100.0)
        if (stop == null && take == null && trail == null) {
            return baseCandidates
        }
        return baseCandidates.flatMap { candidate ->
            listOf(
                candidate,
                candidate.copy(execution = candidate.execution.copy(stopLossPercent = stop)),
                candidate.copy(execution = candidate.execution.copy(takeProfitPercent = take)),
                candidate.copy(execution = candidate.execution.copy(stopLossPercent = stop, takeProfitPercent = take)),
                candidate.copy(execution = candidate.execution.copy(stopLossPercent = stop, takeProfitPercent = take, trailingStopPercent = trail))
            ).distinctBy {
                "${it.execution.stopLossPercent}_${it.execution.takeProfitPercent}_${it.execution.trailingStopPercent}_${it.strategy.smaFastPeriod}_${it.strategy.smaSlowPeriod}_${it.strategy.rsiPeriod}_${it.strategy.breakoutLookback}_${it.strategy.atrPeriod}_${it.strategy.astroSignalWindowBars}_${it.strategy.astroIncludeSunEvents}_${it.strategy.astroIncludeMoonEvents}_${it.strategy.astroIncludeMoonPhases}_${it.strategy.astroIncludeAspects}_${it.strategy.astroAspectsMoonOnly}"
            }
        }
    }

    private fun evenlySample(
        items: List<BacktestRunRequest>,
        targetCount: Int
    ): List<BacktestRunRequest> {
        if (items.isEmpty()) return emptyList()
        if (targetCount >= items.size) return items
        if (targetCount <= 1) return listOf(items.first())
        val step = (items.size - 1).toDouble() / (targetCount - 1).toDouble()
        val indexes = (0 until targetCount).map { idx -> (idx * step).toInt().coerceIn(0, items.lastIndex) }
        return indexes.distinct().map { items[it] }
    }

    private fun intRange(minValue: Int, maxValue: Int, stepValue: Int): IntProgression {
        val safeMin = minValue.coerceAtLeast(1)
        val safeMax = maxValue.coerceAtLeast(safeMin)
        val safeStep = stepValue.coerceAtLeast(1)
        return safeMin..safeMax step safeStep
    }

    private fun scoreResult(
        result: de.tradebuddy.domain.model.BacktestResult,
        objective: BacktestOptimizationObjective
    ): Double {
        val metrics = result.metrics
        return when (objective) {
            BacktestOptimizationObjective.TotalReturn -> metrics.totalReturnPercent - (metrics.maxDrawdownPercent * 0.35)
            BacktestOptimizationObjective.Sharpe -> (metrics.sharpeRatio * 10.0) + (metrics.sortinoRatio * 4.0) - (metrics.maxDrawdownPercent * 0.25)
            BacktestOptimizationObjective.ProfitFactor -> (metrics.profitFactor * 12.0) + (metrics.winRatePercent * 0.15) - (metrics.maxDrawdownPercent * 0.35)
            BacktestOptimizationObjective.Calmar -> {
                val dd = metrics.maxDrawdownPercent.coerceAtLeast(0.5)
                (metrics.totalReturnPercent / dd) * 10.0 + metrics.sharpeRatio
            }
            BacktestOptimizationObjective.Balanced ->
                metrics.totalReturnPercent +
                    (metrics.sharpeRatio * 7.5) +
                    (metrics.sortinoRatio * 5.0) -
                    (metrics.maxDrawdownPercent * 0.8) +
                    (metrics.winRatePercent * 0.1)
        }
    }
}

private fun BacktestRunRequest.toParameterLabel(): String = buildString {
    when (strategy.template) {
        BacktestStrategyTemplate.SunMoonAstroFlow -> {
            append("ASTRO W")
            append(strategy.astroSignalWindowBars)
            append(" | Sun ")
            append(if (strategy.astroIncludeSunEvents) "on" else "off")
            append(" | Moon ")
            append(if (strategy.astroIncludeMoonEvents) "on" else "off")
            append(" | Phases ")
            append(if (strategy.astroIncludeMoonPhases) "on" else "off")
            append(" | Aspects ")
            append(if (strategy.astroIncludeAspects) "on" else "off")
            if (strategy.astroIncludeAspects) {
                append(" (")
                append(if (strategy.astroAspectsMoonOnly) "MoonOnly" else "All")
                append(")")
            }
        }
        else -> {
            append("SMA ")
            append(strategy.smaFastPeriod)
            append("/")
            append(strategy.smaSlowPeriod)
            append(" | RSI ")
            append(strategy.rsiPeriod)
            append(" (")
            append(strategy.rsiOversold.trimToString())
            append("/")
            append(strategy.rsiOverbought.trimToString())
            append(") | BO ")
            append(strategy.breakoutLookback)
            append(" | ATR ")
            append(strategy.atrPeriod)
        }
    }
    append(" | SL/TP ")
    append(execution.stopLossPercent?.trimToString() ?: "-")
    append("/")
    append(execution.takeProfitPercent?.trimToString() ?: "-")
    append(" | TR ")
    append(execution.trailingStopPercent?.trimToString() ?: "-")
}

private data class WalkForwardFold(
    val train: List<OhlcvCandle>,
    val test: List<OhlcvCandle>
)

private fun buildWalkForwardFolds(
    candles: List<OhlcvCandle>,
    splits: Int
): List<WalkForwardFold> {
    if (candles.size < 80 || splits < 2) return emptyList()
    val safeSplits = splits.coerceIn(2, 12)
    val segmentSize = (candles.size / (safeSplits + 1)).coerceAtLeast(24)
    if (segmentSize * 2 > candles.size) return emptyList()

    val folds = mutableListOf<WalkForwardFold>()
    for (index in 0 until safeSplits) {
        val trainEndExclusive = (segmentSize * (index + 1)).coerceAtMost(candles.size - 1)
        val testStart = trainEndExclusive
        val testEndExclusive = if (index == safeSplits - 1) {
            candles.size
        } else {
            (testStart + segmentSize).coerceAtMost(candles.size)
        }
        if (testEndExclusive - testStart < 8) continue
        if (trainEndExclusive < 24) continue
        val train = candles.subList(0, trainEndExclusive)
        val test = candles.subList(testStart, testEndExclusive)
        if (train.isNotEmpty() && test.isNotEmpty()) {
            folds += WalkForwardFold(train = train, test = test)
        }
    }
    return folds
}

private fun BacktestRunRequest.withRange(
    from: Instant,
    to: Instant
): BacktestRunRequest = copy(
    data = data.copy(
        from = from,
        to = to,
        forceRefresh = false
    )
)

enum class BacktestCustomPreset(val json: String) {
    TrendFollowing(
        json = """
            {
              "entryLong": {
                "all": [
                  { "left": { "indicator": "ema", "period": 21 }, "op": "GT", "right": { "indicator": "ema", "period": 55 } },
                  { "left": { "indicator": "close" }, "op": "GT", "right": { "indicator": "ema", "period": 21 } }
                ]
              },
              "exitLong": {
                "any": [
                  { "left": { "indicator": "close" }, "op": "LT", "right": { "indicator": "ema", "period": 21 } },
                  { "left": { "indicator": "rsi", "period": 14 }, "op": "GT", "right": { "value": 78.0 } }
                ]
              },
              "entryShort": {
                "all": [
                  { "left": { "indicator": "ema", "period": 21 }, "op": "LT", "right": { "indicator": "ema", "period": 55 } },
                  { "left": { "indicator": "close" }, "op": "LT", "right": { "indicator": "ema", "period": 21 } }
                ]
              },
              "exitShort": {
                "any": [
                  { "left": { "indicator": "close" }, "op": "GT", "right": { "indicator": "ema", "period": 21 } },
                  { "left": { "indicator": "rsi", "period": 14 }, "op": "LT", "right": { "value": 22.0 } }
                ]
              }
            }
        """.trimIndent()
    ),
    MeanReversion(
        json = """
            {
              "entryLong": {
                "all": [
                  { "left": { "indicator": "rsi", "period": 14 }, "op": "LT", "right": { "value": 30.0 } },
                  { "left": { "indicator": "close" }, "op": "LT", "right": { "indicator": "sma", "period": 20 } }
                ]
              },
              "exitLong": {
                "any": [
                  { "left": { "indicator": "close" }, "op": "GT", "right": { "indicator": "sma", "period": 20 } },
                  { "left": { "indicator": "rsi", "period": 14 }, "op": "GT", "right": { "value": 62.0 } }
                ]
              },
              "entryShort": {
                "all": [
                  { "left": { "indicator": "rsi", "period": 14 }, "op": "GT", "right": { "value": 70.0 } },
                  { "left": { "indicator": "close" }, "op": "GT", "right": { "indicator": "sma", "period": 20 } }
                ]
              },
              "exitShort": {
                "any": [
                  { "left": { "indicator": "close" }, "op": "LT", "right": { "indicator": "sma", "period": 20 } },
                  { "left": { "indicator": "rsi", "period": 14 }, "op": "LT", "right": { "value": 38.0 } }
                ]
              }
            }
        """.trimIndent()
    ),
    Breakout(
        json = """
            {
              "entryLong": {
                "all": [
                  { "left": { "indicator": "close" }, "op": "GT", "right": { "indicator": "sma", "period": 55 } },
                  { "left": { "indicator": "atr", "period": 14 }, "op": "GT", "right": { "value": 0.0 } }
                ]
              },
              "exitLong": {
                "any": [
                  { "left": { "indicator": "close" }, "op": "LT", "right": { "indicator": "ema", "period": 21 } }
                ]
              },
              "entryShort": {
                "all": [
                  { "left": { "indicator": "close" }, "op": "LT", "right": { "indicator": "sma", "period": 55 } },
                  { "left": { "indicator": "atr", "period": 14 }, "op": "GT", "right": { "value": 0.0 } }
                ]
              },
              "exitShort": {
                "any": [
                  { "left": { "indicator": "close" }, "op": "GT", "right": { "indicator": "ema", "period": 21 } }
                ]
              }
            }
        """.trimIndent()
    )
}

private fun String.parsePositiveDouble(max: Double = 1_000_000_000.0): Double? =
    trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it > 0.0 && it <= max }

private fun String.parseNullablePositiveDouble(max: Double = 1_000_000_000.0): Double? {
    val normalized = trim()
    if (normalized.isEmpty()) return null
    return normalized
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it > 0.0 && it <= max }
}

private fun String.parsePositiveInt(max: Int = 100_000): Int? =
    trim()
        .toIntOrNull()
        ?.takeIf { it > 0 && it <= max }

private fun String.parseNonNegativeInt(max: Int = 100_000): Int? =
    trim()
        .toIntOrNull()
        ?.takeIf { it >= 0 && it <= max }

private fun String.parseNonNegativeDouble(max: Double = 1_000_000_000.0): Double? =
    trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it >= 0.0 && it <= max }

private fun Double.trimToString(): String {
    val rounded = kotlin.math.round(this * 10_000.0) / 10_000.0
    val text = rounded.toString()
    return if (text.endsWith(".0")) text.dropLast(2) else text
}

@Serializable
private data class SummaryExportPayload(
    val runId: String,
    val symbol: String,
    val timeframe: String,
    val strategy: String,
    val from: String,
    val to: String,
    val metrics: MetricsExportPayload,
    val notes: List<String>
)

@Serializable
private data class MetricsExportPayload(
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

