package de.tradebuddy.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
actual fun createTextClipEntry(text: AnnotatedString): ClipEntry =
    ClipEntry(StringSelection(text.text))
