package de.tradebuddy.domain.util

import de.tradebuddy.domain.model.City
import de.tradebuddy.domain.model.SunMoonTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompactEventTimeRulesTest {

    @Test
    fun inUserTodayRange_includesStartOfDay() {
        val userZone = ZoneId.of("Europe/Berlin")
        val now = Instant.parse("2026-03-10T12:00:00Z")
        val atStart = now.atZone(userZone).toLocalDate().atStartOfDay(userZone)
        assertTrue(isInUserTodayRange(atStart, userZone, now))
    }

    @Test
    fun inUserTodayRange_excludesBeforeStartOfDay() {
        val userZone = ZoneId.of("Europe/Berlin")
        val now = Instant.parse("2026-03-10T12:00:00Z")
        val beforeStart = now.atZone(userZone).toLocalDate().atStartOfDay(userZone).minusMinutes(1)
        assertFalse(isInUserTodayRange(beforeStart, userZone, now))
    }

    @Test
    fun inUserTodayRange_excludesNextDayStart() {
        val userZone = ZoneId.of("Europe/Berlin")
        val now = Instant.parse("2026-03-10T12:00:00Z")
        val nextDayStart = now.atZone(userZone).toLocalDate().plusDays(1).atStartOfDay(userZone)
        assertFalse(isInUserTodayRange(nextDayStart, userZone, now))
    }

    @Test
    fun chipNeeded_whenDatesDiffer() {
        assertTrue(
            needsTimezoneChip(
                eventZoneDate = LocalDate.of(2026, 2, 16),
                userZoneDate = LocalDate.of(2026, 2, 15)
            )
        )
    }

    @Test
    fun chipNotNeeded_whenDatesEqual() {
        assertFalse(
            needsTimezoneChip(
                eventZoneDate = LocalDate.of(2026, 2, 16),
                userZoneDate = LocalDate.of(2026, 2, 16)
            )
        )
    }

    @Test
    fun chipLabel_tomorrow_withPositiveOffset() {
        val label = buildChipLabel(
            eventZoneDate = LocalDate.of(2026, 2, 16),
            userZoneDate = LocalDate.of(2026, 2, 15),
            offsetMinutes = 60
        )
        assertEquals("+1h · in anderer Zeitzone schon morgen", label)
    }

    @Test
    fun chipLabel_yesterday_withNegativeOffset() {
        val label = buildChipLabel(
            eventZoneDate = LocalDate.of(2026, 2, 15),
            userZoneDate = LocalDate.of(2026, 2, 16),
            offsetMinutes = -360
        )
        assertEquals("-6h · in anderer Zeitzone noch gestern", label)
    }

    @Test
    fun chipLabel_supportsHalfHourOffsets() {
        val label = buildChipLabel(
            eventZoneDate = LocalDate.of(2026, 2, 16),
            userZoneDate = LocalDate.of(2026, 2, 15),
            offsetMinutes = 330
        )
        assertEquals("+5h30m · in anderer Zeitzone schon morgen", label)
    }

    @Test
    fun chipLabel_null_whenEventZoneMissing() {
        val label = buildChipLabel(
            eventZoneDate = null,
            userZoneDate = LocalDate.of(2026, 2, 15),
            offsetMinutes = 0
        )
        assertNull(label)
    }

    @Test
    fun buildCompactEvents_handlesDstTransition_consistently() {
        val city = city("New York", "US", "America/New_York")
        val targetDate = LocalDate.of(2026, 3, 8) // US DST start date
        val instant = Instant.parse("2026-03-08T06:30:00Z") // 01:30 local pre-switch
        val userZone = ZoneId.of("Europe/Berlin")
        val events = buildCompactEvents(
            results = listOf(
                SunMoonTimes(
                    date = targetDate,
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
            targetDate = LocalDate.of(2026, 3, 8)
        )
        assertEquals(1, events.size)
        assertNotNull(events.first().userTime)
    }

    @Test
    fun buildCompactEvents_changesWhenUserZoneChanges() {
        val city = city("Tokyo", "JP", "Asia/Tokyo")
        val instant = Instant.parse("2026-02-15T23:30:00Z")
        val source = SunMoonTimes(
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
        val ny = buildCompactEvents(
            results = listOf(source),
            userZone = ZoneId.of("America/New_York"),
            targetDate = LocalDate.of(2026, 2, 15)
        )
        val berlin = buildCompactEvents(
            results = listOf(source),
            userZone = ZoneId.of("Europe/Berlin"),
            targetDate = LocalDate.of(2026, 2, 16)
        )
        assertEquals(1, ny.size)
        assertEquals(1, berlin.size)
        assertTrue(ny.first().timezoneChipLabel?.contains("morgen") == true)
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
