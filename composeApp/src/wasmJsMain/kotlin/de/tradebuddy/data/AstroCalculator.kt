package de.tradebuddy.data

import de.tradebuddy.data.astronomy.Body
import de.tradebuddy.data.astronomy.Ecliptic
import de.tradebuddy.data.astronomy.Equator
import de.tradebuddy.data.astronomy.GeoVector
import de.tradebuddy.data.astronomy.Horizon
import de.tradebuddy.data.astronomy.JsDate
import de.tradebuddy.data.astronomy.MoonQuarter
import de.tradebuddy.data.astronomy.NextMoonQuarter
import de.tradebuddy.data.astronomy.Observer
import de.tradebuddy.data.astronomy.SearchMoonQuarter
import de.tradebuddy.data.astronomy.SearchRiseSet
import de.tradebuddy.domain.model.AstroAspectEvent
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.AstroCalendarScope
import de.tradebuddy.domain.model.AstroPlanet
import de.tradebuddy.domain.model.ASTRO_MAX_ORB_DEGREES
import de.tradebuddy.domain.model.ASTRO_MIN_ORB_DEGREES
import de.tradebuddy.domain.model.City
import de.tradebuddy.domain.model.DEFAULT_ASTRO_ASPECT_ORBS
import de.tradebuddy.domain.model.MoonPhaseEvent
import de.tradebuddy.domain.model.MoonPhaseType
import de.tradebuddy.domain.model.SunMoonTimes
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.YearMonth
import kotlin.math.abs
import kotlinx.coroutines.yield

class AstroCalculator {
    private companion object {
        const val ASPECT_SCAN_STEP_MINUTES = 30L
        const val ROOT_TOLERANCE_SECONDS = 1L
        const val MAX_WINDOW_SEARCH_DAYS = 2L
    }

    fun calculate(date: LocalDate, city: City): SunMoonTimes {
        val zone = ZoneId.of(city.zoneId)
        val startInstant = date.atStartOfDay(zone).toInstant()
        val observer = Observer(city.latitude, city.longitude, city.elevationMeters)
        val limitDays = 1.0

        val sunrise = searchRiseSet(Body.Sun, observer, +1, startInstant, limitDays)?.atZone(zone)
        val sunset = searchRiseSet(Body.Sun, observer, -1, startInstant, limitDays)?.atZone(zone)
        val moonrise = searchRiseSet(Body.Moon, observer, +1, startInstant, limitDays)?.atZone(zone)
        val moonset = searchRiseSet(Body.Moon, observer, -1, startInstant, limitDays)?.atZone(zone)

        val sunriseAz = sunrise?.toInstant()?.let { azimuthAt(Body.Sun, it, observer) }
        val sunsetAz = sunset?.toInstant()?.let { azimuthAt(Body.Sun, it, observer) }
        val moonriseAz = moonrise?.toInstant()?.let { azimuthAt(Body.Moon, it, observer) }
        val moonsetAz = moonset?.toInstant()?.let { azimuthAt(Body.Moon, it, observer) }

        return SunMoonTimes(
            date = date,
            city = city,
            sunrise = sunrise,
            sunset = sunset,
            moonrise = moonrise,
            moonset = moonset,
            sunriseAzimuthDeg = sunriseAz,
            sunsetAzimuthDeg = sunsetAz,
            moonriseAzimuthDeg = moonriseAz,
            moonsetAzimuthDeg = moonsetAz
        )
    }

    fun moonPhases(month: YearMonth, userZone: ZoneId): List<MoonPhaseEvent> {
        val startInstant = month.atDay(1).atStartOfDay(userZone).toInstant()
        val endInstant = month.plusMonths(1).atDay(1).atStartOfDay(userZone).toInstant()
        val searchStart = startInstant.minus(Duration.ofDays(2))

        val out = ArrayList<MoonPhaseEvent>(6)
        var quarter: MoonQuarter = SearchMoonQuarter(searchStart.toJsDate())
        while (true) {
            val instant = quarter.time.toInstantCompat()
            if (!instant.isBefore(endInstant)) break
            if (!instant.isBefore(startInstant)) {
                out += MoonPhaseEvent(
                    type = quarterToPhaseType(quarter.quarter),
                    time = instant.atZone(userZone),
                    utcTime = instant.atZone(ZoneOffset.UTC),
                    instant = instant
                )
            }
            quarter = NextMoonQuarter(quarter)
        }
        return out
    }

