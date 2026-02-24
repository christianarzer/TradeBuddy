package de.tradebuddy.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AppThemeStyle

private val LightBaseBackground = Color(0xFFF7F9FC)
private val LightBaseSurface = Color(0xFFFFFFFF)
private val LightBaseSurfaceVariant = Color(0xFFEEF2F7)
private val LightBaseOn = Color(0xFF142033)
private val LightBaseOnVariant = Color(0xFF4A5668)
private val LightBaseOutline = Color(0xFFC8D1DE)
private val LightBaseOutlineVariant = Color(0xFFDDE4EE)

private val DarkBaseBackground = Color(0xFF0E1116)
private val DarkBaseSurface = Color(0xFF151A21)
private val DarkBaseSurfaceVariant = Color(0xFF212833)
private val DarkBaseOn = Color(0xFFE8ECF2)
private val DarkBaseOnVariant = Color(0xFFB2BCCB)
private val DarkBaseOutline = Color(0xFF3A4557)
private val DarkBaseOutlineVariant = Color(0xFF2D3545)

private fun lightScheme(
    primary: Color,
    primaryContainer: Color,
    secondary: Color,
    secondaryContainer: Color,
    tertiary: Color,
    tertiaryContainer: Color,
    background: Color = LightBaseBackground,
    surface: Color = LightBaseSurface,
    surfaceVariant: Color = LightBaseSurfaceVariant
) = lightColorScheme(
    primary = primary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = primaryContainer,
    onPrimaryContainer = LightBaseOn,
    secondary = secondary,
    onSecondary = LightBaseOn,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = LightBaseOn,
    tertiary = tertiary,
    onTertiary = LightBaseOn,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = LightBaseOn,
    background = background,
    onBackground = LightBaseOn,
    surface = surface,
    onSurface = LightBaseOn,
    surfaceTint = Color.Transparent,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = LightBaseOnVariant,
    outline = LightBaseOutline,
    outlineVariant = LightBaseOutlineVariant,
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF)
)

private fun darkScheme(
    primary: Color,
    primaryContainer: Color,
    secondary: Color,
    secondaryContainer: Color,
    tertiary: Color,
    tertiaryContainer: Color,
    background: Color = DarkBaseBackground,
    surface: Color = DarkBaseSurface,
    surfaceVariant: Color = DarkBaseSurfaceVariant,
    onSurfaceVariant: Color = DarkBaseOnVariant,
    outline: Color = DarkBaseOutline,
    outlineVariant: Color = DarkBaseOutlineVariant
) = darkColorScheme(
    primary = primary,
    onPrimary = Color(0xFF0B1614),
    primaryContainer = primaryContainer,
    onPrimaryContainer = Color(0xFFE6FFF9),
    secondary = secondary,
    onSecondary = Color(0xFF231100),
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = Color(0xFFFFE8C4),
    tertiary = tertiary,
    onTertiary = Color(0xFF0B1620),
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = Color(0xFFD8ECFF),
    background = background,
    onBackground = DarkBaseOn,
    surface = surface,
    onSurface = DarkBaseOn,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    outlineVariant = outlineVariant,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF3A0A00)
)

val NeonLightColors = lightScheme(
    primary = Color(0xFF0EA5E9),
    primaryContainer = Color(0xFFCFFAFE),
    secondary = Color(0xFF84CC16),
    secondaryContainer = Color(0xFFECFCCB),
    tertiary = Color(0xFFF97316),
    tertiaryContainer = Color(0xFFFFE7D1),
    background = Color(0xFFF5FBFF),
    surfaceVariant = Color(0xFFE7F2F9)
)

val NeonDarkColors = darkScheme(
    primary = Color(0xFF38BDF8),
    primaryContainer = Color(0xFF0B4B6B),
    secondary = Color(0xFFBEF264),
    secondaryContainer = Color(0xFF365314),
    tertiary = Color(0xFFFDBA74),
    tertiaryContainer = Color(0xFF7C2D12),
    background = Color(0xFF0A1116),
    surface = Color(0xFF101A21),
    surfaceVariant = Color(0xFF17252F),
    onSurfaceVariant = Color(0xFFB3C7D6),
    outline = Color(0xFF334A5B),
    outlineVariant = Color(0xFF263848)
)

