package de.tradebuddy.domain.model

import io.github.cosinekitty.astronomy.Body
import java.time.Instant
import java.time.LocalDate

const val MOON_GLYPH: String = "\u263D\uFE0E"

enum class AstroAspectType(
    val label: String,
    val angleDegrees: Double,
    val glyph: String
) {
    Conjunction(label = "Konjunktion", angleDegrees = 0.0, glyph = "\u260C\uFE0E"),
    Sextile(label = "Sextil", angleDegrees = 60.0, glyph = "\u26B9\uFE0E"),
    Square(label = "Quadrat", angleDegrees = 90.0, glyph = "\u25A1\uFE0E"),
    Trine(label = "Trigon", angleDegrees = 120.0, glyph = "\u25B3\uFE0E"),
    Opposition(label = "Opposition", angleDegrees = 180.0, glyph = "\u260D\uFE0E")
}

enum class AstroPlanet(
    val body: Body,
    val label: String,
    val glyph: String
) {
    Moon(body = Body.Moon, label = "Mond", glyph = "\u263D\uFE0E"),
    Sun(body = Body.Sun, label = "Sonne", glyph = "\u2609\uFE0E"),
    Mercury(body = Body.Mercury, label = "Merkur", glyph = "\u263F\uFE0E"),
    Venus(body = Body.Venus, label = "Venus", glyph = "\u2640\uFE0E"),
    Mars(body = Body.Mars, label = "Mars", glyph = "\u2642\uFE0E"),
    Jupiter(body = Body.Jupiter, label = "Jupiter", glyph = "\u2643\uFE0E"),
    Saturn(body = Body.Saturn, label = "Saturn", glyph = "\u2644\uFE0E"),
    Uranus(body = Body.Uranus, label = "Uranus", glyph = "\u2645\uFE0E"),
    Neptune(body = Body.Neptune, label = "Neptun", glyph = "\u2646\uFE0E"),
    Pluto(body = Body.Pluto, label = "Pluto", glyph = "\u2647\uFE0E")
}

enum class AstroSortMode {
    ExactTime,
    AspectType
}

enum class AstroCalendarScope {
    MoonOnly,
    AllPlanets
}

enum class AstroEventStatus {
    Upcoming,
    Exact,
    Passed
}

const val ASTRO_MIN_ORB_DEGREES: Double = 0.5
const val ASTRO_MAX_ORB_DEGREES: Double = 10.0
const val ASTRO_ORB_STEP_DEGREES: Double = 0.1

// Transit-oriented major aspect orbs from Astrotheme's transits FAQ:
// https://www.astrotheme.com/astrology_aspects.php
// conjunction 2.6, opposition 2.5, trine 2.3, square 2.3, sextile 2.3.
val DEFAULT_ASTRO_ASPECT_ORBS: Map<AstroAspectType, Double> = mapOf(
    AstroAspectType.Conjunction to 2.6,
    AstroAspectType.Sextile to 2.3,
    AstroAspectType.Square to 2.3,
    AstroAspectType.Trine to 2.3,
    AstroAspectType.Opposition to 2.5
)

data class AstroAspectEvent(
    val date: LocalDate,
    val aspectType: AstroAspectType,
    val primaryPlanet: AstroPlanet,
    val secondaryPlanet: AstroPlanet,
    val exactInstant: Instant,
    val windowStartInstant: Instant,
    val windowEndInstant: Instant
)

data class AstroWeekDaySummary(
    val date: LocalDate,
    val eventCount: Int
)
