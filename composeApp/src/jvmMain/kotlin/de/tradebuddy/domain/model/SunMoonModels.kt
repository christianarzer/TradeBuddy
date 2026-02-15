package de.tradebuddy.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

data class City(
    val label: String,
    val country: String,
    val countryCode: String,
    val zoneId: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double = 10.0
)

data class SunMoonTimes(
    val date: LocalDate,
    val city: City,
    val sunrise: ZonedDateTime?,
    val sunset: ZonedDateTime?,
    val moonrise: ZonedDateTime?,
    val moonset: ZonedDateTime?,
    val sunriseAzimuthDeg: Double?,
    val sunsetAzimuthDeg: Double?,
    val moonriseAzimuthDeg: Double?,
    val moonsetAzimuthDeg: Double?
)

enum class CompactEventType {
    Sunrise,
    Sunset,
    Moonrise,
    Moonset
}

data class CompactEvent(
    val cityLabel: String,
    val cityKey: String,
    val eventType: CompactEventType,
    val azimuthDeg: Double?,
    val cityTime: ZonedDateTime?,
    val userTime: ZonedDateTime?,
    val utcTime: ZonedDateTime?,
    val userInstant: Instant?,
    val cityDayOffset: Int
)

enum class MoonPhaseType {
    NewMoon,
    FirstQuarter,
    FullMoon,
    LastQuarter
}

data class MoonPhaseEvent(
    val type: MoonPhaseType,
    val time: ZonedDateTime,
    val utcTime: ZonedDateTime,
    val instant: Instant
)

data class TrendSeries(
    val label: String,
    val values: List<Double?>
)

data class MonthTrend(
    val city: City,
    val days: List<LocalDate>,
    val sun: TrendSeries,
    val moon: TrendSeries
)

