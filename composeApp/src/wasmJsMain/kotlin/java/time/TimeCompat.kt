package java.time

import java.time.format.DateTimeFormatter
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate as KLocalDate
import kotlinx.datetime.LocalDateTime as KLocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant as KInstant

class Duration private constructor(
    val seconds: Long
) {
    companion object {
        fun ofDays(days: Long): Duration = Duration(days * 86_400L)
        fun ofDays(days: Int): Duration = ofDays(days.toLong())
        fun ofMinutes(minutes: Long): Duration = Duration(minutes * 60L)
        fun ofMinutes(minutes: Int): Duration = ofMinutes(minutes.toLong())

        fun between(startInclusive: Instant, endExclusive: Instant): Duration {
            val diffMillis = endExclusive.toEpochMilli() - startInclusive.toEpochMilli()
            return Duration(diffMillis / 1_000L)
        }
    }
}

open class ZoneId internal constructor(
    val id: String
) {
    internal val timeZone: TimeZone = parseTimeZone(id)
    val rules: ZoneRules = ZoneRules(this)

    companion object {
        fun of(id: String): ZoneId = ZoneId(id)
        fun systemDefault(): ZoneId = ZoneId(systemTimeZoneId())
    }
}

class ZoneOffset private constructor(
    val totalSeconds: Int
) : ZoneId(offsetId(totalSeconds)) {
    companion object {
        val UTC: ZoneOffset = ZoneOffset(0)

        fun ofTotalSeconds(totalSeconds: Int): ZoneOffset =
            if (totalSeconds == 0) UTC else ZoneOffset(totalSeconds)
    }
}

class ZoneRules internal constructor(
    private val zoneId: ZoneId
) {
    fun getOffset(instant: Instant): ZoneOffset {
        val local = instantToLocalDateTime(instant.toEpochMilli(), zoneId.timeZone)
        val shifted = local.toInstant(TimeZone.UTC)
        val seconds = ((shifted.toEpochMilliseconds() - instant.toEpochMilli()) / 1_000L).toInt()
        return ZoneOffset.ofTotalSeconds(seconds)
    }
}

class Instant internal constructor(
    internal val raw: KInstant
) : Comparable<Instant> {

    val epochSecond: Long get() = toEpochMilli() / 1_000L

    companion object {
        val MAX: Instant = KInstant.parse("9999-12-31T23:59:59.999Z").toCompat()

        fun now(): Instant = ofEpochMilli(JsDateCtor.now().toLong())
        fun ofEpochMilli(millis: Long): Instant = KInstant.fromEpochMilliseconds(millis).toCompat()
    }

    fun toEpochMilli(): Long = raw.toEpochMilliseconds()

    fun isBefore(other: Instant): Boolean = raw < other.raw
    fun isAfter(other: Instant): Boolean = raw > other.raw

    fun plus(duration: Duration): Instant =
        KInstant.fromEpochMilliseconds(toEpochMilli() + duration.seconds * 1_000L).toCompat()

    fun minus(duration: Duration): Instant =
        KInstant.fromEpochMilliseconds(toEpochMilli() - duration.seconds * 1_000L).toCompat()

    fun atZone(zone: ZoneId): ZonedDateTime = ZonedDateTime(this, zone)

    override fun compareTo(other: Instant): Int = raw.compareTo(other.raw)

    override fun equals(other: Any?): Boolean = other is Instant && raw == other.raw
    override fun hashCode(): Int = raw.hashCode()
    override fun toString(): String = raw.toString()
}

class LocalDate internal constructor(
    internal val raw: KLocalDate
) : Comparable<LocalDate> {
    val dayOfMonth: Int get() = raw.day

    companion object {
        fun now(zoneId: ZoneId): LocalDate = Instant.now().raw.toLocalDate(zoneId)
    }

    fun plusDays(days: Long): LocalDate = raw.plus(DatePeriod(days = days.toInt())).toCompat()
    fun minusDays(days: Long): LocalDate = raw.minus(DatePeriod(days = days.toInt())).toCompat()

    fun atStartOfDay(zone: ZoneId): ZonedDateTime {
        val instant = raw.atStartOfDayIn(zone.timeZone).toCompat()
        return ZonedDateTime(instant, zone)
    }

    fun atTime(hour: Int, minute: Int): LocalDateTime =
        LocalDateTime(KLocalDateTime(raw.year, raw.month.ordinal + 1, raw.day, hour, minute))

    fun lengthOfMonth(): Int = daysInMonth(raw.year, raw.month.ordinal + 1)

    fun format(formatter: DateTimeFormatter): String = formatter.format(this)

    override fun compareTo(other: LocalDate): Int = raw.compareTo(other.raw)
    override fun equals(other: Any?): Boolean = other is LocalDate && raw == other.raw
    override fun hashCode(): Int = raw.hashCode()
    override fun toString(): String = raw.toString()
}

