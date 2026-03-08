package de.tradebuddy.di

import de.tradebuddy.data.AstroCalculator
import de.tradebuddy.data.AstroCalendarRepository
import de.tradebuddy.data.AppStoragePaths
import de.tradebuddy.data.BacktestingRepository
import de.tradebuddy.data.BinanceKlineDataProvider
import de.tradebuddy.data.BybitKlineDataProvider
import de.tradebuddy.data.CityDataSource
import de.tradebuddy.data.DefaultAstroCalendarRepository
import de.tradebuddy.data.DefaultBacktestingRepository
import de.tradebuddy.data.DefaultCityDataSource
import de.tradebuddy.data.DefaultMarketEventsRepository
import de.tradebuddy.data.DefaultMoonPhaseRepository
import de.tradebuddy.data.DefaultPortfolioRepository
import de.tradebuddy.data.DefaultSunMoonRepository
import de.tradebuddy.data.DefaultTasksRepository
import de.tradebuddy.data.BeaReleaseDatesDataSource
import de.tradebuddy.data.BlsCalendarIcsDataSource
import de.tradebuddy.data.CensusEconomicCalendarDataSource
import de.tradebuddy.data.CompositeMarketEventsDataSource
import de.tradebuddy.data.EcbStatsCalendarDataSource
import de.tradebuddy.data.EurostatCalendarIcsDataSource
import de.tradebuddy.data.FileSettingsRepository
import de.tradebuddy.data.FileStatisticsRepository
import de.tradebuddy.data.FredMarketEventsDataSource
import de.tradebuddy.data.MarketEventsRepository
import de.tradebuddy.data.MoonPhaseRepository
import de.tradebuddy.data.NamedMarketEventsDataSource
import de.tradebuddy.data.OnsReleaseCalendarDataSource
import de.tradebuddy.data.OkxKlineDataProvider
import de.tradebuddy.data.PortfolioRepository
import de.tradebuddy.data.SettingsRepository
import de.tradebuddy.data.StatisticsRepository
import de.tradebuddy.data.SunMoonRepository
import de.tradebuddy.data.TasksRepository
import de.tradebuddy.data.TradingEconomicsMarketEventsDataSource
import de.tradebuddy.data.WebStorageDocument
import de.tradebuddy.domain.model.BacktestExchange
import de.tradebuddy.logging.AppLog
import de.tradebuddy.presentation.BacktestingViewModel
import de.tradebuddy.presentation.MarketEventsViewModel
import de.tradebuddy.presentation.PortfolioViewModel
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.presentation.TasksViewModel
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.Dispatchers
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import java.time.Instant
import kotlin.js.ExperimentalWasmJsInterop

