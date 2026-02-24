package de.tradebuddy.presentation

import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AppThemeStyle
import de.tradebuddy.domain.model.AstroAspectEvent
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.AstroCalendarScope
import de.tradebuddy.domain.model.AstroPlanet
import de.tradebuddy.domain.model.AstroSortMode
import de.tradebuddy.domain.model.AstroWeekDaySummary
import de.tradebuddy.domain.model.City
import de.tradebuddy.domain.model.CompactEvent
import de.tradebuddy.domain.model.DEFAULT_ASTRO_ASPECT_ORBS
import de.tradebuddy.domain.model.MonthTrend
import de.tradebuddy.domain.model.MoonPhaseEvent
import de.tradebuddy.domain.model.StatEntry
import de.tradebuddy.domain.model.SunMoonTimes
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import org.jetbrains.compose.resources.StringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.tab_compact
import trade_buddy.composeapp.generated.resources.tab_details
import trade_buddy.composeapp.generated.resources.tab_astro_aspects
import trade_buddy.composeapp.generated.resources.tab_astro_feature
import trade_buddy.composeapp.generated.resources.tab_export
import trade_buddy.composeapp.generated.resources.tab_moon_phases
import trade_buddy.composeapp.generated.resources.tab_statistics
import trade_buddy.composeapp.generated.resources.tab_trend

enum class AppScreen {
    SunMoon,
    AstroCalendar,
    Settings,
    Logs,
    TimeOptimizer
}

enum class SunMoonTab(val label: StringResource) {
    Compact(Res.string.tab_compact),
    Statistics(Res.string.tab_statistics),
    Details(Res.string.tab_details),
    Trend(Res.string.tab_trend),
    Astro(Res.string.tab_astro_feature),
    Export(Res.string.tab_export)
}

enum class AstroCalendarTab(val label: StringResource) {
    Aspects(Res.string.tab_astro_aspects),
    MoonPhases(Res.string.tab_moon_phases)
}

data class MonthTrendUiState(
    val month: YearMonth,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val trend: MonthTrend? = null
)

data class MoonPhaseUiState(
    val month: YearMonth,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val phases: List<MoonPhaseEvent> = emptyList()
)

data class AstroCalendarUiState(
    val allEvents: List<AstroAspectEvent> = emptyList(),
    val week: List<AstroWeekDaySummary> = emptyList(),
    val selectedAspects: Set<AstroAspectType> = AstroAspectType.entries.toSet(),
    val selectedPlanets: Set<AstroPlanet> = AstroPlanet.entries.toSet(),
    val scope: AstroCalendarScope = AstroCalendarScope.AllPlanets,
    val sortMode: AstroSortMode = AstroSortMode.ExactTime,
    val aspectOrbs: Map<AstroAspectType, Double> = DEFAULT_ASTRO_ASPECT_ORBS,
    val loadingProgress: Float? = null,
    val isLoading: Boolean = false,
    val error: StringResource? = null
)

data class TimeOptimizerDayRow(
    val date: LocalDate,
    val sunrise: ZonedDateTime?,
    val sunset: ZonedDateTime?,
    val moonrise: ZonedDateTime?,
    val moonset: ZonedDateTime?,
    val astroEventCount: Int,
    val astroInstants: List<Instant> = emptyList(),
    val firstAstroInstant: Instant?,
    val lastAstroInstant: Instant?
)

data class TimeOptimizerUiState(
    val month: YearMonth,
    val selectedCityKey: String? = null,
    val exportCityKeys: Set<String> = emptySet(),
    val exportUseCityTimeZones: Boolean = true,
    val includeSun: Boolean = true,
    val includeMoon: Boolean = true,
    val includeAstro: Boolean = true,
    val rows: List<TimeOptimizerDayRow> = emptyList(),
    val rowsByCity: Map<String, List<TimeOptimizerDayRow>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: StringResource? = null
)

data class SunMoonUiState(
    val selectedDate: LocalDate,
    val userZone: ZoneId,
    val trend: MonthTrendUiState,
    val moonPhases: MoonPhaseUiState,
    val astroCalendar: AstroCalendarUiState = AstroCalendarUiState(),
    val screen: AppScreen = AppScreen.SunMoon,
    val themeStyle: AppThemeStyle = AppThemeStyle.Slate,
    val themeMode: AppThemeMode = AppThemeMode.Dark,
    val selectedTab: SunMoonTab = SunMoonTab.Compact,
    val selectedAstroTab: AstroCalendarTab = AstroCalendarTab.Aspects,
    val nowInstant: Instant = Instant.now(),
    val sunTimeOffsetMinutes: Int = 0,
    val moonTimeOffsetMinutes: Int = 0,
    val astroTimeOffsetMinutes: Int = 0,
    val showUtcTime: Boolean = true,
    val showAzimuth: Boolean = true,
    val showSun: Boolean = true,
    val showMoon: Boolean = true,
    val showRise: Boolean = true,
    val showSet: Boolean = true,
    val allCities: List<City> = emptyList(),
    val selectedCityKeys: Set<String> = emptySet(),
    val results: List<SunMoonTimes> = emptyList(),
    val filteredResults: List<SunMoonTimes> = emptyList(),
    val compactSourceResults: List<SunMoonTimes> = emptyList(),
    val compactEvents: List<CompactEvent> = emptyList(),
    val timeOptimizer: TimeOptimizerUiState = TimeOptimizerUiState(month = YearMonth.from(selectedDate)),
    val stats: List<StatEntry> = emptyList(),
    val loadingProgress: Float? = null,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val showDatePicker: Boolean = false
)

