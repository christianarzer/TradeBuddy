package de.tradebuddy.presentation

import de.tradebuddy.data.MarketEventsRepository
import de.tradebuddy.domain.model.EventWatchPreference
import de.tradebuddy.domain.model.MarketEvent
import de.tradebuddy.domain.model.MarketEventDayGroup
import de.tradebuddy.domain.model.MarketEventImpact
import de.tradebuddy.domain.model.MarketEventType
import de.tradebuddy.domain.model.MarketEventsFilter
import de.tradebuddy.domain.model.MarketRegion
import de.tradebuddy.logging.AppLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class MarketEventsViewModel(
    private val repository: MarketEventsRepository
) : AutoCloseable {
    private companion object {
        const val PastWindowHours = 8L
        const val FutureWindowDays = 120L
        const val DefaultRefreshMaxAgeMillis = 2L * 60L * 60L * 1000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val userZone: ZoneId = ZoneId.systemDefault()

    private val _state = MutableStateFlow(MarketEventsUiState())
    val state: StateFlow<MarketEventsUiState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
        startClock()
    }

    fun refreshIfNeeded(
        forceRefresh: Boolean = false,
        maxAgeMillis: Long = DefaultRefreshMaxAgeMillis
    ) {
        val current = _state.value
        if (current.isLoading && !forceRefresh) return
        val nowMillis = Instant.now().toEpochMilli()
        val isStale = current.lastUpdated == null ||
            (nowMillis - current.lastUpdated.toEpochMilli()) >= maxAgeMillis
        if (forceRefresh || current.allEvents.isEmpty() || isStale) {
            refresh(forceRefresh = forceRefresh)
        }
    }

    fun refresh(forceRefresh: Boolean = false) {
        if (refreshJob?.isActive == true && !forceRefresh) return
        refreshJob?.cancel()
        refreshJob = scope.launch {
            AppLog.info(
                tag = "MarketEventsViewModel",
                message = "Refreshing market events (forceRefresh=$forceRefresh)"
            )
            _state.update {
                it.copy(
                    isLoading = true,
                    loadingProgress = 0.08f,
                    loadingSourceName = null,
                    loadingSourceIndex = 0,
                    loadingSourceCount = 0,
                    errorMessage = null
                )
            }
            // Give Compose one frame to render loading state before IO-heavy refresh starts.
            yield()
            val (from, to) = resolveRefreshWindow()

            runCatching {
                val events = withContext(Dispatchers.Default) {
                    repository.loadUpcomingEvents(
                        from = from,
                        to = to,
                        forceRefresh = forceRefresh,
                        onSourceProgress = { progress ->
                            val mappedProgress = (0.08f + progress.fraction.coerceIn(0f, 1f) * 0.80f).coerceIn(0f, 0.92f)
                            _state.update { current ->
                                if (!current.isLoading) {
                                    current
                                } else {
                                    val currentProgress = current.loadingProgress ?: 0.08f
                                    current.copy(
                                        loadingProgress = maxOf(currentProgress, mappedProgress),
                                        loadingSourceName = progress.sourceName,
                                        loadingSourceIndex = progress.sourceIndex,
                                        loadingSourceCount = progress.sourceCount
                                    )
                                }
                            }
                        }
                    )
                }
                _state.update { current ->
                    current.copy(
                        loadingProgress = 0.95f,
                        loadingSourceName = null,
                        loadingSourceIndex = 0,
                        loadingSourceCount = 0
                    )
                }
                val watchPreferences = withContext(Dispatchers.Default) {
                    repository.loadWatchPreferences()
                }
                events to watchPreferences
            }.onSuccess { (events, watchPreferences) ->
                AppLog.info(
                    tag = "MarketEventsViewModel",
                    message = "Loaded ${events.size} market events and ${watchPreferences.size} watch entries"
                )
                _state.update { current ->
                    current
                        .copy(
                            isLoading = false,
                            loadingProgress = null,
                            loadingSourceName = null,
                            loadingSourceIndex = 0,
                            loadingSourceCount = 0,
                            errorMessage = null,
                            allEvents = events,
                            watchPreferences = watchPreferences,
                            lastUpdated = Instant.now()
                        )
                        .withFilteredGroups()
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                AppLog.error(
                    tag = "MarketEventsViewModel",
                    message = "Failed to refresh market events",
                    throwable = error
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = null,
                        loadingSourceName = null,
                        loadingSourceIndex = 0,
                        loadingSourceCount = 0,
                        errorMessage = error.message ?: "Markttermine konnten nicht geladen werden"
                    )
                }
            }
        }
    }

    fun toggleImpact(impact: MarketEventImpact) {
        _state.update { current ->
            val next = current.filter.impacts.toMutableSet().apply {
                if (contains(impact)) remove(impact) else add(impact)
            }
            current.copy(filter = current.filter.copy(impacts = next.ifEmpty { setOf(impact) })).withFilteredGroups()
        }
    }

    fun toggleRegion(region: MarketRegion) {
        _state.update { current ->
            val next = current.filter.regions.toMutableSet().apply {
                if (contains(region)) remove(region) else add(region)
            }
            current.copy(filter = current.filter.copy(regions = next)).withFilteredGroups()
        }
    }

    fun clearRegionFilters() {
        _state.update { current ->
            current.copy(filter = current.filter.copy(regions = emptySet())).withFilteredGroups()
        }
    }

    fun toggleEventType(eventType: MarketEventType) {
        _state.update { current ->
            val next = current.filter.eventTypes.toMutableSet().apply {
                if (contains(eventType)) remove(eventType) else add(eventType)
            }
            current.copy(filter = current.filter.copy(eventTypes = next)).withFilteredGroups()
        }
    }

    fun clearEventTypeFilters() {
        _state.update { current ->
            current.copy(filter = current.filter.copy(eventTypes = emptySet())).withFilteredGroups()
        }
    }

    fun setWatchlistOnly(enabled: Boolean) {
        _state.update { current ->
            current.copy(filter = current.filter.copy(watchlistOnly = enabled)).withFilteredGroups()
        }
    }

    fun clearFilters() {
        _state.update { current ->
            current.copy(
                selectedDay = null,
                filter = MarketEventsFilter()
            ).withFilteredGroups()
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { current ->
            current.copy(searchQuery = query).withFilteredGroups()
        }
    }

    fun selectDay(day: LocalDate?) {
        _state.update { current ->
            current.copy(selectedDay = day).withFilteredGroups()
        }
    }

    fun selectToday() {
        selectDay(LocalDate.now(userZone))
    }

    fun selectTomorrow() {
        selectDay(LocalDate.now(userZone).plusDays(1))
    }

    fun clearDaySelection() {
        selectDay(null)
    }

    fun toggleWatchlist(eventId: String, watchlisted: Boolean) {
        scope.launch {
            runCatching {
                repository.toggleWatchlist(eventId, watchlisted)
                repository.loadWatchPreferences()
            }.onSuccess { updated ->
                _state.update { current ->
                    current.copy(watchPreferences = updated).withFilteredGroups()
                }
                AppLog.info(
                    tag = "MarketEventsViewModel",
                    message = if (watchlisted) "Event $eventId zur Merkliste hinzugefügt" else "Event $eventId aus Merkliste entfernt"
                )
            }.onFailure { error ->
                AppLog.error(
                    tag = "MarketEventsViewModel",
                    message = "Failed to update watchlist state for event $eventId",
                    throwable = error
                )
                _state.update { current ->
                    current.copy(errorMessage = error.message ?: "Merkliste konnte nicht aktualisiert werden")
                }
            }
        }
    }

    fun setReminder(eventId: String, enabled: Boolean, minutesBefore: Int?) {
        scope.launch {
            runCatching {
                repository.setReminder(eventId, enabled, minutesBefore)
                repository.loadWatchPreferences()
            }.onSuccess { updated ->
                _state.update { current ->
                    current.copy(watchPreferences = updated).withFilteredGroups()
                }
                AppLog.info(
                    tag = "MarketEventsViewModel",
                    message = if (enabled) {
                        "Erinnerung für Event $eventId aktiviert (${minutesBefore ?: 30} Min)"
                    } else {
                        "Erinnerung für Event $eventId deaktiviert"
                    }
                )
            }.onFailure { error ->
                AppLog.error(
                    tag = "MarketEventsViewModel",
                    message = "Failed to update reminder for event $eventId",
                    throwable = error
                )
                _state.update { current ->
                    current.copy(errorMessage = error.message ?: "Erinnerung konnte nicht aktualisiert werden")
                }
            }
        }
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

    private fun resolveRefreshWindow(): Pair<Instant, Instant> {
        val dayStart = LocalDate.now(userZone).atStartOfDay(userZone).toInstant()
        val from = Instant.ofEpochMilli(dayStart.toEpochMilli() - PastWindowHours * 60L * 60L * 1000L)
        val to = Instant.ofEpochMilli(
            dayStart.toEpochMilli() + (FutureWindowDays + 1L) * 24L * 60L * 60L * 1000L - 1_000L
        )
        return from to to
    }

    private fun MarketEventsUiState.withFilteredGroups(): MarketEventsUiState {
        val watchlistIds = watchPreferences.map { it.eventId }.toSet()
        val normalizedQuery = searchQuery.trim().lowercase()
        val filtered = allEvents
            .asSequence()
            .filter { event -> event.impact in filter.impacts }
            .filter { event -> filter.regions.isEmpty() || event.region in filter.regions }
            .filter { event -> filter.eventTypes.isEmpty() || event.type in filter.eventTypes }
            .filter { event -> !filter.watchlistOnly || event.id in watchlistIds }
            .filter { event ->
                normalizedQuery.isBlank() ||
                    event.title.lowercase().contains(normalizedQuery) ||
                    event.type.label.lowercase().contains(normalizedQuery) ||
                    event.region.label.lowercase().contains(normalizedQuery) ||
                    event.countryCode.lowercase().contains(normalizedQuery) ||
                    (event.description?.lowercase()?.contains(normalizedQuery) == true)
            }
            .filter { event ->
                val selected = selectedDay ?: return@filter true
                event.scheduledAt.atZone(userZone).toLocalDate() == selected
            }
            .sortedWith(
                compareBy<MarketEvent> { it.scheduledAt }
                    .thenByDescending { it.impact.priority }
                    .thenBy { it.title }
            )
            .toList()

        val groups = filtered
            .groupBy { it.scheduledAt.atZone(userZone).toLocalDate() }
            .entries
            .sortedBy { it.key }
            .map { entry ->
                MarketEventDayGroup(
                    date = entry.key,
                    events = entry.value
                )
            }
        return copy(filteredGroups = groups)
    }
}
