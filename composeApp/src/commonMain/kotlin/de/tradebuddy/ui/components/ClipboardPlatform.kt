package de.tradebuddy.ui.components

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberCopyTextToClipboard(): (String) -> Unit
