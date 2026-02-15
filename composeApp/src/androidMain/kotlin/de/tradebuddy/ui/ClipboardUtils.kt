package de.tradebuddy.ui

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString

actual fun createTextClipEntry(text: AnnotatedString): ClipEntry {
    val clip = ClipData.newPlainText("text", text.text)
    return ClipEntry(clip)
}