class LocalDateTime internal constructor(
    internal val raw: KLocalDateTime
) {
    fun atZone(zone: ZoneId): ZonedDateTime =
        ZonedDateTime(raw.toInstant(zone.timeZone).toCompat(), zone)
}

class LocalTime internal constructor(
    val hour: Int,
    val minute: Int,
    val second: Int
) {
    fun format(formatter: DateTimeFormatter): String = formatter.format(this)
}

class YearMonth private constructor(
    private val year: Int,
    private val month: Int
) {
    companion object {
        fun from(date: LocalDate): YearMonth = YearMonth(date.raw.year, date.raw.month.ordinal + 1)
        fun from(zonedDateTime: ZonedDateTime): YearMonth = from(zonedDateTime.toLocalDate())
    }

    fun plusMonths(months: Long): YearMonth {
        val total = year * 12L + (month - 1) + months
        val y = floorDiv(total, 12L).toInt()
        val m = ((total % 12L + 12L) % 12L).toInt() + 1
        return YearMonth(y, m)
    }

    fun atDay(day: Int): LocalDate = KLocalDate(year, month, day).toCompat()
    fun lengthOfMonth(): Int = daysInMonth(year, month)

    override fun equals(other: Any?): Boolean =
        other is YearMonth && year == other.year && month == other.month

    override fun hashCode(): Int = 31 * year + month
    override fun toString(): String = "$year-${month.toString().padStart(2, '0')}"
}

class ZonedDateTime internal constructor(
    private val instant: Instant,
    private val zone: ZoneId
) : Comparable<ZonedDateTime> {

    fun toInstant(): Instant = instant

    fun withZoneSameInstant(zoneId: ZoneId): ZonedDateTime = ZonedDateTime(instant, zoneId)

    fun plusMinutes(minutes: Long): ZonedDateTime =
        ZonedDateTime(instant.plus(Duration.ofMinutes(minutes)), zone)

    fun toLocalDate(): LocalDate = localDateTime().date.toCompat()

    fun toLocalTime(): LocalTime {
        val local = localDateTime().time
        return LocalTime(local.hour, local.minute, local.second)
    }

    fun format(formatter: DateTimeFormatter): String = formatter.format(this)

    internal fun localDateTime(): KLocalDateTime =
        instantToLocalDateTime(instant.toEpochMilli(), zone.timeZone)

    override fun compareTo(other: ZonedDateTime): Int = instant.compareTo(other.instant)
    override fun equals(other: Any?): Boolean =
        other is ZonedDateTime && instant == other.instant && zone.id == other.zone.id

    override fun hashCode(): Int = 31 * instant.hashCode() + zone.id.hashCode()
    override fun toString(): String = "${instant}@${zone.id}"
}

private fun KInstant.toCompat(): Instant = Instant(this)
private fun KLocalDate.toCompat(): LocalDate = LocalDate(this)
private fun KInstant.toLocalDate(zoneId: ZoneId): LocalDate =
    instantToLocalDateTime(toEpochMilliseconds(), zoneId.timeZone).date.toCompat()

@JsName("Date")
external object JsDateCtor {
    fun now(): Double
}

@JsName("Date")
private external class JsDateFull(millis: Double) {
    fun getUTCFullYear(): Int
    fun getUTCMonth(): Int
    fun getUTCDate(): Int
    fun getUTCHours(): Int
    fun getUTCMinutes(): Int
    fun getUTCSeconds(): Int
}

@JsName("Intl")
external object JsIntl {
    class DateTimeFormat {
        fun resolvedOptions(): JsResolvedOptions
    }
}

external interface JsResolvedOptions {
    val timeZone: String?
}

/**
 * Converts epoch millis to a [KLocalDateTime] at the given [TimeZone].
 *
 * For UTC (and fixed-offset) zones we compute the date-time components
 * from the epoch millis directly via JS `Date.getUTC*()`, bypassing
 * kotlinx-datetime's `toLocalDateTime` which can mishandle UTC on WASM-JS.
 *
 * For named zones we delegate to kotlinx-datetime which correctly uses the
 * browser's local-time facilities.
 */