val TerminalLightColors = lightScheme(
    primary = Color(0xFF15803D),
    primaryContainer = Color(0xFFC7F6D4),
    secondary = Color(0xFFEA580C),
    secondaryContainer = Color(0xFFFFE0C2),
    tertiary = Color(0xFF0EA5E9),
    tertiaryContainer = Color(0xFFCCEFFF),
    background = Color(0xFFF2F8F3),
    surfaceVariant = Color(0xFFE1EEE4)
)

val TerminalDarkColors = darkScheme(
    primary = Color(0xFF22C55E),
    primaryContainer = Color(0xFF0F3D24),
    secondary = Color(0xFFF87171),
    secondaryContainer = Color(0xFF5B1A14),
    tertiary = Color(0xFF38BDF8),
    tertiaryContainer = Color(0xFF0E3B52),
    background = Color(0xFF0B100C),
    surface = Color(0xFF121814),
    surfaceVariant = Color(0xFF1C231F),
    onSurfaceVariant = Color(0xFFA6B3AB),
    outline = Color(0xFF34423A),
    outlineVariant = Color(0xFF273128)
)

val MidnightLightColors = lightScheme(
    primary = Color(0xFF1D4ED8),
    primaryContainer = Color(0xFFDDE6FF),
    secondary = Color(0xFF0891B2),
    secondaryContainer = Color(0xFFCFF2FF),
    tertiary = Color(0xFF10B981),
    tertiaryContainer = Color(0xFFD6F5E8),
    background = Color(0xFFF1F5FF),
    surfaceVariant = Color(0xFFE1E9F9)
)

val MidnightDarkColors = darkScheme(
    primary = Color(0xFF60A5FA),
    primaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF22D3EE),
    secondaryContainer = Color(0xFF0F4C59),
    tertiary = Color(0xFFA7F3D0),
    tertiaryContainer = Color(0xFF0E5A43),
    background = Color(0xFF0B1020),
    surface = Color(0xFF121A2B),
    surfaceVariant = Color(0xFF1C2438),
    onSurfaceVariant = Color(0xFFB3BFD4),
    outline = Color(0xFF364057),
    outlineVariant = Color(0xFF2A3346)
)

val HorizonLightColors = lightScheme(
    primary = Color(0xFFF97316),
    primaryContainer = Color(0xFFFFE3D1),
    secondary = Color(0xFF0EA5E9),
    secondaryContainer = Color(0xFFCFEFFF),
    tertiary = Color(0xFF22C55E),
    tertiaryContainer = Color(0xFFD6F5E0),
    background = Color(0xFFFFF7F2),
    surfaceVariant = Color(0xFFF5E8DE)
)

val HorizonDarkColors = darkScheme(
    primary = Color(0xFFFB923C),
    primaryContainer = Color(0xFF7C2D12),
    secondary = Color(0xFF38BDF8),
    secondaryContainer = Color(0xFF0B3B52),
    tertiary = Color(0xFF4ADE80),
    tertiaryContainer = Color(0xFF0B3B2A),
    background = Color(0xFF140D0A),
    surface = Color(0xFF1B120E),
    surfaceVariant = Color(0xFF2A1B14),
    onSurfaceVariant = Color(0xFFCBB9AF),
    outline = Color(0xFF4C362C),
    outlineVariant = Color(0xFF3A2A22)
)

val OceanLightColors = lightScheme(
    primary = Color(0xFF0284C7),
    primaryContainer = Color(0xFFCAE9FF),
    secondary = Color(0xFF06B6D4),
    secondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFF22C55E),
    tertiaryContainer = Color(0xFFD6F5E0),
    background = Color(0xFFF0FAFF),
    surfaceVariant = Color(0xFFDDEFF7)
)