    suspend fun moonPlanetAspects(
        date: LocalDate,
        zoneId: ZoneId,
        aspectOrbs: Map<AstroAspectType, Double>,
        scope: AstroCalendarScope
    ): List<AstroAspectEvent> {
        require(aspectOrbs.isNotEmpty()) { "aspectOrbs must not be empty." }

        val dayStart = date.atStartOfDay(zoneId).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        val events = ArrayList<AstroAspectEvent>(64)
        val seen = HashSet<String>()
        val pairs = aspectPairsForScope(scope)

        for ((primary, secondary) in pairs) {
            for (aspect in AstroAspectType.entries) {
                val orbDegrees = aspectOrbs[aspect] ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(aspect)
                require(orbDegrees in ASTRO_MIN_ORB_DEGREES..ASTRO_MAX_ORB_DEGREES) {
                    "Invalid orb for ${aspect.name}: $orbDegrees"
                }
                for (directionalTarget in directionalTargetsFor(aspect.angleDegrees)) {
                    val exactTimes = findAspectRootsInDay(
                        dayStart = dayStart,
                        dayEnd = dayEnd,
                        primaryBody = bodyOf(primary),
                        secondaryBody = bodyOf(secondary),
                        targetAngle = directionalTarget
                    )
                    for (exact in exactTimes) {
                        val dedupeKey =
                            "${primary.name}-${secondary.name}-${aspect.name}-${exact.epochSecond}"
                        if (!seen.add(dedupeKey)) continue

                        val windowStart = findWindowBoundary(
                            exact = exact,
                            primaryBody = bodyOf(primary),
                            secondaryBody = bodyOf(secondary),
                            targetAngle = directionalTarget,
                            orbDegrees = orbDegrees,
                            backwards = true
                        ) ?: exact
                        val windowEnd = findWindowBoundary(
                            exact = exact,
                            primaryBody = bodyOf(primary),
                            secondaryBody = bodyOf(secondary),
                            targetAngle = directionalTarget,
                            orbDegrees = orbDegrees,
                            backwards = false
                        ) ?: exact
                        events += AstroAspectEvent(
                            date = date,
                            aspectType = aspect,
                            primaryPlanet = primary,
                            secondaryPlanet = secondary,
                            exactInstant = exact,
                            windowStartInstant = windowStart,
                            windowEndInstant = windowEnd
                        )
                    }
                }
                yield()
            }
            yield()
        }

        return events.sortedBy { it.exactInstant }
    }

    private suspend fun findAspectRootsInDay(
        dayStart: Instant,
        dayEnd: Instant,
        primaryBody: String,
        secondaryBody: String,
        targetAngle: Double
    ): List<Instant> {
        val roots = mutableListOf<Instant>()
        var left = dayStart
        var leftValue = aspectDeltaAt(left, primaryBody, secondaryBody, targetAngle)
        var stepCounter = 0

        while (left.isBefore(dayEnd)) {
            val right = minInstant(left.plus(Duration.ofMinutes(ASPECT_SCAN_STEP_MINUTES)), dayEnd)
            val rightValue = aspectDeltaAt(right, primaryBody, secondaryBody, targetAngle)
            if (crossesRoot(leftValue, rightValue)) {
                val root = findRoot(left, right) { instant ->
                    aspectDeltaAt(instant, primaryBody, secondaryBody, targetAngle)
                }
                if (!root.isBefore(dayStart) && root.isBefore(dayEnd)) {
                    val duplicate = roots.any { existing ->
                        abs(Duration.between(existing, root).seconds) <= ROOT_TOLERANCE_SECONDS
                    }
                    if (!duplicate) {
                        roots += root
                    }
                }
            }
            left = right
            leftValue = rightValue
            stepCounter += 1
            if (stepCounter % 8 == 0) {
                yield()
            }
        }
        return roots
    }

    private suspend fun findWindowBoundary(
        exact: Instant,
        primaryBody: String,
        secondaryBody: String,
        targetAngle: Double,
        orbDegrees: Double,
        backwards: Boolean
    ): Instant? {
        val step = Duration.ofMinutes(ASPECT_SCAN_STEP_MINUTES)
        val limit = if (backwards) {
            exact.minus(Duration.ofDays(MAX_WINDOW_SEARCH_DAYS))
        } else {
            exact.plus(Duration.ofDays(MAX_WINDOW_SEARCH_DAYS))
        }
        var near = exact
        var nearValue = windowDeltaAt(near, primaryBody, secondaryBody, targetAngle, orbDegrees)
        var stepCounter = 0
        while (true) {
            val far = if (backwards) near.minus(step) else near.plus(step)
            val outOfBounds = if (backwards) far.isBefore(limit) else far.isAfter(limit)
            if (outOfBounds) return null

            val farValue = windowDeltaAt(far, primaryBody, secondaryBody, targetAngle, orbDegrees)
            if (crossesRoot(nearValue, farValue)) {
                val left = if (backwards) far else near
                val right = if (backwards) near else far
                return findRoot(left, right) { instant ->
                    windowDeltaAt(instant, primaryBody, secondaryBody, targetAngle, orbDegrees)
                }
            }
            near = far
            nearValue = farValue
            stepCounter += 1
            if (stepCounter % 8 == 0) {
                yield()
            }
        }
    }

    private suspend fun findRoot(
        left: Instant,
        right: Instant,
        function: (Instant) -> Double
    ): Instant {
        var a = left
        var b = right
        var fa = function(a)
        var fb = function(b)

        if (abs(fa) < 1.0e-9) return a
        if (abs(fb) < 1.0e-9) return b

        for (iteration in 0 until 80) {
            val mid = midpoint(a, b)
            val fm = function(mid)
            if (abs(Duration.between(a, b).seconds) <= ROOT_TOLERANCE_SECONDS || abs(fm) < 1.0e-7) {
                return mid
            }

            val midOnLeftSide = (fa <= 0.0 && fm <= 0.0) || (fa >= 0.0 && fm >= 0.0)
            if (midOnLeftSide) {
                a = mid
                fa = fm
            } else {
                b = mid
                fb = fm
            }
            if (iteration % 8 == 7) {
                yield()
            }
        }

        return midpoint(a, b)
    }

