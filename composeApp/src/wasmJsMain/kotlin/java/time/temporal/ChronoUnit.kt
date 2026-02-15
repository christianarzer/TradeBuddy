package java.time.temporal

import java.time.LocalDate

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
