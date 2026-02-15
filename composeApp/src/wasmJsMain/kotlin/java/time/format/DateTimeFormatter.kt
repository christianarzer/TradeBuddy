package java.time.format

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Locale

class DateTimeFormatter private constructor(
    private val pattern: String,
    private val locale: Locale
) {
    companion object {
        fun ofPattern(pattern: String, locale: Locale = Locale.ROOT): DateTimeFormatter =
            DateTimeFormatter(pattern, locale)
    }

    fun format(value: LocalDate): String {
        val day = value.raw.dayOfMonth
        val month = value.raw.monthNumber
        val year = value.raw.year
        val weekday = weekdayIndex(value)
        return render(pattern, year, month, day, weekday, null)
    }

    fun format(value: LocalTime): String =
        render(
            pattern = pattern,
            year = null,
            month = null,
            day = null,
            weekday = null,
            time = TimeParts(value.hour, value.minute, value.second)
        )

    fun format(value: ZonedDateTime): String {
        val local = value.localDateTime()
        val date = local.date
        val time = local.time
        val weekday = weekdayIndex(date.year, date.monthNumber, date.dayOfMonth)
        return render(
            pattern = pattern,
            year = date.year,
            month = date.monthNumber,
            day = date.dayOfMonth,
            weekday = weekday,
            time = TimeParts(time.hour, time.minute, time.second)
        )
    }

    private fun render(
        pattern: String,
        year: Int?,
        month: Int?,
        day: Int?,
        weekday: Int?,
        time: TimeParts?
    ): String {
        val monthsLong = monthNames(locale)
        val weekdaysShort = weekdayNamesShort(locale)
        val values = linkedMapOf(
            "yyyy" to (year?.toString()?.padStart(4, '0') ?: ""),
            "MMMM" to (month?.let { monthsLong[it - 1] } ?: ""),
            "MM" to (month?.toString()?.padStart(2, '0') ?: ""),
            "dd" to (day?.toString()?.padStart(2, '0') ?: ""),
            "EEE" to (weekday?.let { weekdaysShort[it] } ?: ""),
            "HH" to (time?.hour?.toString()?.padStart(2, '0') ?: ""),
            "mm" to (time?.minute?.toString()?.padStart(2, '0') ?: ""),
            "ss" to (time?.second?.toString()?.padStart(2, '0') ?: "")
        )

        var out = pattern
        values.forEach { (token, replacement) ->
            out = out.replace(token, replacement)
        }
        return out
    }
}

private data class TimeParts(
    val hour: Int,
    val minute: Int,
    val second: Int
)

private fun monthNames(locale: Locale): List<String> = when (locale.languageTag) {
    "de-DE" -> listOf(
        "Januar", "Februar", "Maerz", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember"
    )
    else -> listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
}

private fun weekdayNamesShort(locale: Locale): List<String> = when (locale.languageTag) {
    "de-DE" -> listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    else -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
}

private fun weekdayIndex(value: LocalDate): Int =
    weekdayIndex(value.raw.year, value.raw.monthNumber, value.raw.dayOfMonth)

private fun weekdayIndex(year: Int, month: Int, day: Int): Int {
    // Zeller style; returns Monday=0 ... Sunday=6.
    var y = year
    var m = month
    if (m < 3) {
        m += 12
        y -= 1
    }
    val k = y % 100
    val j = y / 100
    val h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7
    val zellerToMonday = when (h) {
        0 -> 5 // Saturday
        1 -> 6 // Sunday
        2 -> 0 // Monday
        3 -> 1
        4 -> 2
        5 -> 3
        else -> 4
    }
    return zellerToMonday
}

