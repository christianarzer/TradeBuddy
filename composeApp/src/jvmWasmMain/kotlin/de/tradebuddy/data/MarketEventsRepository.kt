package de.tradebuddy.data

import de.tradebuddy.domain.model.EventWatchPreference
import de.tradebuddy.domain.model.MarketEvent
import de.tradebuddy.domain.model.MarketEventImpact
import de.tradebuddy.domain.model.MarketEventType
import de.tradebuddy.domain.model.MarketRegion
import de.tradebuddy.logging.AppLog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant as KotlinInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

interface MarketEventsDataSource {
    suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent>
}

enum class MarketEventsSourceProgressPhase {
    Started,
    Completed
}

data class MarketEventsSourceProgress(
    val sourceName: String,
    val sourceIndex: Int,
    val sourceCount: Int,
    val phase: MarketEventsSourceProgressPhase,
    val eventCount: Int = 0,
    val failed: Boolean = false
) {
    val fraction: Float
        get() = when (phase) {
            MarketEventsSourceProgressPhase.Started ->
                if (sourceCount <= 0) {
                    0f
                } else {
                    ((sourceIndex - 1).coerceAtLeast(0)).toFloat() / sourceCount.toFloat()
                }

            MarketEventsSourceProgressPhase.Completed ->
                if (sourceCount <= 0) 1f else sourceIndex.coerceAtMost(sourceCount).toFloat() / sourceCount.toFloat()
        }
}

interface ProgressAwareMarketEventsDataSource : MarketEventsDataSource {
    suspend fun fetchUpcoming(
        from: Instant,
        to: Instant,
        onSourceProgress: (MarketEventsSourceProgress) -> Unit
    ): List<MarketEvent>
}

object NoopMarketEventsDataSource : MarketEventsDataSource {
    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> = emptyList()
}

class TradingEconomicsMarketEventsDataSource(
    private val credentialsProvider: () -> String = { "guest:guest" },
    private val endpoint: String = "https://api.tradingeconomics.com/calendar",
    private val countries: List<String> = DefaultTradingEconomicsCountries,
    private val fetcher: suspend (String, Map<String, String>) -> String = { _, _ -> "[]" }
) : MarketEventsDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> {
        yield()
        val credentials = credentialsProvider().ifBlank { "guest:guest" }
        val effectiveCountries = if (credentials.equals("guest:guest", ignoreCase = true)) {
            listOf("united states")
        } else {
            countries
        }
        val fromDate = from.atZone(ZoneOffset.UTC).toLocalDate().toString()
        val toDate = to.atZone(ZoneOffset.UTC).toLocalDate().toString()
        val headers = mapOf(
            "Accept" to "application/json",
            "User-Agent" to BrowserLikeUserAgent
        )
        val allEntries = mutableListOf<TradingEconomicsCalendarEntry>()

        for (country in effectiveCountries) {
            yield()
            val encodedCountry = country.encodeAsQueryParameter()
            val dateRangeUrl = "$endpoint/country/$encodedCountry/$fromDate/$toDate?c=${credentials.encodeAsQueryParameter()}&f=json"
            val snapshotUrl = "$endpoint/country/$encodedCountry?c=${credentials.encodeAsQueryParameter()}&f=json"

            val payload = runCatching { fetcher(dateRangeUrl, headers) }
                .recoverCatching { fetcher(snapshotUrl, headers) }
                .getOrElse { error ->
                    if (credentials.equals("guest:guest", ignoreCase = true) && error.isHttpStatus(403)) {
                        AppLog.info(
                            tag = "TradingEconomicsDataSource",
                            message = "TradingEconomics guest access does not allow country endpoint '$country' (HTTP 403)."
                        )
                        continue
                    }
                    AppLog.warn(
                        tag = "TradingEconomicsDataSource",
                        message = "Failed to load TradingEconomics country endpoint for '$country'",
                        throwable = error
                    )
                    continue
                }
            val parsed = runCatching {
                json.decodeFromString(ListSerializer(TradingEconomicsCalendarEntry.serializer()), payload)
            }.getOrElse { decodeError ->
                AppLog.warn(
                    tag = "TradingEconomicsDataSource",
                    message = "Failed to decode TradingEconomics payload for '$country'",
                    throwable = decodeError
                )
                continue
            }
            allEntries += parsed
        }

        val events = allEntries
            .asSequence()
            .mapNotNull(TradingEconomicsCalendarEntry::toMarketEventOrNull)
            .filter { it.scheduledAt in from..to }
            .toList()

        if (allEntries.isEmpty()) {
            AppLog.warn(
                tag = "TradingEconomicsDataSource",
                message = "TradingEconomics returned 0 rows across countries. Endpoint=$endpoint, credentials=${
                    if (credentials.equals("guest:guest", ignoreCase = true)) "guest" else "configured"
                }"
            )
        } else if (events.isEmpty()) {
            AppLog.info(
                tag = "TradingEconomicsDataSource",
                message = "TradingEconomics returned ${allEntries.size} rows, but 0 fell into refresh window [$from .. $to]."
            )
        }
        return events
    }

    private companion object {
        val DefaultTradingEconomicsCountries = listOf(
            "united states",
            "euro area",
            "united kingdom",
            "japan",
            "china",
            "canada",
            "australia",
            "new zealand",
            "switzerland"
        )
    }
}

class BlsCalendarIcsDataSource(
    private val endpoints: List<String> = DefaultBlsScheduleEndpoints,
    private val fetcher: suspend (String, Map<String, String>) -> String = { _, _ -> "" }
) : MarketEventsDataSource {
    private val unavailableEndpoints = mutableSetOf<String>()
    private val loggedUnavailableEndpoints = mutableSetOf<String>()

    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> {
        yield()
        val years = (from.atZone(ZoneOffset.UTC).year..to.atZone(ZoneOffset.UTC).year).toSet()
        val resolvedEndpoints = years
            .flatMap { year -> endpoints.map { endpoint -> endpoint.replace("{year}", year.toString()) } }
            .distinct()

        val events = mutableListOf<MarketEvent>()
        for (endpoint in resolvedEndpoints) {
            yield()
            if (endpoint in unavailableEndpoints) continue
            val payload = runCatching {
                fetcher(
                    endpoint,
                    mapOf(
                        "Accept" to "text/html,application/xhtml+xml,*/*",
                        "User-Agent" to BrowserLikeUserAgent,
                        "Accept-Language" to "en-US,en;q=0.9",
                        "Referer" to "https://www.bls.gov/"
                    )
                )
            }.getOrElse { error ->
                val status = error.httpStatusCode()
                if (status == 400 || status == 403 || status == 404) {
                    unavailableEndpoints += endpoint
                    if (endpoint !in loggedUnavailableEndpoints) {
                        loggedUnavailableEndpoints += endpoint
                        AppLog.warn(
                            tag = "BlsCalendarIcsDataSource",
                            message = "BLS schedule endpoint unavailable (HTTP $status): $endpoint"
                        )
                    }
                    continue
                }
                throw error
            }

            val parsed = parseBlsScheduleEvents(payload, from = from, to = to)
            if (parsed.isNotEmpty()) {
                AppLog.info(
                    tag = "BlsCalendarIcsDataSource",
                    message = "Loaded ${parsed.size} events from BLS schedule endpoint $endpoint"
                )
            }
            events += parsed
        }
        if (events.isEmpty()) {
            AppLog.warn(
                tag = "BlsCalendarIcsDataSource",
                message = "BLS returned 0 events for [$from .. $to]. Endpoints may be blocked or outside date window."
            )
        }
        return events
            .distinctBy { it.id }
            .sortedBy { it.scheduledAt }
    }

    private companion object {
        val DefaultBlsScheduleEndpoints = listOf(
            "https://www.bls.gov/schedule/",
            "https://www.bls.gov/schedule/{year}/home.htm",
            "https://www.bls.gov/schedule/news_release/default.asp"
        )
    }
}

