package de.tradebuddy.engine

import de.tradebuddy.data.AstroCalculator
import de.tradebuddy.data.DefaultCityDataSource
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.AstroCalendarScope
import de.tradebuddy.domain.model.BacktestAnalysis
import de.tradebuddy.domain.model.BacktestExitReason
import de.tradebuddy.domain.model.BacktestMarginMode
import de.tradebuddy.domain.model.BacktestMarketData
import de.tradebuddy.domain.model.BacktestOrderType
import de.tradebuddy.domain.model.BacktestPositionSizingMode
import de.tradebuddy.domain.model.BacktestPricePoint
import de.tradebuddy.domain.model.BacktestResult
import de.tradebuddy.domain.model.BacktestRunRequest
import de.tradebuddy.domain.model.BacktestSignal
import de.tradebuddy.domain.model.BacktestStrategyTemplate
import de.tradebuddy.domain.model.BacktestTrade
import de.tradebuddy.domain.model.BacktestTradeSide
import de.tradebuddy.domain.model.BacktestEquityPoint
import de.tradebuddy.domain.model.DEFAULT_ASTRO_ASPECT_ORBS
import de.tradebuddy.domain.model.MoonPhaseType
import de.tradebuddy.domain.model.OhlcvCandle
import de.tradebuddy.domain.model.StrategySignalPoint
import de.tradebuddy.domain.util.key
import de.tradebuddy.metrics.BacktestingMetrics
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BacktestingEngine {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun run(
        request: BacktestRunRequest,
        marketData: BacktestMarketData,
        onProgress: (Float) -> Unit = {}
    ): BacktestResult {
        val candles = marketData.candles.sortedBy { it.openTime }
        if (candles.size < 3) {
            return emptyResult(request, marketData.notes)
        }

        val closeSeries = candles.map { it.close }
        val fastSma = sma(closeSeries, request.strategy.smaFastPeriod)
        val slowSma = sma(closeSeries, request.strategy.smaSlowPeriod)
        val fastEma = ema(closeSeries, request.strategy.smaFastPeriod)
        val slowEma = ema(closeSeries, request.strategy.smaSlowPeriod)
        val rsi = rsi(closeSeries, request.strategy.rsiPeriod)
        val atr = atr(candles, request.strategy.atrPeriod)
        val breakoutHigh = rollingHigh(candles.map { it.high }, request.strategy.breakoutLookback)
        val breakoutLow = rollingLow(candles.map { it.low }, request.strategy.breakoutLookback)
        val indicatorContext = IndicatorContext(
            candles = candles,
            close = closeSeries,
            fastSma = fastSma,
            slowSma = slowSma,
            fastEma = fastEma,
            slowEma = slowEma,
            rsi = rsi,
            atr = atr
        )
        val customRules = parseCustomRules(request.strategy.customStrategyJson)
        val warmupStartIndex = request.data.warmupBars.coerceIn(1, candles.lastIndex)
        val engineNotes = mutableListOf<String>()
        val astroSignalsByIndex = if (request.strategy.template == BacktestStrategyTemplate.SunMoonAstroFlow) {
            buildSunMoonAstroSignals(
                candles = candles,
                request = request,
                notes = engineNotes
            )
        } else {
            emptyMap()
        }

        var cash = request.execution.initialBalance
        var position: PositionState? = null
        var runningPeak = request.execution.initialBalance
        var dayKey = candles.first().openTime.atZone(ZoneOffset.UTC).toLocalDate().toString()
        var dayStartEquity = request.execution.initialBalance
        var dayBlocked = false

        val trades = mutableListOf<BacktestTrade>()
        val equityCurve = mutableListOf<BacktestEquityPoint>()
        val signals = mutableListOf<StrategySignalPoint>()

        for (index in warmupStartIndex until candles.lastIndex) {
            if (index % 64 == 0) {
                val done = (index - warmupStartIndex).coerceAtLeast(0)
                val total = (candles.lastIndex - warmupStartIndex).coerceAtLeast(1)
                onProgress(done.toFloat() / total.toFloat())
                yield()
            }

            val candle = candles[index]
            val next = candles[index + 1]
            val candleDay = candle.openTime.atZone(ZoneOffset.UTC).toLocalDate().toString()
            if (candleDay != dayKey) {
                dayKey = candleDay
                dayStartEquity = markToMarket(cash, candle.close, position)
                dayBlocked = false
            }

            position = position?.let { open ->
                val fundingBpsPerBar = request.execution.fundingBpsPerDay / 10_000.0 / 1_440.0 * request.data.timeframe.approxMinutes
                val funding = when (open.side) {
                    BacktestTradeSide.Long -> open.notional * fundingBpsPerBar
                    BacktestTradeSide.Short -> -open.notional * fundingBpsPerBar
                }
                cash -= funding
                open.copy(
                    maxSinceEntry = max(open.maxSinceEntry, candle.high),
                    minSinceEntry = min(open.minSinceEntry, candle.low),
                    fundingPaid = open.fundingPaid + funding
                )
            }

            val riskExit = position?.let {
                resolveRiskExit(it, candle, request.execution.stopLossPercent, request.execution.takeProfitPercent, request.execution.trailingStopPercent)
            }
            if (riskExit != null) {
                val (exitPrice, reason) = riskExit
                val openPositionState = position
                val closed = closePosition(request, openPositionState, exitPrice, candle.closeTime, reason, isMaker = false)
                cash = applyCloseCash(cash, closed)
                trades += closed.toTrade(request.data.symbol)
                signals += StrategySignalPoint(
                    symbol = request.data.symbol,
                    time = candle.closeTime,
                    price = exitPrice,
                    signal = if (openPositionState.side == BacktestTradeSide.Long) BacktestSignal.Sell else BacktestSignal.Cover
                )
                position = null
            }

            val signal = generateSignal(
                index = index,
                request = request,
                position = position,
                breakoutHigh = breakoutHigh,
                breakoutLow = breakoutLow,
                indicatorContext = indicatorContext,
                customRules = customRules,
                astroSignalsByIndex = astroSignalsByIndex
            )
            if (signal != null) {
                signals += StrategySignalPoint(request.data.symbol, candle.closeTime, candle.close, signal)
            }

            val equity = markToMarket(cash, candle.close, position)
            runningPeak = max(runningPeak, equity)
            val drawdown = if (runningPeak == 0.0) 0.0 else (1.0 - (equity / runningPeak)) * 100.0
            if (request.execution.killSwitchMaxDrawdownPercent != null && drawdown >= request.execution.killSwitchMaxDrawdownPercent) {
                val openPositionState = position
                if (openPositionState != null) {
                    val closed = closePosition(request, openPositionState, candle.close, candle.closeTime, BacktestExitReason.KillSwitch, isMaker = false)
                    cash = applyCloseCash(cash, closed)
                    trades += closed.toTrade(request.data.symbol)
                    signals += StrategySignalPoint(
                        symbol = request.data.symbol,
                        time = candle.closeTime,
                        price = candle.close,
                        signal = if (openPositionState.side == BacktestTradeSide.Long) BacktestSignal.Sell else BacktestSignal.Cover
                    )
                    position = null
                }
                equityCurve += BacktestEquityPoint(candle.closeTime, cash)
                break
            }

            val intradayReturn = if (dayStartEquity == 0.0) 0.0 else ((equity / dayStartEquity) - 1.0) * 100.0
            if (request.execution.dailyLossLimitPercent != null && intradayReturn <= -request.execution.dailyLossLimitPercent) {
                dayBlocked = true
                val openPositionState = position
                if (openPositionState != null) {
                    val closed = closePosition(request, openPositionState, candle.close, candle.closeTime, BacktestExitReason.DailyLossLimit, isMaker = false)
                    cash = applyCloseCash(cash, closed)
                    trades += closed.toTrade(request.data.symbol)
                    signals += StrategySignalPoint(
                        symbol = request.data.symbol,
                        time = candle.closeTime,
                        price = candle.close,
                        signal = if (openPositionState.side == BacktestTradeSide.Long) BacktestSignal.Sell else BacktestSignal.Cover
                    )
                    position = null
                }
            }

            when {
                !dayBlocked && position == null && signal == BacktestSignal.Buy -> {
                    val opened = openPosition(request, BacktestTradeSide.Long, next, cash)
                    if (opened != null) {
                        cash = opened.cashAfterEntry
                        position = opened.position
                        signals += StrategySignalPoint(
                            symbol = request.data.symbol,
                            time = opened.position.entryTime,
                            price = opened.position.entryPrice,
                            signal = BacktestSignal.Buy
                        )
                    }
                }

                !dayBlocked && position == null && signal == BacktestSignal.Short && request.execution.allowShort -> {
                    val opened = openPosition(request, BacktestTradeSide.Short, next, cash)
                    if (opened != null) {
                        cash = opened.cashAfterEntry
                        position = opened.position
                        signals += StrategySignalPoint(
                            symbol = request.data.symbol,
                            time = opened.position.entryTime,
                            price = opened.position.entryPrice,
                            signal = BacktestSignal.Short
                        )
                    }
                }

                position != null && position.side == BacktestTradeSide.Long && (signal == BacktestSignal.Sell || signal == BacktestSignal.Short) -> {
                    val openPositionState = position
                    val fill = resolveExitFill(request, openPositionState.side, next)
                    if (fill != null) {
                        val closed = closePosition(request, openPositionState, fill.price, next.openTime, BacktestExitReason.Signal, fill.isMaker)
                        cash = applyCloseCash(cash, closed)
                        trades += closed.toTrade(request.data.symbol)
                        signals += StrategySignalPoint(
                            symbol = request.data.symbol,
                            time = next.openTime,
                            price = fill.price,
                            signal = BacktestSignal.Sell
                        )
                        position = null
                    }
                }

                position != null && position.side == BacktestTradeSide.Short && (signal == BacktestSignal.Cover || signal == BacktestSignal.Buy) -> {
                    val openPositionState = position
                    val fill = resolveExitFill(request, openPositionState.side, next)
                    if (fill != null) {
                        val closed = closePosition(request, openPositionState, fill.price, next.openTime, BacktestExitReason.Signal, fill.isMaker)
                        cash = applyCloseCash(cash, closed)
                        trades += closed.toTrade(request.data.symbol)
                        signals += StrategySignalPoint(
                            symbol = request.data.symbol,
                            time = next.openTime,
                            price = fill.price,
                            signal = BacktestSignal.Cover
                        )
                        position = null
                    }
                }
            }

            equityCurve += BacktestEquityPoint(candle.closeTime, markToMarket(cash, candle.close, position))
        }

        position?.let { open ->
            val close = closePosition(request, open, candles.last().close, candles.last().closeTime, BacktestExitReason.EndOfTest, isMaker = false)
            cash = applyCloseCash(cash, close)
            trades += close.toTrade(request.data.symbol)
            signals += StrategySignalPoint(
                symbol = request.data.symbol,
                time = candles.last().closeTime,
                price = candles.last().close,
                signal = if (open.side == BacktestTradeSide.Long) BacktestSignal.Sell else BacktestSignal.Cover
            )
        }
        equityCurve += BacktestEquityPoint(candles.last().closeTime, cash)

        val drawdownCurve = BacktestingMetrics.buildDrawdownCurve(equityCurve)
        val metrics = BacktestingMetrics.calculate(
            startEquity = request.execution.initialBalance,
            endEquity = cash,
            timeframe = request.data.timeframe,
            from = request.data.from,
            to = request.data.to,
            trades = trades,
            equityCurve = equityCurve
        )
        val analysis: BacktestAnalysis = BacktestingMetrics.buildAnalysis(trades, equityCurve)
        onProgress(1f)

        return BacktestResult(
            runId = UUID.randomUUID().toString(),
            request = request,
            candlesCount = candles.size,
            candleSeries = candles,
            trades = trades,
            priceCurve = candles.map { BacktestPricePoint(time = it.closeTime, price = it.close) },
            equityCurve = equityCurve,
            drawdownCurve = drawdownCurve,
            signalPoints = if (request.strategy.showSignalPreview) signals.distinctBy { "${it.time.toEpochMilli()}_${it.signal}" } else signals.filter {
                it.signal == BacktestSignal.Buy || it.signal == BacktestSignal.Sell || it.signal == BacktestSignal.Short || it.signal == BacktestSignal.Cover
            }.distinctBy { "${it.time.toEpochMilli()}_${it.signal}" },
            metrics = metrics,
            analysis = analysis,
            dataNotes = marketData.notes + engineNotes
        )
    }

    private fun emptyResult(request: BacktestRunRequest, notes: List<String>): BacktestResult {
        val metrics = BacktestingMetrics.calculate(
            startEquity = request.execution.initialBalance,
            endEquity = request.execution.initialBalance,
            timeframe = request.data.timeframe,
            from = request.data.from,
            to = request.data.to,
            trades = emptyList(),
            equityCurve = emptyList()
        )
        return BacktestResult(
            runId = UUID.randomUUID().toString(),
            request = request,
            candlesCount = 0,
            candleSeries = emptyList(),
            trades = emptyList(),
            priceCurve = emptyList(),
            equityCurve = emptyList(),
            drawdownCurve = emptyList(),
            signalPoints = emptyList(),
            metrics = metrics,
            analysis = BacktestingMetrics.buildAnalysis(emptyList(), emptyList()),
            dataNotes = notes
        )
    }

    private suspend fun buildSunMoonAstroSignals(
        candles: List<OhlcvCandle>,
        request: BacktestRunRequest,
        notes: MutableList<String>
    ): Map<Int, BacktestSignal> {
        if (candles.isEmpty()) return emptyMap()
        val strategy = request.strategy
        if (!strategy.astroIncludeSunEvents &&
            !strategy.astroIncludeMoonEvents &&
            !strategy.astroIncludeMoonPhases &&
            !strategy.astroIncludeAspects
        ) {
            notes += "Sun/Moon/Astro: keine Signalquellen aktiviert."
            return emptyMap()
        }

        val cities = DefaultCityDataSource.cities()
        val city = cities.firstOrNull { it.key() == strategy.astroCityKey }
            ?: cities.firstOrNull { it.label == "Frankfurt" }
            ?: cities.firstOrNull()
        if (city == null) {
            notes += "Sun/Moon/Astro: keine Stadt verfügbar."
            return emptyMap()
        }
        val cityZone = ZoneId.of(city.zoneId)
        val calculator = AstroCalculator()
        val fromDate = candles.first().openTime.atZone(cityZone).toLocalDate().minusDays(1)
        val toDate = candles.last().closeTime.atZone(cityZone).toLocalDate().plusDays(1)
        val timedSignals = mutableListOf<TimedSignalCandidate>()

        var sunEventCount = 0
        var moonEventCount = 0
        var moonPhaseCount = 0
        var aspectCount = 0

        var cursorDate = fromDate
        var dayIndex = 0
        while (cursorDate <= toDate) {
            if (strategy.astroIncludeSunEvents || strategy.astroIncludeMoonEvents) {
                val dayTimes = calculator.calculate(cursorDate, city)
                if (strategy.astroIncludeSunEvents) {
                    dayTimes.sunrise?.toInstant()?.let {
                        timedSignals += TimedSignalCandidate(instant = it, signal = BacktestSignal.Buy)
                        sunEventCount += 1
                    }
                    dayTimes.sunset?.toInstant()?.let {
                        timedSignals += TimedSignalCandidate(instant = it, signal = bearishSignal(request.execution.allowShort))
                        sunEventCount += 1
                    }
                }
                if (strategy.astroIncludeMoonEvents) {
                    dayTimes.moonrise?.toInstant()?.let {
                        timedSignals += TimedSignalCandidate(instant = it, signal = BacktestSignal.Buy)
                        moonEventCount += 1
                    }
                    dayTimes.moonset?.toInstant()?.let {
                        timedSignals += TimedSignalCandidate(instant = it, signal = bearishSignal(request.execution.allowShort))
                        moonEventCount += 1
                    }
                }
            }

            if (strategy.astroIncludeAspects) {
                val scope = if (strategy.astroAspectsMoonOnly) {
                    AstroCalendarScope.MoonOnly
                } else {
                    AstroCalendarScope.AllPlanets
                }
                val aspects = calculator.moonPlanetAspects(
                    date = cursorDate,
                    zoneId = cityZone,
                    aspectOrbs = DEFAULT_ASTRO_ASPECT_ORBS,
                    scope = scope
                )
                aspects.forEach { aspect ->
                    val signal = when (aspect.aspectType) {
                        AstroAspectType.Conjunction, AstroAspectType.Sextile, AstroAspectType.Trine -> BacktestSignal.Buy
                        AstroAspectType.Square, AstroAspectType.Opposition -> bearishSignal(request.execution.allowShort)
                    }
                    timedSignals += TimedSignalCandidate(instant = aspect.exactInstant, signal = signal)
                    aspectCount += 1
                }
            }

            cursorDate = cursorDate.plusDays(1)
            dayIndex += 1
            if (dayIndex % 6 == 0) {
                yield()
            }
        }

        if (strategy.astroIncludeMoonPhases) {
            val months = mutableListOf<YearMonth>()
            var monthCursorDate = YearMonth.from(fromDate).atDay(1)
            val monthEndDate = YearMonth.from(toDate).atDay(1)
            while (monthCursorDate <= monthEndDate) {
                months += YearMonth.from(monthCursorDate)
                monthCursorDate = YearMonth.from(monthCursorDate).plusMonths(1).atDay(1)
            }
            months.forEachIndexed { index, month ->
                calculator.moonPhases(month, cityZone).forEach { phase ->
                    val signal = when (phase.type) {
                        MoonPhaseType.NewMoon, MoonPhaseType.FirstQuarter -> BacktestSignal.Buy
                        MoonPhaseType.FullMoon, MoonPhaseType.LastQuarter -> bearishSignal(request.execution.allowShort)
                    }
                    timedSignals += TimedSignalCandidate(instant = phase.instant, signal = signal)
                    moonPhaseCount += 1
                }
                if (index % 2 == 1) {
                    yield()
                }
            }
        }

        val windowBars = strategy.astroSignalWindowBars.coerceAtLeast(0)
        val indexedCandidates = mutableListOf<IndexedSignalCandidate>()
        timedSignals.forEach { candidate ->
            val centerIndex = nearestCandleIndexByTime(candles, candidate.instant) ?: return@forEach
            val startIndex = (centerIndex - windowBars).coerceAtLeast(0)
            val endIndex = (centerIndex + windowBars).coerceAtMost(candles.lastIndex)
            for (idx in startIndex..endIndex) {
                indexedCandidates += IndexedSignalCandidate(
                    index = idx,
                    signal = candidate.signal,
                    distance = abs(idx - centerIndex)
                )
            }
        }

        val resolved = indexedCandidates
            .groupBy { it.index }
            .mapValues { (_, candidatesForIndex) ->
                candidatesForIndex.minWithOrNull(
                    compareBy<IndexedSignalCandidate> { it.distance }.thenBy { signalPriority(it.signal) }
                )?.signal ?: BacktestSignal.Buy
            }

        notes += "Sun/Moon/Astro ${city.label}: Sun $sunEventCount, Moon $moonEventCount, Phases $moonPhaseCount, Aspects $aspectCount, Signal-Kerzen ${resolved.size}."
        return resolved
    }

    private fun bearishSignal(allowShort: Boolean): BacktestSignal =
        if (allowShort) BacktestSignal.Short else BacktestSignal.Sell

    private fun signalPriority(signal: BacktestSignal): Int = when (signal) {
        BacktestSignal.Buy -> 0
        BacktestSignal.Short -> 1
        BacktestSignal.Sell -> 2
        BacktestSignal.Cover -> 3
    }

    private fun nearestCandleIndexByTime(candles: List<OhlcvCandle>, instant: Instant): Int? {
        if (candles.isEmpty()) return null
        var low = 0
        var high = candles.lastIndex
        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midTime = candles[mid].closeTime
            when {
                midTime < instant -> low = mid + 1
                midTime > instant -> high = mid - 1
                else -> return mid
            }
        }
        val right = low.coerceIn(0, candles.lastIndex)
        val left = (right - 1).coerceAtLeast(0)
        val rightDistance = abs(candles[right].closeTime.toEpochMilli() - instant.toEpochMilli())
        val leftDistance = abs(candles[left].closeTime.toEpochMilli() - instant.toEpochMilli())
        return if (leftDistance <= rightDistance) left else right
    }

    private fun generateSignal(
        index: Int,
        request: BacktestRunRequest,
        position: PositionState?,
        breakoutHigh: List<Double?>,
        breakoutLow: List<Double?>,
        indicatorContext: IndicatorContext,
        customRules: CustomStrategyRules?,
        astroSignalsByIndex: Map<Int, BacktestSignal>
    ): BacktestSignal? {
        if (index <= 0) return null
        return when (request.strategy.template) {
            BacktestStrategyTemplate.SmaCrossover -> {
                val fc = indicatorContext.fastSma[index] ?: return null
                val sc = indicatorContext.slowSma[index] ?: return null
                val fp = indicatorContext.fastSma[index - 1] ?: return null
                val sp = indicatorContext.slowSma[index - 1] ?: return null
                when {
                    fp <= sp && fc > sc -> BacktestSignal.Buy
                    fp >= sp && fc < sc && request.execution.allowShort -> BacktestSignal.Short
                    fp >= sp && fc < sc -> BacktestSignal.Sell
                    else -> null
                }
            }

            BacktestStrategyTemplate.RsiMeanReversion -> {
                val cur = indicatorContext.rsi[index] ?: return null
                when {
                    cur <= request.strategy.rsiOversold -> BacktestSignal.Buy
                    cur >= request.strategy.rsiOverbought && request.execution.allowShort -> BacktestSignal.Short
                    cur >= request.strategy.rsiOverbought -> BacktestSignal.Sell
                    else -> null
                }
            }

            BacktestStrategyTemplate.Breakout -> {
                val upper = breakoutHigh[index] ?: return null
                val lower = breakoutLow[index] ?: return null
                val close = indicatorContext.close[index]
                when {
                    close > upper -> BacktestSignal.Buy
                    close < lower && request.execution.allowShort -> BacktestSignal.Short
                    close < lower -> BacktestSignal.Sell
                    else -> null
                }
            }

            BacktestStrategyTemplate.SmaRsiConfluence -> {
                val fc = indicatorContext.fastSma[index] ?: return null
                val sc = indicatorContext.slowSma[index] ?: return null
                val fp = indicatorContext.fastSma[index - 1] ?: return null
                val sp = indicatorContext.slowSma[index - 1] ?: return null
                val r = indicatorContext.rsi[index] ?: return null
                when {
                    fp <= sp && fc > sc && r < request.strategy.rsiOverbought -> BacktestSignal.Buy
                    fp >= sp && fc < sc && r > request.strategy.rsiOversold && request.execution.allowShort -> BacktestSignal.Short
                    fp >= sp && fc < sc -> BacktestSignal.Sell
                    else -> null
                }
            }

            BacktestStrategyTemplate.AtrVolatilityBreakout -> {
                val atr = indicatorContext.atr[index] ?: return null
                val prevClose = indicatorContext.close[index - 1]
                val close = indicatorContext.close[index]
                val highRef = breakoutHigh[index] ?: return null
                val lowRef = breakoutLow[index] ?: return null
                val upThreshold = prevClose + atr * 0.6
                val downThreshold = prevClose - atr * 0.6
                when {
                    close > highRef && close > upThreshold -> BacktestSignal.Buy
                    close < lowRef && close < downThreshold && request.execution.allowShort -> BacktestSignal.Short
                    close < lowRef -> BacktestSignal.Sell
                    else -> null
                }
            }

            BacktestStrategyTemplate.SunMoonAstroFlow -> astroSignalsByIndex[index]

            BacktestStrategyTemplate.Custom -> {
                evaluateCustomSignal(
                    index = index,
                    position = position,
                    allowShort = request.execution.allowShort,
                    rules = customRules,
                    indicatorContext = indicatorContext
                ) ?: run {
                    val close = indicatorContext.close[index]
                    val base = indicatorContext.fastSma[index] ?: return null
                    val dist = if (base == 0.0) 0.0 else ((close / base) - 1.0) * 100.0
                    when {
                        dist >= 1.0 -> BacktestSignal.Buy
                        dist <= -1.0 && request.execution.allowShort -> BacktestSignal.Short
                        dist <= -1.0 -> BacktestSignal.Sell
                        else -> null
                    }
                }
            }
        }
    }

    private fun openPosition(request: BacktestRunRequest, side: BacktestTradeSide, next: OhlcvCandle, cash: Double): OpenedPosition? {
        val fill = resolveEntryFill(request, side, next) ?: return null
        val leverage = request.execution.leverage.coerceAtLeast(1.0)
        val feeBps = if (fill.isMaker) request.execution.makerFeeBps else request.execution.takerFeeBps
        val notional = when (request.execution.sizingMode) {
            BacktestPositionSizingMode.FixedAmount -> request.execution.fixedPositionAmount
            BacktestPositionSizingMode.PercentOfEquity -> cash * (request.execution.percentOfEquity / 100.0)
            BacktestPositionSizingMode.AtrRisk -> cash * (request.execution.atrRiskPercent / 100.0) * 5.0
            BacktestPositionSizingMode.VolatilityTarget -> cash * (request.execution.targetVolatilityPercent / 100.0)
        }.coerceIn(0.0, cash * leverage)
        val fillRatio = (request.execution.fillRatioPercent / 100.0).coerceIn(0.01, 1.0)
        val cap = next.volume * fill.price * (request.execution.maxVolumeParticipationPercent / 100.0)
        val actualNotional = min(notional * fillRatio, cap).coerceAtLeast(0.0)
        if (actualNotional <= 0.0) return null
        val fee = actualNotional * (feeBps / 10_000.0)
        val margin = if (request.execution.marginMode == BacktestMarginMode.Cash) actualNotional else actualNotional / leverage
        val cashAfter = cash - margin - fee
        if (cashAfter < -1e-6) return null
        return OpenedPosition(
            PositionState(side, next.openTime, fill.price, actualNotional / fill.price, actualNotional, margin, fee, fill.price, fill.price, 0.0),
            cashAfter
        )
    }

    private fun resolveRiskExit(position: PositionState, candle: OhlcvCandle, stopLoss: Double?, takeProfit: Double?, trailingStop: Double?): Pair<Double, BacktestExitReason>? {
        return when (position.side) {
            BacktestTradeSide.Long -> {
                val stop = stopLoss?.let { position.entryPrice * (1.0 - it / 100.0) }
                val take = takeProfit?.let { position.entryPrice * (1.0 + it / 100.0) }
                val trail = trailingStop?.let { position.maxSinceEntry.coerceAtLeast(candle.high) * (1.0 - it / 100.0) }
                when {
                    stop != null && candle.low <= stop -> stop to BacktestExitReason.StopLoss
                    take != null && candle.high >= take -> take to BacktestExitReason.TakeProfit
                    trail != null && candle.low <= trail -> trail to BacktestExitReason.TrailingStop
                    else -> null
                }
            }

            BacktestTradeSide.Short -> {
                val stop = stopLoss?.let { position.entryPrice * (1.0 + it / 100.0) }
                val take = takeProfit?.let { position.entryPrice * (1.0 - it / 100.0) }
                val trail = trailingStop?.let { position.minSinceEntry.coerceAtMost(candle.low) * (1.0 + it / 100.0) }
                when {
                    stop != null && candle.high >= stop -> stop to BacktestExitReason.StopLoss
                    take != null && candle.low <= take -> take to BacktestExitReason.TakeProfit
                    trail != null && candle.high >= trail -> trail to BacktestExitReason.TrailingStop
                    else -> null
                }
            }
        }
    }

    private fun closePosition(request: BacktestRunRequest, position: PositionState, exitPrice: Double, exitTime: Instant, reason: BacktestExitReason, isMaker: Boolean): ClosedPosition {
        val exitNotional = position.quantity * exitPrice
        val exitFee = exitNotional * ((if (isMaker) request.execution.makerFeeBps else request.execution.takerFeeBps) / 10_000.0)
        val gross = when (position.side) {
            BacktestTradeSide.Long -> (exitPrice - position.entryPrice) * position.quantity
            BacktestTradeSide.Short -> (position.entryPrice - exitPrice) * position.quantity
        }
        val net = gross - position.entryFee - exitFee - position.fundingPaid
        val pnlPct = if (position.notional == 0.0) 0.0 else (net / position.notional) * 100.0
        val duration = ((exitTime.toEpochMilli() - position.entryTime.toEpochMilli()) / 60_000L).coerceAtLeast(0L)
        val maePercent = when (position.side) {
            BacktestTradeSide.Long -> ((position.minSinceEntry / position.entryPrice) - 1.0) * 100.0
            BacktestTradeSide.Short -> ((position.entryPrice / position.maxSinceEntry) - 1.0) * 100.0
        }
        val mfePercent = when (position.side) {
            BacktestTradeSide.Long -> ((position.maxSinceEntry / position.entryPrice) - 1.0) * 100.0
            BacktestTradeSide.Short -> ((position.entryPrice / position.minSinceEntry) - 1.0) * 100.0
        }
        return ClosedPosition(
            position.side, position.entryTime, exitTime, position.entryPrice, exitPrice, position.quantity, position.margin, net, pnlPct,
            duration, maePercent, mfePercent, position.entryFee + exitFee, position.fundingPaid, reason
        )
    }

    private fun applyCloseCash(cash: Double, closed: ClosedPosition): Double = cash + closed.margin + closed.pnl

    private fun markToMarket(cash: Double, price: Double, position: PositionState?): Double {
        if (position == null) return cash
        val unrealized = when (position.side) {
            BacktestTradeSide.Long -> (price - position.entryPrice) * position.quantity
            BacktestTradeSide.Short -> (position.entryPrice - price) * position.quantity
        }
        return cash + position.margin + unrealized
    }

    private fun resolveEntryFill(request: BacktestRunRequest, side: BacktestTradeSide, candle: OhlcvCandle): Fill? = when (request.execution.orderType) {
        BacktestOrderType.Market -> Fill(entryPrice(side, candle.open, request.execution.slippageBps / 10_000.0), false)
        BacktestOrderType.Limit -> {
            val off = request.execution.limitOffsetBps / 10_000.0
            val px = if (side == BacktestTradeSide.Long) candle.open * (1.0 - off) else candle.open * (1.0 + off)
            val touched = if (side == BacktestTradeSide.Long) candle.low <= px else candle.high >= px
            if (touched) Fill(px, true) else null
        }
    }

    private fun resolveExitFill(request: BacktestRunRequest, side: BacktestTradeSide, candle: OhlcvCandle): Fill? = when (request.execution.orderType) {
        BacktestOrderType.Market -> Fill(exitPrice(side, candle.open, request.execution.slippageBps / 10_000.0), false)
        BacktestOrderType.Limit -> {
            val off = request.execution.limitOffsetBps / 10_000.0
            val px = if (side == BacktestTradeSide.Long) candle.open * (1.0 + off) else candle.open * (1.0 - off)
            val touched = if (side == BacktestTradeSide.Long) candle.high >= px else candle.low <= px
            if (touched) Fill(px, true) else null
        }
    }

    private fun entryPrice(side: BacktestTradeSide, raw: Double, slip: Double): Double =
        if (side == BacktestTradeSide.Long) raw * (1.0 + slip) else raw * (1.0 - slip)

    private fun exitPrice(side: BacktestTradeSide, raw: Double, slip: Double): Double =
        if (side == BacktestTradeSide.Long) raw * (1.0 - slip) else raw * (1.0 + slip)

    private fun sma(values: List<Double>, period: Int): List<Double?> {
        if (period <= 0) return List(values.size) { null }
        val out = MutableList<Double?>(values.size) { null }
        var sum = 0.0
        values.forEachIndexed { i, value ->
            sum += value
            if (i >= period) sum -= values[i - period]
            if (i >= period - 1) out[i] = sum / period
        }
        return out
    }

    private fun rollingHigh(values: List<Double>, lookback: Int): List<Double?> =
        values.indices.map { i -> if (i < lookback) null else values.subList(i - lookback, i).maxOrNull() }

    private fun rollingLow(values: List<Double>, lookback: Int): List<Double?> =
        values.indices.map { i -> if (i < lookback) null else values.subList(i - lookback, i).minOrNull() }

    private fun rsi(values: List<Double>, period: Int): List<Double?> {
        if (values.size < 2 || period <= 1) return List(values.size) { null }
        val out = MutableList<Double?>(values.size) { null }
        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 1 until values.size) {
            val d = values[i] - values[i - 1]
            val g = max(d, 0.0)
            val l = max(-d, 0.0)
            if (i <= period) {
                avgGain += g
                avgLoss += l
                if (i == period) {
                    avgGain /= period
                    avgLoss /= period
                    val rs = if (avgLoss == 0.0) Double.POSITIVE_INFINITY else avgGain / avgLoss
                    out[i] = 100.0 - 100.0 / (1.0 + rs)
                }
            } else {
                avgGain = ((avgGain * (period - 1)) + g) / period
                avgLoss = ((avgLoss * (period - 1)) + l) / period
                val rs = if (avgLoss == 0.0) Double.POSITIVE_INFINITY else avgGain / avgLoss
                out[i] = 100.0 - 100.0 / (1.0 + rs)
            }
        }
        return out
    }

    private fun ema(values: List<Double>, period: Int): List<Double?> {
        if (period <= 0 || values.isEmpty()) return List(values.size) { null }
        val out = MutableList<Double?>(values.size) { null }
        val alpha = 2.0 / (period + 1.0)
        var value: Double? = null
        values.forEachIndexed { index, sample ->
            val previous = value
            value = if (previous == null) sample else (sample * alpha) + (previous * (1.0 - alpha))
            if (index >= period - 1) {
                out[index] = value
            }
        }
        return out
    }

    private fun atr(candles: List<OhlcvCandle>, period: Int): List<Double?> {
        if (candles.size < 2 || period <= 0) return List(candles.size) { null }
        val trueRanges = MutableList(candles.size) { 0.0 }
        trueRanges[0] = candles[0].high - candles[0].low
        for (index in 1 until candles.size) {
            val current = candles[index]
            val previousClose = candles[index - 1].close
            val tr = max(
                current.high - current.low,
                max(abs(current.high - previousClose), abs(current.low - previousClose))
            )
            trueRanges[index] = tr
        }
        return sma(trueRanges, period)
    }

    private fun parseCustomRules(raw: String): CustomStrategyRules? {
        val candidate = raw.trim()
        if (candidate.isEmpty()) return null
        return runCatching {
            val root = json.parseToJsonElement(candidate).jsonObject
            CustomStrategyRules(
                entryLong = root.parseRuleGroup("entryLong"),
                exitLong = root.parseRuleGroup("exitLong"),
                entryShort = root.parseRuleGroup("entryShort"),
                exitShort = root.parseRuleGroup("exitShort")
            )
        }.getOrNull()
    }

    private fun evaluateCustomSignal(
        index: Int,
        position: PositionState?,
        allowShort: Boolean,
        rules: CustomStrategyRules?,
        indicatorContext: IndicatorContext
    ): BacktestSignal? {
        if (rules == null) return null
        return when (position?.side) {
            null -> {
                when {
                    evaluateRuleGroup(rules.entryLong, index, indicatorContext) -> BacktestSignal.Buy
                    allowShort && evaluateRuleGroup(rules.entryShort, index, indicatorContext) -> BacktestSignal.Short
                    else -> null
                }
            }

            BacktestTradeSide.Long -> {
                if (evaluateRuleGroup(rules.exitLong, index, indicatorContext)) BacktestSignal.Sell else null
            }

            BacktestTradeSide.Short -> {
                if (evaluateRuleGroup(rules.exitShort, index, indicatorContext)) BacktestSignal.Cover else null
            }
        }
    }

    private fun evaluateRuleGroup(
        group: CustomRuleGroup?,
        index: Int,
        indicatorContext: IndicatorContext
    ): Boolean {
        if (group == null) return false
        val allConditions = group.all.map { evaluateCondition(it, index, indicatorContext) }
        val anyConditions = group.any.map { evaluateCondition(it, index, indicatorContext) }
        val nestedGroups = group.groups.map { evaluateRuleGroup(it, index, indicatorContext) }

        val allOk = allConditions.all { it } && nestedGroups.all { it }
        val anyOk = if (group.any.isEmpty()) true else anyConditions.any { it }
        return allOk && anyOk
    }

    private fun evaluateCondition(
        condition: CustomRuleCondition,
        index: Int,
        indicatorContext: IndicatorContext
    ): Boolean {
        val left = resolveOperand(condition.left, index, indicatorContext) ?: return false
        val right = resolveOperand(condition.right, index, indicatorContext) ?: return false
        val previousLeft = resolveOperand(condition.left, index - 1, indicatorContext)
        val previousRight = resolveOperand(condition.right, index - 1, indicatorContext)
        return when (condition.op.uppercase()) {
            "GT" -> left > right
            "GTE" -> left >= right
            "LT" -> left < right
            "LTE" -> left <= right
            "EQ" -> abs(left - right) <= 1e-9
            "CROSS_UP" -> previousLeft != null && previousRight != null && previousLeft <= previousRight && left > right
            "CROSS_DOWN" -> previousLeft != null && previousRight != null && previousLeft >= previousRight && left < right
            else -> false
        }
    }

    private fun resolveOperand(
        operand: CustomRuleOperand,
        index: Int,
        indicatorContext: IndicatorContext
    ): Double? {
        if (index < 0 || index >= indicatorContext.close.size) return null
        operand.value?.let { return it }

        val indicator = operand.indicator?.lowercase().orEmpty()
        return when (indicator) {
            "close" -> indicatorContext.close[index]
            "open" -> indicatorContext.candles[index].open
            "high" -> indicatorContext.candles[index].high
            "low" -> indicatorContext.candles[index].low
            "volume" -> indicatorContext.candles[index].volume
            "sma" -> {
                val period = operand.period ?: return null
                indicatorContext.sma(period)[index]
            }
            "ema" -> {
                val period = operand.period ?: return null
                indicatorContext.ema(period)[index]
            }
            "rsi" -> {
                val period = operand.period ?: return null
                indicatorContext.rsi(period)[index]
            }
            "atr" -> {
                val period = operand.period ?: return null
                indicatorContext.atr(period)[index]
            }
            else -> null
        }
    }
}

