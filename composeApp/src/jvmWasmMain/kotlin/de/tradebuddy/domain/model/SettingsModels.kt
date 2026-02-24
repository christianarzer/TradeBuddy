package de.tradebuddy.domain.model

enum class AppThemeStyle(val key: String) {
    Midnight("midnight"),
    Ocean("ocean"),
    Slate("slate"),
    Aurora("aurora"),
    Copper("copper"),
    Nimbus("nimbus");

    companion object {
        fun fromKey(key: String): AppThemeStyle? = when (key) {
            "graphite" -> Slate
            "ember" -> Ocean
            "sand" -> Nimbus
            "olive" -> Aurora
            "neon" -> Slate
            "terminal" -> Copper
            "horizon" -> Ocean
            "arctic" -> Nimbus
            "pulse" -> Aurora
            "ruby" -> Copper
            else -> entries.firstOrNull { it.key == key }
        }
    }
}

enum class AppThemeMode(val key: String) {
    Light("light"),
    Dark("dark");

    companion object {
        fun fromKey(key: String): AppThemeMode? = entries.firstOrNull { it.key == key }
    }
}

data class UserSettings(
    val themeStyle: AppThemeStyle,
    val themeMode: AppThemeMode,
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
