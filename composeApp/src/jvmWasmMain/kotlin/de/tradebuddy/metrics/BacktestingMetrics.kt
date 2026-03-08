package de.tradebuddy.metrics

import de.tradebuddy.domain.model.BacktestAnalysis
import de.tradebuddy.domain.model.BacktestEquityPoint
import de.tradebuddy.domain.model.BacktestMetrics
import de.tradebuddy.domain.model.BacktestMonteCarloResult
import de.tradebuddy.domain.model.BacktestMonthlyReturn
import de.tradebuddy.domain.model.BacktestTimeframe
import de.tradebuddy.domain.model.BacktestTrade
import de.tradebuddy.domain.model.BacktestWalkForwardResult
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

object BacktestingMetrics {
    fun buildDrawdownCurve(equityCurve: List<BacktestEquityPoint>): List<BacktestEquityPoint> {
        if (equityCurve.isEmpty()) return emptyList()
        var rollingPeak = equityCurve.first().equity
        return equityCurve.map { point ->
            if (point.equity > rollingPeak) rollingPeak = point.equity
            val drawdown = if (rollingPeak == 0.0) 0.0 else ((point.equity / rollingPeak) - 1.0) * 100.0
            point.copy(drawdownPercent = drawdown)
        }
    }

    fun calculate(
        startEquity: Double,
        endEquity: Double,
        timeframe: BacktestTimeframe,
        from: Instant,
        to: Instant,
        trades: List<BacktestTrade>,
        equityCurve: List<BacktestEquityPoint>
    ): BacktestMetrics {
        val returns = periodicReturns(equityCurve)
        val wins = trades.count { it.pnl > 0.0 }
        val winRate = if (trades.isEmpty()) 0.0 else (wins.toDouble() / trades.size.toDouble()) * 100.0
        val totalReturn = if (startEquity == 0.0) 0.0 else ((endEquity / startEquity) - 1.0) * 100.0

        val days = ((to.toEpochMilli() - from.toEpochMilli()) / 86_400_000L).coerceAtLeast(1L)
        val cagr = if (startEquity > 0.0 && endEquity > 0.0 && days >= 365L) {
            ((endEquity / startEquity).pow(365.0 / days.toDouble()) - 1.0) * 100.0
        } else {
            null
        }

        val drawdownCurve = buildDrawdownCurve(equityCurve)
        val maxDrawdown = drawdownCurve.minOfOrNull { it.drawdownPercent }?.let { -it }?.coerceAtLeast(0.0) ?: 0.0

        val grossProfit = trades.filter { it.pnl > 0.0 }.sumOf { it.pnl }
        val grossLoss = trades.filter { it.pnl < 0.0 }.sumOf { it.pnl }
        val profitFactor = when {
            grossLoss == 0.0 && grossProfit > 0.0 -> 99.0
            grossLoss == 0.0 -> 0.0
            else -> grossProfit / abs(grossLoss)
        }

        val avgTrade = if (trades.isEmpty()) 0.0 else trades.sumOf { it.pnlPercent } / trades.size.toDouble()
        val expectancy = avgTrade
        val exposureTime = exposurePercent(from = from, to = to, trades = trades)
        val periodsPerYear = periodsPerYear(timeframe)
        val sharpe = sharpeRatio(returns, periodsPerYear)
        val sortino = sortinoRatio(returns, periodsPerYear)
        val annualVolatility = annualizedVolatility(returns, periodsPerYear)

        return BacktestMetrics(
            winRatePercent = winRate,
            totalReturnPercent = totalReturn,
            cagrPercent = cagr,
            maxDrawdownPercent = maxDrawdown,
            sharpeRatio = sharpe,
            sortinoRatio = sortino,
            profitFactor = profitFactor,
            expectancyPercent = expectancy,
            annualVolatilityPercent = annualVolatility,
            tradeCount = trades.size,
            averageTradePercent = avgTrade,
            exposureTimePercent = exposureTime,
            startEquity = startEquity,
            endEquity = endEquity
        )
    }

    fun buildAnalysis(
        trades: List<BacktestTrade>,
        equityCurve: List<BacktestEquityPoint>,
        monteCarloIterations: Int = 400,
        walkForwardSplits: Int = 4
    ): BacktestAnalysis {
        return BacktestAnalysis(
            monthlyReturns = monthlyReturns(equityCurve),
            walkForward = walkForward(equityCurve, walkForwardSplits),
            monteCarlo = monteCarloBootstrap(trades, monteCarloIterations)
        )
    }

    fun monthlyReturns(equityCurve: List<BacktestEquityPoint>): List<BacktestMonthlyReturn> {
        if (equityCurve.size < 2) return emptyList()
        data class Marker(val year: Int, val month: Int, val first: Double, val last: Double)
        val grouped = equityCurve
            .groupBy {
                val date = it.time.atZone(ZoneOffset.UTC).toLocalDate()
                date.year to date.monthValue
            }
            .entries
            .sortedWith(
                compareBy<Map.Entry<Pair<Int, Int>, List<BacktestEquityPoint>>> { it.key.first }
                    .thenBy { it.key.second }
            )
            .map { entry ->
                val key = entry.key
                val points = entry.value
                Marker(
                    year = key.first,
                    month = key.second,
                    first = points.first().equity,
                    last = points.last().equity
                )
            }
        return grouped.map { marker ->
            val ret = if (marker.first == 0.0) 0.0 else ((marker.last / marker.first) - 1.0) * 100.0
            BacktestMonthlyReturn(marker.year, marker.month, ret)
        }
    }