class AppContainer(
    cityDataSource: CityDataSource = DefaultCityDataSource,
    calculator: AstroCalculator = AstroCalculator()
) {
    private companion object {
        const val DemoFredApiKey = "abcdefghijklmnopqrstuvwxyz123456"
        const val ApiKeyCacheMillis = 60_000L
    }

    private val repository: SunMoonRepository =
        DefaultSunMoonRepository(calculator, cityDataSource, Dispatchers.Default)
    private val moonPhaseRepository: MoonPhaseRepository =
        DefaultMoonPhaseRepository(calculator, Dispatchers.Default)
    private val astroCalendarRepository: AstroCalendarRepository =
        DefaultAstroCalendarRepository(calculator, Dispatchers.Default)
    private val settingsRepository: SettingsRepository =
        FileSettingsRepository(dispatcher = Dispatchers.Default)
    private val statisticsRepository: StatisticsRepository =
        FileStatisticsRepository(dispatcher = Dispatchers.Default)
    private val marketEventsDataSource = CompositeMarketEventsDataSource(
        sources = listOf(
            NamedMarketEventsDataSource(
                name = "TradingEconomics",
                dataSource = TradingEconomicsMarketEventsDataSource(
                    fetcher = ::httpGet
                ),
                logEmptyResults = false
            ),
            NamedMarketEventsDataSource(
                name = "ONS",
                dataSource = OnsReleaseCalendarDataSource(
                    fetcher = ::httpGet
                ),
                logEmptyResults = false
            ),
            NamedMarketEventsDataSource(
                name = "USCensus",
                dataSource = CensusEconomicCalendarDataSource(
                    fetcher = ::httpGet
                ),
                logEmptyResults = false
            ),
            NamedMarketEventsDataSource(
                name = "BLS",
                dataSource = BlsCalendarIcsDataSource(
                    fetcher = ::httpGet
                )
            ),
            NamedMarketEventsDataSource(
                name = "BEA",
                dataSource = BeaReleaseDatesDataSource(
                    fetcher = ::httpGet
                )
            ),
            NamedMarketEventsDataSource(
                name = "ECB",
                dataSource = EcbStatsCalendarDataSource(
                    fetcher = ::httpGet
                )
            ),
            NamedMarketEventsDataSource(
                name = "Eurostat",
                dataSource = EurostatCalendarIcsDataSource(
                    fetcher = ::httpGet
                ),
                logEmptyResults = false
            ),
            NamedMarketEventsDataSource(
                name = "FRED",
                dataSource = FredMarketEventsDataSource(
                    apiKeyProvider = ::resolveFredApiKey,
                    fetcher = ::httpGet
                )
            )
        )
    )
    private val marketEventsRepository: MarketEventsRepository =
        DefaultMarketEventsRepository(
            watchlistDocument = WebStorageDocument(
                key = AppStoragePaths.marketEventsWatchlistPath(),
                dispatcher = Dispatchers.Default
            ),
            cacheDocument = WebStorageDocument(
                key = AppStoragePaths.marketEventsCachePath(),
                dispatcher = Dispatchers.Default
            ),
            remoteDataSource = marketEventsDataSource
        )
    private val portfolioRepository: PortfolioRepository =
        DefaultPortfolioRepository(
            document = WebStorageDocument(
                key = AppStoragePaths.portfolioPath(),
                dispatcher = Dispatchers.Default
            )
        )
    private val tasksRepository: TasksRepository =
        DefaultTasksRepository(
            document = WebStorageDocument(
                key = AppStoragePaths.tasksPath(),
                dispatcher = Dispatchers.Default
            )
        )
    private val backtestingRepository: BacktestingRepository =
        DefaultBacktestingRepository(
            historyDocument = WebStorageDocument(
                key = AppStoragePaths.backtestingHistoryPath(),
                dispatcher = Dispatchers.Default
            ),
            candleCacheDocument = WebStorageDocument(
                key = AppStoragePaths.backtestingCandleCachePath(),
                dispatcher = Dispatchers.Default
            ),
            symbolsCacheDocument = WebStorageDocument(
                key = AppStoragePaths.backtestingSymbolsCachePath(),
                dispatcher = Dispatchers.Default
            ),
            providers = mapOf(
                BacktestExchange.BinanceSpot to BinanceKlineDataProvider(fetcher = ::httpGet),
                BacktestExchange.BybitSpot to BybitKlineDataProvider(fetcher = ::httpGet),
                BacktestExchange.OkxSpot to OkxKlineDataProvider(fetcher = ::httpGet)
            )
        )
    private var cachedFredApiKey: String? = null
    private var cachedFredApiKeyResolvedAtMillis: Long = 0L
    private var liveFredApiKey: String? = null
    private var cachedFmpApiKey: String? = null
    private var cachedFmpApiKeyResolvedAtMillis: Long = 0L
    private var liveFmpApiKey: String? = null

    fun createSunMoonViewModel(): SunMoonViewModel =
        SunMoonViewModel(
            repository = repository,
            moonPhaseRepository = moonPhaseRepository,
            astroCalendarRepository = astroCalendarRepository,
            settingsRepository = settingsRepository,
            statisticsRepository = statisticsRepository
        )

    fun createMarketEventsViewModel(): MarketEventsViewModel =
        MarketEventsViewModel(repository = marketEventsRepository)

    fun createPortfolioViewModel(): PortfolioViewModel =
        PortfolioViewModel(repository = portfolioRepository)

    fun createTasksViewModel(): TasksViewModel =
        TasksViewModel(repository = tasksRepository)

    fun createBacktestingViewModel(): BacktestingViewModel =
        BacktestingViewModel(repository = backtestingRepository)

    fun setMarketEventsApiKey(apiKey: String) {
        val normalized = apiKey.trim().ifBlank { null }
        liveFredApiKey = normalized
        val nowMillis = Instant.now().toEpochMilli()
        if (normalized != null) {
            cachedFredApiKey = normalized
            cachedFredApiKeyResolvedAtMillis = nowMillis
        } else {
            cachedFredApiKey = null
            cachedFredApiKeyResolvedAtMillis = 0L
        }
    }

    fun setMarketEventsFmpApiKey(apiKey: String) {
        val normalized = apiKey.trim().ifBlank { null }
        liveFmpApiKey = normalized
        val nowMillis = Instant.now().toEpochMilli()
        if (normalized != null) {
            cachedFmpApiKey = normalized
            cachedFmpApiKeyResolvedAtMillis = nowMillis
        } else {
            cachedFmpApiKey = null
            cachedFmpApiKeyResolvedAtMillis = 0L
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private suspend fun httpGet(url: String, headers: Map<String, String>): String {
        val requestHeaders = Headers()
        headers.forEach { (key, value) -> requestHeaders.append(key, value) }
        val requestInit = RequestInit(
            method = "GET",
            headers = requestHeaders
        )
        val response: Response = try {
            window.fetch(url, requestInit).await()
        } catch (error: Throwable) {
            AppLog.error(
                tag = "MarketEventsHttp",
                message = "Fetch request failed for $url",
                throwable = error
            )
            throw error
        }
        if (!response.ok) {
            val body = try {
                response.text().await()
            } catch (_: Throwable) {
                ""
            }
            val snippet = body.toCompactLogSnippet()
            val message = if (snippet.isBlank()) {
                "HTTP ${response.status} for $url"
            } else {
                "HTTP ${response.status} for $url: $snippet"
            }
            AppLog.error(
                tag = "MarketEventsHttp",
                message = "HTTP ${response.status} while loading market events",
                throwable = IllegalStateException(message)
            )
            error(message)
        }
        return try {
            response.text().await()
        } catch (error: Throwable) {
            AppLog.error(
                tag = "MarketEventsHttp",
                message = "Failed to read response body for $url",
                throwable = error
            )
            throw error
        }
    }

    private suspend fun resolveFredApiKey(): String {
        val nowMillis = Instant.now().toEpochMilli()
        val live = liveFredApiKey
        if (!live.isNullOrBlank()) {
            cachedFredApiKey = live
            cachedFredApiKeyResolvedAtMillis = nowMillis
            return live
        }
        val saved = runCatching { settingsRepository.loadSettings()?.marketEventsApiKey?.trim() }
            .onFailure { error ->
                AppLog.warn(
                    tag = "AppContainer",
                    message = "Failed to read market-events API key from browser settings",
                    throwable = error
                )
            }
            .getOrNull()
        if (!saved.isNullOrBlank()) {
            liveFredApiKey = saved
            cachedFredApiKey = saved
            cachedFredApiKeyResolvedAtMillis = nowMillis
            return saved
        }
        val cached = cachedFredApiKey
        if (!cached.isNullOrBlank() && (nowMillis - cachedFredApiKeyResolvedAtMillis) < ApiKeyCacheMillis) {
            return cached
        }
        val resolved = DemoFredApiKey
        cachedFredApiKey = resolved
        cachedFredApiKeyResolvedAtMillis = nowMillis
        return resolved
    }

    private suspend fun resolveFmpApiKey(): String {
        val nowMillis = Instant.now().toEpochMilli()
        val live = liveFmpApiKey
        if (!live.isNullOrBlank()) {
            cachedFmpApiKey = live
            cachedFmpApiKeyResolvedAtMillis = nowMillis
            return live
        }
        val saved = runCatching { settingsRepository.loadSettings()?.marketEventsFmpApiKey?.trim() }
            .onFailure { error ->
                AppLog.warn(
                    tag = "AppContainer",
                    message = "Failed to read FMP API key from browser settings",
                    throwable = error
                )
            }
            .getOrNull()
        if (!saved.isNullOrBlank()) {
            liveFmpApiKey = saved
            cachedFmpApiKey = saved
            cachedFmpApiKeyResolvedAtMillis = nowMillis
            return saved
        }
        val cached = cachedFmpApiKey
        if (!cached.isNullOrBlank() && (nowMillis - cachedFmpApiKeyResolvedAtMillis) < ApiKeyCacheMillis) {
            return cached
        }
        cachedFmpApiKey = ""
        cachedFmpApiKeyResolvedAtMillis = nowMillis
        return ""
    }

    private fun String.toCompactLogSnippet(maxLength: Int = 320): String {
        if (isBlank()) return ""
        val compact = replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= maxLength) compact else compact.take(maxLength) + "..."
    }
}