private data class TimedSignalCandidate(
    val instant: Instant,
    val signal: BacktestSignal
)

private data class IndexedSignalCandidate(
    val index: Int,
    val signal: BacktestSignal,
    val distance: Int
)

private data class IndicatorContext(
    val candles: List<OhlcvCandle>,
    val close: List<Double>,
    val fastSma: List<Double?>,
    val slowSma: List<Double?>,
    val fastEma: List<Double?>,
    val slowEma: List<Double?>,
    val rsi: List<Double?>,
    val atr: List<Double?>,
    private val smaCache: MutableMap<Int, List<Double?>> = mutableMapOf(),
    private val emaCache: MutableMap<Int, List<Double?>> = mutableMapOf(),
    private val rsiCache: MutableMap<Int, List<Double?>> = mutableMapOf(),
    private val atrCache: MutableMap<Int, List<Double?>> = mutableMapOf()
) {
    fun sma(period: Int): List<Double?> =
        smaCache.getOrPut(period) { calculateSma(close, period) }

    fun ema(period: Int): List<Double?> =
        emaCache.getOrPut(period) { calculateEma(close, period) }

    fun rsi(period: Int): List<Double?> =
        rsiCache.getOrPut(period) { calculateRsi(close, period) }

    fun atr(period: Int): List<Double?> =
        atrCache.getOrPut(period) { calculateAtr(candles, period) }
}

