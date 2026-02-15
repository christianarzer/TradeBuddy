package de.tradebuddy.logging

@JsName("Date")
private external object JsDateCtor {
    fun now(): Double
}

internal actual fun nowEpochMillis(): Long = JsDateCtor.now().toLong()