class EurostatCalendarIcsDataSource(
    private val endpoints: List<String> = DefaultEurostatIcsEndpoints,
    private val discoveryEndpoint: String = "https://ec.europa.eu/eurostat/web/main/news/release-calendar",
    private val releaseCalendarPageEndpoint: String = "https://ec.europa.eu/eurostat/web/main/news/release-calendar",
    private val fetcher: suspend (String, Map<String, String>) -> String = { _, _ -> "" }
) : MarketEventsDataSource {
    private val unavailableEndpoints = mutableSetOf<String>()
    private val loggedUnavailableEndpoints = mutableSetOf<String>()

    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> {
        yield()
        val dynamicEndpoint = discoverEurostatIcsEndpoint()
        val candidateEndpoints = buildList {
            if (!dynamicEndpoint.isNullOrBlank()) add(dynamicEndpoint)
            addAll(endpoints)
        }.distinct()
        val events = mutableListOf<MarketEvent>()
        for (endpoint in candidateEndpoints) {
            yield()
            if (endpoint in unavailableEndpoints) continue
            val payload = runCatching {
                fetcher(
                    endpoint,
                    mapOf(
                        "Accept" to "text/calendar,text/plain,*/*",
                        "User-Agent" to BrowserLikeUserAgent,
                        "Accept-Language" to "en-US,en;q=0.9",
                        "Referer" to "https://ec.europa.eu/eurostat/web/main/news/release-calendar"
                    )
                )
            }.getOrElse { error ->
                if (error.isHttpStatus(404) || error.isHttpStatus(403) || error.isHttpStatus(410)) {
                    unavailableEndpoints += endpoint
                    if (endpoint !in loggedUnavailableEndpoints) {
                        loggedUnavailableEndpoints += endpoint
                        AppLog.warn(
                            tag = "EurostatCalendarIcsDataSource",
                            message = "Eurostat iCal endpoint unavailable (HTTP ${error.httpStatusCode()}): $endpoint"
                        )
                    }
                    continue
                }
                throw error
            }

            val parsed = parseIcsEvents(payload, defaultZone = ZoneId.of("Europe/Brussels"))
                .asSequence()
                .mapNotNull { event ->
                    val scheduledAt = event.scheduledAt ?: return@mapNotNull null
                    if (scheduledAt !in from..to) return@mapNotNull null
                    val title = event.summary.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    MarketEvent(
                        id = event.uid.takeIf { it.isNotBlank() } ?: "eurostat-${title.take(32)}-${scheduledAt.toEpochMilli()}",
                        title = title,
                        type = inferEventType(category = event.description, event = title),
                        region = MarketRegion.EuroArea,
                        countryCode = "EU",
                        countryName = "Euro Area",
                        scheduledAt = scheduledAt,
                        impact = inferImpactFromTitle(title),
                        source = "Eurostat",
                        description = event.description
                    )
                }
                .toList()
            if (parsed.isNotEmpty()) {
                AppLog.info(
                    tag = "EurostatCalendarIcsDataSource",
                    message = "Loaded ${parsed.size} events from Eurostat endpoint $endpoint"
                )
            }
            events += parsed
        }

        if (events.isEmpty()) {
            val pageEvents = fetchFromReleaseCalendarPage(from, to)
            if (pageEvents.isNotEmpty()) {
                AppLog.info(
                    tag = "EurostatCalendarIcsDataSource",
                    message = "Loaded ${pageEvents.size} events from Eurostat release-calendar page fallback."
                )
                return pageEvents
            }
        }
        if (events.isEmpty() && unavailableEndpoints.isNotEmpty()) {
            AppLog.warn(
                tag = "EurostatCalendarIcsDataSource",
                message = "Eurostat returned 0 events for [$from .. $to]. All configured iCal endpoints are unavailable."
            )
        }
        return events
    }

    private suspend fun fetchFromReleaseCalendarPage(from: Instant, to: Instant): List<MarketEvent> {
        val payload = runCatching {
            fetcher(
                releaseCalendarPageEndpoint,
                mapOf(
                    "Accept" to "text/html,application/xhtml+xml,*/*",
                    "User-Agent" to BrowserLikeUserAgent,
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Referer" to "https://ec.europa.eu/eurostat/"
                )
            )
        }.getOrElse { error ->
            AppLog.info(
                tag = "EurostatCalendarIcsDataSource",
                message = "Eurostat release-calendar page fallback unavailable (HTTP ${error.httpStatusCode() ?: "n/a"}): $releaseCalendarPageEndpoint"
            )
            return emptyList()
        }
        return parseEurostatReleaseCalendarPageEvents(payload, from, to)
    }

    private suspend fun discoverEurostatIcsEndpoint(): String? {
        val payload = runCatching {
            fetcher(
                discoveryEndpoint,
                mapOf(
                    "Accept" to "text/html,application/xhtml+xml,*/*",
                    "User-Agent" to BrowserLikeUserAgent,
                    "Accept-Language" to "en-US,en;q=0.9"
                )
            )
        }.getOrElse { error ->
            AppLog.info(
                tag = "EurostatCalendarIcsDataSource",
                message = "Could not fetch Eurostat discovery page (HTTP ${error.httpStatusCode() ?: "n/a"}): $discoveryEndpoint"
            )
            return null
        }

        val absoluteMatch = Regex("""https://ec\.europa\.eu/eurostat/[^\s"'<>]+\.ics""", RegexOption.IGNORE_CASE)
            .find(payload)
            ?.value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (absoluteMatch != null) {
            AppLog.info(
                tag = "EurostatCalendarIcsDataSource",
                message = "Discovered Eurostat iCal endpoint via official page: $absoluteMatch"
            )
            return absoluteMatch
        }
        val relativeMatch = Regex("""/eurostat/[^\s"'<>]+\.ics""", RegexOption.IGNORE_CASE)
            .find(payload)
            ?.value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (relativeMatch != null) {
            val resolved = "https://ec.europa.eu$relativeMatch"
            AppLog.info(
                tag = "EurostatCalendarIcsDataSource",
                message = "Discovered Eurostat iCal endpoint via official page: $resolved"
            )
            return resolved
        }
        return null
    }

    private companion object {
        val DefaultEurostatIcsEndpoints = emptyList<String>()
    }
}