private fun instantToLocalDateTime(epochMillis: Long, zone: TimeZone): KLocalDateTime {
    val offsetSeconds = fixedOffsetSeconds(zone)
    if (offsetSeconds != null) {
        val adjusted = epochMillis + offsetSeconds * 1_000L
        val d = JsDateFull(adjusted.toDouble())
        return KLocalDateTime(
            d.getUTCFullYear(),
            d.getUTCMonth() + 1,
            d.getUTCDate(),
            d.getUTCHours(),
            d.getUTCMinutes(),
            d.getUTCSeconds()
        )
    }
    // Named zone — kotlinx-datetime handles these correctly on WASM-JS
    return KInstant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(zone)
}

/**
 * If [zone] is a fixed-offset zone (UTC, Z, GMT, ±HH:MM, UTC±HH:MM)
 * returns the offset in seconds; otherwise returns `null`.
 */
private fun fixedOffsetSeconds(zone: TimeZone): Long? {
    val id = zone.id
    if (id == "UTC" || id == "Z" || id == "GMT") return 0L
    UtcOffsetZoneRegex.matchEntire(id)?.groupValues?.getOrNull(1)?.let { return parseOffsetSeconds(it) }
    OffsetZoneRegex.matchEntire(id)?.let { return parseOffsetSeconds(id) }
    return null
}

private fun parseOffsetSeconds(offset: String): Long {
    val sign = if (offset.startsWith('-')) -1L else 1L
    val abs = offset.removePrefix("+").removePrefix("-")
    val parts = abs.split(":")
    val hours = parts.getOrNull(0)?.toLongOrNull() ?: 0L
    val minutes = parts.getOrNull(1)?.toLongOrNull() ?: 0L
    return sign * (hours * 3600L + minutes * 60L)
}


private fun systemTimeZoneId(): String {
    val browserId = runCatching {
        JsIntl.DateTimeFormat().resolvedOptions().timeZone
    }.getOrNull()
    val normalizedBrowserId = normalizedTimeZoneId(browserId)

    if (normalizedBrowserId != null && !normalizedBrowserId.equals("UTC", ignoreCase = true)) {
        return normalizedBrowserId
    }

    val systemId = normalizedTimeZoneId(TimeZone.currentSystemDefault().id)
    if (systemId != null && !systemId.equals("UTC", ignoreCase = true)) {
        return systemId
    }

    return "Europe/Berlin"
}

private fun String.isUsableSystemZoneId(): Boolean =
    isNotBlank() &&
        !equals("SYSTEM", ignoreCase = true) &&
        !equals("Etc/Unknown", ignoreCase = true)

private val OffsetZoneRegex = Regex("^[+-][0-9]{2}:[0-9]{2}$")
private val UtcOffsetZoneRegex = Regex("^(?:UTC|GMT)([+-][0-9]{2}:[0-9]{2})$", RegexOption.IGNORE_CASE)
private val ZeroZuluRegex = Regex("^Z$", RegexOption.IGNORE_CASE)

private fun normalizedTimeZoneId(candidate: String?): String? {
    val value = candidate?.takeIf { it.isUsableSystemZoneId() } ?: return null
    if (ZeroZuluRegex.matches(value)) return "UTC"
    UtcOffsetZoneRegex.matchEntire(value)?.groupValues?.getOrNull(1)?.let { return "UTC$it" }
    if (OffsetZoneRegex.matches(value)) return "UTC$value"
    return runCatching { TimeZone.of(value).id }.getOrNull()
}

private fun parseTimeZone(id: String): TimeZone =
    runCatching { TimeZone.of(id) }
        .recoverCatching {
            val normalizedOffsetId = when {
                ZeroZuluRegex.matches(id) -> "UTC"
                UtcOffsetZoneRegex.matches(id) -> {
                    val offset = UtcOffsetZoneRegex.matchEntire(id)!!.groupValues[1]
                    "UTC$offset"
                }
                OffsetZoneRegex.matches(id) -> "UTC$id"
                else -> null
            }
            if (normalizedOffsetId != null) {
                TimeZone.of(normalizedOffsetId)
            } else {
                throw it
            }
        }
        .getOrElse { TimeZone.UTC }

private fun offsetId(totalSeconds: Int): String {
    if (totalSeconds == 0) return "Z"
    val sign = if (totalSeconds >= 0) "+" else "-"
    val abs = kotlin.math.abs(totalSeconds)
    val hh = abs / 3600
    val mm = (abs % 3600) / 60
    return "$sign${hh.toString().padStart(2, '0')}:${mm.toString().padStart(2, '0')}"
}

private fun floorDiv(value: Long, divisor: Long): Long {
    var q = value / divisor
    val r = value % divisor
    if (r != 0L && ((value xor divisor) < 0L)) {
        q -= 1
    }
    return q
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 30
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
