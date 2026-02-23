package de.tradebuddy.time

import kotlin.OptIn
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsModule("@js-joda/timezone")
private external val jsJodaTimezoneModule: JsAny

@OptIn(ExperimentalWasmJsInterop::class)
fun ensureTimeZoneDatabaseLoaded() {
    jsJodaTimezoneModule
}
