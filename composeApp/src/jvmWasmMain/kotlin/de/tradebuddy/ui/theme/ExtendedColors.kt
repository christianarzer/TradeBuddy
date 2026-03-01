package de.tradebuddy.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.AstroPlanet

data class ExtendedColors(
    val positive: Color,
    val onPositive: Color,
    val positiveContainer: Color,
    val negative: Color,
    val onNegative: Color,
    val negativeContainer: Color,
    val warning: Color,
    val info: Color,
    val neutral: Color,
    val impactLow: Color,
    val impactMedium: Color,
    val impactHigh: Color,
    val watchlist: Color,
    val chartBackground: Color,
    val gridLine: Color,
    val candleUp: Color,
    val candleDown: Color,
    val shellBackground: Color,
    val shellBorder: Color,
    val shellDivider: Color,
    val sidebarTextMuted: Color,
    val sidebarSelection: Color,
    val toolbarSurface: Color,
    val tableRowHover: Color,
    val tableRowSelected: Color,
    val regionUnitedStates: Color,
    val regionEuroArea: Color,
    val regionUnitedKingdom: Color,
    val regionJapan: Color,
    val regionChina: Color,
    val regionGlobal: Color,
    val portfolioPalette: List<Color>,
    val aspectConjunction: Color,
    val aspectSextile: Color,
    val aspectSquare: Color,
    val aspectTrine: Color,
    val aspectOpposition: Color,
    val planetMoon: Color,
    val planetSun: Color,
    val planetMercury: Color,
    val planetVenus: Color,
    val planetMars: Color,
    val planetJupiter: Color,
    val planetSaturn: Color,
    val planetUranus: Color,
    val planetNeptune: Color,
    val planetPluto: Color
)

fun ExtendedColors.colorFor(type: AstroAspectType): Color = when (type) {
    AstroAspectType.Conjunction -> aspectConjunction
    AstroAspectType.Sextile -> aspectSextile
    AstroAspectType.Square -> aspectSquare
    AstroAspectType.Trine -> aspectTrine
    AstroAspectType.Opposition -> aspectOpposition
}

fun ExtendedColors.colorFor(planet: AstroPlanet): Color = when (planet) {
    AstroPlanet.Moon -> planetMoon
    AstroPlanet.Sun -> planetSun
    AstroPlanet.Mercury -> planetMercury
    AstroPlanet.Venus -> planetVenus
    AstroPlanet.Mars -> planetMars
    AstroPlanet.Jupiter -> planetJupiter
    AstroPlanet.Saturn -> planetSaturn
    AstroPlanet.Uranus -> planetUranus
    AstroPlanet.Neptune -> planetNeptune
    AstroPlanet.Pluto -> planetPluto
}

val LocalExtendedColors = compositionLocalOf { defaultExtendedColors() }

fun extendedColorsFor(mode: AppThemeMode): ExtendedColors =
    if (mode == AppThemeMode.Dark) darkExtendedColors() else lightExtendedColors()

private fun defaultExtendedColors(): ExtendedColors = darkExtendedColors()

private fun darkExtendedColors() = ExtendedColors(
    positive = AppPalette.Green,
    onPositive = AppPalette.Black,
    positiveContainer = AppPalette.Green.copy(alpha = 0.24f),
    negative = AppPalette.Red,
    onNegative = AppPalette.Black,
    negativeContainer = AppPalette.Red.copy(alpha = 0.24f),
    warning = AppPalette.Yellow,
    info = AppPalette.Blue,
    neutral = AppPalette.White80,
    impactLow = AppPalette.Blue,
    impactMedium = AppPalette.Yellow,
    impactHigh = AppPalette.Red,
    watchlist = AppPalette.Indigo,
    chartBackground = DashboardSurfaceDark,
    gridLine = AppPalette.White10,
    candleUp = AppPalette.Green,
    candleDown = AppPalette.Red,
    shellBackground = AppPalette.SurfaceDark,
    shellBorder = AppPalette.White10,
    shellDivider = AppPalette.White10,
    sidebarTextMuted = AppPalette.White,
    sidebarSelection = AppPalette.White10,
    toolbarSurface = AppPalette.SurfaceDarkAlt,
    tableRowHover = AppPalette.White4,
    tableRowSelected = AppPalette.White10,
    regionUnitedStates = AppPalette.Blue,
    regionEuroArea = AppPalette.Indigo,
    regionUnitedKingdom = AppPalette.Purple,
    regionJapan = AppPalette.Cyan,
    regionChina = AppPalette.Orange,
    regionGlobal = AppPalette.White80,
    portfolioPalette = AppPalette.PortfolioPalette,
    aspectConjunction = AppPalette.Yellow,
    aspectSextile = AppPalette.Cyan,
    aspectSquare = AppPalette.Orange,
    aspectTrine = AppPalette.Green,
    aspectOpposition = AppPalette.Red,
    planetMoon = AppPalette.Blue,
    planetSun = AppPalette.Yellow,
    planetMercury = AppPalette.Cyan,
    planetVenus = AppPalette.Purple,
    planetMars = AppPalette.Red,
    planetJupiter = AppPalette.Cyan,
    planetSaturn = AppPalette.Green,
    planetUranus = AppPalette.Cyan,
    planetNeptune = AppPalette.Indigo,
    planetPluto = AppPalette.White80
)

private fun lightExtendedColors() = ExtendedColors(
    positive = AppPalette.Green,
    onPositive = AppPalette.Black,
    positiveContainer = AppPalette.Green.copy(alpha = 0.20f),
    negative = AppPalette.Red,
    onNegative = AppPalette.Black,
    negativeContainer = AppPalette.Red.copy(alpha = 0.20f),
    warning = AppPalette.Yellow,
    info = AppPalette.Blue,
    neutral = AppPalette.Black40,
    impactLow = AppPalette.Blue,
    impactMedium = AppPalette.Yellow,
    impactHigh = AppPalette.Red,
    watchlist = AppPalette.Indigo,
    chartBackground = DashboardSurfaceLight,
    gridLine = AppPalette.Black10,
    candleUp = AppPalette.Green,
    candleDown = AppPalette.Red,
    shellBackground = AppPalette.SurfaceLight,
    shellBorder = AppPalette.Black10,
    shellDivider = AppPalette.Black10,
    sidebarTextMuted = AppPalette.Black80,
    sidebarSelection = AppPalette.Black4,
    toolbarSurface = AppPalette.SurfaceLightAlt,
    tableRowHover = AppPalette.Black4,
    tableRowSelected = AppPalette.Black10,
    regionUnitedStates = AppPalette.Blue,
    regionEuroArea = AppPalette.Indigo,
    regionUnitedKingdom = AppPalette.Purple,
    regionJapan = AppPalette.Cyan,
    regionChina = AppPalette.Orange,
    regionGlobal = AppPalette.Black40,
    portfolioPalette = AppPalette.PortfolioPalette,
    aspectConjunction = AppPalette.Yellow,
    aspectSextile = AppPalette.Cyan,
    aspectSquare = AppPalette.Orange,
    aspectTrine = AppPalette.Green,
    aspectOpposition = AppPalette.Red,
    planetMoon = AppPalette.Blue,
    planetSun = AppPalette.Yellow,
    planetMercury = AppPalette.Cyan,
    planetVenus = AppPalette.Purple,
    planetMars = AppPalette.Red,
    planetJupiter = AppPalette.Cyan,
    planetSaturn = AppPalette.Green,
    planetUranus = AppPalette.Cyan,
    planetNeptune = AppPalette.Indigo,
    planetPluto = AppPalette.Black80
)
