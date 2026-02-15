@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package de.tradebuddy

import androidx.compose.ui.window.CanvasBasedWindow
import de.tradebuddy.ui.AppRoot

fun main() {
    CanvasBasedWindow(title = "TradeBuddy") {
        AppRoot()
    }
}