private fun JsonObject.parseRuleGroup(key: String): CustomRuleGroup? =
    get(key)
        ?.jsonObject
        ?.toCustomRuleGroup()

private fun JsonObject.toCustomRuleGroup(): CustomRuleGroup {
    fun JsonElement.asObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

    val allConditions = (this["all"] as? JsonArray)
        ?.mapNotNull { it.asObjectOrNull()?.toCustomRuleCondition() }
        .orEmpty()
    val anyConditions = (this["any"] as? JsonArray)
        ?.mapNotNull { it.asObjectOrNull()?.toCustomRuleCondition() }
        .orEmpty()
    val nestedGroups = (this["groups"] as? JsonArray)
        ?.mapNotNull { it.asObjectOrNull()?.toCustomRuleGroup() }
        .orEmpty()
    return CustomRuleGroup(
        all = allConditions,
        any = anyConditions,
        groups = nestedGroups
    )
}

private fun JsonObject.toCustomRuleCondition(): CustomRuleCondition? {
    val left = get("left")?.runCatching { jsonObject }?.getOrNull()?.toCustomRuleOperand() ?: return null
    val right = get("right")?.runCatching { jsonObject }?.getOrNull()?.toCustomRuleOperand() ?: return null
    val op = get("op")?.jsonPrimitive?.content?.trim().orEmpty()
    if (op.isEmpty()) return null
    return CustomRuleCondition(left = left, op = op, right = right)
}

