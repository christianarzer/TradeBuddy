package de.tradebuddy.domain.util

import de.tradebuddy.domain.model.CompactEvent
import de.tradebuddy.domain.model.CompactEventType
import de.tradebuddy.domain.model.SunMoonTimes
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

fun buildCompactEvents(
    results: List<SunMoonTimes>,
    userZone: ZoneId,
    targetDate: LocalDate
): List<CompactEvent> {
    fun mk(
        r: SunMoonTimes,
        eventType: CompactEventType,
        cityTime: ZonedDateTime?,
        az: Double?
    ): CompactEvent {
        val userTime = cityTime?.withZoneSameInstant(userZone)
        val utcTime = cityTime?.withZoneSameInstant(ZoneOffset.UTC)
        val cityDayOffset = if (cityTime != null) {
            ChronoUnit.DAYS.between(targetDate, cityTime.toLocalDate()).toInt()
        } else {
            0
        }
        return CompactEvent(
            cityLabel = r.city.label,
            cityKey = r.city.key(),
            eventType = eventType,
            azimuthDeg = az,
            cityTime = cityTime,
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

