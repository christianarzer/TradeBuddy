package de.tradebuddy.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

fun toUserZonedDateTime(
    eventTime: ZonedDateTime?,
    eventZone: ZoneId?,
    userZone: ZoneId
): ZonedDateTime? {
    if (eventTime == null) return null
    return if (eventZone != null) {
        eventTime.toInstant().atZone(userZone)
    } else {
        eventTime.withZoneSameInstant(userZone)
    }
}

fun isInUserTodayRange(
    userZonedDateTime: ZonedDateTime?,
    userZone: ZoneId,
    now: Instant
): Boolean {
    val today = now.atZone(userZone).toLocalDate()
    return isInUserDateRange(userZonedDateTime, userZone, today)
}

fun needsTimezoneChip(eventZoneDate: LocalDate?, userZoneDate: LocalDate?): Boolean =
    eventZoneDate != null && userZoneDate != null && eventZoneDate != userZoneDate

fun buildChipLabel(
    eventZoneDate: LocalDate?,
    userZoneDate: LocalDate?,
    offsetMinutes: Int
): String? {
    val eventDate = eventZoneDate ?: return null
    val userDate = userZoneDate ?: return null
    if (!needsTimezoneChip(eventDate, userDate)) return null
    val dayDelta = ChronoUnit.DAYS.between(userDate, eventDate)
    val dayLabel = when {
        dayDelta > 0 -> "in anderer Zeitzone schon morgen"
        dayDelta < 0 -> "in anderer Zeitzone noch gestern"
        else -> "anderes Tagesdatum"
    }
    return "${formatOffsetHours(offsetMinutes)} · $dayLabel"
}

internal fun isInUserDateRange(
    userZonedDateTime: ZonedDateTime?,
    userZone: ZoneId,
    targetDate: LocalDate
): Boolean {
    if (userZonedDateTime == null) return false
    val instant = userZonedDateTime.toInstant()
    val start = targetDate.atStartOfDay(userZone).toInstant()
    val end = targetDate.plusDays(1).atStartOfDay(userZone).toInstant()
    return !instant.isBefore(start) && instant.isBefore(end)
}

internal fun formatOffsetHours(offsetMinutes: Int): String {
    val sign = if (offsetMinutes >= 0) "+" else "-"
    val absMinutes = abs(offsetMinutes)
    val hours = absMinutes / 60
    val minutes = absMinutes % 60
    return if (minutes == 0) {
        "${sign}${hours}h"
    } else {
        "${sign}${hours}h${minutes}m"
    }
}
