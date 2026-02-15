package de.tradebuddy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.tradebuddy.logging.AppLog
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.browser.window

@Composable
@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun rememberCopyTextToClipboard(): (String) -> Unit =
    remember {
        { text ->
            runCatching {
                window.navigator.clipboard.writeText(text)
            }.onFailure { error ->
                AppLog.warn(
                    tag = "Clipboard",
                    message = "Failed to copy text to clipboard",
                    throwable = error
                )
            }
        }
    }