    private fun windowDeltaAt(
        instant: Instant,
        primaryBody: String,
        secondaryBody: String,
        targetAngle: Double,
        orbDegrees: Double
    ): Double = abs(aspectDeltaAt(instant, primaryBody, secondaryBody, targetAngle)) - orbDegrees

    private fun aspectDeltaAt(
        instant: Instant,
        primaryBody: String,
        secondaryBody: String,
        targetAngle: Double
    ): Double {
        val primaryLongitude = geocentricEclipticLongitude(primaryBody, instant)
        val secondaryLongitude = geocentricEclipticLongitude(secondaryBody, instant)
        val separation = normalize360(primaryLongitude - secondaryLongitude)
        return normalizeSigned(separation - targetAngle)
    }

    private fun aspectPairsForScope(scope: AstroCalendarScope): List<Pair<AstroPlanet, AstroPlanet>> =
        when (scope) {
            AstroCalendarScope.MoonOnly -> {
                AstroPlanet.entries
                    .asSequence()
                    .filterNot { it == AstroPlanet.Moon }
                    .map { planet -> AstroPlanet.Moon to planet }
                    .toList()
            }

            AstroCalendarScope.AllPlanets -> {
                val planets = AstroPlanet.entries
                buildList {
                    for (firstIndex in planets.indices) {
                        for (secondIndex in (firstIndex + 1) until planets.size) {
                            add(planets[firstIndex] to planets[secondIndex])
                        }
                    }
                }
            }
        }

    private fun geocentricEclipticLongitude(body: String, instant: Instant): Double {
        val geocentric = GeoVector(body, instant.toJsDate(), true)
        return Ecliptic(geocentric).elon
    }

    private fun crossesRoot(a: Double, b: Double): Boolean {
        if (!a.isFinite() || !b.isFinite()) return false
        if (a == 0.0 || b == 0.0) return true
        val changesSign = (a < 0.0 && b > 0.0) || (a > 0.0 && b < 0.0)
        return changesSign && minOf(abs(a), abs(b)) < 90.0
    }

    private fun minInstant(a: Instant, b: Instant): Instant = if (a.isBefore(b)) a else b

    private fun midpoint(a: Instant, b: Instant): Instant =
        Instant.ofEpochMilli((a.toEpochMilli() + b.toEpochMilli()) / 2L)

    private fun normalize360(value: Double): Double {
        var normalized = value % 360.0
        if (normalized < 0.0) normalized += 360.0
        return normalized
    }

    private fun normalizeSigned(value: Double): Double {
        val normalized = normalize360(value)
        return if (normalized > 180.0) normalized - 360.0 else normalized
    }

    private fun directionalTargetsFor(aspectAngle: Double): List<Double> {
        val normalized = normalize360(aspectAngle)
        if (normalized == 0.0 || normalized == 180.0) return listOf(normalized)
        val mirror = normalize360(360.0 - normalized)
        return if (mirror == normalized) listOf(normalized) else listOf(normalized, mirror)
    }

    private fun azimuthAt(body: String, instant: Instant, observer: Observer): Double {
        val equ = Equator(body, instant.toJsDate(), observer, true, true)
        return Horizon(instant.toJsDate(), observer, equ.ra, equ.dec, "normal").azimuth
    }

    private fun searchRiseSet(
        body: String,
        observer: Observer,
        direction: Int,
        start: Instant,
        limitDays: Double
    ): Instant? =
        SearchRiseSet(body, observer, direction, start.toJsDate(), limitDays)?.toInstantCompat()

    private fun quarterToPhaseType(quarter: Int): MoonPhaseType =
        when (quarter) {
            0 -> MoonPhaseType.NewMoon
            1 -> MoonPhaseType.FirstQuarter
            2 -> MoonPhaseType.FullMoon
            3 -> MoonPhaseType.LastQuarter
            else -> MoonPhaseType.NewMoon
        }

    private fun bodyOf(planet: AstroPlanet): String = when (planet) {
        AstroPlanet.Moon -> Body.Moon
        AstroPlanet.Sun -> Body.Sun
        AstroPlanet.Mercury -> Body.Mercury
        AstroPlanet.Venus -> Body.Venus
        AstroPlanet.Mars -> Body.Mars
        AstroPlanet.Jupiter -> Body.Jupiter
        AstroPlanet.Saturn -> Body.Saturn
        AstroPlanet.Uranus -> Body.Uranus
        AstroPlanet.Neptune -> Body.Neptune
        AstroPlanet.Pluto -> Body.Pluto
    }
}

private fun Instant.toJsDate(): JsDate = JsDate(toEpochMilli().toDouble())
private fun de.tradebuddy.data.astronomy.AstroTime.toInstantCompat(): Instant =
    Instant.ofEpochMilli(date.getTime().toLong())
