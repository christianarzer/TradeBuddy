package de.tradebuddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AppThemeStyle

@Composable
fun AppTheme(
    themeStyle: AppThemeStyle,
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val extendedColors = remember(themeStyle, themeMode) {
        extendedColorsFor(themeStyle, themeMode)
    }
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorSchemeFor(themeStyle, themeMode),
            typography = appTypography(),
            shapes = AppShapes,
            content = content
        )
    }
}

val MaterialTheme.extended: ExtendedColors
    @Composable get() = LocalExtendedColors.current
