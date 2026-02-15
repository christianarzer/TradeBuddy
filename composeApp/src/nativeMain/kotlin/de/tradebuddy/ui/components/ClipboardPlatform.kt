package de.tradebuddy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberCopyTextToClipboard(): (String) -> Unit =
    remember {
        { _ -> Unit }
    }
