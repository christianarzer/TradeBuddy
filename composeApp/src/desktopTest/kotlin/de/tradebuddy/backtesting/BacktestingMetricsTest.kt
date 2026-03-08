package de.tradebuddy.backtesting

import de.tradebuddy.domain.model.BacktestEquityPoint
import de.tradebuddy.domain.model.BacktestExitReason
import de.tradebuddy.domain.model.BacktestTimeframe
import de.tradebuddy.domain.model.BacktestTrade
import de.tradebuddy.domain.model.BacktestTradeSide
import de.tradebuddy.metrics.BacktestingMetrics
import java.time.Instant
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BacktestingMetricsTest {
    @Test
    fun calculatesWinRateAndMaxDrawdown() {
        val from = Instant.parse("2026-01-01T00:00:00Z")
        val to = Instant.parse("2026-01-20T00:00:00Z")
        val trades = listOf(
            trade(entry = "2026-01-02T00:00:00Z", exit = "2026-01-03T00:00:00Z", pnl = 100.0, pnlPercent = 1.0),
            trade(entry = "2026-01-05T00:00:00Z", exit = "2026-01-06T00:00:00Z", pnl = -200.0, pnlPercent = -2.0)
        )
        val equityCurve = listOf(
            BacktestEquityPoint(time = from, equity = 10_000.0),
            BacktestEquityPoint(time = from.plusSeconds(3600), equity = 11_000.0),
            BacktestEquityPoint(time = from.plusSeconds(7200), equity = 9_000.0),
            BacktestEquityPoint(time = from.plusSeconds(10_800), equity = 12_000.0)
        )

        val metrics = BacktestingMetrics.calculate(
            startEquity = 10_000.0,
            endEquity = 12_000.0,
            timeframe = BacktestTimeframe.H1,
            from = from,
            to = to,
            trades = trades,
            equityCurve = equityCurve
        )

        assertEquals(50.0, metrics.winRatePercent, 0.0001)
        assertTrue(abs(metrics.maxDrawdownPercent - 18.1818) < 0.01)
        assertEquals(2, metrics.tradeCount)
    }
}

private fun trade(
    entry: String,
    exit: String,
    pnl: Double,
    pnlPercent: Double
): BacktestTrade = BacktestTrade(
    id = "t-$entry",
    symbol = "BTCUSDT",
    side = BacktestTradeSide.Long,
    entryTime = Instant.parse(entry),
    exitTime = Instant.parse(exit),
    entryPrice = 100.0,
    exitPrice = 101.0,
    quantity = 1.0,
    pnl = pnl,
    pnlPercent = pnlPercent,
    durationMinutes = 60L,
    maePercent = -0.5,
    mfePercent = 0.8,
    feesPaid = 0.0,
    fundingPaid = 0.0,
    exitReason = BacktestExitReason.Signal
)
