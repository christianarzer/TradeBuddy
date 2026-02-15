package de.tradebuddy.domain.model

enum class AppThemeStyle(val key: String) {
    Neon("neon"),
    Terminal("terminal"),
    Midnight("midnight"),
    Horizon("horizon"),
    Ocean("ocean"),
    Slate("slate"),
    Aurora("aurora"),
    Copper("copper"),
    Arctic("arctic"),
    Nimbus("nimbus"),
    Pulse("pulse"),
    Ruby("ruby");

    companion object {
        fun fromKey(key: String): AppThemeStyle? = when (key) {
            "graphite" -> Neon
            "ember" -> Horizon
            "sand" -> Nimbus
            "olive" -> Pulse
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
    val showUtcTime: Boolean,
    val showAzimuth: Boolean,
    val showSun: Boolean,
    val showMoon: Boolean,
    val showRise: Boolean,
    val showSet: Boolean,
    val aspectOrbs: Map<AstroAspectType, Double>
)
