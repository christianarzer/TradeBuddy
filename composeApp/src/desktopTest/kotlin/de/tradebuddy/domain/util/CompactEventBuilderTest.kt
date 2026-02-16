package de.tradebuddy.domain.util

import de.tradebuddy.domain.model.City
import de.tradebuddy.domain.model.CompactEventType
import de.tradebuddy.domain.model.SunMoonTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

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