val OceanDarkColors = darkScheme(
    primary = Color(0xFF38BDF8),
    primaryContainer = Color(0xFF0B4B6B),
    secondary = Color(0xFF22D3EE),
    secondaryContainer = Color(0xFF0B4D59),
    tertiary = Color(0xFF4ADE80),
    tertiaryContainer = Color(0xFF0B3B2A),
    background = Color(0xFF07171F),
    surface = Color(0xFF0E1F2A),
    surfaceVariant = Color(0xFF152B38)
)

val SlateLightColors = lightScheme(
    primary = Color(0xFF334155),
    primaryContainer = Color(0xFFDDE3EA),
    secondary = Color(0xFF64748B),
    secondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF0EA5E9),
    tertiaryContainer = Color(0xFFCCEFFF),
    background = Color(0xFFF4F6F8),
    surfaceVariant = Color(0xFFE2E6EA)
)

val SlateDarkColors = darkScheme(
    primary = Color(0xFF94A3B8),
    primaryContainer = Color(0xFF334155),
    secondary = Color(0xFF64748B),
    secondaryContainer = Color(0xFF1F2937),
    tertiary = Color(0xFF38BDF8),
    tertiaryContainer = Color(0xFF0B3B52),
    background = Color(0xFF0D1117),
    surface = Color(0xFF141A22),
    surfaceVariant = Color(0xFF1D2531)
)

val AuroraLightColors = lightScheme(
    primary = Color(0xFF059669),
    primaryContainer = Color(0xFFBFF3E2),
    secondary = Color(0xFF10B981),
    secondaryContainer = Color(0xFFD4F8EB),
    tertiary = Color(0xFF2563EB),
    tertiaryContainer = Color(0xFFDCE7FF),
    background = Color(0xFFF1FBF7),
    surfaceVariant = Color(0xFFDFF3EC)
)

val AuroraDarkColors = darkScheme(
    primary = Color(0xFF34D399),
    primaryContainer = Color(0xFF0F4C3A),
    secondary = Color(0xFF22D3EE),
    secondaryContainer = Color(0xFF0B4D59),
    tertiary = Color(0xFF60A5FA),
    tertiaryContainer = Color(0xFF1E3A8A),
    background = Color(0xFF081412),
    surface = Color(0xFF0F1C19),
    surfaceVariant = Color(0xFF162724)
)

val CopperLightColors = lightScheme(
    primary = Color(0xFFB45309),
    primaryContainer = Color(0xFFFFE1C7),
    secondary = Color(0xFFD97706),
    secondaryContainer = Color(0xFFFFE7C7),
    tertiary = Color(0xFF0F766E),
    tertiaryContainer = Color(0xFFCCF2EA),
    background = Color(0xFFFFF5ED),
    surfaceVariant = Color(0xFFF3E5D6)
)

val CopperDarkColors = darkScheme(
    primary = Color(0xFFFB923C),
    primaryContainer = Color(0xFF7C2D12),
    secondary = Color(0xFFFBBF24),
    secondaryContainer = Color(0xFF6B3D00),
    tertiary = Color(0xFF5EEAD4),
    tertiaryContainer = Color(0xFF115E59),
    background = Color(0xFF150D08),
    surface = Color(0xFF1D120D),
    surfaceVariant = Color(0xFF2A1B14)
)

val ArcticLightColors = lightScheme(
    primary = Color(0xFF0EA5E9),
    primaryContainer = Color(0xFFCDEFFE),
    secondary = Color(0xFF38BDF8),
    secondaryContainer = Color(0xFFDFF4FF),
    tertiary = Color(0xFF14B8A6),
    tertiaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFFF3FAFF),
    surfaceVariant = Color(0xFFE1F0FA)
)

val ArcticDarkColors = darkScheme(
    primary = Color(0xFF7DD3FC),
    primaryContainer = Color(0xFF0B4B6B),
    secondary = Color(0xFF38BDF8),
    secondaryContainer = Color(0xFF0B3B52),
    tertiary = Color(0xFF2DD4BF),
    tertiaryContainer = Color(0xFF0B4A44),
    background = Color(0xFF07131A),
    surface = Color(0xFF0E1C24),
    surfaceVariant = Color(0xFF152833)
)

