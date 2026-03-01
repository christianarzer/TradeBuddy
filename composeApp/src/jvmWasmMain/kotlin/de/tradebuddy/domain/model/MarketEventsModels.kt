package de.tradebuddy.domain.model

import java.time.Instant
import java.time.LocalDate

enum class MarketEventImpact(val priority: Int) {
    Low(1),
    Medium(2),
    High(3)
}

enum class MarketRegion(val label: String) {
    UnitedStates("USA"),
    EuroArea("Euro Area"),
    UnitedKingdom("UK"),
    Japan("Japan"),
    China("China"),
    Global("Global")
}

enum class MarketEventType(val label: String) {
    InterestRate("Interest Rate"),
    Inflation("Inflation"),
    Employment("Employment"),
    Gdp("GDP"),
    CentralBank("Central Bank"),
    Manufacturing("Manufacturing"),
    ConsumerConfidence("Consumer Confidence"),
    Other("Other")
}

data class MarketEvent(
    val id: String,
    val title: String,
    val type: MarketEventType,
    val region: MarketRegion,
    val countryCode: String,
    val scheduledAt: Instant,
    val impact: MarketEventImpact,
    val source: String,
    val description: String? = null,
    val actual: String? = null,
    val forecast: String? = null,
    val previous: String? = null
) {
    val isMarketMoving: Boolean
        get() = impact == MarketEventImpact.High ||
            type == MarketEventType.InterestRate ||
            type == MarketEventType.CentralBank
}

data class MarketEventDayGroup(
    val date: LocalDate,
    val events: List<MarketEvent>
)

data class MarketEventsFilter(
    val impacts: Set<MarketEventImpact> = MarketEventImpact.entries.toSet(),
    val regions: Set<MarketRegion> = emptySet(),
    val eventTypes: Set<MarketEventType> = emptySet(),
    val watchlistOnly: Boolean = false
)

data class EventWatchPreference(
    val eventId: String,
    val notifyEnabled: Boolean = false,
    val reminderMinutesBefore: Int? = null
)

