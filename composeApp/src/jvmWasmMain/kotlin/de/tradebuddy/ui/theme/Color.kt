package de.tradebuddy.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import de.tradebuddy.domain.model.AppAccentColor
import de.tradebuddy.domain.model.AppThemeMode

private val LightBackground = AppPalette.SurfaceLight
private val LightSurface = AppPalette.SurfaceLight
private val LightSurfaceSoft = AppPalette.SurfaceLightAlt
private val LightSurfaceMuted = AppPalette.SurfaceLightSoft
private val LightOnSurface = AppPalette.Black
private val LightOnSurfaceMuted = AppPalette.Black80
private val LightOutline = AppPalette.Black10
private val LightOutlineVariant = AppPalette.Black4

private val DarkBackground = AppPalette.SurfaceDark
private val DarkSurface = AppPalette.SurfaceDark
private val DarkSurfaceSoft = AppPalette.SurfaceDarkAlt
private val DarkSurfaceMuted = AppPalette.SurfaceDarkSoft
private val DarkOnSurface = AppPalette.White
private val DarkOnSurfaceMuted = AppPalette.White
private val DarkOutline = AppPalette.White10
private val DarkOutlineVariant = AppPalette.White20

val TradeBuddyLightColorScheme = lightColorScheme(
    primary = LightOnSurface,
    onPrimary = AppPalette.White,
    primaryContainer = AppPalette.SurfaceLightSoft,
    onPrimaryContainer = LightOnSurface,
    secondary = AppPalette.Indigo,
    onSecondary = AppPalette.Black,
    secondaryContainer = AppPalette.SurfaceLightSoft,
    onSecondaryContainer = AppPalette.Black,
    tertiary = AppPalette.Blue,
    onTertiary = AppPalette.Black,
    tertiaryContainer = AppPalette.SurfaceLightInfo,
    onTertiaryContainer = AppPalette.Black,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceTint = Color.Transparent,
    surfaceVariant = LightSurfaceSoft,
    onSurfaceVariant = LightOnSurfaceMuted,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = AppPalette.Red,
    onError = AppPalette.Black
)

val TradeBuddyDarkColorScheme = darkColorScheme(
    // Dark reference in Figma uses the lavender brand accent for active/selected elements.
    primary = AppPalette.Purple,
    onPrimary = AppPalette.Black,
    primaryContainer = AppPalette.SurfaceDarkSoft,
    onPrimaryContainer = AppPalette.White,
    secondary = AppPalette.Indigo,
    onSecondary = AppPalette.Black,
    secondaryContainer = AppPalette.SurfaceDarkSoft,
    onSecondaryContainer = AppPalette.White,
    tertiary = AppPalette.Blue,
    onTertiary = AppPalette.Black,
    tertiaryContainer = AppPalette.SurfaceDarkSoft,
    onTertiaryContainer = AppPalette.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceTint = Color.Transparent,
    surfaceVariant = DarkSurfaceSoft,
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = AppPalette.Red,
    onError = AppPalette.Black
)

val DashboardSurfaceLight: Color = LightSurfaceSoft
val DashboardSurfaceDark: Color = DarkSurfaceSoft
val DashboardMutedSurfaceLight: Color = LightSurfaceMuted
val DashboardMutedSurfaceDark: Color = DarkSurfaceMuted

fun colorSchemeFor(
    mode: AppThemeMode,
    accent: AppAccentColor
) = if (mode == AppThemeMode.Light) {
    TradeBuddyLightColorScheme.copy(
        secondary = accentColorFor(accent),
        onSecondary = AppPalette.Black,
        secondaryContainer = AppPalette.SurfaceLightSoft,
        onSecondaryContainer = AppPalette.Black
    )
} else {
    TradeBuddyDarkColorScheme.copy(
        primary = accentColorFor(accent),
        onPrimary = AppPalette.Black,
        secondary = accentColorFor(accent),
        onSecondary = AppPalette.Black,
        secondaryContainer = AppPalette.SurfaceDarkSoft,
        onSecondaryContainer = AppPalette.White
    )
}