    fun walkForward(
        equityCurve: List<BacktestEquityPoint>,
        splitCount: Int
    ): BacktestWalkForwardResult? {
        if (equityCurve.size < 40 || splitCount < 2) return null
        val safeSplits = splitCount.coerceAtMost(10)
        val chunkSize = (equityCurve.size / safeSplits).coerceAtLeast(2)
        val inSampleReturns = mutableListOf<Double>()
        val outSampleReturns = mutableListOf<Double>()
        for (i in 0 until safeSplits - 1) {
            val trainStart = i * chunkSize
            val trainEnd = (trainStart + chunkSize - 1).coerceAtMost(equityCurve.lastIndex)
            val testStart = trainEnd
            val testEnd = (testStart + chunkSize - 1).coerceAtMost(equityCurve.lastIndex)
            if (testEnd <= testStart) continue

            val trainReturn = percentReturn(equityCurve[trainStart].equity, equityCurve[trainEnd].equity)
            val testReturn = percentReturn(equityCurve[testStart].equity, equityCurve[testEnd].equity)
            inSampleReturns += trainReturn
            outSampleReturns += testReturn
        }
        if (inSampleReturns.isEmpty() || outSampleReturns.isEmpty()) return null
        val inAvg = inSampleReturns.average()
        val outAvg = outSampleReturns.average()
        val stability = if (inAvg == 0.0) 0.0 else ((outAvg / inAvg) * 100.0).coerceIn(-200.0, 200.0)
        return BacktestWalkForwardResult(
            splitCount = safeSplits,
            inSampleAverageReturnPercent = inAvg,
            outOfSampleAverageReturnPercent = outAvg,
            stabilityScorePercent = stability
        )
    }

    fun monteCarloBootstrap(
        trades: List<BacktestTrade>,
        iterations: Int
    ): BacktestMonteCarloResult? {
        if (trades.size < 3 || iterations <= 0) return null
        val pnlPercents = trades.map { it.pnlPercent }
        if (pnlPercents.isEmpty()) return null
        val random = Random(42)
        val samples = MutableList(iterations) { 0.0 }
        for (i in 0 until iterations) {
            var acc = 0.0
            repeat(pnlPercents.size) {
                val pick = pnlPercents[random.nextInt(pnlPercents.size)]
                acc += pick
            }
            samples[i] = acc
        }
        val sorted = samples.sorted()
        val p05 = sorted[(sorted.size * 0.05).roundToInt().coerceIn(0, sorted.lastIndex)]
        val p95 = sorted[(sorted.size * 0.95).roundToInt().coerceIn(0, sorted.lastIndex)]
        val median = sorted[sorted.size / 2]
        return BacktestMonteCarloResult(
            iterations = iterations,
            medianReturnPercent = median,
            p05ReturnPercent = p05,
            p95ReturnPercent = p95
        )
    }

    private fun percentReturn(start: Double, end: Double): Double =
        if (start == 0.0) 0.0 else ((end / start) - 1.0) * 100.0

    private fun periodicReturns(equityCurve: List<BacktestEquityPoint>): List<Double> =
        if (equityCurve.size < 2) {
            emptyList()
        } else {
            equityCurve.zipWithNext { prev, current ->
                if (prev.equity == 0.0) 0.0 else (current.equity / prev.equity) - 1.0
            }
        }

    private fun sharpeRatio(
        returns: List<Double>,
        periodsPerYear: Double
    ): Double {
        if (returns.size < 2) return 0.0
        val mean = returns.average()
        val variance = returns.map { value -> (value - mean) * (value - mean) }.average()
        val stdDev = sqrt(variance)
        if (stdDev == 0.0) return 0.0
        return (mean / stdDev) * sqrt(periodsPerYear)
    }

    private fun sortinoRatio(
        returns: List<Double>,
        periodsPerYear: Double
    ): Double {
        if (returns.size < 2) return 0.0
        val mean = returns.average()
        val downside = returns.filter { it < 0.0 }
        if (downside.isEmpty()) return 99.0
        val downsideVariance = downside.map { value -> value * value }.average()
        val downsideDev = sqrt(downsideVariance)
        if (downsideDev == 0.0) return 0.0
        return (mean / downsideDev) * sqrt(periodsPerYear)
    }

    private fun annualizedVolatility(
        returns: List<Double>,
        periodsPerYear: Double
    ): Double {
        if (returns.size < 2) return 0.0
        val mean = returns.average()
        val variance = returns.map { value -> (value - mean) * (value - mean) }.average()
        return sqrt(variance) * sqrt(periodsPerYear) * 100.0
    }

    private fun periodsPerYear(timeframe: BacktestTimeframe): Double {
        val minutesPerYear = 365.0 * 24.0 * 60.0
        return minutesPerYear / timeframe.approxMinutes.toDouble().coerceAtLeast(1.0)
    }

    private fun exposurePercent(
        from: Instant,
        to: Instant,
        trades: List<BacktestTrade>
    ): Double {
        if (trades.isEmpty()) return 0.0
        val totalWindowMinutes = ((to.toEpochMilli() - from.toEpochMilli()) / 60_000L).coerceAtLeast(1L)
        val exposureMinutes = trades.sumOf { it.durationMinutes.coerceAtLeast(0L) }
        return (exposureMinutes.toDouble() / totalWindowMinutes.toDouble()) * 100.0
    }
}
