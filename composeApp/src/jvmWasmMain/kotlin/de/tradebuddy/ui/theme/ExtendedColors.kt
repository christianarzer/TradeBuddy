package de.tradebuddy.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AppThemeStyle
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.AstroPlanet

data class ExtendedColors(
    // Trading semantics
    val positive: Color,
    val onPositive: Color,
    val positiveContainer: Color,
    val negative: Color,
    val onNegative: Color,
    val negativeContainer: Color,
    val warning: Color,
    val info: Color,
    val neutral: Color,

    // Chart colors
    val chartBackground: Color,
    val gridLine: Color,
    val candleUp: Color,
    val candleDown: Color,

    // Astro – aspects (5 types)
    val aspectConjunction: Color,
    val aspectSextile: Color,
    val aspectSquare: Color,
    val aspectTrine: Color,
    val aspectOpposition: Color,

    // Astro – planets (10 planets)
    val planetMoon: Color,
    val planetSun: Color,
    val planetMercury: Color,
    val planetVenus: Color,
    val planetMars: Color,
    val planetJupiter: Color,
    val planetSaturn: Color,
    val planetUranus: Color,
    val planetNeptune: Color,
    val planetPluto: Color,
)

// ─── Aspect & Planet helpers ───────────────────────────────────────────────

fun ExtendedColors.colorFor(type: AstroAspectType): Color = when (type) {
    AstroAspectType.Conjunction -> aspectConjunction
    AstroAspectType.Sextile     -> aspectSextile
    AstroAspectType.Square      -> aspectSquare
    AstroAspectType.Trine       -> aspectTrine
    AstroAspectType.Opposition  -> aspectOpposition
}

fun ExtendedColors.colorFor(planet: AstroPlanet): Color = when (planet) {
    AstroPlanet.Moon    -> planetMoon
    AstroPlanet.Sun     -> planetSun
    AstroPlanet.Mercury -> planetMercury
    AstroPlanet.Venus   -> planetVenus
    AstroPlanet.Mars    -> planetMars
    AstroPlanet.Jupiter -> planetJupiter
    AstroPlanet.Saturn  -> planetSaturn
    AstroPlanet.Uranus  -> planetUranus
    AstroPlanet.Neptune -> planetNeptune
    AstroPlanet.Pluto   -> planetPluto
}

// ─── CompositionLocal ─────────────────────────────────────────────────────

val LocalExtendedColors = compositionLocalOf { defaultExtendedColors() }

// ─── Factory ──────────────────────────────────────────────────────────────

fun extendedColorsFor(style: AppThemeStyle, mode: AppThemeMode): ExtendedColors =
    if (mode == AppThemeMode.Dark) darkExtendedColors() else lightExtendedColors()

// ─── Default (dark neutral) ───────────────────────────────────────────────

private fun defaultExtendedColors(): ExtendedColors = darkExtendedColors()

// ─── Dark palette ─────────────────────────────────────────────────────────

private fun darkExtendedColors() = ExtendedColors(
    // Trading
    positive          = Color(0xFF4CAF50),
    onPositive        = Color(0xFFFFFFFF),
    positiveContainer = Color(0xFF1A3A1A),
    negative          = Color(0xFFEF5350),
    onNegative        = Color(0xFFFFFFFF),
    negativeContainer = Color(0xFF3A1A1A),
    warning           = Color(0xFFFFB300),
    info              = Color(0xFF42A5F5),
    neutral           = Color(0xFF78909C),

    // Chart
    chartBackground = Color(0xFF1A1F28),
    gridLine        = Color(0xFF2D3545),
    candleUp        = Color(0xFF4CAF50),
    candleDown      = Color(0xFFEF5350),

    // Aspects – vibrant for dark backgrounds
    aspectConjunction = Color(0xFFCA8A04),
    aspectSextile     = Color(0xFF0D9488),
    aspectSquare      = Color(0xFFEA580C),
    aspectTrine       = Color(0xFF16A34A),
    aspectOpposition  = Color(0xFFDC2626),

    // Planets – vibrant for dark backgrounds
    planetMoon    = Color(0xFF3B82F6),
    planetSun     = Color(0xFFF59E0B),
    planetMercury = Color(0xFF0EA5E9),
    planetVenus   = Color(0xFFDB2777),
    planetMars    = Color(0xFFEF4444),
    planetJupiter = Color(0xFF14B8A6),
    planetSaturn  = Color(0xFF65A30D),
    planetUranus  = Color(0xFF06B6D4),
    planetNeptune = Color(0xFF4F46E5),
    planetPluto   = Color(0xFF64748B),
)

// ─── Light palette ────────────────────────────────────────────────────────

private fun lightExtendedColors() = ExtendedColors(
    // Trading
    positive          = Color(0xFF2E7D32),
    onPositive        = Color(0xFFFFFFFF),
    positiveContainer = Color(0xFFE8F5E9),
    negative          = Color(0xFFC62828),
    onNegative        = Color(0xFFFFFFFF),
    negativeContainer = Color(0xFFFFEBEE),
    warning           = Color(0xFFE65100),
    info              = Color(0xFF1565C0),
    neutral           = Color(0xFF546E7A),

    // Chart
    chartBackground = Color(0xFFF5F7FA),
    gridLine        = Color(0xFFDDE4EE),
    candleUp        = Color(0xFF2E7D32),
    candleDown      = Color(0xFFC62828),

    // Aspects – slightly darker for light backgrounds
    aspectConjunction = Color(0xFFB45309),
    aspectSextile     = Color(0xFF0F766E),
    aspectSquare      = Color(0xFFC2410C),
    aspectTrine       = Color(0xFF15803D),
    aspectOpposition  = Color(0xFFB91C1C),

    // Planets – slightly darker for light backgrounds
    planetMoon    = Color(0xFF2563EB),
    planetSun     = Color(0xFFD97706),
    planetMercury = Color(0xFF0284C7),
    planetVenus   = Color(0xFFBE185D),
    planetMars    = Color(0xFFDC2626),
    planetJupiter = Color(0xFF0F766E),
    planetSaturn  = Color(0xFF4D7C0F),
    planetUranus  = Color(0xFF0891B2),
    planetNeptune = Color(0xFF4338CA),
    planetPluto   = Color(0xFF475569),
)