private fun JsonObject.toCustomRuleOperand(): CustomRuleOperand {
    return CustomRuleOperand(
        indicator = get("indicator")?.jsonPrimitive?.contentOrNull?.trim(),
        period = get("period")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
        value = get("value")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
    )
}

private data class CustomStrategyRules(
    val entryLong: CustomRuleGroup? = null,
    val exitLong: CustomRuleGroup? = null,
    val entryShort: CustomRuleGroup? = null,
    val exitShort: CustomRuleGroup? = null
)

private data class CustomRuleGroup(
    val all: List<CustomRuleCondition> = emptyList(),
    val any: List<CustomRuleCondition> = emptyList(),
    val groups: List<CustomRuleGroup> = emptyList()
)

private data class CustomRuleCondition(
    val left: CustomRuleOperand,
    val op: String,
    val right: CustomRuleOperand
)

private data class CustomRuleOperand(
    val indicator: String? = null,
    val period: Int? = null,
    val value: Double? = null
)

private data class PositionState(
    val side: BacktestTradeSide,
    val entryTime: Instant,
    val entryPrice: Double,
    val quantity: Double,
    val notional: Double,
    val margin: Double,
    val entryFee: Double,
    val maxSinceEntry: Double,
    val minSinceEntry: Double,
    val fundingPaid: Double
)

