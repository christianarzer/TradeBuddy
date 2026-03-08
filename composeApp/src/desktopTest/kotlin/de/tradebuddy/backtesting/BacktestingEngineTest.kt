package de.tradebuddy.backtesting

import de.tradebuddy.domain.model.BacktestDataRequest
import de.tradebuddy.domain.model.BacktestExchange
import de.tradebuddy.domain.model.BacktestExecutionSettings
import de.tradebuddy.domain.model.BacktestMarketData
import de.tradebuddy.domain.model.BacktestRunRequest
import de.tradebuddy.domain.model.BacktestStrategyConfig
import de.tradebuddy.domain.model.BacktestStrategyTemplate
import de.tradebuddy.domain.model.BacktestTimeframe
import de.tradebuddy.domain.model.BacktestTradeSide
import de.tradebuddy.domain.model.OhlcvCandle
import de.tradebuddy.engine.BacktestingEngine
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class BacktestingEngineTest {
    @Test
    fun smaCrossoverProducesDeterministicTrade() = runBlocking {
        val from = Instant.parse("2026-01-01T00:00:00Z")
        val candles = listOf(
            candle("2026-01-01T00:00:00Z", 100.0, 100.5, 99.5, 100.0),
            candle("2026-01-01T01:00:00Z", 100.0, 100.5, 99.5, 100.0),
            candle("2026-01-01T02:00:00Z", 100.0, 100.5, 99.5, 100.0),
            candle("2026-01-01T03:00:00Z", 100.0, 110.5, 99.5, 110.0),
            candle("2026-01-01T04:00:00Z", 90.0, 91.0, 89.0, 90.0),
            candle("2026-01-01T05:00:00Z", 95.0, 96.0, 94.0, 95.0)
        )
        val to = candles.last().closeTime
        val request = BacktestRunRequest(
            data = BacktestDataRequest(
                exchange = BacktestExchange.BinanceSpot,
                symbol = "BTCUSDT",
                timeframe = BacktestTimeframe.H1,
                from = from,
                to = to,
                warmupBars = 1
            ),
            strategy = BacktestStrategyConfig(
                template = BacktestStrategyTemplate.SmaCrossover,
                smaFastPeriod = 2,
                smaSlowPeriod = 3
            ),
            execution = BacktestExecutionSettings(
                initialBalance = 10_000.0,
                makerFeeBps = 0.0,
                takerFeeBps = 0.0,
                slippageBps = 0.0,
                allowShort = false
            )
        )

        val result = BacktestingEngine().run(
            request = request,
            marketData = BacktestMarketData(candles = candles)
        )

        assertEquals(1, result.trades.size)
        val trade = result.trades.first()
        assertEquals(BacktestTradeSide.Long, trade.side)
        assertEquals(candles[4].openTime, trade.entryTime)
        assertEquals(candles.last().closeTime, trade.exitTime)
        assertTrue(result.metrics.tradeCount == 1)
    }
}

private fun candle(
    openTimeIso: String,
    open: Double,
    high: Double,
    low: Double,
    close: Double
): OhlcvCandle {
    val openTime = Instant.parse(openTimeIso)
    val closeTime = openTime.plusSeconds(3600)
    return OhlcvCandle(
        openTime = openTime,
        closeTime = closeTime,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = 1000.0
    )
}
