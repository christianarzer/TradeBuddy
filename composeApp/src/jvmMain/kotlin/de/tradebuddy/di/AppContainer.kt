package de.tradebuddy.di

import de.tradebuddy.data.AstroCalculator
import de.tradebuddy.data.AstroCalendarRepository
import de.tradebuddy.data.AppStoragePaths
import de.tradebuddy.data.CityDataSource
import de.tradebuddy.data.DefaultAstroCalendarRepository
import de.tradebuddy.data.DefaultCityDataSource
import de.tradebuddy.data.DefaultMarketEventsRepository
import de.tradebuddy.data.DefaultMoonPhaseRepository
import de.tradebuddy.data.DefaultPortfolioRepository
import de.tradebuddy.data.DefaultSunMoonRepository
import de.tradebuddy.data.DefaultTasksRepository
import de.tradebuddy.data.FileStorageDocument
import de.tradebuddy.data.FileSettingsRepository
import de.tradebuddy.data.FileStatisticsRepository
import de.tradebuddy.data.FredMarketEventsDataSource
import de.tradebuddy.data.MarketEventsRepository
import de.tradebuddy.data.MoonPhaseRepository
import de.tradebuddy.data.PortfolioRepository
import de.tradebuddy.data.SettingsRepository
import de.tradebuddy.data.StatisticsRepository
import de.tradebuddy.data.SunMoonRepository
import de.tradebuddy.data.TasksRepository
import de.tradebuddy.presentation.MarketEventsViewModel
import de.tradebuddy.presentation.PortfolioViewModel
import de.tradebuddy.presentation.SunMoonViewModel
import de.tradebuddy.presentation.TasksViewModel
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppContainer(
    cityDataSource: CityDataSource = DefaultCityDataSource,
    calculator: AstroCalculator = AstroCalculator()
) {
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
    private val marketEventsDataSource = FredMarketEventsDataSource(
        apiKeyProvider = ::resolveFredApiKey,
        fetcher = ::httpGet
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

    private suspend fun httpGet(
        url: String,
        headers: Map<String, String>
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
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
                error("HTTP $statusCode for $url: $body")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun resolveFredApiKey(): String {
        val localProperties = readLocalProperties()
        return firstNotBlank(
            System.getenv("FRED_API_KEY"),
            System.getenv("TRADEBUDDY_FRED_API_KEY"),
            System.getProperty("fred.api.key"),
            System.getProperty("tradebuddy.fred.api.key"),
            localProperties["FRED_API_KEY"],
            localProperties["TRADEBUDDY_FRED_API_KEY"],
            localProperties["fred.api.key"],
            localProperties["tradebuddy.fred.api.key"],
            "abcdefghijklmnopqrstuvwxyz123456"
        ).orEmpty()
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
}

