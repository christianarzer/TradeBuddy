package de.tradebuddy.data

import de.tradebuddy.domain.model.EventWatchPreference
import de.tradebuddy.domain.model.MarketEvent
import de.tradebuddy.domain.model.MarketEventImpact
import de.tradebuddy.domain.model.MarketEventType
import de.tradebuddy.domain.model.MarketRegion
import java.time.Instant
import kotlin.time.Instant as KotlinInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface MarketEventsDataSource {
    suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent>
}

object NoopMarketEventsDataSource : MarketEventsDataSource {
    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> = emptyList()
}

class TradingEconomicsMarketEventsDataSource(
    private val credentialsProvider: () -> String = { "guest:guest" },
    private val endpoint: String = "https://api.tradingeconomics.com/calendar",
    private val fetcher: suspend (String, Map<String, String>) -> String = { _, _ -> "[]" }
) : MarketEventsDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> {
        val credentials = credentialsProvider().ifBlank { "guest:guest" }
        val url = "$endpoint?c=${credentials.encodeAsQueryParameter()}&f=json"
        val payload = fetcher(
            url,
            mapOf(
                "Accept" to "application/json",
                "User-Agent" to "TradeBuddy/1.0"
            )
        )
        val raw = json.decodeFromString(ListSerializer(TradingEconomicsCalendarEntry.serializer()), payload)
        return raw.asSequence()
            .mapNotNull(TradingEconomicsCalendarEntry::toMarketEventOrNull)
            .filter { it.scheduledAt in from..to }
            .toList()
    }
}

class FredMarketEventsDataSource(
    private val apiKeyProvider: () -> String = { DemoFredApiKey },
    private val releasesEndpoint: String = "https://api.stlouisfed.org/fred/releases",
    private val releaseDatesEndpoint: String = "https://api.stlouisfed.org/fred/release/dates",
    private val fetcher: suspend (String, Map<String, String>) -> String = { _, _ -> "{}" }
) : MarketEventsDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> {
        val apiKey = apiKeyProvider().ifBlank { DemoFredApiKey }
        val headers = mapOf(
            "Accept" to "application/json",
            "User-Agent" to "TradeBuddy/1.0"
        )

        val releasesUrl = buildString {
            append(releasesEndpoint)
            append("?api_key=")
            append(apiKey.encodeAsQueryParameter())
            append("&file_type=json&limit=1000&order_by=name&sort_order=asc")
        }
        val releasesPayload = fetcher(releasesUrl, headers)
        val releases = json.decodeFromString(FredReleasesPayload.serializer(), releasesPayload)
            .releases
            .mapNotNull { dto ->
                val id = dto.id ?: return@mapNotNull null
                val name = dto.name?.trim().takeUnless { it.isNullOrEmpty() } ?: return@mapNotNull null
                val classification = classifyFredRelease(name) ?: return@mapNotNull null
                FredTrackedRelease(
                    id = id,
                    name = name,
                    classification = classification
                )
            }
            .distinctBy { it.id }
            .sortedByDescending { it.classification.priorityScore }
            .take(MaxFredReleases)

        if (releases.isEmpty()) return emptyList()

        val events = mutableListOf<MarketEvent>()
        for (release in releases) {
            val datesUrl = buildString {
                append(releaseDatesEndpoint)
                append("?release_id=")
                append(release.id)
                append("&api_key=")
                append(apiKey.encodeAsQueryParameter())
                append("&file_type=json&include_release_dates_with_no_data=true")
                append("&limit=1000&sort_order=asc")
            }
            val datesPayload = fetcher(datesUrl, headers)
            val dateEntries = json.decodeFromString(FredReleaseDatesPayload.serializer(), datesPayload).releaseDates
            dateEntries.asSequence()
                .mapNotNull { entry ->
                    val dateText = entry.date?.trim().takeUnless { it.isNullOrEmpty() } ?: return@mapNotNull null
                    val instant = parseMarketEventInstant(
                        "${dateText}T${DefaultReleaseHourUtc.toString().padStart(2, '0')}:${DefaultReleaseMinuteUtc.toString().padStart(2, '0')}:00Z"
                    ) ?: return@mapNotNull null
                    if (instant !in from..to) return@mapNotNull null
                    MarketEvent(
                        id = "fred-${release.id}-${dateText}",
                        title = release.name,
                        type = release.classification.type,
                        region = MarketRegion.UnitedStates,
                        countryCode = "US",
                        scheduledAt = instant,
                        impact = release.classification.impact,
                        source = "FRED",
                        description = "FRED Veröffentlichungstermine (Uhrzeit approximiert)"
                    )
                }
                .forEach(events::add)
        }

        return events
            .distinctBy { it.id }
            .sortedBy { it.scheduledAt }
    }

    private companion object {
        const val DefaultReleaseHourUtc = 13
        const val DefaultReleaseMinuteUtc = 30
        const val MaxFredReleases = 16
        const val DemoFredApiKey = "abcdefghijklmnopqrstuvwxyz123456"
    }
}

