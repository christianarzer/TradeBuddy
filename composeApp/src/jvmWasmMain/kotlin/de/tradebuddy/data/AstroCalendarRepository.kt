package de.tradebuddy.data

import de.tradebuddy.domain.model.AstroAspectEvent
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.AstroCalendarScope
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private data class CacheKey(
        val date: LocalDate,
        val zoneId: String,
        val scope: AstroCalendarScope,
        val orbsSignature: String
    )

    private val cacheMutex = Mutex()
    private val dayCache = LinkedHashMap<CacheKey, List<AstroAspectEvent>>()
    private val cacheMaxEntries = 180

    override suspend fun loadDay(
        date: LocalDate,
        zoneId: ZoneId,
        aspectOrbs: Map<AstroAspectType, Double>,
        scope: AstroCalendarScope
    ): List<AstroAspectEvent> {
        val key = CacheKey(
            date = date,
            zoneId = zoneId.id,
            scope = scope,
            orbsSignature = AstroAspectType.entries.joinToString("|") { aspect ->
                "${aspect.name}:${aspectOrbs[aspect]}"
            }
        )

        cacheMutex.withLock {
            dayCache[key]?.let { return it }
        }

        val computed = withContext(dispatcher) {
            calculator.moonPlanetAspects(
                date = date,
                zoneId = zoneId,
                aspectOrbs = aspectOrbs,
                scope = scope
            )
        }

        return cacheMutex.withLock {
            dayCache[key]?.let { return it }
            dayCache[key] = computed
            while (dayCache.size > cacheMaxEntries) {
                val eldest = dayCache.entries.firstOrNull()?.key ?: break
                dayCache.remove(eldest)
            }
            computed
        }
    }
}
