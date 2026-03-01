package de.tradebuddy.domain.model

enum class AppThemeMode(val key: String) {
    Light("light"),
    Dark("dark");

    companion object {
        fun fromKey(key: String): AppThemeMode? = entries.firstOrNull { it.key == key }
    }
}

enum class AppAccentColor(val key: String) {
    Purple("purple"),
    Indigo("indigo"),
    Blue("blue"),
    Green("green"),
    Yellow("yellow"),
    Orange("orange"),
    Cyan("cyan");

    companion object {
        fun fromKey(key: String): AppAccentColor? = entries.firstOrNull { it.key == key }
    }
}

data class UserSettings(
    val themeMode: AppThemeMode,
    val accentColor: AppAccentColor,
    val selectedCityKeys: Set<String>,
    val sunTimeOffsetMinutes: Int,
    val moonTimeOffsetMinutes: Int,
    val astroTimeOffsetMinutes: Int,
    val showUtcTime: Boolean,
    val showAzimuth: Boolean,
    val showSun: Boolean,
    val showMoon: Boolean,
    val showRise: Boolean,
    val showSet: Boolean,
    val aspectOrbs: Map<AstroAspectType, Double>
)
