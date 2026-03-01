package de.tradebuddy.presentation

import de.tradebuddy.data.MarketEventsRepository
import de.tradebuddy.domain.model.EventWatchPreference
import de.tradebuddy.domain.model.MarketEvent
import de.tradebuddy.domain.model.MarketEventDayGroup
import de.tradebuddy.domain.model.MarketEventImpact
import de.tradebuddy.domain.model.MarketEventType
import de.tradebuddy.domain.model.MarketEventsFilter
import de.tradebuddy.domain.model.MarketRegion
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MarketEventsViewModel(
    private val repository: MarketEventsRepository
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val userZone: ZoneId = ZoneId.systemDefault()

    private val _state = MutableStateFlow(MarketEventsUiState())
    val state: StateFlow<MarketEventsUiState> = _state.asStateFlow()

    init {
        refresh()
        startClock()
    }

    fun refresh() {
        scope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val nowEpoch = Instant.now().toEpochMilli()
            val from = Instant.ofEpochMilli(nowEpoch - (60L * 60L * 8L * 1000L))
            val to = Instant.ofEpochMilli(nowEpoch + (60L * 60L * 24L * 45L * 1000L))

            runCatching {
                val events = repository.loadUpcomingEvents(from, to)
                val watchPreferences = repository.loadWatchPreferences()
                events to watchPreferences
            }.onSuccess { (events, watchPreferences) ->
                _state.update { current ->
                    current
                        .copy(
                            isLoading = false,
                            errorMessage = null,
                            allEvents = events,
                            watchPreferences = watchPreferences,
                            lastUpdated = Instant.now()
                        )
                        .withFilteredGroups()
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
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
            repository.toggleWatchlist(eventId, watchlisted)
            val updated = repository.loadWatchPreferences()
            _state.update { current ->
                current.copy(watchPreferences = updated).withFilteredGroups()
            }
        }
    }

    fun setReminder(eventId: String, enabled: Boolean, minutesBefore: Int?) {
        scope.launch {
            repository.setReminder(eventId, enabled, minutesBefore)
            val updated = repository.loadWatchPreferences()
            _state.update { current ->
                current.copy(watchPreferences = updated).withFilteredGroups()
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
