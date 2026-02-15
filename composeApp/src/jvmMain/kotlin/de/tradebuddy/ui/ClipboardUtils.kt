package de.tradebuddy.ui

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString

expect fun createTextClipEntry(text: AnnotatedString): ClipEntry