interface MarketEventsRepository {
    suspend fun loadUpcomingEvents(from: Instant, to: Instant): List<MarketEvent>
    suspend fun loadWatchPreferences(): Set<EventWatchPreference>
    suspend fun toggleWatchlist(eventId: String, watchlisted: Boolean)
    suspend fun setReminder(eventId: String, enabled: Boolean, minutesBefore: Int?)
}

class DefaultMarketEventsRepository(
    private val watchlistDocument: StorageDocument,
    private val remoteDataSource: MarketEventsDataSource = NoopMarketEventsDataSource,
    private val cacheDocument: StorageDocument? = null
) : MarketEventsRepository {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private var remoteCache: CachedRemoteSnapshot? = null

    override suspend fun loadUpcomingEvents(from: Instant, to: Instant): List<MarketEvent> {
        return loadRemoteWithCache(from, to).sortedWith(
            compareByDescending<MarketEvent> { it.impact.priority }
                .thenBy { it.scheduledAt }
                .thenBy { it.title }
        )
    }

    override suspend fun loadWatchPreferences(): Set<EventWatchPreference> {
        val payload = watchlistDocument.readText() ?: return emptySet()
        return runCatching { json.decodeFromString(WatchlistPayload.serializer(), payload) }
            .getOrElse { WatchlistPayload() }
            .items
            .map { dto ->
                EventWatchPreference(
                    eventId = dto.eventId,
                    notifyEnabled = dto.notifyEnabled,
                    reminderMinutesBefore = dto.reminderMinutesBefore
                )
            }
            .toSet()
    }

    override suspend fun toggleWatchlist(eventId: String, watchlisted: Boolean) {
        val current = loadWatchPreferences().toMutableMap()
        if (watchlisted) {
            val existing = current[eventId]
            current[eventId] = existing ?: EventWatchPreference(eventId = eventId)
        } else {
            current.remove(eventId)
        }
        saveWatchPreferences(current.values.toSet())
    }

    override suspend fun setReminder(eventId: String, enabled: Boolean, minutesBefore: Int?) {
        val current = loadWatchPreferences().toMutableMap()
        val existing = current[eventId] ?: EventWatchPreference(eventId = eventId)
        current[eventId] = existing.copy(
            notifyEnabled = enabled,
            reminderMinutesBefore = minutesBefore?.coerceIn(5, 720)
        )
        saveWatchPreferences(current.values.toSet())
    }

    private suspend fun loadRemoteWithCache(from: Instant, to: Instant): List<MarketEvent> {
        val now = Instant.now()
        val cached = remoteCache
        if (cached != null && cached.validUntil.isAfter(now) && cached.contains(from, to)) {
            return cached.events.filter { it.scheduledAt in from..to }
        }

        val persistedCache = loadPersistedCache()
        if (persistedCache != null && persistedCache.validUntil.isAfter(now) && persistedCache.contains(from, to)) {
            remoteCache = persistedCache
            return persistedCache.events.filter { it.scheduledAt in from..to }
        }

        return runCatching {
            remoteDataSource.fetchUpcoming(from, to)
        }.mapCatching { fetched ->
            if (fetched.isNotEmpty()) {
                val snapshot = CachedRemoteSnapshot(
                    validUntil = Instant.ofEpochMilli(now.toEpochMilli() + (RemoteCacheSeconds * 1000L)),
                    rangeFrom = from,
                    rangeTo = to,
                    events = fetched
                )
                remoteCache = snapshot
                persistCache(snapshot)
            }
            fetched
        }.getOrElse { fetchError ->
            val fallback = persistedCache?.takeIf { snapshot ->
                snapshot.events.isNotEmpty() &&
                    snapshot.fetchedAt.isAfter(Instant.ofEpochMilli(now.toEpochMilli() - (StaleFallbackSeconds * 1000L)))
            }
            if (fallback != null) {
                fallback.events.filter { it.scheduledAt in from..to }
            } else {
                throw fetchError
            }
        }
    }

    private suspend fun saveWatchPreferences(items: Set<EventWatchPreference>) {
        if (items.isEmpty()) {
            watchlistDocument.clear()
            return
        }
        val payload = WatchlistPayload(
            items = items
                .sortedBy { it.eventId }
                .map { pref ->
                    WatchPreferenceDto(
                        eventId = pref.eventId,
                        notifyEnabled = pref.notifyEnabled,
                        reminderMinutesBefore = pref.reminderMinutesBefore
                    )
                }
        )
        watchlistDocument.writeText(json.encodeToString(WatchlistPayload.serializer(), payload))
    }

    private fun Set<EventWatchPreference>.toMutableMap(): MutableMap<String, EventWatchPreference> =
        associateBy { it.eventId }.toMutableMap()

    private data class CachedRemoteSnapshot(
        val validUntil: Instant,
        val rangeFrom: Instant,
        val rangeTo: Instant,
        val fetchedAt: Instant = Instant.now(),
        val events: List<MarketEvent>
    ) {
        fun contains(from: Instant, to: Instant): Boolean = from >= rangeFrom && to <= rangeTo
    }

    private companion object {
        const val RemoteCacheSeconds = 10 * 60L
        const val StaleFallbackSeconds = 24 * 60 * 60L
    }

    private suspend fun loadPersistedCache(): CachedRemoteSnapshot? {
        val document = cacheDocument ?: return null
        val payload = document.readText() ?: return null
        val dto = runCatching { json.decodeFromString(RemoteCachePayload.serializer(), payload) }.getOrNull()
            ?: return null
        val events = dto.events.mapNotNull(RemoteCacheEventDto::toDomainOrNull)
        if (events.isEmpty()) return null
        return CachedRemoteSnapshot(
            validUntil = Instant.ofEpochMilli(dto.validUntilEpochMillis),
            rangeFrom = Instant.ofEpochMilli(dto.rangeFromEpochMillis),
            rangeTo = Instant.ofEpochMilli(dto.rangeToEpochMillis),
            fetchedAt = Instant.ofEpochMilli(dto.fetchedAtEpochMillis),
            events = events
        )
    }

    private suspend fun persistCache(snapshot: CachedRemoteSnapshot) {
        val document = cacheDocument ?: return
        val payload = RemoteCachePayload(
            schemaVersion = 1,
            fetchedAtEpochMillis = snapshot.fetchedAt.toEpochMilli(),
            validUntilEpochMillis = snapshot.validUntil.toEpochMilli(),
            rangeFromEpochMillis = snapshot.rangeFrom.toEpochMilli(),
            rangeToEpochMillis = snapshot.rangeTo.toEpochMilli(),
            events = snapshot.events.map(MarketEvent::toCacheDto)
        )
        document.writeText(json.encodeToString(RemoteCachePayload.serializer(), payload))
    }
}