private data class Fill(val price: Double, val isMaker: Boolean)

private data class OpenedPosition(val position: PositionState, val cashAfterEntry: Double)

private data class ClosedPosition(
    val side: BacktestTradeSide,
    val entryTime: Instant,
    val exitTime: Instant,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val margin: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val durationMinutes: Long,
    val maePercent: Double,
    val mfePercent: Double,
    val fees: Double,
    val funding: Double,
    val exitReason: BacktestExitReason
) {
    fun toTrade(symbol: String): BacktestTrade = BacktestTrade(
        id = UUID.randomUUID().toString(),
        symbol = symbol,
        side = side,
        entryTime = entryTime,
        exitTime = exitTime,
        entryPrice = entryPrice,
        exitPrice = exitPrice,
        quantity = quantity,
        pnl = pnl,
        pnlPercent = pnlPercent,
        durationMinutes = durationMinutes,
        maePercent = maePercent,
        mfePercent = mfePercent,
        feesPaid = fees,
        fundingPaid = funding,
        exitReason = exitReason
    )
}

private fun calculateSma(values: List<Double>, period: Int): List<Double?> {
    if (period <= 0) return List(values.size) { null }
    val out = MutableList<Double?>(values.size) { null }
    var sum = 0.0
    values.forEachIndexed { index, value ->
        sum += value
        if (index >= period) sum -= values[index - period]
        if (index >= period - 1) out[index] = sum / period
    }
    return out
}

