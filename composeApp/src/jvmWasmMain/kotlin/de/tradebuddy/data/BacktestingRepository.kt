package de.tradebuddy.data

import de.tradebuddy.domain.model.BacktestDataRequest
import de.tradebuddy.domain.model.BacktestAnalysis
import de.tradebuddy.domain.model.BacktestEdgeGateDecision
import de.tradebuddy.domain.model.BacktestEdgeValidation
import de.tradebuddy.domain.model.BacktestEdgeValidationStatus
import de.tradebuddy.domain.model.BacktestExchange
import de.tradebuddy.domain.model.BacktestExecutionSettings
import de.tradebuddy.domain.model.BacktestExitReason
import de.tradebuddy.domain.model.BacktestHistoryRun
import de.tradebuddy.domain.model.BacktestMarginMode
import de.tradebuddy.domain.model.BacktestMarketData
import de.tradebuddy.domain.model.BacktestMetrics
import de.tradebuddy.domain.model.BacktestMonteCarloResult
import de.tradebuddy.domain.model.BacktestMonthlyReturn
import de.tradebuddy.domain.model.BacktestOrderType
import de.tradebuddy.domain.model.BacktestPositionSizingMode
import de.tradebuddy.domain.model.BacktestPricePoint
import de.tradebuddy.domain.model.BacktestResult
import de.tradebuddy.domain.model.BacktestRunRequest
import de.tradebuddy.domain.model.BacktestSignal
import de.tradebuddy.domain.model.BacktestStrategyConfig
import de.tradebuddy.domain.model.BacktestStrategyTemplate
import de.tradebuddy.domain.model.BacktestTimeframe
import de.tradebuddy.domain.model.BacktestTrade
import de.tradebuddy.domain.model.BacktestTradeSide
import de.tradebuddy.domain.model.BacktestWalkForwardResult
import de.tradebuddy.domain.model.BacktestEquityPoint
import de.tradebuddy.domain.model.OhlcvCandle
import de.tradebuddy.domain.model.StrategySignalPoint
import de.tradebuddy.logging.AppLog
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface BacktestingRepository {
    suspend fun loadCandles(
        request: BacktestDataRequest,
        onProgress: (fraction: Float, message: String) -> Unit = { _, _ -> }
    ): BacktestMarketData
    suspend fun loadSymbols(
        exchange: BacktestExchange,
        forceRefresh: Boolean = false
    ): List<String>

    suspend fun loadHistory(): List<BacktestHistoryRun>
    suspend fun saveHistoryRun(run: BacktestHistoryRun)
    suspend fun importCandlesCsv(
        exchange: BacktestExchange,
        symbol: String,
        timeframe: BacktestTimeframe,
        csvContent: String
    ): Int
    suspend fun deleteHistoryRun(runId: String)
    suspend fun clearHistory()
}

interface CandleDataProvider {
    suspend fun fetchCandles(
        request: BacktestDataRequest,
        onProgress: (fraction: Float, message: String) -> Unit = { _, _ -> }
    ): BacktestMarketData

    suspend fun fetchSymbols(): List<String> = emptyList()
}

private val PreferredQuoteAssets: Set<String> = setOf("USDT", "USDC", "USD", "EUR", "BTC", "ETH")

