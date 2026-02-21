package de.tradebuddy.data

import de.tradebuddy.domain.model.MoonPhaseEvent
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MoonPhaseRepository {
    suspend fun loadMonth(month: YearMonth, zoneId: ZoneId): List<MoonPhaseEvent>
}

class DefaultMoonPhaseRepository(
    private val calculator: AstroCalculator,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : MoonPhaseRepository {

    override suspend fun loadMonth(month: YearMonth, zoneId: ZoneId): List<MoonPhaseEvent> =
        withContext(dispatcher) {
            calculator.moonPhases(month, zoneId)
        }
}