@Serializable
private data class WatchlistPayload(
    val schemaVersion: Int = 1,
    val items: List<WatchPreferenceDto> = emptyList()
)

@Serializable
private data class WatchPreferenceDto(
    val eventId: String,
    val notifyEnabled: Boolean = false,
    val reminderMinutesBefore: Int? = null
)

@Serializable
private data class RemoteCachePayload(
    val schemaVersion: Int = 1,
    val fetchedAtEpochMillis: Long,
    val validUntilEpochMillis: Long,
    val rangeFromEpochMillis: Long,
    val rangeToEpochMillis: Long,
    val events: List<RemoteCacheEventDto> = emptyList()
)

@Serializable
private data class RemoteCacheEventDto(
    val id: String,
    val title: String,
    val type: String,
    val region: String,
    val countryCode: String,
    val scheduledAtEpochMillis: Long,
    val impact: String,
    val source: String,
    val description: String? = null,
    val actual: String? = null,
    val forecast: String? = null,
    val previous: String? = null
) {
    fun toDomainOrNull(): MarketEvent? {
        val resolvedType = runCatching { MarketEventType.valueOf(type) }.getOrNull() ?: return null
        val resolvedRegion = runCatching { MarketRegion.valueOf(region) }.getOrNull() ?: return null
        val resolvedImpact = runCatching { MarketEventImpact.valueOf(impact) }.getOrNull() ?: return null
        return MarketEvent(
            id = id,
            title = title,
            type = resolvedType,
            region = resolvedRegion,
            countryCode = countryCode,
            scheduledAt = Instant.ofEpochMilli(scheduledAtEpochMillis),
            impact = resolvedImpact,
            source = source,
            description = description,
            actual = actual,
            forecast = forecast,
            previous = previous
        )
    }
}