class OnsReleaseCalendarDataSource(
    private val endpoint: String = "https://api.beta.ons.gov.uk/v1/search/releases",
    private val releaseTypes: List<String> = listOf("type-upcoming", "type-published"),
    private val fetcher: suspend (String, Map<String, String>) -> String = { _, _ -> "{}" }
) : MarketEventsDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> {
        yield()
        val fromDate = from.atZone(ZoneOffset.UTC).toLocalDate().toString()
        val toDate = to.atZone(ZoneOffset.UTC).toLocalDate().toString()
        val headers = mapOf(
            "Accept" to "application/json",
            "User-Agent" to BrowserLikeUserAgent
        )
        val events = mutableListOf<MarketEvent>()
        val effectiveReleaseTypes = releaseTypes.ifEmpty { listOf("type-upcoming") }

        for (releaseType in effectiveReleaseTypes) {
            yield()
            val url = buildString {
                append(endpoint)
                append("?limit=1000")
                append("&sort=release_date_asc")
                append("&highlight=false")
                append("&fromDate=")
                append(fromDate.encodeAsQueryParameter())
                append("&toDate=")
                append(toDate.encodeAsQueryParameter())
                append("&release-type=")
                append(releaseType.encodeAsQueryParameter())
            }

            val payload = runCatching { fetcher(url, headers) }.getOrElse { error ->
                AppLog.warn(
                    tag = "OnsReleaseCalendarDataSource",
                    message = "Failed to load ONS release calendar for release-type '$releaseType'",
                    throwable = error
                )
                continue
            }

            val parsed = parseOnsReleasePayload(payload, from = from, to = to)
            events += parsed
        }

        return events
            .distinctBy { it.id }
            .sortedBy { it.scheduledAt }
    }

    private fun parseOnsReleasePayload(payload: String, from: Instant, to: Instant): List<MarketEvent> {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return emptyList()
        val items = root["items"]?.jsonArray.orEmpty()
        val zone = ZoneId.of("Europe/London")

        return items
            .asSequence()
            .mapNotNull { it as? JsonObject }
            .mapNotNull { item ->
                val title = item.stringFromKeys("title", "description", "uri")?.trim().orEmpty()
                if (title.isBlank()) return@mapNotNull null
                val rawReleaseDate = item.stringFromKeys("release_date", "releaseDate", "date", "published_date")
                val scheduledAt = parseMarketEventInstant(rawReleaseDate)
                    ?: parseFlexibleDate(rawReleaseDate)
                        ?.atTime(7, 0)
                        ?.atZone(zone)
                        ?.toInstant()
                    ?: return@mapNotNull null
                if (scheduledAt !in from..to) return@mapNotNull null

                val uri = item.stringFromKeys("uri", "id").orEmpty()
                val releaseType = item.stringFromKeys("release_type", "type")
                MarketEvent(
                    id = uri.takeIf { it.isNotBlank() } ?: "ons-${scheduledAt.toEpochMilli()}-${title.take(32).lowercase()}",
                    title = title,
                    type = inferEventType(category = releaseType ?: "ONS", event = title),
                    region = MarketRegion.UnitedKingdom,
                    countryCode = "GB",
                    countryName = "United Kingdom",
                    scheduledAt = scheduledAt,
                    impact = inferImpactFromTitle(title),
                    source = "ONS",
                    description = releaseType ?: "ONS release calendar"
                )
            }
            .toList()
    }

    private fun JsonObject.stringFromKeys(vararg keys: String): String? =
        keys
            .asSequence()
            .mapNotNull { key -> this[key].asScalarString() }
            .map { value -> value.trim() }
            .firstOrNull { it.isNotEmpty() }
}

class CensusEconomicCalendarDataSource(
    private val endpoints: List<String> = DefaultEndpoints,
    private val fetcher: suspend (String, Map<String, String>) -> String = { _, _ -> "" }
) : MarketEventsDataSource {
    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> {
        yield()
        val years = (from.atZone(ZoneOffset.UTC).year..to.atZone(ZoneOffset.UTC).year).toSet()
        val resolvedEndpoints = years
            .flatMap { year -> endpoints.map { endpoint -> endpoint.replace("{year}", year.toString()) } }
            .distinct()

        val headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,*/*",
            "User-Agent" to BrowserLikeUserAgent
        )

        val events = mutableListOf<MarketEvent>()
        for (endpoint in resolvedEndpoints) {
            yield()
            val payload = runCatching { fetcher(endpoint, headers) }.getOrElse { error ->
                AppLog.warn(
                    tag = "CensusEconomicCalendarDataSource",
                    message = "Failed to load Census economic calendar endpoint: $endpoint",
                    throwable = error
                )
                continue
            }
            val year = Regex("""calendar-listview-(\d{4})\.html""", RegexOption.IGNORE_CASE)
                .find(endpoint)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
            val parsed = parseCensusCalendarEvents(payload, from = from, to = to, yearHint = year)
            if (parsed.isNotEmpty()) {
                AppLog.info(
                    tag = "CensusEconomicCalendarDataSource",
                    message = "Loaded ${parsed.size} events from Census endpoint $endpoint"
                )
            }
            events += parsed
        }

        return events
            .distinctBy { it.id }
            .sortedBy { it.scheduledAt }
    }

    private companion object {
        val DefaultEndpoints = listOf(
            "https://www.census.gov/econ/currentdata/calendar-listview-{year}.html"
        )
    }
}

class BeaReleaseDatesDataSource(
    private val endpoint: String = "https://apps.bea.gov/API/signup/release_dates.json",
    private val fetcher: suspend (String, Map<String, String>) -> String = { _, _ -> "{}" }
) : MarketEventsDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> {
        val payload = fetcher(
            endpoint,
            mapOf(
                "Accept" to "application/json",
                "User-Agent" to "TradeBuddy/1.0"
            )
        )
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }
            .getOrElse { return emptyList() }

        val events = root.entries.asSequence()
            .mapNotNull { entry ->
                val seriesName = entry.key.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val releaseInstants = extractBeaReleaseInstants(entry.value as? JsonObject ?: return@mapNotNull null)
                if (releaseInstants.isEmpty()) return@mapNotNull null
                seriesName to releaseInstants
            }
            .flatMap { (seriesName, releaseInstants) ->
                releaseInstants.asSequence().mapNotNull { scheduledAt ->
                    if (scheduledAt !in from..to) return@mapNotNull null
                    MarketEvent(
                        id = "bea-${seriesName.take(32)}-${scheduledAt.toEpochMilli()}",
                        title = seriesName,
                        type = inferEventType(category = "BEA", event = seriesName),
                        region = MarketRegion.UnitedStates,
                        countryCode = "US",
                        countryName = "United States",
                        scheduledAt = scheduledAt,
                        impact = inferImpactFromTitle(seriesName),
                        source = "BEA",
                        description = "BEA release schedule"
                    )
                }
            }
            .toList()
        if (events.isEmpty()) {
            AppLog.info(
                tag = "BeaReleaseDatesDataSource",
                message = "BEA returned 0 events in requested window [$from .. $to]."
            )
        }
        return events
    }
}

class EcbStatsCalendarDataSource(
    private val endpoint: String = "https://www.ecb.europa.eu/events/calendar/statscal/html/index.en.html",
    private val fetcher: suspend (String, Map<String, String>) -> String = { _, _ -> "" }
) : MarketEventsDataSource {

    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> {
        val payload = fetcher(
            endpoint,
            mapOf(
                "Accept" to "text/html,application/xhtml+xml,*/*",
                "User-Agent" to "TradeBuddy/1.0"
            )
        )
        val lines = payload
            .stripHtmlTags()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        if (lines.isEmpty()) return emptyList()

        val events = mutableListOf<MarketEvent>()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val match = EcbDateTimePattern.find(line)
            if (match != null) {
                val (day, month, year, hour, minute, _) = match.destructured
                val scheduledAt = runCatching {
                    LocalDateTime.of(
                        year.toInt(),
                        month.toInt(),
                        day.toInt(),
                        hour.toInt(),
                        minute.toInt()
                    ).atZone(ZoneId.of("Europe/Berlin")).toInstant()
                }.getOrNull()
                val title = lines
                    .drop(index + 1)
                    .firstOrNull { candidate ->
                        !EcbReferencePattern.containsMatchIn(candidate) &&
                            !EcbDateTimePattern.containsMatchIn(candidate) &&
                            candidate.any { it.isLetter() }
                    }
                    ?.trim()
                    .orEmpty()

                if (scheduledAt != null && scheduledAt in from..to && title.isNotBlank()) {
                    events += MarketEvent(
                        id = "ecb-${title.take(32)}-${scheduledAt.toEpochMilli()}",
                        title = title,
                        type = inferEventType(category = "ECB", event = title),
                        region = MarketRegion.EuroArea,
                        countryCode = "EU",
                        countryName = "Euro Area",
                        scheduledAt = scheduledAt,
                        impact = inferImpactFromTitle(title),
                        source = "ECB",
                        description = "ECB statistical release calendar"
                    )
                }
            }
            index += 1
        }
        return events
    }

    private companion object {
        val EcbDateTimePattern = Regex("""(\d{2})/(\d{2})/(\d{4})\s+(\d{2}):(\d{2})\s+(CET|CEST)""")
        val EcbReferencePattern = Regex("""^(Reference period|All times are CET|Last update)""", RegexOption.IGNORE_CASE)
    }
}

