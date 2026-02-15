package de.tradebuddy.domain.util

import java.time.ZoneOffset
import kotlin.math.abs

fun formatUtcOffset(offset: ZoneOffset): String {
    val totalMinutes = offset.totalSeconds / 60
    val sign = if (totalMinutes >= 0) "+" else "-"
    val absMin = abs(totalMinutes)
    val hh = absMin / 60
    val mm = absMin % 60
    return "%s%02d:%02d".format(sign, hh, mm)
}

fun formatOffsetDiff(diffSeconds: Int): String {
    if (diffSeconds == 0) return "+/-0h"
    val sign = if (diffSeconds > 0) "+" else "-"
    val a = abs(diffSeconds)
    val hours = a / 3600
    val minutes = (a % 3600) / 60
    return if (minutes == 0) "${sign}${hours}h" else "${sign}${hours}h ${minutes}m"
}

