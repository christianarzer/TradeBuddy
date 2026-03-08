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
import de.tradebuddy.data.FileStorageDocument
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
import de.tradebuddy.logging.AppLog
import de.tradebuddy.presentation.BacktestingViewModel
import de.tradebuddy.presentation.MarketEventsViewModel
import de.tradebuddy.presentation.PortfolioViewModel
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.presentation.TasksViewModel
import de.tradebuddy.validation.PythonEdgeLabValidator
import de.tradebuddy.domain.model.BacktestExchange
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppContainer(
    cityDataSource: CityDataSource = DefaultCityDataSource,
    calculator: AstroCalculator = AstroCalculator()
) {
    private companion object {
        const val ApiKeyCacheMillis = 60_000L
        const val DemoFredApiKey = "abcdefghijklmnopqrstuvwxyz123456"
    }

    private val repository: SunMoonRepository =
        DefaultSunMoonRepository(calculator, cityDataSource, Dispatchers.Default)
    private val moonPhaseRepository: MoonPhaseRepository =
        DefaultMoonPhaseRepository(calculator, Dispatchers.Default)
    private val astroCalendarRepository: AstroCalendarRepository =
        DefaultAstroCalendarRepository(calculator, Dispatchers.Default)
    private val settingsRepository: SettingsRepository =
        FileSettingsRepository(dispatcher = Dispatchers.IO)
    private val statisticsRepository: StatisticsRepository =
        FileStatisticsRepository(dispatcher = Dispatchers.IO)
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
            watchlistDocument = FileStorageDocument(
                file = AppStoragePaths.marketEventsWatchlistPath(),
                dispatcher = Dispatchers.IO
            ),
            cacheDocument = FileStorageDocument(
                file = AppStoragePaths.marketEventsCachePath(),
                dispatcher = Dispatchers.IO
            ),
            remoteDataSource = marketEventsDataSource
        )
    private val portfolioRepository: PortfolioRepository =
        DefaultPortfolioRepository(
            document = FileStorageDocument(
                file = AppStoragePaths.portfolioPath(),
                dispatcher = Dispatchers.IO
            )
        )
    private val tasksRepository: TasksRepository =
        DefaultTasksRepository(
            document = FileStorageDocument(
                file = AppStoragePaths.tasksPath(),
                dispatcher = Dispatchers.IO
            )
        )
    private val backtestingRepository: BacktestingRepository =
        DefaultBacktestingRepository(
            historyDocument = FileStorageDocument(
                file = AppStoragePaths.backtestingHistoryPath(),
                dispatcher = Dispatchers.IO
            ),
            candleCacheDocument = FileStorageDocument(
                file = AppStoragePaths.backtestingCandleCachePath(),
                dispatcher = Dispatchers.IO
            ),
            symbolsCacheDocument = FileStorageDocument(
                file = AppStoragePaths.backtestingSymbolsCachePath(),
                dispatcher = Dispatchers.IO
            ),
            providers = mapOf(
                BacktestExchange.BinanceSpot to BinanceKlineDataProvider(fetcher = ::httpGet),
                BacktestExchange.BybitSpot to BybitKlineDataProvider(fetcher = ::httpGet),
                BacktestExchange.OkxSpot to OkxKlineDataProvider(fetcher = ::httpGet)
            )
        )
    @Volatile
    private var cachedFredApiKey: String? = null
    @Volatile
    private var cachedFredApiKeyResolvedAtMillis: Long = 0L
    @Volatile
    private var liveFredApiKey: String? = null
    @Volatile
    private var cachedFmpApiKey: String? = null
    @Volatile
    private var cachedFmpApiKeyResolvedAtMillis: Long = 0L
    @Volatile
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
        BacktestingViewModel(
            repository = backtestingRepository,
            edgeLabValidator = PythonEdgeLabValidator()
        )

    fun setMarketEventsApiKey(apiKey: String) {
        val normalized = apiKey.trim().ifBlank { null }
        liveFredApiKey = normalized
        if (normalized != null) {
            cachedFredApiKey = normalized
            cachedFredApiKeyResolvedAtMillis = System.currentTimeMillis()
        } else {
            cachedFredApiKey = null
            cachedFredApiKeyResolvedAtMillis = 0L
        }
    }

    fun setMarketEventsFmpApiKey(apiKey: String) {
        val normalized = apiKey.trim().ifBlank { null }
        liveFmpApiKey = normalized
        if (normalized != null) {
            cachedFmpApiKey = normalized
            cachedFmpApiKeyResolvedAtMillis = System.currentTimeMillis()
        } else {
            cachedFmpApiKey = null
            cachedFmpApiKeyResolvedAtMillis = 0L
        }
    }

    private suspend fun httpGet(
        url: String,
        headers: Map<String, String>
    ): String = withContext(Dispatchers.IO) {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }
        try {
            val statusCode = connection.responseCode
            val body = (if (statusCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (statusCode !in 200..299) {
                val snippet = body.toCompactLogSnippet()
                val message = if (snippet.isBlank()) {
                    "HTTP $statusCode for $url"
                } else {
                    "HTTP $statusCode for $url: $snippet"
                }
                error(message)
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun resolveFredApiKey(): String {
        val nowMillis = System.currentTimeMillis()
        val liveKey = liveFredApiKey
        if (!liveKey.isNullOrBlank()) {
            cachedFredApiKey = liveKey
            cachedFredApiKeyResolvedAtMillis = nowMillis
            return liveKey
        }
        val savedKey = runCatching { settingsRepository.loadSettings()?.marketEventsApiKey?.trim() }
            .onFailure { error ->
                AppLog.warn(
                    tag = "AppContainer",
                    message = "Failed to read market-events API key from settings",
                    throwable = error
                )
            }
            .getOrNull()
        if (!savedKey.isNullOrBlank()) {
            liveFredApiKey = savedKey
            cachedFredApiKey = savedKey
            cachedFredApiKeyResolvedAtMillis = nowMillis
            return savedKey
        }
        val cached = cachedFredApiKey
        if (!cached.isNullOrBlank() && (nowMillis - cachedFredApiKeyResolvedAtMillis) < ApiKeyCacheMillis) {
            return cached
        }
        val localProperties = readLocalProperties()
        val resolved = firstNotBlank(
            System.getenv("FRED_API_KEY"),
            System.getenv("TRADEBUDDY_FRED_API_KEY"),
            System.getProperty("fred.api.key"),
            System.getProperty("tradebuddy.fred.api.key"),
            localProperties["FRED_API_KEY"],
            localProperties["TRADEBUDDY_FRED_API_KEY"],
            localProperties["fred.api.key"],
            localProperties["tradebuddy.fred.api.key"],
            DemoFredApiKey
        ).orEmpty()
        cachedFredApiKey = resolved
        cachedFredApiKeyResolvedAtMillis = nowMillis
        return resolved
    }

    private suspend fun resolveFmpApiKey(): String {
        val nowMillis = System.currentTimeMillis()
        val liveKey = liveFmpApiKey
        if (!liveKey.isNullOrBlank()) {
            cachedFmpApiKey = liveKey
            cachedFmpApiKeyResolvedAtMillis = nowMillis
            return liveKey
        }
        val savedKey = runCatching { settingsRepository.loadSettings()?.marketEventsFmpApiKey?.trim() }
            .onFailure { error ->
                AppLog.warn(
                    tag = "AppContainer",
                    message = "Failed to read FMP API key from settings",
                    throwable = error
                )
            }
            .getOrNull()
        if (!savedKey.isNullOrBlank()) {
            liveFmpApiKey = savedKey
            cachedFmpApiKey = savedKey
            cachedFmpApiKeyResolvedAtMillis = nowMillis
            return savedKey
        }
        val cached = cachedFmpApiKey
        if (!cached.isNullOrBlank() && (nowMillis - cachedFmpApiKeyResolvedAtMillis) < ApiKeyCacheMillis) {
            return cached
        }
        val localProperties = readLocalProperties()
        val resolved = firstNotBlank(
            System.getenv("FMP_API_KEY"),
            System.getenv("TRADEBUDDY_FMP_API_KEY"),
            System.getProperty("fmp.api.key"),
            System.getProperty("tradebuddy.fmp.api.key"),
            localProperties["FMP_API_KEY"],
            localProperties["TRADEBUDDY_FMP_API_KEY"],
            localProperties["fmp.api.key"],
            localProperties["tradebuddy.fmp.api.key"]
        ).orEmpty()
        cachedFmpApiKey = resolved
        cachedFmpApiKeyResolvedAtMillis = nowMillis
        return resolved
    }

    private fun readLocalProperties(): Map<String, String> {
        val candidates = listOf(
            File("local.properties"),
            File(System.getProperty("user.dir"), "local.properties"),
            File(System.getProperty("user.home"), ".tradebuddy/local.properties")
        )
        val file = candidates.firstOrNull { it.exists() && it.isFile } ?: return emptyMap()
        return runCatching {
            val props = Properties()
            file.inputStream().use(props::load)
            props.entries.associate { (key, value) ->
                key.toString().trim() to value.toString().trim()
            }
        }.getOrDefault(emptyMap())
    }

    private fun firstNotBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()

    private fun String.toCompactLogSnippet(maxLength: Int = 320): String {
        if (isBlank()) return ""
        val compact = replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= maxLength) compact else compact.take(maxLength) + "..."
    }
}