class FredMarketEventsDataSource(
    private val apiKeyProvider: suspend () -> String = { DemoFredApiKey },
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
                val classification = classifyFredRelease(name) ?: FredFallbackClassification
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
                        countryName = "United States",
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
        const val MaxFredReleases = 32
        const val DemoFredApiKey = "abcdefghijklmnopqrstuvwxyz123456"
        val FredFallbackClassification = FredReleaseClassification(
            type = MarketEventType.Other,
            impact = MarketEventImpact.Low,
            priorityScore = 100
        )
    }
}

class FmpMarketEventsDataSource(
    private val apiKeyProvider: suspend () -> String = { "" },
    private val endpoints: List<FmpCalendarEndpoint> = DefaultCalendarEndpoints,
    private val fetcher: suspend (String, Map<String, String>) -> String = { _, _ -> "[]" }
) : MarketEventsDataSource {

    private val json = Json { ignoreUnknownKeys = true }
    private var fullyRestrictedApiKey: String? = null
    private val restrictedEndpointsByKey = mutableMapOf<String, MutableSet<String>>()
    private val loggedRestrictions = mutableSetOf<String>()
    private val loggedFullyRestrictedKeys = mutableSetOf<String>()

    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> {
        yield()
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) {
            AppLog.info(
                tag = "FmpMarketEventsDataSource",
                message = "Skipped FMP fetch because no API key is configured"
            )
            return emptyList()
        }
        if (apiKey == fullyRestrictedApiKey) {
            if (apiKey !in loggedFullyRestrictedKeys) {
                loggedFullyRestrictedKeys += apiKey
                AppLog.info(
                    tag = "FmpMarketEventsDataSource",
                    message = "FMP economic-calendar endpoints are restricted for this API key. Source will be skipped."
                )
            }
            return emptyList()
        }

        val fromDate = from.atZone(ZoneOffset.UTC).toLocalDate().toString()
        val toDate = to.atZone(ZoneOffset.UTC).toLocalDate().toString()

        val blockedEndpoints = restrictedEndpointsByKey.getOrPut(apiKey) { mutableSetOf() }
        var successfulFetch = false
        val mergedEvents = mutableListOf<MarketEvent>()

        for (endpoint in endpoints) {
            yield()
            if (endpoint.name in blockedEndpoints) continue

            val payload = runCatching {
                fetcher(
                    endpoint.buildUrl(apiKey = apiKey, fromDate = fromDate, toDate = toDate),
                    mapOf(
                        "Accept" to "application/json",
                        "User-Agent" to "TradeBuddy/1.0"
                    )
                )
            }.getOrElse { error ->
                when {
                    error.isFmpPlanRestriction() -> {
                        blockedEndpoints += endpoint.name
                        val restrictionKey = "$apiKey|${endpoint.name}"
                        if (restrictionKey !in loggedRestrictions) {
                            loggedRestrictions += restrictionKey
                            AppLog.info(
                                tag = "FmpMarketEventsDataSource",
                                message = "FMP endpoint '${endpoint.name}' is not available for the current plan (HTTP 402)."
                            )
                        }
                        continue
                    }

                    error.isHttpStatus(404) || error.isHttpStatus(410) -> {
                        blockedEndpoints += endpoint.name
                        AppLog.info(
                            tag = "FmpMarketEventsDataSource",
                            message = "FMP endpoint '${endpoint.name}' not available (HTTP ${error.httpStatusCode()})."
                        )
                        continue
                    }

                    error.isHttpStatus(429) -> {
                        AppLog.warn(
                            tag = "FmpMarketEventsDataSource",
                            message = "FMP rate limit reached (HTTP 429) on endpoint '${endpoint.name}'.",
                            throwable = error
                        )
                        continue
                    }

                    else -> throw error
                }
            }

            successfulFetch = true
            val parsedEvents = parseFmpCalendarPayload(payload)
            val eventsInWindow = parsedEvents
                .asSequence()
                .filter { it.scheduledAt in from..to }
                .toList()
            if (eventsInWindow.isEmpty() && parsedEvents.isNotEmpty()) {
                val firstDate = parsedEvents.minOfOrNull { it.scheduledAt }
                val lastDate = parsedEvents.maxOfOrNull { it.scheduledAt }
                AppLog.info(
                    tag = "FmpMarketEventsDataSource",
                    message = "FMP endpoint '${endpoint.name}' returned ${parsedEvents.size} entries, but none in refresh window [$from .. $to]. Payload range=[$firstDate .. $lastDate]."
                )
            }
            mergedEvents += eventsInWindow
            if (eventsInWindow.isNotEmpty()) break
        }

        if (!successfulFetch && blockedEndpoints.size >= endpoints.size) {
            fullyRestrictedApiKey = apiKey
            if (apiKey !in loggedFullyRestrictedKeys) {
                loggedFullyRestrictedKeys += apiKey
                AppLog.info(
                    tag = "FmpMarketEventsDataSource",
                    message = "FMP economic-calendar source disabled after all configured endpoints were restricted."
                )
            }
        }
        if (successfulFetch && mergedEvents.isEmpty()) {
            AppLog.info(
                tag = "FmpMarketEventsDataSource",
                message = "FMP returned no usable market events for refresh window [$from .. $to]."
            )
        }

        return mergedEvents
            .distinctBy { it.id }
            .sortedBy { it.scheduledAt }
    }

    private fun parseFmpCalendarPayload(payload: String): List<MarketEvent> {
        val root = runCatching { json.parseToJsonElement(payload) }.getOrNull() ?: return emptyList()
        val entries = when {
            root is JsonObject && "data" in root -> root["data"]?.jsonArray.orEmpty()
            root is JsonObject && "economicCalendar" in root -> root["economicCalendar"]?.jsonArray.orEmpty()
            root is kotlinx.serialization.json.JsonArray -> root
            else -> emptyList()
        }

        return entries
            .asSequence()
            .mapNotNull { it as? JsonObject }
            .mapNotNull { entry -> entry.toFmpMarketEventOrNull() }
            .toList()
    }

    private fun JsonObject.toFmpMarketEventOrNull(): MarketEvent? {
        val date = stringFromKeys("date", "Date")
        val scheduledAt = parseMarketEventInstant(date) ?: return null
        val title = stringFromKeys("event", "Event", "name", "Name", "category", "Category") ?: return null
        val category = stringFromKeys("category", "Category")
        val country = stringFromKeys("country", "Country", "countryCode", "CountryCode").orEmpty()
        val countryCode = inferCountryCode(country)
        val consensusValue = stringFromKeys("consensus", "Consensus")
        val forecastValue = stringFromKeys("forecast", "Forecast", "estimate", "Estimate") ?: consensusValue
        val id = stringFromKeys("id", "Id")
            ?: "fmp-${countryCode.lowercase()}-${title.take(32)}-${scheduledAt.toEpochMilli()}"

        return MarketEvent(
            id = id,
            title = title,
            type = inferEventType(category = category, event = title),
            region = inferRegion(country),
            countryCode = countryCode,
            countryName = country.takeIf { it.isNotBlank() },
            scheduledAt = scheduledAt,
            impact = parseFmpImpact(
                impact = stringFromKeys("impact", "Impact"),
                importance = intFromKeys("importance", "Importance")
            ),
            source = "FMP",
            description = category,
            actual = stringFromKeys("actual", "Actual"),
            consensus = consensusValue,
            forecast = forecastValue,
            previous = stringFromKeys("previous", "Previous")
        )
    }

    private fun JsonObject.stringFromKeys(vararg keys: String): String? =
        keys
            .asSequence()
            .mapNotNull { key -> this[key].asScalarString() }
            .map { value -> value.trim() }
            .firstOrNull { it.isNotEmpty() }

    private fun JsonObject.intFromKeys(vararg keys: String): Int? =
        stringFromKeys(*keys)?.toIntOrNull()

    private companion object {
        val DefaultCalendarEndpoints = listOf(
            FmpCalendarEndpoint(
                name = "api-v3/economic_calendar",
                baseUrl = "https://financialmodelingprep.com/api/v3/economic_calendar"
            ),
            FmpCalendarEndpoint(
                name = "stable/economic-calendar",
                baseUrl = "https://financialmodelingprep.com/stable/economic-calendar"
            )
        )
    }
}

