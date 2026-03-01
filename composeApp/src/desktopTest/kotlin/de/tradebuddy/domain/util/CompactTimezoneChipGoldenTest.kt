package de.tradebuddy.domain.util

import de.tradebuddy.domain.model.City
import de.tradebuddy.domain.model.CompactEventType
import de.tradebuddy.domain.model.SunMoonTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class CompactTimezoneChipGoldenTest {

    @Test
    fun chipRendering_goldenSnapshot() {
        val tokyo = city("Tokyo", "JP", "Asia/Tokyo")
        val berlin = city("Berlin", "DE", "Europe/Berlin")
        val instant = Instant.parse("2026-02-15T23:30:00Z")

        val events = buildCompactEvents(
            results = listOf(
                SunMoonTimes(
                    date = LocalDate.of(2026, 2, 16),
                    city = tokyo,
                    sunrise = instant.atZone(ZoneId.of(tokyo.zoneId)),
                    sunset = null,
                    moonrise = null,
                    moonset = null,
                    sunriseAzimuthDeg = null,
                    sunsetAzimuthDeg = null,
                    moonriseAzimuthDeg = null,
                    moonsetAzimuthDeg = null
                ),
                SunMoonTimes(
                    date = LocalDate.of(2026, 2, 16),
                    city = berlin,
                    sunrise = instant.atZone(ZoneId.of(berlin.zoneId)),
                    sunset = null,
                    moonrise = null,
                    moonset = null,
                    sunriseAzimuthDeg = null,
                    sunsetAzimuthDeg = null,
                    moonriseAzimuthDeg = null,
                    moonsetAzimuthDeg = null
                )
            ),
            userZone = ZoneId.of("America/New_York"),
            targetDate = LocalDate.of(2026, 2, 15)
        )
            .filter { it.eventType == CompactEventType.Sunrise }
            .sortedBy { it.cityLabel }

        val snapshot = events.joinToString("\n") { e ->
            "${e.cityLabel}|chip=${e.timezoneChipLabel ?: "none"}"
        }
        val expected = """
Berlin|chip=+6h · in anderer Zeitzone schon morgen
Tokyo|chip=+14h · in anderer Zeitzone schon morgen
""".trimIndent()
        assertEquals(expected, snapshot)
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

