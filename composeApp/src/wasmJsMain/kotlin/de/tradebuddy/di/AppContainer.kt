package de.tradebuddy.di

import de.tradebuddy.data.AstroCalculator
import de.tradebuddy.data.AstroCalendarRepository
import de.tradebuddy.data.CityDataSource
import de.tradebuddy.data.DefaultAstroCalendarRepository
import de.tradebuddy.data.DefaultCityDataSource
import de.tradebuddy.data.DefaultMoonPhaseRepository
import de.tradebuddy.data.DefaultSunMoonRepository
import de.tradebuddy.data.FileSettingsRepository
import de.tradebuddy.data.FileStatisticsRepository
import de.tradebuddy.data.MoonPhaseRepository
import de.tradebuddy.data.SettingsRepository
import de.tradebuddy.data.StatisticsRepository
import de.tradebuddy.data.SunMoonRepository
import de.tradebuddy.presentation.SunMoonViewModel
import kotlinx.coroutines.Dispatchers

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

    fun createSunMoonViewModel(): SunMoonViewModel =
        SunMoonViewModel(
            repository = repository,
            moonPhaseRepository = moonPhaseRepository,
            astroCalendarRepository = astroCalendarRepository,
            settingsRepository = settingsRepository,
            statisticsRepository = statisticsRepository
        )
}

