package de.tradebuddy.data

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
import io.github.cosinekitty.astronomy.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.YearMonth
import kotlin.math.abs

class AstroCalculator {
    private companion object {
        const val ASPECT_SCAN_STEP_MINUTES = 30L
        const val ROOT_TOLERANCE_SECONDS = 1L
        const val MAX_WINDOW_SEARCH_DAYS = 2L
    }

    fun calculate(date: LocalDate, city: City): SunMoonTimes {
        val zone = ZoneId.of(city.zoneId)
        val startInstant = date.atStartOfDay(zone).toInstant()
        val startTime = Time.fromMillisecondsSince1970(startInstant.toEpochMilli())
        val observer = Observer(city.latitude, city.longitude, city.elevationMeters)
        val limitDays = 1.0
        val sunriseT = searchRiseSet(Body.Sun, observer, Direction.Rise, startTime, limitDays)
        val sunsetT = searchRiseSet(Body.Sun, observer, Direction.Set, startTime, limitDays)
        val moonriseT = searchRiseSet(Body.Moon, observer, Direction.Rise, startTime, limitDays)
        val moonsetT = searchRiseSet(Body.Moon, observer, Direction.Set, startTime, limitDays)
        val sunriseAz = sunriseT?.let { azimuthAt(Body.Sun, it, observer) }
        val sunsetAz = sunsetT?.let { azimuthAt(Body.Sun, it, observer) }
        val moonriseAz = moonriseT?.let { azimuthAt(Body.Moon, it, observer) }
        val moonsetAz = moonsetT?.let { azimuthAt(Body.Moon, it, observer) }
        return SunMoonTimes(
            date = date,
            city = city,
            sunrise = sunriseT?.toZonedDateTime(zone),
            sunset = sunsetT?.toZonedDateTime(zone),
            moonrise = moonriseT?.toZonedDateTime(zone),
            moonset = moonsetT?.toZonedDateTime(zone),
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
        for (quarter in moonQuartersAfter(Time.fromMillisecondsSince1970(searchStart.toEpochMilli()))) {
            val instant = Instant.ofEpochMilli(quarter.time.toMillisecondsSince1970())
            if (!instant.isBefore(endInstant)) break
            if (instant.isBefore(startInstant)) continue

            val localTime = instant.atZone(userZone)
            out += MoonPhaseEvent(
                type = quarter.toPhaseType(),
                time = localTime,
                utcTime = instant.atZone(ZoneOffset.UTC),
                instant = instant
            )
        }
        return out
    }

    fun moonPlanetAspects(
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
                        primaryBody = primary.body,
                        secondaryBody = secondary.body,
                        targetAngle = directionalTarget
                    )
                    for (exact in exactTimes) {
                        val dedupeKey =
                            "${primary.name}-${secondary.name}-${aspect.name}-${exact.epochSecond}"
                        if (!seen.add(dedupeKey)) continue

                        val windowStart = findWindowBoundary(
                            exact = exact,
                            primaryBody = primary.body,
                            secondaryBody = secondary.body,
                            targetAngle = directionalTarget,
                            orbDegrees = orbDegrees,
                            backwards = true
                        ) ?: exact
                        val windowEnd = findWindowBoundary(
                            exact = exact,
                            primaryBody = primary.body,
                            secondaryBody = secondary.body,
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
            }
        }

        return events.sortedBy { it.exactInstant }
    }

    private fun findAspectRootsInDay(
        dayStart: Instant,
        dayEnd: Instant,
        primaryBody: Body,
        secondaryBody: Body,
        targetAngle: Double
    ): List<Instant> {
        val roots = mutableListOf<Instant>()
        var left = dayStart
        var leftValue = aspectDeltaAt(left, primaryBody, secondaryBody, targetAngle)

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
        }
        return roots
    }

    private fun findWindowBoundary(
        exact: Instant,
        primaryBody: Body,
        secondaryBody: Body,
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
        }
    }

    private fun findRoot(
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

        repeat(80) {
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
        }

        return midpoint(a, b)
    }

    private fun windowDeltaAt(
        instant: Instant,
        primaryBody: Body,
        secondaryBody: Body,
        targetAngle: Double,
        orbDegrees: Double
    ): Double = abs(aspectDeltaAt(instant, primaryBody, secondaryBody, targetAngle)) - orbDegrees

    private fun aspectDeltaAt(
        instant: Instant,
        primaryBody: Body,
        secondaryBody: Body,
        targetAngle: Double
    ): Double {
        val time = Time.fromMillisecondsSince1970(instant.toEpochMilli())
        val primaryLongitude = geocentricEclipticLongitude(primaryBody, time)
        val secondaryLongitude = geocentricEclipticLongitude(secondaryBody, time)
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

    private fun geocentricEclipticLongitude(body: Body, time: Time): Double {
        val geocentric = geoVector(body, time, Aberration.Corrected)
        return equatorialToEcliptic(geocentric).elon
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

    private fun azimuthAt(body: Body, time: Time, observer: Observer): Double {
        val equ = equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val hor = horizon(time, observer, equ.ra, equ.dec, Refraction.Normal)
        return hor.azimuth
    }

    private fun Time.toZonedDateTime(zone: ZoneId): ZonedDateTime =
        Instant.ofEpochMilli(this.toMillisecondsSince1970()).atZone(zone)

    private fun MoonQuarterInfo.toPhaseType(): MoonPhaseType =
        when (quarter) {
            0 -> MoonPhaseType.NewMoon
            1 -> MoonPhaseType.FirstQuarter
            2 -> MoonPhaseType.FullMoon
            3 -> MoonPhaseType.LastQuarter
            else -> MoonPhaseType.NewMoon
        }
}