class BinanceKlineDataProvider(
    private val fetcher: suspend (String, Map<String, String>) -> String
) : CandleDataProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchCandles(
        request: BacktestDataRequest,
        onProgress: (fraction: Float, message: String) -> Unit
    ): BacktestMarketData {
        val symbol = request.symbol.uppercase()
        val interval = request.timeframe.binanceInterval
        val headers = mapOf(
            "Accept" to "application/json",
            "User-Agent" to "TradeBuddy/1.0"
        )
        val startMillis = request.from.toEpochMilli()
        val endMillis = request.to.toEpochMilli()
        val candles = mutableListOf<OhlcvCandle>()
        var cursor = startMillis
        var page = 0
        val maxPages = 50_000

        while (cursor <= endMillis && page < maxPages) {
            page += 1
            onProgress(
                ((cursor - startMillis).toFloat() / (endMillis - startMillis).coerceAtLeast(1L).toFloat())
                    .coerceIn(0f, 1f),
                "Binance: Seite $page"
            )
            yield()

            val url = buildString {
                append("https://api.binance.com/api/v3/klines")
                append("?symbol=").append(symbol)
                append("&interval=").append(interval)
                append("&startTime=").append(cursor)
                append("&endTime=").append(endMillis)
                append("&limit=1000")
            }

            val payload = fetchWithRetry(
                url = url,
                headers = headers,
                maxAttempts = 4
            )
            val rows = runCatching { json.parseToJsonElement(payload).jsonArray }.getOrElse { error ->
                AppLog.warn(
                    tag = "BinanceKlineDataProvider",
                    message = "Konnte Binance-Kline-Payload nicht parsen",
                    throwable = error
                )
                break
            }
            if (rows.isEmpty()) break

            val parsed = rows.mapNotNull { row ->
                val values = row.jsonArray
                val openTime = values.getOrNull(0)?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
                val open = values.getOrNull(1)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val high = values.getOrNull(2)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val low = values.getOrNull(3)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val close = values.getOrNull(4)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val volume = values.getOrNull(5)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val closeTime = values.getOrNull(6)?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
                OhlcvCandle(
                    openTime = Instant.ofEpochMilli(openTime),
                    closeTime = Instant.ofEpochMilli(closeTime),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            }

            if (parsed.isEmpty()) break
            candles += parsed

            val lastOpen = parsed.last().openTime.toEpochMilli()
            val nextCursor = lastOpen + request.timeframe.approxMinutes.toLong().milliseconds.inWholeMilliseconds
            if (nextCursor <= cursor) break
            cursor = nextCursor
            if (parsed.size < 1000) break
        }

        val normalized = candles
            .distinctBy { it.openTime }
            .sortedBy { it.openTime }
            .filter { it.openTime >= request.from && it.openTime <= request.to }
        val notes = buildDataQualityNotes(
            candles = normalized,
            timeframe = request.timeframe,
            from = request.from,
            to = request.to
        )
        return BacktestMarketData(
            candles = normalized,
            notes = notes,
            servedFromCache = false
        )
    }

    override suspend fun fetchSymbols(): List<String> {
        val headers = mapOf(
            "Accept" to "application/json",
            "User-Agent" to "TradeBuddy/1.0"
        )
        val payload = fetchWithRetry(
            url = "https://api.binance.com/api/v3/exchangeInfo?permissions=SPOT",
            headers = headers,
            maxAttempts = 4
        )
        val root = json.parseToJsonElement(payload).jsonObject
        val symbols = root["symbols"]?.jsonArray.orEmpty().mapNotNull { node ->
            val row = node.jsonObject
            val symbol = row["symbol"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: return@mapNotNull null
            val status = row["status"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val isSpotTradingAllowed = row["isSpotTradingAllowed"]?.jsonPrimitive?.contentOrNull
                ?.toBooleanStrictOrNull()
                ?: true
            val quoteAsset = row["quoteAsset"]?.jsonPrimitive?.contentOrNull?.uppercase().orEmpty()
            if (status != "TRADING") return@mapNotNull null
            if (!isSpotTradingAllowed) return@mapNotNull null
            if (quoteAsset !in PreferredQuoteAssets) return@mapNotNull null
            symbol
        }
        return symbols
            .mapNotNull { it.trim().uppercase().takeIf { value -> value.isNotBlank() } }
            .distinct()
            .sorted()
    }

    private suspend fun fetchWithRetry(
        url: String,
        headers: Map<String, String>,
        maxAttempts: Int
    ): String {
        var attempt = 0
        var lastError: Throwable? = null
        while (attempt < maxAttempts) {
            attempt += 1
            val result = runCatching { fetcher(url, headers) }
            if (result.isSuccess) {
                return result.getOrThrow()
            }

            val error = result.exceptionOrNull() ?: IllegalStateException("Unbekannter Netzwerkfehler")
            lastError = error
            val status = error.httpStatusCode()
            val retryable = status == 429 || status == 418 || status == 502 || status == 503 || status == 504
            if (!retryable || attempt >= maxAttempts) {
                break
            }
            val waitMillis = 400L * (1L shl (attempt - 1))
            AppLog.warn(
                tag = "BinanceKlineDataProvider",
                message = "Binance Rate-Limit/Serverfehler (HTTP $status), Retry in ${waitMillis}ms"
            )
            delay(waitMillis)
        }
        throw lastError ?: IllegalStateException("Unbekannter Binance-Fehler")
    }
}

class BybitKlineDataProvider(
    private val fetcher: suspend (String, Map<String, String>) -> String
) : CandleDataProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchCandles(
        request: BacktestDataRequest,
        onProgress: (fraction: Float, message: String) -> Unit
    ): BacktestMarketData {
        val symbol = request.symbol.uppercase()
        val interval = request.timeframe.bybitInterval()
        val stepMillis = request.timeframe.approxMinutes.toLong().milliseconds.inWholeMilliseconds
        val headers = mapOf(
            "Accept" to "application/json",
            "User-Agent" to "TradeBuddy/1.0"
        )
        val startMillis = request.from.toEpochMilli()
        val endMillis = request.to.toEpochMilli()
        val candles = mutableListOf<OhlcvCandle>()
        var cursor = endMillis
        var page = 0
        val maxPages = 50_000

        while (cursor >= startMillis && page < maxPages) {
            page += 1
            onProgress(
                (1f - ((cursor - startMillis).toFloat() / (endMillis - startMillis).coerceAtLeast(1L).toFloat()))
                    .coerceIn(0f, 1f),
                "Bybit: Seite $page"
            )
            yield()

            val url = buildString {
                append("https://api.bybit.com/v5/market/kline")
                append("?category=spot")
                append("&symbol=").append(symbol)
                append("&interval=").append(interval)
                append("&start=").append(startMillis)
                append("&end=").append(cursor)
                append("&limit=1000")
            }

            val payload = fetchWithRetry(
                url = url,
                headers = headers,
                maxAttempts = 4
            )
            val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrElse { error ->
                AppLog.warn(
                    tag = "BybitKlineDataProvider",
                    message = "Konnte Bybit-Kline-Payload nicht parsen",
                    throwable = error
                )
                break
            }
            val retCode = root["retCode"]?.jsonPrimitive?.content?.toIntOrNull()
            if (retCode != null && retCode != 0) {
                val retMsg = root["retMsg"]?.jsonPrimitive?.content.orEmpty()
                AppLog.warn(
                    tag = "BybitKlineDataProvider",
                    message = "Bybit antwortete mit retCode=$retCode ($retMsg)"
                )
                break
            }
            val rows = root["result"]
                ?.jsonObject
                ?.get("list")
                ?.jsonArray
                .orEmpty()
            if (rows.isEmpty()) break

            val parsed = rows.mapNotNull { row ->
                val values = row.jsonArray
                val openTime = values.getOrNull(0)?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
                val open = values.getOrNull(1)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val high = values.getOrNull(2)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val low = values.getOrNull(3)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val close = values.getOrNull(4)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val volume = values.getOrNull(5)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val closeTime = openTime + stepMillis - 1L
                OhlcvCandle(
                    openTime = Instant.ofEpochMilli(openTime),
                    closeTime = Instant.ofEpochMilli(closeTime),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            }
                .sortedBy { it.openTime }

            if (parsed.isEmpty()) break
            candles += parsed

            val oldestOpen = parsed.first().openTime.toEpochMilli()
            val nextCursor = oldestOpen - stepMillis
            if (nextCursor >= cursor) break
            cursor = nextCursor
            if (parsed.size < 1000) break
        }

        val normalized = candles
            .distinctBy { it.openTime }
            .sortedBy { it.openTime }
            .filter { it.openTime >= request.from && it.openTime <= request.to }
        val notes = buildDataQualityNotes(
            candles = normalized,
            timeframe = request.timeframe,
            from = request.from,
            to = request.to
        )
        return BacktestMarketData(
            candles = normalized,
            notes = notes,
            servedFromCache = false
        )
    }

    override suspend fun fetchSymbols(): List<String> {
        val headers = mapOf(
            "Accept" to "application/json",
            "User-Agent" to "TradeBuddy/1.0"
        )
        val symbols = mutableListOf<String>()
        var cursor: String? = null
        var page = 0
        do {
            page += 1
            val url = buildString {
                append("https://api.bybit.com/v5/market/instruments-info?category=spot&limit=1000")
                cursor?.takeIf { it.isNotBlank() }?.let { nextCursor ->
                    append("&cursor=").append(nextCursor)
                }
            }
            val payload = fetchWithRetry(
                url = url,
                headers = headers,
                maxAttempts = 4
            )
            val root = json.parseToJsonElement(payload).jsonObject
            val retCode = root["retCode"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            if (retCode != null && retCode != 0) {
                val retMsg = root["retMsg"]?.jsonPrimitive?.contentOrNull.orEmpty()
                error("Bybit Symbol-Endpoint retCode=$retCode ($retMsg)")
            }
            val result = root["result"]?.jsonObject
            val rows = result?.get("list")?.jsonArray.orEmpty()
            rows.forEach { node ->
                val row = node.jsonObject
                val symbol = row["symbol"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: return@forEach
                val status = row["status"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val quoteCoin = row["quoteCoin"]?.jsonPrimitive?.contentOrNull?.uppercase().orEmpty()
                if (status != "Trading") return@forEach
                if (quoteCoin !in PreferredQuoteAssets) return@forEach
                symbols += symbol
            }
            cursor = result
                ?.get("nextPageCursor")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }
        } while (cursor != null && page < 20)

        return symbols
            .mapNotNull { it.trim().uppercase().takeIf { value -> value.isNotBlank() } }
            .distinct()
            .sorted()
    }

    private suspend fun fetchWithRetry(
        url: String,
        headers: Map<String, String>,
        maxAttempts: Int
    ): String {
        var attempt = 0
        var lastError: Throwable? = null
        while (attempt < maxAttempts) {
            attempt += 1
            val result = runCatching { fetcher(url, headers) }
            if (result.isSuccess) {
                return result.getOrThrow()
            }

            val error = result.exceptionOrNull() ?: IllegalStateException("Unbekannter Netzwerkfehler")
            lastError = error
            val status = error.httpStatusCode()
            val retryable = status == 429 || status == 418 || status == 502 || status == 503 || status == 504
            if (!retryable || attempt >= maxAttempts) {
                break
            }
            val waitMillis = 400L * (1L shl (attempt - 1))
            AppLog.warn(
                tag = "BybitKlineDataProvider",
                message = "Bybit Rate-Limit/Serverfehler (HTTP $status), Retry in ${waitMillis}ms"
            )
            delay(waitMillis)
        }
        throw lastError ?: IllegalStateException("Unbekannter Bybit-Fehler")
    }
}

class OkxKlineDataProvider(
    private val fetcher: suspend (String, Map<String, String>) -> String
) : CandleDataProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchCandles(
        request: BacktestDataRequest,
        onProgress: (fraction: Float, message: String) -> Unit
    ): BacktestMarketData {
        val instId = normalizeOkxSpotSymbol(request.symbol)
        val bar = request.timeframe.okxBar()
        val stepMillis = request.timeframe.approxMinutes.toLong().milliseconds.inWholeMilliseconds
        val headers = mapOf(
            "Accept" to "application/json",
            "User-Agent" to "TradeBuddy/1.0"
        )
        val startMillis = request.from.toEpochMilli()
        val endMillis = request.to.toEpochMilli()
        val candles = mutableListOf<OhlcvCandle>()
        var afterCursor = endMillis
        var page = 0
        val maxPages = 50_000

        while (afterCursor >= startMillis && page < maxPages) {
            page += 1
            onProgress(
                (1f - ((afterCursor - startMillis).toFloat() / (endMillis - startMillis).coerceAtLeast(1L).toFloat()))
                    .coerceIn(0f, 1f),
                "OKX: Seite $page"
            )
            yield()

            val url = buildString {
                append("https://www.okx.com/api/v5/market/history-candles")
                append("?instId=").append(instId)
                append("&bar=").append(bar)
                append("&after=").append(afterCursor)
                append("&limit=300")
            }

            val payload = fetchWithRetry(
                url = url,
                headers = headers,
                maxAttempts = 4
            )
            val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrElse { error ->
                AppLog.warn(
                    tag = "OkxKlineDataProvider",
                    message = "Konnte OKX-Kline-Payload nicht parsen",
                    throwable = error
                )
                break
            }
            val code = root["code"]?.jsonPrimitive?.content
            if (!code.isNullOrBlank() && code != "0") {
                val msg = root["msg"]?.jsonPrimitive?.content.orEmpty()
                AppLog.warn(
                    tag = "OkxKlineDataProvider",
                    message = "OKX antwortete mit code=$code ($msg)"
                )
                break
            }
            val rows = root["data"]?.jsonArray.orEmpty()
            if (rows.isEmpty()) break

            val parsed = rows.mapNotNull { row ->
                val values = row.jsonArray
                val openTime = values.getOrNull(0)?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
                val open = values.getOrNull(1)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val high = values.getOrNull(2)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val low = values.getOrNull(3)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val close = values.getOrNull(4)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val volume = values.getOrNull(5)?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                val closeTime = openTime + stepMillis - 1L
                OhlcvCandle(
                    openTime = Instant.ofEpochMilli(openTime),
                    closeTime = Instant.ofEpochMilli(closeTime),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            }
                .sortedBy { it.openTime }

            if (parsed.isEmpty()) break
            candles += parsed

            val oldestOpen = parsed.first().openTime.toEpochMilli()
            val nextCursor = oldestOpen - stepMillis
            if (nextCursor >= afterCursor) break
            afterCursor = nextCursor
            if (parsed.size < 300) break
        }

        val normalized = candles
            .distinctBy { it.openTime }
            .sortedBy { it.openTime }
            .filter { it.openTime >= request.from && it.openTime <= request.to }
        val notes = buildDataQualityNotes(
            candles = normalized,
            timeframe = request.timeframe,
            from = request.from,
            to = request.to
        )
        return BacktestMarketData(
            candles = normalized,
            notes = notes,
            servedFromCache = false
        )
    }

    override suspend fun fetchSymbols(): List<String> {
        val headers = mapOf(
            "Accept" to "application/json",
            "User-Agent" to "TradeBuddy/1.0"
        )
        val payload = fetchWithRetry(
            url = "https://www.okx.com/api/v5/public/instruments?instType=SPOT",
            headers = headers,
            maxAttempts = 4
        )
        val root = json.parseToJsonElement(payload).jsonObject
        val code = root["code"]?.jsonPrimitive?.contentOrNull
        if (code != null && code != "0") {
            val message = root["msg"]?.jsonPrimitive?.contentOrNull.orEmpty()
            error("OKX Symbol-Endpoint code=$code ($message)")
        }
        val symbols = root["data"]?.jsonArray.orEmpty().mapNotNull { node ->
            val row = node.jsonObject
            val instId = row["instId"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: return@mapNotNull null
            val state = row["state"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (state != "live") return@mapNotNull null
            val parts = instId.split('-')
            if (parts.size != 2) return@mapNotNull null
            val base = parts[0]
            val quote = parts[1]
            if (quote !in PreferredQuoteAssets) return@mapNotNull null
            "$base$quote"
        }
        return symbols
            .mapNotNull { it.trim().uppercase().takeIf { value -> value.isNotBlank() } }
            .distinct()
            .sorted()
    }

    private suspend fun fetchWithRetry(
        url: String,
        headers: Map<String, String>,
        maxAttempts: Int
    ): String {
        var attempt = 0
        var lastError: Throwable? = null
        while (attempt < maxAttempts) {
            attempt += 1
            val result = runCatching { fetcher(url, headers) }
            if (result.isSuccess) {
                return result.getOrThrow()
            }

            val error = result.exceptionOrNull() ?: IllegalStateException("Unbekannter Netzwerkfehler")
            lastError = error
            val status = error.httpStatusCode()
            val retryable = status == 429 || status == 500 || status == 502 || status == 503 || status == 504
            if (!retryable || attempt >= maxAttempts) {
                break
            }
            val waitMillis = 400L * (1L shl (attempt - 1))
            AppLog.warn(
                tag = "OkxKlineDataProvider",
                message = "OKX Rate-Limit/Serverfehler (HTTP $status), Retry in ${waitMillis}ms"
            )
            delay(waitMillis)
        }
        throw lastError ?: IllegalStateException("Unbekannter OKX-Fehler")
    }
}

class DefaultBacktestingRepository(
    private val historyDocument: StorageDocument,
    private val candleCacheDocument: StorageDocument,
    private val symbolsCacheDocument: StorageDocument,
    private val providers: Map<BacktestExchange, CandleDataProvider>
) : BacktestingRepository {
    private companion object {
        const val MaxHistoryEntries = 60
        const val MaxCandleKeys = 30
        const val MaxCandlesPerKey = 120_000
        const val MaxSymbolKeys = 8
        const val SymbolsCacheTtlMillis = 24L * 60L * 60L * 1000L
    }

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    override suspend fun loadCandles(
        request: BacktestDataRequest,
        onProgress: (fraction: Float, message: String) -> Unit
    ): BacktestMarketData {
        val normalizedRequest = request.copy(symbol = request.symbol.uppercase())
        val cachePayload = readCandleCachePayload()
        val cacheKey = buildCandleCacheKey(normalizedRequest)
        val cachedEntry = cachePayload.entries.firstOrNull { it.key == cacheKey }
        val cachedCandles = cachedEntry?.candles?.map { it.toDomain() }.orEmpty()
        val cachedSubset = subsetForRange(cachedCandles, normalizedRequest.from, normalizedRequest.to)

        if (!normalizedRequest.forceRefresh &&
            cachedSubset.isNotEmpty() &&
            isRangeCovered(cachedCandles, normalizedRequest)
        ) {
            onProgress(1f, "Kerzen aus Cache")
            return BacktestMarketData(
                candles = cachedSubset,
                notes = buildDataQualityNotes(cachedSubset, normalizedRequest.timeframe, normalizedRequest.from, normalizedRequest.to),
                servedFromCache = true
            )
        }

        val provider = providers[normalizedRequest.exchange]
            ?: error("Kein Datenprovider für ${normalizedRequest.exchange.id} konfiguriert")
        onProgress(0.02f, "Marktdaten werden geladen")
        val fetched = provider.fetchCandles(normalizedRequest) { progress, message ->
            onProgress(progress.coerceIn(0f, 1f), message)
        }

        if (fetched.candles.isEmpty() && cachedSubset.isNotEmpty()) {
            AppLog.warn(
                tag = "BacktestingRepository",
                message = "Remote lieferte keine Kerzen. Fallback auf Cache für $cacheKey."
            )
            return BacktestMarketData(
                candles = cachedSubset,
                notes = fetched.notes + "Fallback auf lokale Cache-Daten",
                servedFromCache = true
            )
        }

        val merged = mergeCandles(cachedCandles, fetched.candles)
        val inRange = subsetForRange(merged, normalizedRequest.from, normalizedRequest.to)
        writeCandleCachePayload(
            current = cachePayload,
            key = cacheKey,
            candles = merged
        )

        return fetched.copy(
            candles = inRange,
            notes = (fetched.notes + buildDataQualityNotes(inRange, normalizedRequest.timeframe, normalizedRequest.from, normalizedRequest.to))
                .distinct()
        )
    }

    override suspend fun loadSymbols(
        exchange: BacktestExchange,
        forceRefresh: Boolean
    ): List<String> {
        val normalizedExchange = exchange
        val cachePayload = readSymbolsCachePayload()
        val cacheEntry = cachePayload.entries.firstOrNull { it.exchange == normalizedExchange.id }
        val cachedSymbols = cacheEntry
            ?.symbols
            .orEmpty()
            .normalizeSymbolList()
        val isFresh = cacheEntry != null &&
            (Instant.now().toEpochMilli() - cacheEntry.updatedAtEpochMillis) <= SymbolsCacheTtlMillis

        if (!forceRefresh && cachedSymbols.isNotEmpty() && isFresh) {
            return cachedSymbols
        }

        val provider = providers[normalizedExchange]
        if (provider == null) {
            return cachedSymbols.ifEmpty { fallbackSymbolsForExchange(normalizedExchange) }
        }

        val fetchedSymbols = runCatching { provider.fetchSymbols() }
            .onFailure { error ->
                AppLog.warn(
                    tag = "BacktestingRepository",
                    message = "Symbol-Liste konnte nicht geladen werden fuer ${normalizedExchange.id}",
                    throwable = error
                )
            }
            .getOrDefault(emptyList())
            .normalizeSymbolList()

        if (fetchedSymbols.isNotEmpty()) {
            writeSymbolsCachePayload(
                current = cachePayload,
                exchange = normalizedExchange,
                symbols = fetchedSymbols
            )
            return fetchedSymbols
        }

        if (cachedSymbols.isNotEmpty()) {
            AppLog.warn(
                tag = "BacktestingRepository",
                message = "Symbol-Liste fuer ${normalizedExchange.id} aus Cache genutzt (Remote leer)"
            )
            return cachedSymbols
        }

        return fallbackSymbolsForExchange(normalizedExchange)
    }

    override suspend fun loadHistory(): List<BacktestHistoryRun> {
        val payload = readHistoryPayload()
        return payload.entries
            .sortedByDescending { it.createdAtEpochMillis }
            .mapNotNull { it.toDomainOrNull() }
    }

    override suspend fun saveHistoryRun(run: BacktestHistoryRun) {
        val payload = readHistoryPayload()
        val next = payload.entries
            .filterNot { it.id == run.id }
            .plus(run.toDto())
            .sortedByDescending { it.createdAtEpochMillis }
            .take(MaxHistoryEntries)
        historyDocument.writeText(
            json.encodeToString(
                BacktestHistoryPayload.serializer(),
                BacktestHistoryPayload(entries = next)
            )
        )
    }

    override suspend fun importCandlesCsv(
        exchange: BacktestExchange,
        symbol: String,
        timeframe: BacktestTimeframe,
        csvContent: String
    ): Int {
        val rows = csvContent
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (rows.isEmpty()) return 0

        val delimiter = detectCsvDelimiter(rows.first())
        val hasHeader = looksLikeCsvHeader(rows.first())
        val dataRows = if (hasHeader) rows.drop(1) else rows
        val stepMillis = timeframe.approxMinutes.toLong().milliseconds.inWholeMilliseconds

        val imported = dataRows.mapNotNull { row ->
            val cols = row.split(delimiter).map { it.trim().trim('"') }
            if (cols.size < 5) return@mapNotNull null

            val openTime = parseCsvTimestamp(cols.getOrNull(0)) ?: return@mapNotNull null
            val open = cols.getOrNull(1)?.parseCsvDouble() ?: return@mapNotNull null
            val high = cols.getOrNull(2)?.parseCsvDouble() ?: return@mapNotNull null
            val low = cols.getOrNull(3)?.parseCsvDouble() ?: return@mapNotNull null
            val close = cols.getOrNull(4)?.parseCsvDouble() ?: return@mapNotNull null
            val volume = cols.getOrNull(5)?.parseCsvDouble() ?: 0.0
            val closeTime = parseCsvTimestamp(cols.getOrNull(6))
                ?: Instant.ofEpochMilli(openTime.toEpochMilli() + stepMillis - 1L)

            OhlcvCandle(
                openTime = openTime,
                closeTime = closeTime,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = volume
            )
        }
            .distinctBy { it.openTime }
            .sortedBy { it.openTime }

        if (imported.isEmpty()) {
            AppLog.warn(
                tag = "BacktestingRepository",
                message = "CSV-Import enthielt keine verwertbaren Kerzen"
            )
            return 0
        }

        val normalizedSymbol = symbol.trim().uppercase()
        val key = "${exchange.id}|$normalizedSymbol|${timeframe.id}"
        val payload = readCandleCachePayload()
        val cached = payload.entries
            .firstOrNull { it.key == key }
            ?.candles
            ?.map { it.toDomain() }
            .orEmpty()

        val merged = mergeCandles(cached = cached, fetched = imported)
        writeCandleCachePayload(
            current = payload,
            key = key,
            candles = merged
        )

        return imported.size
    }

    override suspend fun deleteHistoryRun(runId: String) {
        val payload = readHistoryPayload()
        val next = payload.entries.filterNot { it.id == runId }
        historyDocument.writeText(
            json.encodeToString(
                BacktestHistoryPayload.serializer(),
                payload.copy(entries = next, updatedAtEpochMillis = Instant.now().toEpochMilli())
            )
        )
    }

    override suspend fun clearHistory() {
        historyDocument.clear()
    }

    private suspend fun readHistoryPayload(): BacktestHistoryPayload {
        val raw = historyDocument.readText() ?: return BacktestHistoryPayload()
        return runCatching { json.decodeFromString(BacktestHistoryPayload.serializer(), raw) }
            .getOrElse { BacktestHistoryPayload() }
    }

    private suspend fun readCandleCachePayload(): BacktestCandleCachePayload {
        val raw = candleCacheDocument.readText() ?: return BacktestCandleCachePayload()
        return runCatching { json.decodeFromString(BacktestCandleCachePayload.serializer(), raw) }
            .getOrElse { BacktestCandleCachePayload() }
    }

    private suspend fun readSymbolsCachePayload(): BacktestSymbolCachePayload {
        val raw = symbolsCacheDocument.readText() ?: return BacktestSymbolCachePayload()
        return runCatching { json.decodeFromString(BacktestSymbolCachePayload.serializer(), raw) }
            .getOrElse { BacktestSymbolCachePayload() }
    }

    private suspend fun writeCandleCachePayload(
        current: BacktestCandleCachePayload,
        key: String,
        candles: List<OhlcvCandle>
    ) {
        val normalizedCandles = candles
            .sortedBy { it.openTime }
            .distinctBy { it.openTime }
            .takeLast(MaxCandlesPerKey)
            .map { it.toDto() }
        val merged = current.entries
            .filterNot { it.key == key }
            .plus(
                BacktestCandleCacheEntryDto(
                    key = key,
                    updatedAtEpochMillis = Instant.now().toEpochMilli(),
                    candles = normalizedCandles
                )
            )
            .sortedByDescending { it.updatedAtEpochMillis }
            .take(MaxCandleKeys)
        candleCacheDocument.writeText(
            json.encodeToString(
                BacktestCandleCachePayload.serializer(),
                BacktestCandleCachePayload(entries = merged)
            )
        )
    }

    private suspend fun writeSymbolsCachePayload(
        current: BacktestSymbolCachePayload,
        exchange: BacktestExchange,
        symbols: List<String>
    ) {
        val merged = current.entries
            .filterNot { it.exchange == exchange.id }
            .plus(
                BacktestSymbolCacheEntryDto(
                    exchange = exchange.id,
                    updatedAtEpochMillis = Instant.now().toEpochMilli(),
                    symbols = symbols.normalizeSymbolList()
                )
            )
            .sortedByDescending { it.updatedAtEpochMillis }
            .take(MaxSymbolKeys)
        symbolsCacheDocument.writeText(
            json.encodeToString(
                BacktestSymbolCachePayload.serializer(),
                BacktestSymbolCachePayload(entries = merged)
            )
        )
    }
}

private fun mergeCandles(
    cached: List<OhlcvCandle>,
    fetched: List<OhlcvCandle>
): List<OhlcvCandle> =
    (cached + fetched)
        .sortedBy { it.openTime }
        .distinctBy { it.openTime }

private fun subsetForRange(
    candles: List<OhlcvCandle>,
    from: Instant,
    to: Instant
): List<OhlcvCandle> = candles.filter { it.openTime >= from && it.openTime <= to }

private fun isRangeCovered(
    candles: List<OhlcvCandle>,
    request: BacktestDataRequest
): Boolean {
    if (candles.isEmpty()) return false
    val first = candles.first().openTime
    val last = candles.last().openTime
    if (first > request.from || last < request.to) return false

    val stepMillis = request.timeframe.approxMinutes.toLong().milliseconds.inWholeMilliseconds
    val relevant = subsetForRange(candles, request.from, request.to)
    if (relevant.size < 2) return false
    val largeGaps = relevant
        .zipWithNext()
        .count { (a, b) ->
            val gap = b.openTime.toEpochMilli() - a.openTime.toEpochMilli()
            gap > stepMillis * 3
        }
    return largeGaps == 0
}

private fun buildDataQualityNotes(
    candles: List<OhlcvCandle>,
    timeframe: BacktestTimeframe,
    from: Instant,
    to: Instant
): List<String> {
    if (candles.isEmpty()) return listOf("Keine Kerzendaten für den gewählten Zeitraum")
    val stepMillis = timeframe.approxMinutes.toLong().milliseconds.inWholeMilliseconds
    val expectedBars = ((to.toEpochMilli() - from.toEpochMilli()) / stepMillis).coerceAtLeast(1L)
    val gapCount = candles.zipWithNext().count { (a, b) ->
        val gap = b.openTime.toEpochMilli() - a.openTime.toEpochMilli()
        gap > stepMillis * 2
    }
    val notes = mutableListOf<String>()
    if (gapCount > 0) {
        notes += "Datenlücken erkannt: $gapCount größere Kerzenabstände"
    }
    if (candles.size < expectedBars * 0.7) {
        notes += "Unvollständige Historie: ${candles.size} Kerzen für erwartete ca. $expectedBars"
    }
    val duplicates = candles
        .groupBy { it.openTime }
        .count { (_, rows) -> rows.size > 1 }
    if (duplicates > 0) {
        notes += "Doppelte Kerzen bereinigt: $duplicates"
    }
    return notes.distinct()
}

private fun buildCandleCacheKey(request: BacktestDataRequest): String =
    "${request.exchange.id}|${request.symbol.uppercase()}|${request.timeframe.id}"

private fun List<String>.normalizeSymbolList(): List<String> =
    mapNotNull { value -> value.trim().uppercase().takeIf { it.isNotBlank() } }
        .distinct()
        .sorted()

private fun fallbackSymbolsForExchange(exchange: BacktestExchange): List<String> {
    val core = listOf(
        "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT", "ADAUSDT", "DOGEUSDT", "TRXUSDT",
        "TONUSDT", "DOTUSDT", "AVAXUSDT", "LINKUSDT", "MATICUSDT", "LTCUSDT", "BCHUSDT", "ETCUSDT",
        "XLMUSDT", "ATOMUSDT", "NEARUSDT", "UNIUSDT", "APTUSDT", "ARBUSDT", "OPUSDT", "FILUSDT",
        "INJUSDT", "AAVEUSDT", "SUIUSDT", "PEPEUSDT", "SHIBUSDT", "FETUSDT", "RNDRUSDT", "ICPUSDT"
    )
    val extra = when (exchange) {
        BacktestExchange.BinanceSpot -> listOf("1000PEPEUSDT", "WLDUSDT", "PYTHUSDT")
        BacktestExchange.BybitSpot -> listOf("ENAUSDT", "ONDOUSDT", "PENDLEUSDT")
        BacktestExchange.OkxSpot -> listOf("STXUSDT", "KASUSDT", "TAOUSDT")
    }
    return (core + extra).normalizeSymbolList()
}

private fun BacktestTimeframe.bybitInterval(): String = when (this) {
    BacktestTimeframe.M1 -> "1"
    BacktestTimeframe.M5 -> "5"
    BacktestTimeframe.M15 -> "15"
    BacktestTimeframe.H1 -> "60"
    BacktestTimeframe.H4 -> "240"
    BacktestTimeframe.D1 -> "D"
}

private fun BacktestTimeframe.okxBar(): String = when (this) {
    BacktestTimeframe.M1 -> "1m"
    BacktestTimeframe.M5 -> "5m"
    BacktestTimeframe.M15 -> "15m"
    BacktestTimeframe.H1 -> "1H"
    BacktestTimeframe.H4 -> "4H"
    BacktestTimeframe.D1 -> "1Dutc"
}

private fun normalizeOkxSpotSymbol(symbol: String): String {
    val normalized = symbol.trim().uppercase()
    if ('-' in normalized) return normalized

    val quoteCandidates = listOf(
        "USDT", "USDC", "BTC", "ETH", "EUR", "USD", "TRY", "BRL"
    )
    val quote = quoteCandidates.firstOrNull { normalized.endsWith(it) }
        ?: return normalized
    val base = normalized.removeSuffix(quote).ifBlank { return normalized }
    return "$base-$quote"
}

private fun Throwable.httpStatusCode(): Int? =
    Regex("""HTTP\s+(\d{3})""")
        .find(message.orEmpty())
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()

private fun detectCsvDelimiter(sampleLine: String): Char {
    val comma = sampleLine.count { it == ',' }
    val semicolon = sampleLine.count { it == ';' }
    val tab = sampleLine.count { it == '\t' }
    return when {
        semicolon > comma && semicolon >= tab -> ';'
        tab > comma && tab > semicolon -> '\t'
        else -> ','
    }
}

private fun looksLikeCsvHeader(sampleLine: String): Boolean {
    val normalized = sampleLine.lowercase()
    return normalized.contains("open") ||
        normalized.contains("close") ||
        normalized.contains("timestamp") ||
        normalized.contains("time")
}

private fun parseCsvTimestamp(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null
    val token = raw.trim().trim('"')
    val numeric = token.toLongOrNull()
    if (numeric != null) {
        return when {
            numeric > 2_000_000_000_000L -> Instant.ofEpochMilli(numeric)
            numeric > 2_000_000_000L -> Instant.ofEpochMilli(numeric * 1_000L)
            else -> null
        }
    }
    val normalized = token.removeSuffix("Z")
    val dateTimeMatch = Regex(
        """^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}):(\d{2})(?::(\d{2}))?(?:\.\d+)?$"""
    ).matchEntire(normalized) ?: return null
    val seconds = dateTimeMatch.groupValues[6].ifBlank { "0" }.toIntOrNull() ?: 0
    return runCatching {
        LocalDateTime.of(
            dateTimeMatch.groupValues[1].toInt(),
            dateTimeMatch.groupValues[2].toInt(),
            dateTimeMatch.groupValues[3].toInt(),
            dateTimeMatch.groupValues[4].toInt(),
            dateTimeMatch.groupValues[5].toInt(),
            seconds
        ).atZone(ZoneOffset.UTC).toInstant()
    }.getOrNull()
}

private fun String.parseCsvDouble(): Double? =
    trim()
        .replace(',', '.')
        .toDoubleOrNull()

@Serializable
private data class BacktestHistoryPayload(
    val schemaVersion: Int = 1,
    val updatedAtEpochMillis: Long = Instant.now().toEpochMilli(),
    val entries: List<BacktestHistoryRunDto> = emptyList()
)

@Serializable
private data class BacktestCandleCachePayload(
    val schemaVersion: Int = 1,
    val updatedAtEpochMillis: Long = Instant.now().toEpochMilli(),
    val entries: List<BacktestCandleCacheEntryDto> = emptyList()
)

@Serializable
private data class BacktestSymbolCachePayload(
    val schemaVersion: Int = 1,
    val updatedAtEpochMillis: Long = Instant.now().toEpochMilli(),
    val entries: List<BacktestSymbolCacheEntryDto> = emptyList()
)

@Serializable
private data class BacktestCandleCacheEntryDto(
    val key: String,
    val updatedAtEpochMillis: Long,
    val candles: List<OhlcvCandleDto>
)

@Serializable
private data class BacktestSymbolCacheEntryDto(
    val exchange: String,
    val updatedAtEpochMillis: Long,
    val symbols: List<String>
)

@Serializable
private data class OhlcvCandleDto(
    val openTimeEpochMillis: Long,
    val closeTimeEpochMillis: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

@Serializable
private data class BacktestHistoryRunDto(
    val id: String,
    val title: String,
    val createdAtEpochMillis: Long,
    val request: BacktestRunRequestDto,
    val result: BacktestResultDto
)

@Serializable
private data class BacktestRunRequestDto(
    val exchange: String,
    val symbol: String,
    val timeframe: String,
    val fromEpochMillis: Long,
    val toEpochMillis: Long,
    val warmupBars: Int = 200,
    val forceRefresh: Boolean,
    val strategy: BacktestStrategyConfigDto,
    val execution: BacktestExecutionSettingsDto
)

@Serializable
private data class BacktestStrategyConfigDto(
    val template: String,
    val smaFastPeriod: Int,
    val smaSlowPeriod: Int,
    val rsiPeriod: Int,
    val rsiOversold: Double,
    val rsiOverbought: Double,
    val breakoutLookback: Int,
    val atrPeriod: Int = 14,
    val astroCityKey: String = "",
    val astroSignalWindowBars: Int = 1,
    val astroIncludeSunEvents: Boolean = true,
    val astroIncludeMoonEvents: Boolean = true,
    val astroIncludeMoonPhases: Boolean = true,
    val astroIncludeAspects: Boolean = true,
    val astroAspectsMoonOnly: Boolean = true,
    val customStrategyJson: String,
    val showSignalPreview: Boolean
)

@Serializable
private data class BacktestExecutionSettingsDto(
    val initialBalance: Double,
    val sizingMode: String,
    val fixedPositionAmount: Double,
    val percentOfEquity: Double,
    val atrRiskPercent: Double = 1.0,
    val targetVolatilityPercent: Double = 15.0,
    val makerFeeBps: Double? = null,
    val takerFeeBps: Double? = null,
    val feeBps: Double? = null,
    val slippageBps: Double,
    val orderType: String = BacktestOrderType.Market.name,
    val limitOffsetBps: Double = 2.0,
    val fillRatioPercent: Double = 100.0,
    val maxVolumeParticipationPercent: Double = 25.0,
    val allowShort: Boolean,
    val allowHedging: Boolean = false,
    val leverage: Double = 1.0,
    val marginMode: String = BacktestMarginMode.Cash.name,
    val fundingBpsPerDay: Double = 0.0,
    val stopLossPercent: Double?,
    val takeProfitPercent: Double?,
    val trailingStopPercent: Double?,
    val killSwitchMaxDrawdownPercent: Double? = null,
    val dailyLossLimitPercent: Double? = null,
    val maxConcurrentPositions: Int = 1
)

@Serializable
private data class BacktestResultDto(
    val runId: String,
    val candlesCount: Int,
    val candleSeries: List<OhlcvCandleDto> = emptyList(),
    val trades: List<BacktestTradeDto>,
    val priceCurve: List<BacktestPricePointDto> = emptyList(),
    val equityCurve: List<BacktestEquityPointDto>,
    val drawdownCurve: List<BacktestEquityPointDto>,
    val signalPoints: List<StrategySignalPointDto>,
    val metrics: BacktestMetricsDto,
    val analysis: BacktestAnalysisDto? = null,
    val edgeValidation: BacktestEdgeValidationDto? = null,
    val edgeGateDecision: BacktestEdgeGateDecisionDto? = null,
    val dataNotes: List<String>,
    val createdAtEpochMillis: Long
)

@Serializable
private data class BacktestAnalysisDto(
    val monthlyReturns: List<BacktestMonthlyReturnDto> = emptyList(),
    val walkForward: BacktestWalkForwardResultDto? = null,
    val monteCarlo: BacktestMonteCarloResultDto? = null
)

@Serializable
private data class BacktestEdgeValidationDto(
    val status: String,
    val edgeScore: Double,
    val sampleCount: Int,
    val cpcvSplits: Int,
    val cpcvPaths: Int,
    val oosSharpe: Double? = null,
    val oosReturnPercent: Double? = null,
    val oosPositivePathPercent: Double? = null,
    val spaPValue: Double? = null,
    val notes: List<String> = emptyList(),
    val generatedAtEpochMillis: Long
)

@Serializable
private data class BacktestEdgeGateDecisionDto(
    val enabled: Boolean,
    val passed: Boolean,
    val minEdgeScore: Double,
    val maxSpaPValue: Double,
    val minPositivePathsPercent: Double,
    val minTrades: Int,
    val minOosSharpe: Double,
    val requirePassedStatus: Boolean,
    val reasons: List<String> = emptyList(),
    val evaluatedAtEpochMillis: Long
)

@Serializable
private data class BacktestMonthlyReturnDto(
    val year: Int,
    val month: Int,
    val returnPercent: Double
)

@Serializable
private data class BacktestWalkForwardResultDto(
    val splitCount: Int,
    val inSampleAverageReturnPercent: Double,
    val outOfSampleAverageReturnPercent: Double,
    val stabilityScorePercent: Double
)

@Serializable
private data class BacktestMonteCarloResultDto(
    val iterations: Int,
    val medianReturnPercent: Double,
    val p05ReturnPercent: Double,
    val p95ReturnPercent: Double
)

@Serializable
private data class BacktestTradeDto(
    val id: String,
    val symbol: String? = null,
    val side: String,
    val entryTimeEpochMillis: Long,
    val exitTimeEpochMillis: Long,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val durationMinutes: Long,
    val maePercent: Double = 0.0,
    val mfePercent: Double = 0.0,
    val feesPaid: Double = 0.0,
    val fundingPaid: Double = 0.0,
    val exitReason: String
)

@Serializable
private data class BacktestEquityPointDto(
    val timeEpochMillis: Long,
    val equity: Double,
    val drawdownPercent: Double
)

@Serializable
private data class BacktestPricePointDto(
    val timeEpochMillis: Long,
    val price: Double
)

@Serializable
private data class StrategySignalPointDto(
    val symbol: String? = null,
    val timeEpochMillis: Long,
    val price: Double,
    val signal: String
)

@Serializable
private data class BacktestMetricsDto(
    val winRatePercent: Double,
    val totalReturnPercent: Double,
    val cagrPercent: Double?,
    val maxDrawdownPercent: Double,
    val sharpeRatio: Double,
    val sortinoRatio: Double = 0.0,
    val profitFactor: Double,
    val expectancyPercent: Double = 0.0,
    val annualVolatilityPercent: Double = 0.0,
    val tradeCount: Int,
    val averageTradePercent: Double,
    val exposureTimePercent: Double,
    val startEquity: Double,
    val endEquity: Double
)

private fun OhlcvCandle.toDto(): OhlcvCandleDto = OhlcvCandleDto(
    openTimeEpochMillis = openTime.toEpochMilli(),
    closeTimeEpochMillis = closeTime.toEpochMilli(),
    open = open,
    high = high,
    low = low,
    close = close,
    volume = volume
)

private fun OhlcvCandleDto.toDomain(): OhlcvCandle = OhlcvCandle(
    openTime = Instant.ofEpochMilli(openTimeEpochMillis),
    closeTime = Instant.ofEpochMilli(closeTimeEpochMillis),
    open = open,
    high = high,
    low = low,
    close = close,
    volume = volume
)

private fun BacktestHistoryRun.toDto(): BacktestHistoryRunDto = BacktestHistoryRunDto(
    id = id,
    title = title,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    request = request.toDto(),
    result = result.toDto()
)

private fun BacktestRunRequest.toDto(): BacktestRunRequestDto = BacktestRunRequestDto(
    exchange = data.exchange.name,
    symbol = data.symbol,
    timeframe = data.timeframe.name,
    fromEpochMillis = data.from.toEpochMilli(),
    toEpochMillis = data.to.toEpochMilli(),
    warmupBars = data.warmupBars,
    forceRefresh = data.forceRefresh,
    strategy = strategy.toDto(),
    execution = execution.toDto()
)

private fun BacktestStrategyConfig.toDto(): BacktestStrategyConfigDto = BacktestStrategyConfigDto(
    template = template.name,
    smaFastPeriod = smaFastPeriod,
    smaSlowPeriod = smaSlowPeriod,
    rsiPeriod = rsiPeriod,
    rsiOversold = rsiOversold,
    rsiOverbought = rsiOverbought,
    breakoutLookback = breakoutLookback,
    atrPeriod = atrPeriod,
    astroCityKey = astroCityKey,
    astroSignalWindowBars = astroSignalWindowBars,
    astroIncludeSunEvents = astroIncludeSunEvents,
    astroIncludeMoonEvents = astroIncludeMoonEvents,
    astroIncludeMoonPhases = astroIncludeMoonPhases,
    astroIncludeAspects = astroIncludeAspects,
    astroAspectsMoonOnly = astroAspectsMoonOnly,
    customStrategyJson = customStrategyJson,
    showSignalPreview = showSignalPreview
)

private fun BacktestExecutionSettings.toDto(): BacktestExecutionSettingsDto = BacktestExecutionSettingsDto(
    initialBalance = initialBalance,
    sizingMode = sizingMode.name,
    fixedPositionAmount = fixedPositionAmount,
    percentOfEquity = percentOfEquity,
    atrRiskPercent = atrRiskPercent,
    targetVolatilityPercent = targetVolatilityPercent,
    makerFeeBps = makerFeeBps,
    takerFeeBps = takerFeeBps,
    slippageBps = slippageBps,
    orderType = orderType.name,
    limitOffsetBps = limitOffsetBps,
    fillRatioPercent = fillRatioPercent,
    maxVolumeParticipationPercent = maxVolumeParticipationPercent,
    allowShort = allowShort,
    allowHedging = allowHedging,
    leverage = leverage,
    marginMode = marginMode.name,
    fundingBpsPerDay = fundingBpsPerDay,
    stopLossPercent = stopLossPercent,
    takeProfitPercent = takeProfitPercent,
    trailingStopPercent = trailingStopPercent,
    killSwitchMaxDrawdownPercent = killSwitchMaxDrawdownPercent,
    dailyLossLimitPercent = dailyLossLimitPercent,
    maxConcurrentPositions = maxConcurrentPositions
)

private fun BacktestResult.toDto(): BacktestResultDto = BacktestResultDto(
    runId = runId,
    candlesCount = candlesCount,
    candleSeries = candleSeries.map { it.toDto() },
    trades = trades.map { it.toDto() },
    priceCurve = priceCurve.map { it.toDto() },
    equityCurve = equityCurve.map { it.toDto() },
    drawdownCurve = drawdownCurve.map { it.toDto() },
    signalPoints = signalPoints.map { it.toDto() },
    metrics = metrics.toDto(),
    analysis = analysis?.toDto(),
    edgeValidation = edgeValidation?.toDto(),
    edgeGateDecision = edgeGateDecision?.toDto(),
    dataNotes = dataNotes,
    createdAtEpochMillis = createdAt.toEpochMilli()
)

private fun BacktestTrade.toDto(): BacktestTradeDto = BacktestTradeDto(
    id = id,
    symbol = symbol,
    side = side.name,
    entryTimeEpochMillis = entryTime.toEpochMilli(),
    exitTimeEpochMillis = exitTime.toEpochMilli(),
    entryPrice = entryPrice,
    exitPrice = exitPrice,
    quantity = quantity,
    pnl = pnl,
    pnlPercent = pnlPercent,
    durationMinutes = durationMinutes,
    maePercent = maePercent,
    mfePercent = mfePercent,
    feesPaid = feesPaid,
    fundingPaid = fundingPaid,
    exitReason = exitReason.name
)

private fun BacktestEquityPoint.toDto(): BacktestEquityPointDto = BacktestEquityPointDto(
    timeEpochMillis = time.toEpochMilli(),
    equity = equity,
    drawdownPercent = drawdownPercent
)

private fun BacktestPricePoint.toDto(): BacktestPricePointDto = BacktestPricePointDto(
    timeEpochMillis = time.toEpochMilli(),
    price = price
)

private fun StrategySignalPoint.toDto(): StrategySignalPointDto = StrategySignalPointDto(
    symbol = symbol,
    timeEpochMillis = time.toEpochMilli(),
    price = price,
    signal = signal.name
)

private fun BacktestMetrics.toDto(): BacktestMetricsDto = BacktestMetricsDto(
    winRatePercent = winRatePercent,
    totalReturnPercent = totalReturnPercent,
    cagrPercent = cagrPercent,
    maxDrawdownPercent = maxDrawdownPercent,
    sharpeRatio = sharpeRatio,
    sortinoRatio = sortinoRatio,
    profitFactor = profitFactor,
    expectancyPercent = expectancyPercent,
    annualVolatilityPercent = annualVolatilityPercent,
    tradeCount = tradeCount,
    averageTradePercent = averageTradePercent,
    exposureTimePercent = exposureTimePercent,
    startEquity = startEquity,
    endEquity = endEquity
)

private fun BacktestAnalysis.toDto(): BacktestAnalysisDto = BacktestAnalysisDto(
    monthlyReturns = monthlyReturns.map { it.toDto() },
    walkForward = walkForward?.toDto(),
    monteCarlo = monteCarlo?.toDto()
)

private fun BacktestEdgeValidation.toDto(): BacktestEdgeValidationDto = BacktestEdgeValidationDto(
    status = status.name,
    edgeScore = edgeScore,
    sampleCount = sampleCount,
    cpcvSplits = cpcvSplits,
    cpcvPaths = cpcvPaths,
    oosSharpe = oosSharpe,
    oosReturnPercent = oosReturnPercent,
    oosPositivePathPercent = oosPositivePathPercent,
    spaPValue = spaPValue,
    notes = notes,
    generatedAtEpochMillis = generatedAt.toEpochMilli()
)

private fun BacktestEdgeGateDecision.toDto(): BacktestEdgeGateDecisionDto = BacktestEdgeGateDecisionDto(
    enabled = enabled,
    passed = passed,
    minEdgeScore = minEdgeScore,
    maxSpaPValue = maxSpaPValue,
    minPositivePathsPercent = minPositivePathsPercent,
    minTrades = minTrades,
    minOosSharpe = minOosSharpe,
    requirePassedStatus = requirePassedStatus,
    reasons = reasons,
    evaluatedAtEpochMillis = evaluatedAt.toEpochMilli()
)

private fun BacktestMonthlyReturn.toDto(): BacktestMonthlyReturnDto = BacktestMonthlyReturnDto(
    year = year,
    month = month,
    returnPercent = returnPercent
)

private fun BacktestWalkForwardResult.toDto(): BacktestWalkForwardResultDto = BacktestWalkForwardResultDto(
    splitCount = splitCount,
    inSampleAverageReturnPercent = inSampleAverageReturnPercent,
    outOfSampleAverageReturnPercent = outOfSampleAverageReturnPercent,
    stabilityScorePercent = stabilityScorePercent
)

private fun BacktestMonteCarloResult.toDto(): BacktestMonteCarloResultDto = BacktestMonteCarloResultDto(
    iterations = iterations,
    medianReturnPercent = medianReturnPercent,
    p05ReturnPercent = p05ReturnPercent,
    p95ReturnPercent = p95ReturnPercent
)

private fun BacktestHistoryRunDto.toDomainOrNull(): BacktestHistoryRun? {
    val runRequest = request.toDomainOrNull() ?: return null
    val runResult = result.toDomainOrNull(runRequest) ?: return null
    return BacktestHistoryRun(
        id = id,
        title = title,
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        request = runRequest,
        result = runResult
    )
}

private fun BacktestRunRequestDto.toDomainOrNull(): BacktestRunRequest? {
    val exchangeEnum = runCatching { BacktestExchange.valueOf(exchange) }.getOrNull() ?: return null
    val timeframeEnum = runCatching { BacktestTimeframe.valueOf(timeframe) }.getOrNull() ?: return null
    return BacktestRunRequest(
        data = BacktestDataRequest(
            exchange = exchangeEnum,
            symbol = symbol,
            timeframe = timeframeEnum,
            from = Instant.ofEpochMilli(fromEpochMillis),
            to = Instant.ofEpochMilli(toEpochMillis),
            warmupBars = warmupBars.coerceAtLeast(0),
            forceRefresh = forceRefresh
        ),
        strategy = strategy.toDomainOrNull() ?: return null,
        execution = execution.toDomainOrNull() ?: return null
    )
}

private fun BacktestStrategyConfigDto.toDomainOrNull(): BacktestStrategyConfig? =
    BacktestStrategyConfig(
        template = runCatching { BacktestStrategyTemplate.valueOf(template) }.getOrNull() ?: return null,
        smaFastPeriod = smaFastPeriod,
        smaSlowPeriod = smaSlowPeriod,
        rsiPeriod = rsiPeriod,
        rsiOversold = rsiOversold,
        rsiOverbought = rsiOverbought,
        breakoutLookback = breakoutLookback,
        atrPeriod = atrPeriod,
        astroCityKey = astroCityKey,
        astroSignalWindowBars = astroSignalWindowBars.coerceAtLeast(0),
        astroIncludeSunEvents = astroIncludeSunEvents,
        astroIncludeMoonEvents = astroIncludeMoonEvents,
        astroIncludeMoonPhases = astroIncludeMoonPhases,
        astroIncludeAspects = astroIncludeAspects,
        astroAspectsMoonOnly = astroAspectsMoonOnly,
        customStrategyJson = customStrategyJson,
        showSignalPreview = showSignalPreview
    )

private fun BacktestExecutionSettingsDto.toDomainOrNull(): BacktestExecutionSettings? =
    BacktestExecutionSettings(
        initialBalance = initialBalance,
        sizingMode = runCatching { BacktestPositionSizingMode.valueOf(sizingMode) }.getOrNull() ?: return null,
        fixedPositionAmount = fixedPositionAmount,
        percentOfEquity = percentOfEquity,
        atrRiskPercent = atrRiskPercent,
        targetVolatilityPercent = targetVolatilityPercent,
        makerFeeBps = makerFeeBps ?: feeBps ?: 10.0,
        takerFeeBps = takerFeeBps ?: feeBps ?: 10.0,
        slippageBps = slippageBps,
        orderType = runCatching { BacktestOrderType.valueOf(orderType) }.getOrElse { BacktestOrderType.Market },
        limitOffsetBps = limitOffsetBps,
        fillRatioPercent = fillRatioPercent,
        maxVolumeParticipationPercent = maxVolumeParticipationPercent,
        allowShort = allowShort,
        allowHedging = allowHedging,
        leverage = leverage.coerceAtLeast(1.0),
        marginMode = runCatching { BacktestMarginMode.valueOf(marginMode) }.getOrElse { BacktestMarginMode.Cash },
        fundingBpsPerDay = fundingBpsPerDay,
        stopLossPercent = stopLossPercent,
        takeProfitPercent = takeProfitPercent,
        trailingStopPercent = trailingStopPercent,
        killSwitchMaxDrawdownPercent = killSwitchMaxDrawdownPercent,
        dailyLossLimitPercent = dailyLossLimitPercent,
        maxConcurrentPositions = maxConcurrentPositions.coerceAtLeast(1)
    )

private fun BacktestResultDto.toDomainOrNull(request: BacktestRunRequest): BacktestResult? {
    val tradesDomain = trades.mapNotNull { it.toDomainOrNull(defaultSymbol = request.data.symbol) }
    val candleSeriesDomain = candleSeries.map { it.toDomain() }
    val priceCurveDomain = priceCurve.map { it.toDomain() }
    val equityDomain = equityCurve.map { it.toDomain() }
    val drawdownDomain = drawdownCurve.map { it.toDomain() }
    val signalPointsDomain = signalPoints.mapNotNull { it.toDomainOrNull(defaultSymbol = request.data.symbol) }
    val metricsDomain = metrics.toDomain()
    if (trades.size != tradesDomain.size || signalPoints.size != signalPointsDomain.size) {
        return null
    }
    return BacktestResult(
        runId = runId,
        request = request,
        candlesCount = candlesCount,
        candleSeries = candleSeriesDomain,
        trades = tradesDomain,
        priceCurve = priceCurveDomain,
        equityCurve = equityDomain,
        drawdownCurve = drawdownDomain,
        signalPoints = signalPointsDomain,
        metrics = metricsDomain,
        analysis = analysis?.toDomainOrNull(),
        edgeValidation = edgeValidation?.toDomainOrNull(),
        edgeGateDecision = edgeGateDecision?.toDomainOrNull(),
        dataNotes = dataNotes,
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis)
    )
}

private fun BacktestTradeDto.toDomainOrNull(defaultSymbol: String): BacktestTrade? = BacktestTrade(
    id = id,
    symbol = symbol ?: defaultSymbol,
    side = runCatching { BacktestTradeSide.valueOf(side) }.getOrNull() ?: return null,
    entryTime = Instant.ofEpochMilli(entryTimeEpochMillis),
    exitTime = Instant.ofEpochMilli(exitTimeEpochMillis),
    entryPrice = entryPrice,
    exitPrice = exitPrice,
    quantity = quantity,
    pnl = pnl,
    pnlPercent = pnlPercent,
    durationMinutes = durationMinutes,
    maePercent = maePercent,
    mfePercent = mfePercent,
    feesPaid = feesPaid,
    fundingPaid = fundingPaid,
    exitReason = runCatching { BacktestExitReason.valueOf(exitReason) }.getOrNull() ?: return null
)

private fun BacktestEquityPointDto.toDomain(): BacktestEquityPoint = BacktestEquityPoint(
    time = Instant.ofEpochMilli(timeEpochMillis),
    equity = equity,
    drawdownPercent = drawdownPercent
)

private fun BacktestPricePointDto.toDomain(): BacktestPricePoint = BacktestPricePoint(
    time = Instant.ofEpochMilli(timeEpochMillis),
    price = price
)

private fun StrategySignalPointDto.toDomainOrNull(defaultSymbol: String): StrategySignalPoint? = StrategySignalPoint(
    symbol = symbol ?: defaultSymbol,
    time = Instant.ofEpochMilli(timeEpochMillis),
    price = price,
    signal = runCatching { BacktestSignal.valueOf(signal) }.getOrNull() ?: return null
)

private fun BacktestMetricsDto.toDomain(): BacktestMetrics = BacktestMetrics(
    winRatePercent = winRatePercent,
    totalReturnPercent = totalReturnPercent,
    cagrPercent = cagrPercent,
    maxDrawdownPercent = maxDrawdownPercent,
    sharpeRatio = sharpeRatio,
    sortinoRatio = sortinoRatio,
    profitFactor = if (profitFactor.isFinite()) profitFactor else 99.0,
    expectancyPercent = expectancyPercent,
    annualVolatilityPercent = annualVolatilityPercent,
    tradeCount = tradeCount,
    averageTradePercent = averageTradePercent,
    exposureTimePercent = exposureTimePercent,
    startEquity = startEquity,
    endEquity = endEquity
)

private fun BacktestAnalysisDto.toDomainOrNull(): BacktestAnalysis =
    BacktestAnalysis(
        monthlyReturns = monthlyReturns.map { it.toDomain() },
        walkForward = walkForward?.toDomain(),
        monteCarlo = monteCarlo?.toDomain()
    )

private fun BacktestEdgeValidationDto.toDomainOrNull(): BacktestEdgeValidation? {
    val statusValue = runCatching { BacktestEdgeValidationStatus.valueOf(status) }.getOrNull() ?: return null
    return BacktestEdgeValidation(
        status = statusValue,
        edgeScore = edgeScore,
        sampleCount = sampleCount,
        cpcvSplits = cpcvSplits,
        cpcvPaths = cpcvPaths,
        oosSharpe = oosSharpe,
        oosReturnPercent = oosReturnPercent,
        oosPositivePathPercent = oosPositivePathPercent,
        spaPValue = spaPValue,
        notes = notes,
        generatedAt = Instant.ofEpochMilli(generatedAtEpochMillis)
    )
}

private fun BacktestEdgeGateDecisionDto.toDomainOrNull(): BacktestEdgeGateDecision =
    BacktestEdgeGateDecision(
        enabled = enabled,
        passed = passed,
        minEdgeScore = minEdgeScore,
        maxSpaPValue = maxSpaPValue,
        minPositivePathsPercent = minPositivePathsPercent,
        minTrades = minTrades,
        minOosSharpe = minOosSharpe,
        requirePassedStatus = requirePassedStatus,
        reasons = reasons,
        evaluatedAt = Instant.ofEpochMilli(evaluatedAtEpochMillis)
    )

private fun BacktestMonthlyReturnDto.toDomain(): BacktestMonthlyReturn = BacktestMonthlyReturn(
    year = year,
    month = month,
    returnPercent = returnPercent
)

private fun BacktestWalkForwardResultDto.toDomain(): BacktestWalkForwardResult = BacktestWalkForwardResult(
    splitCount = splitCount,
    inSampleAverageReturnPercent = inSampleAverageReturnPercent,
    outOfSampleAverageReturnPercent = outOfSampleAverageReturnPercent,
    stabilityScorePercent = stabilityScorePercent
)

private fun BacktestMonteCarloResultDto.toDomain(): BacktestMonteCarloResult = BacktestMonteCarloResult(
    iterations = iterations,
    medianReturnPercent = medianReturnPercent,
    p05ReturnPercent = p05ReturnPercent,
    p95ReturnPercent = p95ReturnPercent
)
