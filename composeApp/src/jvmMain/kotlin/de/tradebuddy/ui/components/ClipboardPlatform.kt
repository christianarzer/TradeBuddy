package de.tradebuddy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import de.tradebuddy.logging.AppLog
import de.tradebuddy.ui.createTextClipEntry
import kotlinx.coroutines.launch

@Composable
internal actual fun rememberCopyTextToClipboard(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope) {
        { text ->
            scope.launch {
                runCatching {
                    clipboard.setClipEntry(createTextClipEntry(AnnotatedString(text)))
                }.onFailure { error ->
                    AppLog.warn(
                        tag = "Clipboard",
                        message = "Failed to copy text to clipboard",
                        throwable = error
                    )
                }
            }
        }
    }
}
