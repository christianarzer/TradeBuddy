package de.tradebuddy.domain.util

import de.tradebuddy.domain.model.City
import de.tradebuddy.domain.model.CompactEventType
import de.tradebuddy.domain.model.SunMoonTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompactEventBuilderTest {

    @Test
    fun cityDayOffset_isMinusOne_whenCityIsPreviousDay() {
        val userZone = ZoneId.of("Europe/Berlin")
        val city = city("New York", "US", "America/New_York")
        val instant = Instant.parse("2026-02-15T23:30:00Z")

        val events = buildCompactEvents(
            results = listOf(
                SunMoonTimes(
                    date = LocalDate.of(2026, 2, 16),
                    city = city,
                    sunrise = instant.atZone(ZoneId.of(city.zoneId)),
                    sunset = null,
                    moonrise = null,
                    moonset = null,
                    sunriseAzimuthDeg = null,
                    sunsetAzimuthDeg = null,
                    moonriseAzimuthDeg = null,
                    moonsetAzimuthDeg = null
                )
            ),
            userZone = userZone,
            targetDate = LocalDate.of(2026, 2, 16)
        )

        assertEquals(1, events.size)
        assertEquals(CompactEventType.Sunrise, events.first().eventType)
        assertEquals(-1, events.first().cityDayOffset)
        assertTrue(events.first().timezoneChipLabel?.contains("gestern") == true)
    }

    @Test
    fun cityDayOffset_isPlusOne_whenCityIsNextDay() {
        val userZone = ZoneId.of("America/New_York")
        val city = city("Tokyo", "JP", "Asia/Tokyo")
        val instant = Instant.parse("2026-02-16T23:30:00Z")

        val events = buildCompactEvents(
            results = listOf(
                SunMoonTimes(
                    date = LocalDate.of(2026, 2, 16),
                    city = city,
                    sunrise = instant.atZone(ZoneId.of(city.zoneId)),
                    sunset = null,
                    moonrise = null,
                    moonset = null,
                    sunriseAzimuthDeg = null,
                    sunsetAzimuthDeg = null,
                    moonriseAzimuthDeg = null,
                    moonsetAzimuthDeg = null
                )
            ),
            userZone = userZone,
            targetDate = LocalDate.of(2026, 2, 16)
        )

        assertEquals(1, events.size)
        assertEquals(CompactEventType.Sunrise, events.first().eventType)
        assertEquals(1, events.first().cityDayOffset)
        assertTrue(events.first().timezoneChipLabel?.contains("morgen") == true)
    }

    @Test
    fun cityDayOffset_isPlusOne_forSingaporeMoonrise_case() {
        val userZone = ZoneId.of("Europe/Berlin")
        val city = city("Singapur", "SG", "Asia/Singapore")
        // 00:30 next day in Singapore, still previous day in Berlin.
        val instant = Instant.parse("2026-01-15T16:30:00Z")

        val events = buildCompactEvents(
            results = listOf(
                SunMoonTimes(
                    date = LocalDate.of(2026, 1, 15),
                    city = city,
                    sunrise = null,
                    sunset = null,
                    moonrise = instant.atZone(ZoneId.of(city.zoneId)),
                    moonset = null,
                    sunriseAzimuthDeg = null,
                    sunsetAzimuthDeg = null,
                    moonriseAzimuthDeg = null,
                    moonsetAzimuthDeg = null
                )
            ),
            userZone = userZone,
            targetDate = LocalDate.of(2026, 1, 15)
        )

        assertEquals(1, events.size)
        assertEquals(CompactEventType.Moonrise, events.first().eventType)
        assertEquals(1, events.first().cityDayOffset)
        assertTrue(events.first().timezoneChipLabel?.contains("+7h") == true)
    }

    private fun city(label: String, countryCode: String, zoneId: String): City =
        City(
            label = label,
            country = label,
            countryCode = countryCode,
            zoneId = zoneId,
            latitude = 0.0,
            longitude = 0.0
        )
}
