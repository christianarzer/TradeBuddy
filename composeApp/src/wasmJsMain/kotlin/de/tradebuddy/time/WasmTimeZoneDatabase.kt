package de.tradebuddy.time

import kotlin.js.JsAny

@JsModule("@js-joda/timezone")
private external val jsJodaTimezoneModule: JsAny

fun ensureTimeZoneDatabaseLoaded() {
    jsJodaTimezoneModule
}