val NimbusLightColors = lightScheme(
    primary = Color(0xFF2563EB),
    primaryContainer = Color(0xFFDCE7FF),
    secondary = Color(0xFF64748B),
    secondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF14B8A6),
    tertiaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFFF5F7FB),
    surfaceVariant = Color(0xFFE4E9F2)
)

val NimbusDarkColors = darkScheme(
    primary = Color(0xFF60A5FA),
    primaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF94A3B8),
    secondaryContainer = Color(0xFF1F2937),
    tertiary = Color(0xFF2DD4BF),
    tertiaryContainer = Color(0xFF0B4A44),
    background = Color(0xFF0C111A),
    surface = Color(0xFF121A24),
    surfaceVariant = Color(0xFF1B2431),
    onSurfaceVariant = Color(0xFFB5C1D3),
    outline = Color(0xFF364158),
    outlineVariant = Color(0xFF2A3346)
)

val PulseLightColors = lightScheme(
    primary = Color(0xFFEF4444),
    primaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFF59E0B),
    secondaryContainer = Color(0xFFFFE3B3),
    tertiary = Color(0xFF06B6D4),
    tertiaryContainer = Color(0xFFCFFAFE),
    background = Color(0xFFFFF5F4),
    surfaceVariant = Color(0xFFF5E1E0)
)

val PulseDarkColors = darkScheme(
    primary = Color(0xFFF87171),
    primaryContainer = Color(0xFF7F1D1D),
    secondary = Color(0xFFFBBF24),
    secondaryContainer = Color(0xFF6B3D00),
    tertiary = Color(0xFF22D3EE),
    tertiaryContainer = Color(0xFF0B4D59),
    background = Color(0xFF140B0B),
    surface = Color(0xFF1B1111),
    surfaceVariant = Color(0xFF2A1A1A),
    onSurfaceVariant = Color(0xFFC9B3B3),
    outline = Color(0xFF4C3535),
    outlineVariant = Color(0xFF3A2A2A)
)

val RubyLightColors = lightScheme(
    primary = Color(0xFFBE123C),
    primaryContainer = Color(0xFFFFD6E0),
    secondary = Color(0xFFF43F5E),
    secondaryContainer = Color(0xFFFFDEE6),
    tertiary = Color(0xFF0EA5E9),
    tertiaryContainer = Color(0xFFCCEFFF),
    background = Color(0xFFFFF1F3),
    surfaceVariant = Color(0xFFF6DEE3)
)

val RubyDarkColors = darkScheme(
    primary = Color(0xFFF43F5E),
    primaryContainer = Color(0xFF7F1D1D),
    secondary = Color(0xFFFB7185),
    secondaryContainer = Color(0xFF9F1239),
    tertiary = Color(0xFF38BDF8),
    tertiaryContainer = Color(0xFF0B3B52),
    background = Color(0xFF15090D),
    surface = Color(0xFF1D0F14),
    surfaceVariant = Color(0xFF2A1820)
)

fun colorSchemeFor(style: AppThemeStyle, mode: AppThemeMode) = when (style) {
    AppThemeStyle.Midnight -> if (mode == AppThemeMode.Light) MidnightLightColors else MidnightDarkColors
    AppThemeStyle.Ocean -> if (mode == AppThemeMode.Light) OceanLightColors else OceanDarkColors
    AppThemeStyle.Slate -> if (mode == AppThemeMode.Light) SlateLightColors else SlateDarkColors
    AppThemeStyle.Aurora -> if (mode == AppThemeMode.Light) AuroraLightColors else AuroraDarkColors
    AppThemeStyle.Copper -> if (mode == AppThemeMode.Light) CopperLightColors else CopperDarkColors
    AppThemeStyle.Nimbus -> if (mode == AppThemeMode.Light) NimbusLightColors else NimbusDarkColors
}

fun previewColorsFor(style: AppThemeStyle, mode: AppThemeMode): List<Color> {
    val scheme = colorSchemeFor(style, mode)
    return listOf(scheme.primary, scheme.secondary, scheme.tertiary)
}