data class FmpCalendarEndpoint(
    val name: String,
    val baseUrl: String
) {
    fun buildUrl(apiKey: String, fromDate: String, toDate: String): String = buildString {
        append(baseUrl)
        append("?apikey=")
        append(apiKey.encodeAsQueryParameter())
        append("&from=")
        append(fromDate.encodeAsQueryParameter())
        append("&to=")
        append(toDate.encodeAsQueryParameter())
    }
}

class CompositeMarketEventsDataSource(
    private val sources: List<NamedMarketEventsDataSource>
) : ProgressAwareMarketEventsDataSource {

    override suspend fun fetchUpcoming(from: Instant, to: Instant): List<MarketEvent> {
        return fetchUpcoming(from, to) { }
    }

    override suspend fun fetchUpcoming(
        from: Instant,
        to: Instant,
        onSourceProgress: (MarketEventsSourceProgress) -> Unit
    ): List<MarketEvent> {
        yield()
        if (sources.isEmpty()) return emptyList()

        val failures = mutableListOf<Pair<String, Throwable>>()
        val collected = mutableListOf<MarketEvent>()

        val totalSources = sources.size
        for ((index, source) in sources.withIndex()) {
            yield()
            val sourceIndex = index + 1
            onSourceProgress(
                MarketEventsSourceProgress(
                    sourceName = source.name,
                    sourceIndex = sourceIndex,
                    sourceCount = totalSources,
                    phase = MarketEventsSourceProgressPhase.Started
                )
            )
            runCatching {
                withTimeout(SourceTimeoutMillis) {
                    source.dataSource.fetchUpcoming(from, to)
                }
            }
                .onSuccess { events ->
                    collected += events
                    onSourceProgress(
                        MarketEventsSourceProgress(
                            sourceName = source.name,
                            sourceIndex = sourceIndex,
                            sourceCount = totalSources,
                            phase = MarketEventsSourceProgressPhase.Completed,
                            eventCount = events.size
                        )
                    )
                    if (events.isNotEmpty()) {
                        AppLog.info(
                            tag = "CompositeMarketEvents",
                            message = "Loaded ${events.size} events from ${source.name}"
                        )
                    } else if (source.logEmptyResults) {
                        AppLog.info(
                            tag = "CompositeMarketEvents",
                            message = "Loaded 0 events from ${source.name} (no entries in requested window [$from .. $to])."
                        )
                    }
                }
                .onFailure { error ->
                    failures += source.name to error
                    onSourceProgress(
                        MarketEventsSourceProgress(
                            sourceName = source.name,
                            sourceIndex = sourceIndex,
                            sourceCount = totalSources,
                            phase = MarketEventsSourceProgressPhase.Completed,
                            failed = true
                        )
                    )
                    if (error is TimeoutCancellationException) {
                        AppLog.warn(
                            tag = "CompositeMarketEvents",
                            message = "Source ${source.name} timed out after ${SourceTimeoutMillis / 1000}s and was skipped."
                        )
                    } else {
                        AppLog.warn(
                            tag = "CompositeMarketEvents",
                            message = "Source ${source.name} failed",
                            throwable = error
                        )
                    }
                }
        }

        if (collected.isEmpty() && failures.isNotEmpty()) {
            throw failures.first().second
        }

        return deduplicateMarketEvents(collected)
    }

    private companion object {
        const val SourceTimeoutMillis = 12_000L
    }
}

data class NamedMarketEventsDataSource(
    val name: String,
    val dataSource: MarketEventsDataSource,
    val logEmptyResults: Boolean = true
)

interface MarketEventsRepository {
    suspend fun loadUpcomingEvents(
        from: Instant,
        to: Instant,
        forceRefresh: Boolean = false,
        onSourceProgress: ((MarketEventsSourceProgress) -> Unit)? = null
    ): List<MarketEvent>
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

    override suspend fun loadUpcomingEvents(
        from: Instant,
        to: Instant,
        forceRefresh: Boolean,
        onSourceProgress: ((MarketEventsSourceProgress) -> Unit)?
    ): List<MarketEvent> {
        return loadRemoteWithCache(from, to, forceRefresh = forceRefresh, onSourceProgress = onSourceProgress).sortedWith(
            compareByDescending<MarketEvent> { it.impact.priority }
                .thenBy { it.scheduledAt }
                .thenBy { it.title }
        )
    }

