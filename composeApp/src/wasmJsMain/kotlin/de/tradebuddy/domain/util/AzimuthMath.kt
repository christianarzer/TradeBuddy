package de.tradebuddy.domain.util

import kotlin.math.roundToInt

fun azimuthToCardinalIndex(deg: Double): Int {
    return (((deg % 360 + 360) % 360) / 22.5).roundToInt() % 16
}

fun normAz360(v: Double): Double {
    val x = v % 360.0
    return if (x < 0) x + 360.0 else x
}

fun unwrapDegrees(input: List<Double?>): List<Double?> {
    var offset = 0.0
    var last: Double? = null
    val out = ArrayList<Double?>(input.size)

    for (v0 in input) {
        if (v0 == null) {
            out += null
            continue
        }
        var v = v0 + offset
        val lastValue = last
        if (lastValue != null) {
            val diff = v - lastValue
            if (diff > 180.0) {
                offset -= 360.0
                v = v0 + offset
            } else if (diff < -180.0) {
                offset += 360.0
                v = v0 + offset
            }
        }
        out += v
        last = v
    }
    return out
}

