package de.tradebuddy.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance

@Composable
fun appElevatedCardColors(): CardColors {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    return if (isLight) {
        CardDefaults.elevatedCardColors(containerColor = scheme.surface)
    } else {
        CardDefaults.elevatedCardColors()
    }
}
