package de.tradebuddy.logging

import kotlinx.browser.window

internal actual fun nowEpochMillis(): Long = window.performance.now().toLong()
