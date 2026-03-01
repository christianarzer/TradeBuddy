package de.tradebuddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import de.tradebuddy.domain.model.AppAccentColor
import de.tradebuddy.domain.model.AppThemeMode

@Composable
fun AppTheme(
    themeMode: AppThemeMode,
    accentColor: AppAccentColor,
    content: @Composable () -> Unit
) {
    val extendedColors = remember(themeMode, accentColor) {
        extendedColorsFor(themeMode)
    }
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorSchemeFor(themeMode, accentColor),
            typography = appTypography(),
            shapes = AppShapes,
            content = content
        )
    }
}

val MaterialTheme.extended: ExtendedColors
    @Composable get() = LocalExtendedColors.current
