package de.tradebuddy.data

import de.tradebuddy.domain.model.AstroAspectEvent
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.AstroCalendarScope
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AstroCalendarRepository {
    suspend fun loadDay(
        date: LocalDate,
        zoneId: ZoneId,
        aspectOrbs: Map<AstroAspectType, Double>,
        scope: AstroCalendarScope
    ): List<AstroAspectEvent>
}

class DefaultAstroCalendarRepository(
    private val calculator: AstroCalculator,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AstroCalendarRepository {

    override suspend fun loadDay(
        date: LocalDate,
        zoneId: ZoneId,
        aspectOrbs: Map<AstroAspectType, Double>,
        scope: AstroCalendarScope
    ): List<AstroAspectEvent> =
        withContext(dispatcher) {
            calculator.moonPlanetAspects(
                date = date,
                zoneId = zoneId,
                aspectOrbs = aspectOrbs,
                scope = scope
            )
        }
}
