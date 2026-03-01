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
import de.tradebuddy.data.WebStorageDocument
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
import kotlin.js.ExperimentalWasmJsInterop

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
        FileSettingsRepository(dispatcher = Dispatchers.Default)
    private val statisticsRepository: StatisticsRepository =
        FileStatisticsRepository(dispatcher = Dispatchers.Default)
    private val marketEventsDataSource = FredMarketEventsDataSource(
        apiKeyProvider = { "abcdefghijklmnopqrstuvwxyz123456" },
        fetcher = ::httpGet
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

    @OptIn(ExperimentalWasmJsInterop::class)
    private suspend fun httpGet(url: String, headers: Map<String, String>): String {
        val requestHeaders = Headers()
        headers.forEach { (key, value) -> requestHeaders.append(key, value) }
        val requestInit = RequestInit(
            method = "GET",
            headers = requestHeaders
        )
        val response: Response = window.fetch(url, requestInit).await()
        if (!response.ok) {
            error("HTTP ${response.status} for $url")
        }
        return response.text().await()
    }
}

