package de.tradebuddy.presentation

import de.tradebuddy.domain.model.EventWatchPreference
import de.tradebuddy.domain.model.MarketEvent
import de.tradebuddy.domain.model.MarketEventDayGroup
import de.tradebuddy.domain.model.MarketEventImpact
import de.tradebuddy.domain.model.MarketEventType
import de.tradebuddy.domain.model.MarketEventsFilter
import de.tradebuddy.domain.model.MarketRegion
import java.time.Instant
import java.time.LocalDate

data class MarketEventsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val nowInstant: Instant = Instant.now(),
    val searchQuery: String = "",
    val allEvents: List<MarketEvent> = emptyList(),
    val filteredGroups: List<MarketEventDayGroup> = emptyList(),
    val watchPreferences: Set<EventWatchPreference> = emptySet(),
    val filter: MarketEventsFilter = MarketEventsFilter(),
    val selectedDay: LocalDate? = null,
    val lastUpdated: Instant? = null,
    val availableRegions: Set<MarketRegion> = MarketRegion.entries.toSet(),
    val availableEventTypes: Set<MarketEventType> = MarketEventType.entries.toSet(),
    val availableImpactLevels: Set<MarketEventImpact> = MarketEventImpact.entries.toSet()
)
