package de.tradebuddy.presentation

import de.tradebuddy.data.AstroCalendarRepository
import de.tradebuddy.data.MoonPhaseRepository
import de.tradebuddy.data.SettingsRepository
import de.tradebuddy.data.StatisticsRepository
import de.tradebuddy.data.SunMoonRepository
import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AppThemeStyle
import de.tradebuddy.domain.model.AstroAspectEvent
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.AstroCalendarScope
import de.tradebuddy.domain.model.AstroPlanet
import de.tradebuddy.domain.model.AstroSortMode
import de.tradebuddy.domain.model.AstroWeekDaySummary
import de.tradebuddy.domain.model.ASTRO_MAX_ORB_DEGREES
import de.tradebuddy.domain.model.ASTRO_MIN_ORB_DEGREES
import de.tradebuddy.domain.model.ASTRO_ORB_STEP_DEGREES
import de.tradebuddy.domain.model.CompactEvent
import de.tradebuddy.domain.model.CompactEventType
import de.tradebuddy.domain.model.DEFAULT_ASTRO_ASPECT_ORBS
import de.tradebuddy.domain.model.MoveDirection
import de.tradebuddy.domain.model.StatEntry
import de.tradebuddy.domain.model.SunMoonTimes
import de.tradebuddy.domain.model.UserSettings
import de.tradebuddy.domain.util.buildCompactEvents
import de.tradebuddy.domain.util.key
import de.tradebuddy.logging.AppLog
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs
import kotlin.math.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.error_calc
import trade_buddy.composeapp.generated.resources.error_astro_calendar
import trade_buddy.composeapp.generated.resources.error_moon_phases
import trade_buddy.composeapp.generated.resources.error_no_cities
import trade_buddy.composeapp.generated.resources.error_trend

