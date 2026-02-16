package de.tradebuddy.domain.util

import de.tradebuddy.domain.model.CompactEvent
import de.tradebuddy.domain.model.CompactEventType
import de.tradebuddy.domain.model.SunMoonTimes
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

fun buildCompactEvents(
    results: List<SunMoonTimes>,
    userZone: ZoneId,
    targetDate: LocalDate,
    sunTimeOffsetMinutes: Int = 0,
    moonTimeOffsetMinutes: Int = 0
): List<CompactEvent> {
    fun mk(
        r: SunMoonTimes,
        eventType: CompactEventType,
        cityTime: ZonedDateTime?,
        az: Double?
    ): CompactEvent {
        val cityZone = ZoneId.of(r.city.zoneId)
        val offsetMinutes = when (eventType) {
            CompactEventType.Sunrise,
            CompactEventType.Sunset -> sunTimeOffsetMinutes.toLong()

            CompactEventType.Moonrise,
            CompactEventType.Moonset -> moonTimeOffsetMinutes.toLong()
        }
        val adjustedInstant = cityTime
            ?.toInstant()
            ?.plus(Duration.ofMinutes(offsetMinutes))
        val adjustedCityTime = adjustedInstant?.atZone(cityZone)
        val userTime = adjustedInstant?.atZone(userZone)
        val utcTime = adjustedInstant?.atZone(ZoneOffset.UTC)
        val cityDayOffset = adjustedCityTime?.let {
            ChronoUnit.DAYS.between(targetDate, it.toLocalDate()).toInt()
        } ?: 0
        return CompactEvent(
            cityLabel = r.city.label,
            cityKey = r.city.key(),
            eventType = eventType,
            azimuthDeg = az,
            cityTime = adjustedCityTime,
            userTime = userTime,
            utcTime = utcTime,
            userInstant = userTime?.toInstant(),
            cityDayOffset = cityDayOffset
        )
    }

    val out = ArrayList<CompactEvent>(results.size * 4)
    for (r in results) {
        out += mk(r, CompactEventType.Sunrise, r.sunrise, r.sunriseAzimuthDeg)
        out += mk(r, CompactEventType.Sunset, r.sunset, r.sunsetAzimuthDeg)
        out += mk(r, CompactEventType.Moonrise, r.moonrise, r.moonriseAzimuthDeg)
        out += mk(r, CompactEventType.Moonset, r.moonset, r.moonsetAzimuthDeg)
    }
    return out.filter { it.userTime?.toLocalDate() == targetDate }
}