private fun calculateEma(values: List<Double>, period: Int): List<Double?> {
    if (period <= 0 || values.isEmpty()) return List(values.size) { null }
    val out = MutableList<Double?>(values.size) { null }
    val alpha = 2.0 / (period + 1.0)
    var emaValue: Double? = null
    values.forEachIndexed { index, value ->
        val previous = emaValue
        emaValue = if (previous == null) value else (value * alpha) + (previous * (1.0 - alpha))
        if (index >= period - 1) out[index] = emaValue
    }
    return out
}

private fun calculateRsi(values: List<Double>, period: Int): List<Double?> {
    if (values.size < 2 || period <= 1) return List(values.size) { null }
    val out = MutableList<Double?>(values.size) { null }
    var avgGain = 0.0
    var avgLoss = 0.0
    for (index in 1 until values.size) {
        val delta = values[index] - values[index - 1]
        val gain = max(delta, 0.0)
        val loss = max(-delta, 0.0)
        if (index <= period) {
            avgGain += gain
            avgLoss += loss
            if (index == period) {
                avgGain /= period
                avgLoss /= period
                val rs = if (avgLoss == 0.0) Double.POSITIVE_INFINITY else avgGain / avgLoss
                out[index] = 100.0 - 100.0 / (1.0 + rs)
            }
        } else {
            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period
            val rs = if (avgLoss == 0.0) Double.POSITIVE_INFINITY else avgGain / avgLoss
            out[index] = 100.0 - 100.0 / (1.0 + rs)
        }
    }
    return out
}

private fun calculateAtr(candles: List<OhlcvCandle>, period: Int): List<Double?> {
    if (candles.size < 2 || period <= 0) return List(candles.size) { null }
    val trueRanges = MutableList(candles.size) { 0.0 }
    trueRanges[0] = candles[0].high - candles[0].low
    for (index in 1 until candles.size) {
        val current = candles[index]
        val previousClose = candles[index - 1].close
        trueRanges[index] = max(
            current.high - current.low,
            max(abs(current.high - previousClose), abs(current.low - previousClose))
        )
    }
    return calculateSma(trueRanges, period)
}