private data class FredTrackedRelease(
    val id: Int,
    val name: String,
    val classification: FredReleaseClassification
)

private data class FredReleaseClassification(
    val type: MarketEventType,
    val impact: MarketEventImpact,
    val priorityScore: Int
)

@Serializable
private data class FredReleasesPayload(
    val releases: List<FredReleaseDto> = emptyList()
)

@Serializable
private data class FredReleaseDto(
    val id: Int? = null,
    val name: String? = null
)

@Serializable
private data class FredReleaseDatesPayload(
    @SerialName("release_dates")
    val releaseDates: List<FredReleaseDateDto> = emptyList()
)

@Serializable
private data class FredReleaseDateDto(
    val date: String? = null
)

@Serializable
private data class TradingEconomicsCalendarEntry(
    @SerialName("CalendarId")
    val calendarId: String? = null,
    @SerialName("Date")
    val date: String? = null,
    @SerialName("Country")
    val country: String? = null,
    @SerialName("Category")
    val category: String? = null,
    @SerialName("Event")
    val event: String? = null,
    @SerialName("Source")
    val source: String? = null,
    @SerialName("Actual")
    val actual: String? = null,
    @SerialName("Forecast")
    val forecast: String? = null,
    @SerialName("Previous")
    val previous: String? = null,
    @SerialName("Importance")
    val importance: Int? = null
) {
    fun toMarketEventOrNull(): MarketEvent? {
        val scheduledAt = parseMarketEventInstant(date) ?: return null
        val title = event?.trim().takeUnless { it.isNullOrEmpty() }
            ?: category?.trim().takeUnless { it.isNullOrEmpty() }
            ?: return null

        val countryName = country?.trim().orEmpty()
        val region = inferRegion(countryName)
        val countryCode = inferCountryCode(countryName)
        val impact = when {
            (importance ?: 1) >= 3 -> MarketEventImpact.High
            (importance ?: 1) == 2 -> MarketEventImpact.Medium
            else -> MarketEventImpact.Low
        }
        return MarketEvent(
            id = calendarId?.trim().takeUnless { it.isNullOrBlank() }
                ?: "$countryCode-${title.take(24)}-${scheduledAt.toEpochMilli()}",
            title = title,
            type = inferEventType(category = category, event = event),
            region = region,
            countryCode = countryCode,
            scheduledAt = scheduledAt,
            impact = impact,
            source = source?.trim().takeUnless { it.isNullOrEmpty() } ?: "Trading Economics",
            description = category?.trim(),
            actual = actual?.trim().takeUnless { it.isNullOrEmpty() },
            forecast = forecast?.trim().takeUnless { it.isNullOrEmpty() },
            previous = previous?.trim().takeUnless { it.isNullOrEmpty() }
        )
    }
}

private fun parseMarketEventInstant(raw: String?): Instant? {
    val value = raw?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
    runCatching { parseIsoToInstant(value) }.getOrNull()?.let { return it }
    val normalized = normalizeTradingEconomicsDate(value)
    return runCatching { parseIsoToInstant(normalized) }.getOrNull()
}

private fun parseIsoToInstant(value: String): Instant {
    val parsed = KotlinInstant.parse(value)
    return Instant.ofEpochMilli(parsed.toEpochMilliseconds())
}

private fun normalizeTradingEconomicsDate(raw: String): String {
    val value = raw.trim()
    if (value.endsWith("Z", ignoreCase = true) || value.contains('+')) return value
    if (' ' in value && 'T' !in value) {
        return normalizeTradingEconomicsDate(value.replace(' ', 'T'))
    }
    return when {
        value.length == 10 && value[4] == '-' && value[7] == '-' -> "${value}T00:00:00Z"
        value.length == 16 && value[4] == '-' && value[7] == '-' && value[10] == 'T' -> "${value}:00Z"
        value.length == 19 && value[4] == '-' && value[7] == '-' && value[10] == 'T' -> "${value}Z"
        else -> value
    }
}

private fun String.encodeAsQueryParameter(): String {
    if (isEmpty()) return this
    val safe = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~"
    val out = StringBuilder(length + 8)
    for (char in this) {
        if (char in safe) {
            out.append(char)
        } else {
            out.append('%')
            out.append(char.code.toString(16).uppercase().padStart(2, '0'))
        }
    }
    return out.toString()
}

