package java.time.temporal

import java.time.LocalDate
import kotlin.math.abs

class ChronoUnit private constructor() {
    companion object {
        val DAYS: DaysUnit = DaysUnit
    }
}

object DaysUnit {
    fun between(startInclusive: LocalDate, endExclusive: LocalDate): Long {
        val a = startInclusive.raw.toEpochDays()
        val b = endExclusive.raw.toEpochDays()
        return b - a
    }
}

private fun kotlinx.datetime.LocalDate.toEpochDays(): Long {
    // Based on proleptic Gregorian ordinal conversion.
    val y = year
    val m = monthNumber
    val d = dayOfMonth
    val y1 = y - if (m <= 2) 1 else 0
    val era = if (y1 >= 0) y1 / 400 else (y1 - 399) / 400
    val yoe = y1 - era * 400
    val doy = (153 * (m + if (m > 2) -3 else 9) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era.toLong() * 146097L + doe.toLong() - 719468L
}