class SunMoonViewModel(
    private val repository: SunMoonRepository,
    private val moonPhaseRepository: MoonPhaseRepository,
    private val astroCalendarRepository: AstroCalendarRepository,
    private val settingsRepository: SettingsRepository,
    private val statisticsRepository: StatisticsRepository
) : AutoCloseable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val userZone = ZoneId.systemDefault()
    private val allCities = repository.cities()
    private val initialDate = LocalDate.now(userZone)

    private val _state = MutableStateFlow(
        SunMoonUiState(
            selectedDate = initialDate,
            userZone = userZone,
            trend = MonthTrendUiState(month = YearMonth.from(initialDate)),
            moonPhases = MoonPhaseUiState(month = YearMonth.from(initialDate)),
            allCities = allCities,
            selectedCityKeys = allCities.map { it.key() }.toSet()
        )
    )
    val state: StateFlow<SunMoonUiState> = _state.asStateFlow()

    init {
        AppLog.info("SunMoonViewModel", "Initialized")
        loadSettings()
        loadStatistics()
        refresh()
        refreshAstro()
        startClock()
    }

    fun setScreen(screen: AppScreen) {
        _state.update { it.copy(screen = screen) }
    }

    fun toggleTheme() {
        val current = _state.value.themeMode
        val next = if (current == AppThemeMode.Dark) AppThemeMode.Light else AppThemeMode.Dark
        setThemeMode(next)
    }

    fun setThemeStyle(style: AppThemeStyle) {
        _state.update { it.copy(themeStyle = style) }
        persistSettings()
    }

    fun setThemeMode(mode: AppThemeMode) {
        _state.update { it.copy(themeMode = mode) }
        persistSettings()
    }

    fun setShowUtcTime(enabled: Boolean) {
        _state.update { it.copy(showUtcTime = enabled) }
        persistSettings()
    }

    fun setShowAzimuth(enabled: Boolean) {
        _state.update { it.copy(showAzimuth = enabled) }
        applyFilters()
        persistSettings()
    }

    fun setShowSun(enabled: Boolean) {
        _state.update { it.copy(showSun = enabled) }
        applyFilters()
        persistSettings()
    }

    fun setShowMoon(enabled: Boolean) {
        _state.update { it.copy(showMoon = enabled) }
        applyFilters()
        persistSettings()
    }

    fun setShowRise(enabled: Boolean) {
        _state.update { it.copy(showRise = enabled) }
        applyFilters()
        persistSettings()
    }

    fun setShowSet(enabled: Boolean) {
        _state.update { it.copy(showSet = enabled) }
        applyFilters()
        persistSettings()
    }

    fun setAstroAspectEnabled(aspectType: AstroAspectType, enabled: Boolean) {
        _state.update { current ->
            val updated = current.astroCalendar.selectedAspects.toMutableSet().apply {
                if (enabled) add(aspectType) else remove(aspectType)
            }
            current.copy(
                astroCalendar = current.astroCalendar.copy(selectedAspects = updated)
            )
        }
    }

    fun adjustAstroAspectOrb(aspectType: AstroAspectType, increase: Boolean) {
        val delta = if (increase) ASTRO_ORB_STEP_DEGREES else -ASTRO_ORB_STEP_DEGREES
        val currentOrb = _state.value.astroCalendar.aspectOrbs[aspectType]
            ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(aspectType)
        setAstroAspectOrb(aspectType, currentOrb + delta)
    }

    fun setAstroAspectOrb(aspectType: AstroAspectType, orbDegrees: Double) {
        val sanitized = sanitizeAspectOrb(orbDegrees)
        var changed = false
        _state.update { current ->
            val currentOrb = current.astroCalendar.aspectOrbs[aspectType]
                ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(aspectType)
            if (abs(currentOrb - sanitized) < 1.0e-9) {
                return@update current
            }
            changed = true
            val updatedOrbs = current.astroCalendar.aspectOrbs + (aspectType to sanitized)
            current.copy(
                astroCalendar = current.astroCalendar.copy(aspectOrbs = updatedOrbs)
            )
        }
        if (changed) {
            refreshAstro()
            persistSettings()
        }
    }

    fun resetAstroAspectOrbs() {
        val currentOrbs = _state.value.astroCalendar.aspectOrbs
        if (currentOrbs == DEFAULT_ASTRO_ASPECT_ORBS) return
        _state.update { current ->
            current.copy(
                astroCalendar = current.astroCalendar.copy(aspectOrbs = DEFAULT_ASTRO_ASPECT_ORBS)
            )
        }
        refreshAstro()
        persistSettings()
    }

    fun setAstroPlanetEnabled(planet: AstroPlanet, enabled: Boolean) {
        _state.update { current ->
            val updated = current.astroCalendar.selectedPlanets.toMutableSet().apply {
                if (enabled) add(planet) else remove(planet)
            }
            current.copy(
                astroCalendar = current.astroCalendar.copy(selectedPlanets = updated)
            )
        }
    }

    fun setAstroSortMode(mode: AstroSortMode) {
        _state.update { current ->
            current.copy(astroCalendar = current.astroCalendar.copy(sortMode = mode))
        }
    }

    fun setAstroScope(scope: AstroCalendarScope) {
        _state.update { current ->
            current.copy(astroCalendar = current.astroCalendar.copy(scope = scope))
        }
        refreshAstro()
    }

    fun upsertStatEntry(event: CompactEvent, direction: MoveDirection, offsetMinutes: Int) {
        val eventInstant = event.userInstant ?: return
        val existing = _state.value.stats.firstOrNull {
            it.cityKey == event.cityKey &&
                it.eventType == event.eventType &&
                it.eventInstant == eventInstant
        }
        val entry = StatEntry(
            id = existing?.id ?: UUID.randomUUID().toString(),
            cityLabel = event.cityLabel,
            cityKey = event.cityKey,
            eventType = event.eventType,
            eventInstant = eventInstant,
            direction = direction,
            offsetMinutes = offsetMinutes,
            createdAt = existing?.createdAt ?: Instant.now()
        )
        _state.update { current ->
            val updated = if (existing == null) {
                (current.stats + entry).sortedByDescending { it.createdAt }
            } else {
                current.stats.map { stat -> if (stat.id == entry.id) entry else stat }
            }
            current.copy(stats = updated)
        }
        scope.launch {
            if (existing == null) {
                statisticsRepository.addEntry(entry)
            } else {
                statisticsRepository.updateEntry(entry)
            }
        }
    }

    fun deleteStatEntry(entryId: String) {
        _state.update { current ->
            current.copy(stats = current.stats.filterNot { it.id == entryId })
        }
        scope.launch {
            statisticsRepository.deleteEntry(entryId)
        }
    }

    fun deleteStatEntryForEvent(event: CompactEvent) {
        val eventInstant = event.userInstant ?: return
        val entry = _state.value.stats.firstOrNull {
            it.cityKey == event.cityKey &&
                it.eventType == event.eventType &&
                it.eventInstant == eventInstant
        } ?: return
        deleteStatEntry(entry.id)
    }

    fun resetStatistics() {
        _state.update { it.copy(stats = emptyList()) }
        scope.launch {
            statisticsRepository.resetEntries()
        }
    }

    fun setTab(tab: SunMoonTab) {
        _state.update { it.copy(selectedTab = tab) }
        when (tab) {
            SunMoonTab.Trend -> loadTrend(YearMonth.from(_state.value.selectedDate))
            else -> Unit
        }
    }

    fun setAstroTab(tab: AstroCalendarTab) {
        _state.update { it.copy(selectedAstroTab = tab) }
        if (tab == AstroCalendarTab.MoonPhases) {
            loadMoonPhases(YearMonth.from(_state.value.selectedDate))
        }
    }

    fun showDatePicker(show: Boolean) {
        _state.update { it.copy(showDatePicker = show) }
    }

    fun applyCityFilter(keys: Set<String>) {
        _state.update { it.copy(selectedCityKeys = keys) }
        applyFilters()
        persistSettings()
    }

    fun shiftDate(days: Long) {
        setDate(_state.value.selectedDate.plusDays(days))
    }

    fun goToToday() {
        setDate(LocalDate.now(userZone))
    }

    fun shiftMonth(delta: Long) {
        val current = _state.value.selectedDate
        val targetMonth = YearMonth.from(current).plusMonths(delta)
        val clampedDay = minOf(current.dayOfMonth, targetMonth.lengthOfMonth())
        setDate(targetMonth.atDay(clampedDay))
    }

    fun setDate(date: LocalDate) {
        _state.update {
            it.copy(
                selectedDate = date,
                trend = it.trend.copy(month = YearMonth.from(date)),
                moonPhases = it.moonPhases.copy(month = YearMonth.from(date))
            )
        }
        refresh()
        refreshAstro()
        when (_state.value.selectedTab) {
            SunMoonTab.Trend -> loadTrend(YearMonth.from(date))
            else -> Unit
        }
        if (_state.value.selectedAstroTab == AstroCalendarTab.MoonPhases) {
            loadMoonPhases(YearMonth.from(date))
        }
    }

    fun refresh() {
        scope.launch {
            val date = _state.value.selectedDate
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.loadDaily(date) }
                .onSuccess { results ->
                    if (date != _state.value.selectedDate) return@launch
                    _state.update {
                        it.copy(
                            results = results,
                            compactSourceResults = results,
                            isLoading = false
                        )
                    }
                    applyFilters()
                    loadCompactSources(date, results)
                }
                .onFailure { error ->
                    AppLog.error(
                        tag = "SunMoonViewModel",
                        message = "Failed to load daily data for $date",
                        throwable = error
                    )
                    _state.update {
                        it.copy(
                            results = emptyList(),
                            filteredResults = emptyList(),
                            compactSourceResults = emptyList(),
                            compactEvents = emptyList(),
                            isLoading = false,
                            error = Res.string.error_calc
                        )
                    }
                }
        }
    }

    private fun applyFilters() {
        _state.update { current ->
            val filtered = current.results.filter { it.city.key() in current.selectedCityKeys }
            val compactBase = if (current.compactSourceResults.isNotEmpty()) {
                current.compactSourceResults
            } else {
                current.results
            }
            val compactFiltered = compactBase.filter { it.city.key() in current.selectedCityKeys }
            val compact = buildCompactEvents(compactFiltered, current.userZone, current.selectedDate)
                .filter { shouldIncludeEvent(it, current) }
                .sortedWith(
                    compareBy<CompactEvent> { it.userInstant ?: Instant.MAX }
                        .thenBy { it.cityLabel }
                )
            current.copy(filteredResults = filtered, compactEvents = compact)
        }
    }

    private suspend fun loadCompactSources(date: LocalDate, baseResults: List<SunMoonTimes>) {
        runCatching {
            val prev = repository.loadDaily(date.minusDays(1))
            val next = repository.loadDaily(date.plusDays(1))
            prev + baseResults + next
        }.onSuccess { combined ->
            if (date != _state.value.selectedDate) return
            _state.update { it.copy(compactSourceResults = combined) }
            applyFilters()
        }.onFailure { error ->
            AppLog.error(
                tag = "SunMoonViewModel",
                message = "Failed to load compact source window for $date",
                throwable = error
            )
        }
    }

    private fun loadTrend(month: YearMonth) {
        val trendCity = _state.value.allCities.firstOrNull { it.label.equals("New York", ignoreCase = true) }
            ?: _state.value.allCities.firstOrNull()

        if (trendCity == null) {
            _state.update { it.copy(trend = it.trend.copy(error = Res.string.error_no_cities)) }
            return
        }

        scope.launch {
            val targetMonth = month
            _state.update { it.copy(trend = it.trend.copy(isLoading = true, error = null, trend = null)) }
            runCatching { repository.loadMonthlyTrend(targetMonth, trendCity) }
                .onSuccess { trend ->
                    if (targetMonth != _state.value.trend.month) return@launch
                    _state.update { it.copy(trend = it.trend.copy(isLoading = false, trend = trend)) }
                }
                .onFailure { error ->
                    AppLog.error(
                        tag = "SunMoonViewModel",
                        message = "Failed to load trend for $targetMonth",
                        throwable = error
                    )
                    _state.update {
                        it.copy(trend = it.trend.copy(isLoading = false, error = Res.string.error_trend))
                    }
                }
        }
    }

    private fun loadMoonPhases(month: YearMonth) {
        scope.launch {
            val targetMonth = month
            _state.update {
                it.copy(moonPhases = it.moonPhases.copy(isLoading = true, error = null, phases = emptyList()))
            }
            runCatching { moonPhaseRepository.loadMonth(targetMonth, _state.value.userZone) }
                .onSuccess { phases ->
                    if (targetMonth != _state.value.moonPhases.month) return@launch
                    _state.update { it.copy(moonPhases = it.moonPhases.copy(isLoading = false, phases = phases)) }
                }
                .onFailure { error ->
                    AppLog.error(
                        tag = "SunMoonViewModel",
                        message = "Failed to load moon phases for $targetMonth",
                        throwable = error
                    )
                    _state.update {
                        it.copy(
                            moonPhases = it.moonPhases.copy(
                                isLoading = false,
                                error = Res.string.error_moon_phases
                            )
                        )
                    }
                }
        }
    }

    private fun refreshAstro() {
        scope.launch {
            val snapshot = _state.value
            val date = snapshot.selectedDate
            val zoneId = snapshot.userZone
            val aspectOrbs = snapshot.astroCalendar.aspectOrbs
            val scope = snapshot.astroCalendar.scope
            _state.update { current ->
                current.copy(
                    astroCalendar = current.astroCalendar.copy(
                        isLoading = true,
                        error = null
                    )
                )
            }

            runCatching {
                loadAstroWindow(
                    centerDate = date,
                    zoneId = zoneId,
                    aspectOrbs = aspectOrbs,
                    scope = scope
                )
            }.onSuccess { loaded ->
                val current = _state.value
                if (date != current.selectedDate || scope != current.astroCalendar.scope) return@launch
                _state.update { current ->
                    current.copy(
                        astroCalendar = current.astroCalendar.copy(
                            allEvents = loaded.dayEvents,
                            week = loaded.weekSummaries,
                            isLoading = false,
                            error = null
                        )
                    )
                }
            }.onFailure { error ->
                AppLog.error(
                    tag = "SunMoonViewModel",
                    message = "Failed to load astro window for $date",
                    throwable = error
                )
                val current = _state.value
                if (date != current.selectedDate || scope != current.astroCalendar.scope) return@launch
                _state.update { current ->
                    current.copy(
                        astroCalendar = current.astroCalendar.copy(
                            allEvents = emptyList(),
                            week = emptyList(),
                            isLoading = false,
                            error = Res.string.error_astro_calendar
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadAstroWindow(
        centerDate: LocalDate,
        zoneId: ZoneId,
        aspectOrbs: Map<AstroAspectType, Double>,
        scope: AstroCalendarScope
    ): AstroLoadResult = coroutineScope {
        val days = (-3L..3L).map { centerDate.plusDays(it) }
        val jobs = days.associateWith { day ->
            async {
                astroCalendarRepository.loadDay(
                    date = day,
                    zoneId = zoneId,
                    aspectOrbs = aspectOrbs,
                    scope = scope
                )
            }
        }
        val eventsByDay = jobs.mapValues { (_, deferred) -> deferred.await() }
        val week = days.map { day ->
            AstroWeekDaySummary(
                date = day,
                eventCount = eventsByDay[day].orEmpty().size
            )
        }
        AstroLoadResult(
            dayEvents = eventsByDay[centerDate].orEmpty(),
            weekSummaries = week
        )
    }

    private data class AstroLoadResult(
        val dayEvents: List<AstroAspectEvent>,
        val weekSummaries: List<AstroWeekDaySummary>
    )

    private fun loadSettings() {
        scope.launch {
            val snapshot = runCatching { settingsRepository.loadSettings() }
                .onFailure { error ->
                    AppLog.error(
                        tag = "SunMoonViewModel",
                        message = "Failed to load persisted settings",
                        throwable = error
                    )
                }
                .getOrNull()
                ?: return@launch
            _state.update { current ->
                val availableKeys = current.allCities.map { it.key() }.toSet()
                val savedKeys = snapshot.selectedCityKeys
                val filteredKeys = savedKeys?.filter { it in availableKeys }?.toSet()
                val resolvedKeys = when {
                    savedKeys == null -> current.selectedCityKeys
                    savedKeys.isNotEmpty() && filteredKeys.isNullOrEmpty() -> current.selectedCityKeys
                    else -> filteredKeys ?: current.selectedCityKeys
                }
                current.copy(
                    themeStyle = snapshot.themeStyle ?: current.themeStyle,
                    themeMode = snapshot.themeMode ?: current.themeMode,
                    selectedCityKeys = resolvedKeys,
                    showUtcTime = snapshot.showUtcTime ?: current.showUtcTime,
                    showAzimuth = snapshot.showAzimuth ?: current.showAzimuth,
                    showSun = snapshot.showSun ?: current.showSun,
                    showMoon = snapshot.showMoon ?: current.showMoon,
                    showRise = snapshot.showRise ?: current.showRise,
                    showSet = snapshot.showSet ?: current.showSet,
                    astroCalendar = current.astroCalendar.copy(
                        aspectOrbs = snapshot.aspectOrbs ?: current.astroCalendar.aspectOrbs
                    )
                )
            }
            applyFilters()
            refreshAstro()
        }
    }

    private fun loadStatistics() {
        scope.launch {
            val entries = runCatching { statisticsRepository.loadEntries() }
                .onFailure { error ->
                    AppLog.error(
                        tag = "SunMoonViewModel",
                        message = "Failed to load statistics",
                        throwable = error
                    )
                }
                .getOrElse { emptyList() }
            _state.update { it.copy(stats = entries.sortedByDescending { it.createdAt }) }
        }
    }

    private fun persistSettings() {
        val current = _state.value
        scope.launch {
            runCatching {
                settingsRepository.saveSettings(
                    UserSettings(
                        themeStyle = current.themeStyle,
                        themeMode = current.themeMode,
                        selectedCityKeys = current.selectedCityKeys,
                        showUtcTime = current.showUtcTime,
                        showAzimuth = current.showAzimuth,
                        showSun = current.showSun,
                        showMoon = current.showMoon,
                        showRise = current.showRise,
                        showSet = current.showSet,
                        aspectOrbs = current.astroCalendar.aspectOrbs
                    )
                )
            }.onFailure { error ->
                AppLog.error(
                    tag = "SunMoonViewModel",
                    message = "Failed to persist settings",
                    throwable = error
                )
            }
        }
    }

    private fun sanitizeAspectOrb(rawOrb: Double): Double {
        val clamped = rawOrb.coerceIn(ASTRO_MIN_ORB_DEGREES, ASTRO_MAX_ORB_DEGREES)
        return round(clamped * 10.0) / 10.0
    }

    private fun startClock() {
        scope.launch {
            while (isActive) {
                _state.update { it.copy(nowInstant = Instant.now()) }
                delay(30_000)
            }
        }
    }

    override fun close() {
        scope.cancel()
    }

    private fun shouldIncludeEvent(event: CompactEvent, state: SunMoonUiState): Boolean {
        val isSun = event.eventType == CompactEventType.Sunrise || event.eventType == CompactEventType.Sunset
        val isRise = event.eventType == CompactEventType.Sunrise || event.eventType == CompactEventType.Moonrise
        val isSet = event.eventType == CompactEventType.Sunset || event.eventType == CompactEventType.Moonset
        val sunMoonAllowed = if (isSun) state.showSun else state.showMoon
        val riseSetAllowed = if (isRise) state.showRise else state.showSet
        return sunMoonAllowed && riseSetAllowed
    }
}

