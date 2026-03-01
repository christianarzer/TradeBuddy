package de.tradebuddy.ui.theme

import androidx.compose.ui.graphics.Color
import de.tradebuddy.domain.model.AppAccentColor

object AppPalette {
    // Snow Dashboard base ink tone (used as "Black/100%")
    val Black = Color(0xFF1C1C1C)
    val White = Color(0xFFFFFFFF)

    val Black80 = Color(0xCC1C1C1C)
    val Black40 = Color(0x661C1C1C)
    val Black20 = Color(0x331C1C1C)
    val Black10 = Color(0x1A1C1C1C)
    val Black4 = Color(0x0D1C1C1C)

    val White80 = Color(0xCCFFFFFF)
    val White40 = Color(0x66FFFFFF)
    val White20 = Color(0x33FFFFFF)
    val White10 = Color(0x1AFFFFFF)
    val White4 = Color(0x0DFFFFFF)

    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceLightAlt = Color(0xFFF7F9FB)
    val SurfaceLightSoft = Color(0xFFF7F9FB)
    val SurfaceLightInfo = Color(0xFFE3F5FF)

    val SurfaceDark = Color(0xFF1C1C1C)
    // Figma token "Primary/Light" equals white 5% over #1C1C1C -> resulting opaque #282828.
    // Use the resolved opaque value to keep cards consistent across nested surfaces.
    val SurfaceDarkAlt = Color(0xFF282828)
    // 10% white over #1C1C1C -> resulting opaque #343434.
    val SurfaceDarkSoft = Color(0xFF343434)

    val Indigo = Color(0xFF95A4FC)
    val Blue = Color(0xFFB1E3FF)
    val Green = Color(0xFFA1E3CB)
    val Yellow = Color(0xFFFFE999)
    val Orange = Color(0xFFFFCB83)
    val Red = Color(0xFFFF4747)
    val Purple = Color(0xFFC6C7F8)
    val Cyan = Color(0xFFA8C5DA)

    val PortfolioPalette: List<Color> = listOf(
        Indigo,
        Blue,
        Green,
        Yellow,
        Orange,
        Purple
    )

    val PortfolioPaletteHex: List<String> = listOf(
        "#95A4FC",
        "#B1E3FF",
        "#A1E3CB",
        "#FFE999",
        "#FFCB83",
        "#C6C7F8"
    )

    const val DefaultPortfolioHex: String = "#95A4FC"
}

fun accentColorFor(accent: AppAccentColor): Color = when (accent) {
    AppAccentColor.Purple -> AppPalette.Purple
    AppAccentColor.Indigo -> AppPalette.Indigo
    AppAccentColor.Blue -> AppPalette.Blue
    AppAccentColor.Green -> AppPalette.Green
    AppAccentColor.Yellow -> AppPalette.Yellow
    AppAccentColor.Orange -> AppPalette.Orange
    AppAccentColor.Cyan -> AppPalette.Cyan
}