    override suspend fun loadWatchPreferences(): Set<EventWatchPreference> {
        val payload = watchlistDocument.readText() ?: return emptySet()
        return runCatching { json.decodeFromString(WatchlistPayload.serializer(), payload) }
            .onFailure { error ->
                AppLog.warn(
                    tag = "MarketEventsRepository",
                    message = "Failed to decode watchlist payload, falling back to empty watchlist",
                    throwable = error
                )
            }
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

    private suspend fun loadRemoteWithCache(
        from: Instant,
        to: Instant,
        forceRefresh: Boolean,
        onSourceProgress: ((MarketEventsSourceProgress) -> Unit)?
    ): List<MarketEvent> {
        val now = Instant.now()
        val persistedCache = loadPersistedCache()
        if (!forceRefresh) {
            val cached = remoteCache
            if (cached != null && cached.validUntil.isAfter(now) && cached.contains(from, to)) {
                AppLog.info(
                    tag = "MarketEventsRepository",
                    message = "Using in-memory market-events cache (${cached.events.size} events)"
                )
                onSourceProgress?.invoke(
                    MarketEventsSourceProgress(
                        sourceName = "Cache",
                        sourceIndex = 1,
                        sourceCount = 1,
                        phase = MarketEventsSourceProgressPhase.Completed,
                        eventCount = cached.events.size
                    )
                )
                return cached.events.filter { it.scheduledAt in from..to }
            }

            if (persistedCache != null && persistedCache.validUntil.isAfter(now) && persistedCache.contains(from, to)) {
                remoteCache = persistedCache
                AppLog.info(
                    tag = "MarketEventsRepository",
                    message = "Using persisted market-events cache (${persistedCache.events.size} events)"
                )
                onSourceProgress?.invoke(
                    MarketEventsSourceProgress(
                        sourceName = "PersistedCache",
                        sourceIndex = 1,
                        sourceCount = 1,
                        phase = MarketEventsSourceProgressPhase.Completed,
                        eventCount = persistedCache.events.size
                    )
                )
                return persistedCache.events.filter { it.scheduledAt in from..to }
            }
        } else {
            AppLog.info(
                tag = "MarketEventsRepository",
                message = "Force refresh requested for market-events (cache bypassed)"
            )
        }

        return runCatching {
            if (remoteDataSource is ProgressAwareMarketEventsDataSource && onSourceProgress != null) {
                remoteDataSource.fetchUpcoming(from, to, onSourceProgress)
            } else {
                remoteDataSource.fetchUpcoming(from, to)
            }
        }.mapCatching { fetched ->
            val snapshot = CachedRemoteSnapshot(
                validUntil = Instant.ofEpochMilli(now.toEpochMilli() + (RemoteCacheSeconds * 1000L)),
                rangeFrom = from,
                rangeTo = to,
                events = fetched
            )
            remoteCache = snapshot
            persistCache(snapshot)
            AppLog.info(
                tag = "MarketEventsRepository",
                message = "Fetched ${fetched.size} market events from remote source (forceRefresh=$forceRefresh)"
            )
            fetched
        }.getOrElse { fetchError ->
            val fallback = persistedCache?.takeIf { snapshot ->
                snapshot.events.isNotEmpty() &&
                    snapshot.fetchedAt.isAfter(Instant.ofEpochMilli(now.toEpochMilli() - (StaleFallbackSeconds * 1000L)))
            }
            if (fallback != null) {
                AppLog.warn(
                    tag = "MarketEventsRepository",
                    message = "Remote market-events fetch failed, using stale fallback cache",
                    throwable = fetchError
                )
                fallback.events.filter { it.scheduledAt in from..to }
            } else {
                AppLog.error(
                    tag = "MarketEventsRepository",
                    message = "Remote market-events fetch failed and no fallback cache is available",
                    throwable = fetchError
                )
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
        const val RemoteCacheSeconds = 2 * 60 * 60L
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
    val countryName: String? = null,
    val scheduledAtEpochMillis: Long,
    val impact: String,
    val source: String,
    val description: String? = null,
    val actual: String? = null,
    val consensus: String? = null,
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
            countryName = countryName,
            scheduledAt = Instant.ofEpochMilli(scheduledAtEpochMillis),
            impact = resolvedImpact,
            source = source,
            description = description,
            actual = actual,
            consensus = consensus,
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
    val actual: JsonElement? = null,
    @SerialName("Forecast")
    val forecast: JsonElement? = null,
    @SerialName("Previous")
    val previous: JsonElement? = null,
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
            countryName = countryName.takeIf { it.isNotBlank() },
            scheduledAt = scheduledAt,
            impact = impact,
            source = source?.trim().takeUnless { it.isNullOrEmpty() } ?: "Trading Economics",
            description = category?.trim(),
            actual = actual.asScalarString(),
            consensus = null,
            forecast = forecast.asScalarString(),
            previous = previous.asScalarString()
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

private fun deduplicateMarketEvents(events: List<MarketEvent>): List<MarketEvent> =
    events
        .groupBy { event ->
            listOf(
                event.countryCode.uppercase(),
                event.type.name,
                event.title.trim().lowercase(),
                event.scheduledAt.toEpochMilli().toString()
            ).joinToString("|")
        }
        .values
        .map { duplicates ->
            duplicates.maxWithOrNull(
                compareBy<MarketEvent>(
                    { event -> event.impact.priority },
                    { event -> listOf(event.actual, event.consensus, event.forecast, event.previous, event.description).count { !it.isNullOrBlank() } },
                    { event -> if (event.source.equals("FMP", ignoreCase = true)) 1 else 0 }
                )
            ) ?: duplicates.first()
        }

private fun parseFmpImpact(impact: String?, importance: Int?): MarketEventImpact {
    val normalizedImpact = impact?.trim()?.lowercase().orEmpty()
    return when {
        "high" in normalizedImpact || normalizedImpact == "3" || (importance ?: 0) >= 3 -> MarketEventImpact.High
        "medium" in normalizedImpact || normalizedImpact == "2" || (importance ?: 0) == 2 -> MarketEventImpact.Medium
        else -> MarketEventImpact.Low
    }
}

private fun JsonElement?.asScalarString(): String? {
    val element = this ?: return null
    if (element is JsonNull) return null
    return when (element) {
        is JsonPrimitive -> {
            val raw = if (element.isString) element.content else element.toString()
            raw.trim()
        }
        else -> element.toString().trim()
    }.takeIf { it.isNotEmpty() && it != "null" }
}

private fun Throwable.isFmpPlanRestriction(): Boolean {
    val message = message?.lowercase().orEmpty()
    return "http 402" in message ||
        "restricted endpoint" in message ||
        "subscription" in message && "endpoint" in message
}

private fun Throwable.isHttpStatus(statusCode: Int): Boolean =
    httpStatusCode() == statusCode

private fun Throwable.httpStatusCode(): Int? {
    val message = message.orEmpty()
    val match = Regex("""HTTP\s+(\d{3})\b""", RegexOption.IGNORE_CASE).find(message) ?: return null
    return match.groupValues.getOrNull(1)?.toIntOrNull()
}

private fun parseBlsScheduleEvents(rawHtml: String, from: Instant, to: Instant): List<MarketEvent> {
    val lines = rawHtml
        .stripHtmlTags()
        .lineSequence()
        .map { it.trim().replace(Regex("\\s+"), " ") }
        .filter { it.isNotBlank() }
        .toList()
    if (lines.isEmpty()) return emptyList()

    val zone = ZoneId.of("America/New_York")
    val events = mutableListOf<MarketEvent>()

    lines.forEachIndexed { index, line ->
        val releaseDate = parseDateWithPatterns(
            value = line,
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)
        ) ?: return@forEachIndexed

        var releaseTime: LocalTime? = null
        var releaseTitle: String? = null
        val lookaheadEnd = minOf(lines.lastIndex, index + 10)
        for (lookaheadIndex in (index + 1)..lookaheadEnd) {
            val candidate = lines[lookaheadIndex]
            if (parseDateWithPatterns(candidate, DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)) != null) {
                break
            }

            val parsedTime = parseDateTimeWithPatterns(
                value = "2000-01-01 ${candidate.uppercase()}",
                DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.ENGLISH)
            )?.toLocalTime()
            if (parsedTime != null) {
                releaseTime = parsedTime
                continue
            }

            if (isBlsReleaseTitleCandidate(candidate)) {
                releaseTitle = candidate
                break
            }
        }

        val title = releaseTitle?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEachIndexed
        val time = releaseTime ?: return@forEachIndexed
        val scheduledAt = LocalDateTime.of(releaseDate, time)
            .atZone(zone)
            .toInstant()
        if (scheduledAt !in from..to) return@forEachIndexed

        events += MarketEvent(
            id = "bls-${scheduledAt.toEpochMilli()}-${title.take(32).lowercase()}",
            title = title,
            type = inferEventType(category = "BLS", event = title),
            region = MarketRegion.UnitedStates,
            countryCode = "US",
            countryName = "United States",
            scheduledAt = scheduledAt,
            impact = inferImpactFromTitle(title),
            source = "BLS",
            description = "BLS release schedule"
        )
    }

    return events
        .distinctBy { it.id }
        .sortedBy { it.scheduledAt }
}

private fun isBlsReleaseTitleCandidate(value: String): Boolean {
    val candidate = value.trim()
    if (candidate.length < 5) return false
    if (candidate.equals("Date", ignoreCase = true)) return false
    if (candidate.equals("Time", ignoreCase = true)) return false
    if (candidate.equals("Release", ignoreCase = true)) return false
    if (candidate.startsWith("Last Modified", ignoreCase = true)) return false
    if (candidate.startsWith("All times", ignoreCase = true)) return false
    if (candidate.startsWith("Month View", ignoreCase = true)) return false
    if (candidate.startsWith("List View", ignoreCase = true)) return false
    if (candidate.startsWith("Holiday", ignoreCase = true)) return false
    if (candidate.contains("Subscribe to the BLS Online Calendar", ignoreCase = true)) return false
    return candidate.any { it.isLetter() }
}

private fun parseCensusCalendarEvents(rawHtml: String, from: Instant, to: Instant, yearHint: Int?): List<MarketEvent> {
    val normalized = rawHtml
        .stripHtmlTags()
        .replace(Regex("\\s+"), " ")
        .trim()
    if (normalized.isBlank()) return emptyList()

    val resolvedYear = yearHint
        ?: Regex("""\b(20\d{2})\s+Calendar\b""", RegexOption.IGNORE_CASE)
            .find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        ?: from.atZone(ZoneOffset.UTC).year

    val monthPattern =
        "January|February|March|April|May|June|July|August|September|October|November|December"
    val weekdayPattern =
        "Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday"
    val entryRegex = Regex(
        pattern = """(?<month>$monthPattern)\s+(?<day>\d{1,2})\s+(?<weekday>$weekdayPattern)\s+(?<time>\d{1,2}:\d{2}\s*(?:AM|PM))\s+(?<title>.+?)(?=(?:$monthPattern)\s+\d{1,2}\s+(?:$weekdayPattern)\s+\d{1,2}:\d{2}\s*(?:AM|PM)\s+|$)""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    val zone = ZoneId.of("America/New_York")
    val events = mutableListOf<MarketEvent>()
    entryRegex.findAll(normalized).forEach { match ->
        val month = match.groups["month"]?.value ?: return@forEach
        val day = match.groups["day"]?.value?.toIntOrNull() ?: return@forEach
        val timeValue = match.groups["time"]?.value?.trim().orEmpty()
        val rawTitle = match.groups["title"]?.value.orEmpty()
        val title = rawTitle
            .replace(Regex("\\s+"), " ")
            .trim()
            .takeIf { it.isNotBlank() }
            ?: return@forEach

        val date = parseDateWithPatterns(
            "$month $day $resolvedYear",
            DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH)
        ) ?: return@forEach

        val time = parseDateTimeWithPatterns(
            "2000-01-01 ${timeValue.uppercase()}",
            DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a", Locale.ENGLISH)
        )?.toLocalTime() ?: return@forEach

        val scheduledAt = LocalDateTime.of(date, time)
            .atZone(zone)
            .toInstant()
        if (scheduledAt !in from..to) return@forEach

        events += MarketEvent(
            id = "census-${scheduledAt.toEpochMilli()}-${title.take(36).lowercase()}",
            title = title,
            type = inferEventType(category = "Census", event = title),
            region = MarketRegion.UnitedStates,
            countryCode = "US",
            countryName = "United States",
            scheduledAt = scheduledAt,
            impact = inferImpactFromTitle(title),
            source = "US Census",
            description = "U.S. Census Bureau economic indicator calendar"
        )
    }

    return events
        .distinctBy { it.id }
        .sortedBy { it.scheduledAt }
}

private fun parseEurostatReleaseCalendarPageEvents(rawHtml: String, from: Instant, to: Instant): List<MarketEvent> {
    val lines = rawHtml
        .stripHtmlTags()
        .lineSequence()
        .map { it.trim().replace(Regex("\\s+"), " ") }
        .filter { it.isNotBlank() }
        .toList()
    if (lines.isEmpty()) return emptyList()

    val zone = ZoneId.of("Europe/Brussels")
    val events = mutableListOf<MarketEvent>()

    lines.forEachIndexed { index, line ->
        val releaseDate = parseDateWithPatterns(
            value = line,
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
        ) ?: return@forEachIndexed

        var title: String? = null
        val lookaheadEnd = minOf(lines.lastIndex, index + 6)
        for (lookaheadIndex in (index + 1)..lookaheadEnd) {
            val candidate = lines[lookaheadIndex]
            val isNextDate = parseDateWithPatterns(
                value = candidate,
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
            ) != null
            if (isNextDate) break
            if (isEurostatTitleCandidate(candidate)) {
                title = candidate
                break
            }
        }

        val resolvedTitle = title?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEachIndexed
        val scheduledAt = releaseDate
            .atTime(11, 0)
            .atZone(zone)
            .toInstant()
        if (scheduledAt !in from..to) return@forEachIndexed

        events += MarketEvent(
            id = "eurostat-page-${scheduledAt.toEpochMilli()}-${resolvedTitle.take(32).lowercase()}",
            title = resolvedTitle,
            type = inferEventType(category = "Eurostat", event = resolvedTitle),
            region = MarketRegion.EuroArea,
            countryCode = "EU",
            countryName = "Euro Area",
            scheduledAt = scheduledAt,
            impact = inferImpactFromTitle(resolvedTitle),
            source = "Eurostat",
            description = "Eurostat release calendar page"
        )
    }

    return events
        .distinctBy { it.id }
        .sortedBy { it.scheduledAt }
}

private fun isEurostatTitleCandidate(value: String): Boolean {
    val candidate = value.trim()
    if (candidate.length < 4) return false
    if (candidate.startsWith("Release calendar", ignoreCase = true)) return false
    if (candidate.startsWith("News", ignoreCase = true)) return false
    if (candidate.startsWith("Menu", ignoreCase = true)) return false
    if (candidate.startsWith("Home", ignoreCase = true)) return false
    if (candidate.startsWith("Data", ignoreCase = true)) return false
    if (candidate.startsWith("About us", ignoreCase = true)) return false
    if (candidate.startsWith("Contact us", ignoreCase = true)) return false
    if (candidate.startsWith("Help", ignoreCase = true)) return false
    return candidate.any { it.isLetter() }
}

private fun parseIcsEvents(rawIcs: String, defaultZone: ZoneId): List<IcsCalendarEvent> {
    val lines = rawIcs.unfoldIcsLines()
    if (lines.isEmpty()) return emptyList()

    val events = mutableListOf<IcsCalendarEvent>()
    var inEvent = false
    var uid = ""
    var summary = ""
    var description = ""
    var scheduledAt: Instant? = null

    fun flushEvent() {
        if (inEvent && summary.isNotBlank() && scheduledAt != null) {
            events += IcsCalendarEvent(
                uid = uid,
                summary = summary,
                description = description.ifBlank { null },
                scheduledAt = scheduledAt
            )
        }
        uid = ""
        summary = ""
        description = ""
        scheduledAt = null
    }

    lines.forEach { line ->
        when {
            line == "BEGIN:VEVENT" -> {
                inEvent = true
                uid = ""
                summary = ""
                description = ""
                scheduledAt = null
            }

            line == "END:VEVENT" -> {
                flushEvent()
                inEvent = false
            }

            inEvent && line.startsWith("UID:") -> {
                uid = line.substringAfter(':', "").trim().decodeIcsText()
            }

            inEvent && line.startsWith("SUMMARY:") -> {
                summary = line.substringAfter(':', "").trim().decodeIcsText()
            }

            inEvent && line.startsWith("DESCRIPTION:") -> {
                description = line.substringAfter(':', "").trim().decodeIcsText()
            }

            inEvent && line.startsWith("DTSTART") -> {
                scheduledAt = line.parseIcsStartInstant(defaultZone)
            }
        }
    }
    return events
}

private fun String.unfoldIcsLines(): List<String> {
    val source = this.replace("\r\n", "\n").replace('\r', '\n')
    val out = mutableListOf<String>()
    source.lineSequence().forEach { line ->
        if (line.startsWith(" ") || line.startsWith("\t")) {
            if (out.isNotEmpty()) out[out.lastIndex] += line.trimStart()
        } else {
            out += line
        }
    }
    return out
}

private fun String.parseIcsStartInstant(defaultZone: ZoneId): Instant? {
    val parts = split(':', limit = 2)
    if (parts.size != 2) return null
    val meta = parts[0]
    val value = parts[1].trim()
    if (value.isEmpty()) return null

    val params = meta.split(';').drop(1)
    val tzid = params.firstOrNull { it.startsWith("TZID=", ignoreCase = true) }
        ?.substringAfter('=')
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    val isDateOnly = params.any { it.equals("VALUE=DATE", ignoreCase = true) } || value.length == 8
    val zone = runCatching { ZoneId.of(tzid ?: defaultZone.id) }.getOrDefault(defaultZone)
    return parseIcsDateInstant(value = value, zone = zone, dateOnly = isDateOnly)
}

private fun parseIcsDateInstant(value: String, zone: ZoneId, dateOnly: Boolean): Instant? {
    if (dateOnly) {
        return runCatching {
            LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
                .atStartOfDay(zone)
                .toInstant()
        }.getOrNull()
    }
    val compact = value.trim()
    val isUtc = compact.endsWith("Z", ignoreCase = true)
    val normalized = if (isUtc) compact.dropLast(1) else compact

    val dateTime = parseDateTimeWithPatterns(
        normalized,
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"),
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")
    ) ?: return null
    return if (isUtc) {
        dateTime.atZone(ZoneOffset.UTC).toInstant()
    } else {
        dateTime.atZone(zone).toInstant()
    }
}

private fun extractBeaReleaseInstants(obj: JsonObject): List<Instant> {
    val releaseInstants = obj["release_dates"]
        ?.jsonArray
        ?.mapNotNull { node -> parseBeaReleaseInstant(node.asScalarString()) }
        .orEmpty()
    return releaseInstants.distinct().sorted()
}

private fun parseBeaReleaseInstant(raw: String?): Instant? {
    parseMarketEventInstant(raw)?.let { return it }
    val fallbackDate = parseFlexibleDate(raw) ?: return null
    return fallbackDate
        .atTime(8, 30)
        .atZone(ZoneId.of("America/New_York"))
        .toInstant()
}

private fun parseFlexibleDate(raw: String?): LocalDate? {
    val value = raw?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
    parseDateWithPatterns(
        value,
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    )?.let { return it }
    return null
}

private fun String.stripHtmlTags(): String =
    this
        .replace(Regex("(?is)<script.*?</script>"), " ")
        .replace(Regex("(?is)<style.*?</style>"), " ")
        .replace(Regex("(?is)<[^>]+>"), "\n")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

private fun String.decodeIcsText(): String =
    this
        .replace("\\n", "\n")
        .replace("\\N", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")
        .trim()

private fun inferImpactFromTitle(title: String): MarketEventImpact {
    val text = title.lowercase()
    return when {
        "interest rate" in text || "fomc" in text || "policy" in text || "cpi" in text || "inflation" in text ||
            "payroll" in text || "employment" in text || "unemployment" in text || "gdp" in text ->
            MarketEventImpact.High
        "pmi" in text || "confidence" in text || "sentiment" in text || "industrial" in text ->
            MarketEventImpact.Medium
        else -> MarketEventImpact.Low
    }
}

private fun parseDateWithPatterns(value: String, vararg patterns: DateTimeFormatter): LocalDate? {
    patterns.forEach { formatter ->
        runCatching { LocalDate.parse(value, formatter) }.getOrNull()?.let { return it }
    }
    return null
}

private fun parseDateTimeWithPatterns(value: String, vararg patterns: DateTimeFormatter): LocalDateTime? {
    patterns.forEach { formatter ->
        runCatching { LocalDateTime.parse(value, formatter) }.getOrNull()?.let { return it }
    }
    return null
}

private data class IcsCalendarEvent(
    val uid: String,
    val summary: String,
    val description: String?,
    val scheduledAt: Instant?
)

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

private const val BrowserLikeUserAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"

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
    countryName = countryName,
    scheduledAtEpochMillis = scheduledAt.toEpochMilli(),
    impact = impact.name,
    source = source,
    description = description,
    actual = actual,
    consensus = consensus,
    forecast = forecast,
    previous = previous
)