private fun classifyFredRelease(name: String): FredReleaseClassification? {
    val text = name.lowercase()
    return when {
        "federal open market committee" in text || "fomc" in text || "interest rate" in text || "fed funds" in text -> {
            FredReleaseClassification(
                type = MarketEventType.InterestRate,
                impact = MarketEventImpact.High,
                priorityScore = 1000
            )
        }

        "consumer price index" in text || "cpi" in text || "producer price index" in text || "ppi" in text || "inflation" in text -> {
            FredReleaseClassification(
                type = MarketEventType.Inflation,
                impact = MarketEventImpact.High,
                priorityScore = 950
            )
        }

        "employment situation" in text || "nonfarm payroll" in text || "unemployment" in text || "jobless" in text -> {
            FredReleaseClassification(
                type = MarketEventType.Employment,
                impact = MarketEventImpact.High,
                priorityScore = 930
            )
        }

        "gross domestic product" in text || "gdp" in text -> {
            FredReleaseClassification(
                type = MarketEventType.Gdp,
                impact = MarketEventImpact.High,
                priorityScore = 910
            )
        }

        "minutes" in text || "statement" in text || "federal reserve" in text -> {
            FredReleaseClassification(
                type = MarketEventType.CentralBank,
                impact = MarketEventImpact.Medium,
                priorityScore = 860
            )
        }

        "manufacturing" in text || "pmi" in text || "industrial production" in text -> {
            FredReleaseClassification(
                type = MarketEventType.Manufacturing,
                impact = MarketEventImpact.Medium,
                priorityScore = 820
            )
        }

        "confidence" in text || "sentiment" in text -> {
            FredReleaseClassification(
                type = MarketEventType.ConsumerConfidence,
                impact = MarketEventImpact.Medium,
                priorityScore = 780
            )
        }

        else -> null
    }
}

private fun inferEventType(category: String?, event: String?): MarketEventType {
    val text = "${category.orEmpty()} ${event.orEmpty()}".lowercase()
    return when {
        "interest rate" in text || "rate decision" in text || "fomc" in text || "policy rate" in text ->
            MarketEventType.InterestRate

        "cpi" in text || "ppi" in text || "inflation" in text || "price index" in text ->
            MarketEventType.Inflation

        "payroll" in text || "employment" in text || "unemployment" in text || "jobless" in text ->
            MarketEventType.Employment

        "gdp" in text || "gross domestic product" in text ->
            MarketEventType.Gdp

        "central bank" in text || "statement" in text || "minutes" in text ->
            MarketEventType.CentralBank

        "manufacturing" in text || "pmi" in text || "industrial" in text ->
            MarketEventType.Manufacturing

        "confidence" in text || "sentiment" in text ->
            MarketEventType.ConsumerConfidence

        else -> MarketEventType.Other
    }
}

private fun inferRegion(country: String): MarketRegion {
    val normalized = country.lowercase()
    return when {
        normalized.contains("united states") || normalized == "us" || normalized == "usa" ->
            MarketRegion.UnitedStates

        normalized.contains("euro") || normalized.contains("germany") || normalized.contains("france") ||
            normalized.contains("italy") || normalized.contains("spain") ->
            MarketRegion.EuroArea

        normalized.contains("united kingdom") || normalized == "uk" || normalized.contains("britain") ->
            MarketRegion.UnitedKingdom

        normalized.contains("japan") ->
            MarketRegion.Japan

        normalized.contains("china") ->
            MarketRegion.China

        else -> MarketRegion.Global
    }
}

private fun inferCountryCode(country: String): String {
    val trimmed = country.trim()
    if (trimmed.isEmpty()) return "GL"
    return when (trimmed.lowercase()) {
        "united states", "usa", "us" -> "US"
        "euro area", "eurozone" -> "EU"
        "united kingdom", "uk", "great britain" -> "GB"
        "japan" -> "JP"
        "china" -> "CN"
        "canada" -> "CA"
        "australia" -> "AU"
        "new zealand" -> "NZ"
        "switzerland" -> "CH"
        "singapore" -> "SG"
        "hong kong" -> "HK"
        else -> trimmed.take(2).uppercase()
    }
}

private fun MarketEvent.toCacheDto(): RemoteCacheEventDto = RemoteCacheEventDto(
    id = id,
    title = title,
    type = type.name,
    region = region.name,
    countryCode = countryCode,
    scheduledAtEpochMillis = scheduledAt.toEpochMilli(),
    impact = impact.name,
    source = source,
    description = description,
    actual = actual,
    forecast = forecast,
    previous = previous
)

