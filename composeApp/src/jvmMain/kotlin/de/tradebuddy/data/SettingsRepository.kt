package de.tradebuddy.data

import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AppAccentColor
import de.tradebuddy.domain.model.AppDisplayCurrency
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.ASTRO_MAX_ORB_DEGREES
import de.tradebuddy.domain.model.ASTRO_MIN_ORB_DEGREES
import de.tradebuddy.domain.model.DEFAULT_ASTRO_ASPECT_ORBS
import de.tradebuddy.domain.model.UserSettings
import de.tradebuddy.logging.AppLog
import java.io.File
import java.util.Locale
import java.util.Properties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SettingsSnapshot(
    val themeMode: AppThemeMode?,
    val accentColor: AppAccentColor?,
    val displayCurrency: AppDisplayCurrency?,
    val selectedCityKeys: Set<String>?,
    val sunTimeOffsetMinutes: Int?,
    val moonTimeOffsetMinutes: Int?,
    val astroTimeOffsetMinutes: Int?,
    val showUtcTime: Boolean?,
    val showAzimuth: Boolean?,
    val showSun: Boolean?,
    val showMoon: Boolean?,
    val showRise: Boolean?,
    val showSet: Boolean?,
    val aspectOrbs: Map<AstroAspectType, Double>?
)

interface SettingsRepository {
    suspend fun loadSettings(): SettingsSnapshot?
    suspend fun saveSettings(settings: UserSettings)
}

class FileSettingsRepository(
    private val appName: String = "TradeBuddy",
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettingsRepository {

    private val settingsPath: File = AppStoragePaths.settingsPath(appName)

    override suspend fun loadSettings(): SettingsSnapshot? = withContext(dispatcher) {
        if (!settingsPath.exists()) return@withContext null

        runCatching {
            val props = Properties()
            settingsPath.inputStream().use { props.load(it) }

            val themeMode = props.getProperty("themeMode")?.let { AppThemeMode.fromKey(it) }
            val accentColor = props.getProperty("accentColor")?.let { AppAccentColor.fromKey(it) }
            val displayCurrency = props.getProperty("displayCurrency")?.let { AppDisplayCurrency.fromKey(it) }
            val darkTheme = props.getProperty("darkTheme")?.toBooleanStrictOrNull()
            val showUtcTime = props.getProperty("showUtcTime")?.toBooleanStrictOrNull()
            val showAzimuth = props.getProperty("showAzimuth")?.toBooleanStrictOrNull()
            val showSun = props.getProperty("showSun")?.toBooleanStrictOrNull()
            val showMoon = props.getProperty("showMoon")?.toBooleanStrictOrNull()
            val showRise = props.getProperty("showRise")?.toBooleanStrictOrNull()
            val showSet = props.getProperty("showSet")?.toBooleanStrictOrNull()
            val sunTimeOffsetMinutes = props.getProperty("sunTimeOffsetMinutes")?.toIntOrNull()
            val moonTimeOffsetMinutes = props.getProperty("moonTimeOffsetMinutes")?.toIntOrNull()
            val astroTimeOffsetMinutes = props.getProperty("astroTimeOffsetMinutes")?.toIntOrNull()
            val aspectOrbs = parseAspectOrbs(props.getProperty("astroAspectOrbs"))
            val rawCities = props.getProperty("selectedCities")
            val selectedCityKeys = rawCities
                ?.split(';')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()

            if (themeMode == null && accentColor == null && displayCurrency == null && darkTheme == null &&
                selectedCityKeys == null && showUtcTime == null && showAzimuth == null &&
                showSun == null && showMoon == null && showRise == null && showSet == null &&
                sunTimeOffsetMinutes == null && moonTimeOffsetMinutes == null &&
                astroTimeOffsetMinutes == null &&
                aspectOrbs == null
            ) null
            else SettingsSnapshot(
                themeMode = themeMode ?: darkTheme?.let {
                    if (it) AppThemeMode.Dark else AppThemeMode.Light
                },
                accentColor = accentColor,
                displayCurrency = displayCurrency,
                selectedCityKeys = selectedCityKeys,
                sunTimeOffsetMinutes = sunTimeOffsetMinutes,
                moonTimeOffsetMinutes = moonTimeOffsetMinutes,
                astroTimeOffsetMinutes = astroTimeOffsetMinutes,
                showUtcTime = showUtcTime,
                showAzimuth = showAzimuth,
                showSun = showSun,
                showMoon = showMoon,
                showRise = showRise,
                showSet = showSet,
                aspectOrbs = aspectOrbs
            )
        }.onFailure { error ->
            AppLog.error(
                tag = "SettingsRepository",
                message = "Failed to load settings from ${settingsPath.absolutePath}",
                throwable = error
            )
        }.getOrNull()
    }

    override suspend fun saveSettings(settings: UserSettings) = withContext(dispatcher) {
        runCatching {
            settingsPath.parentFile?.mkdirs()
            val props = Properties()
            props.setProperty("themeMode", settings.themeMode.key)
            props.setProperty("accentColor", settings.accentColor.key)
            props.setProperty("displayCurrency", settings.displayCurrency.key)
            props.setProperty("selectedCities", settings.selectedCityKeys.joinToString(";"))
            props.setProperty("sunTimeOffsetMinutes", settings.sunTimeOffsetMinutes.toString())
            props.setProperty("moonTimeOffsetMinutes", settings.moonTimeOffsetMinutes.toString())
            props.setProperty("astroTimeOffsetMinutes", settings.astroTimeOffsetMinutes.toString())
            props.setProperty("showUtcTime", settings.showUtcTime.toString())
            props.setProperty("showAzimuth", settings.showAzimuth.toString())
            props.setProperty("showSun", settings.showSun.toString())
            props.setProperty("showMoon", settings.showMoon.toString())
            props.setProperty("showRise", settings.showRise.toString())
            props.setProperty("showSet", settings.showSet.toString())
            val serializedAspectOrbs = AstroAspectType.entries.joinToString(";") { aspect ->
                val orb = settings.aspectOrbs[aspect] ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(aspect)
                "${aspect.name}=${"%.1f".format(Locale.US, orb)}"
            }
            props.setProperty("astroAspectOrbs", serializedAspectOrbs)
            settingsPath.outputStream().use { props.store(it, "TradeBuddy settings") }
        }.onFailure { error ->
            AppLog.error(
                tag = "SettingsRepository",
                message = "Failed to save settings to ${settingsPath.absolutePath}",
                throwable = error
            )
        }.getOrElse { }
    }

    private fun parseAspectOrbs(rawValue: String?): Map<AstroAspectType, Double>? {
        if (rawValue.isNullOrBlank()) return null
        val parsed = mutableMapOf<AstroAspectType, Double>()
        rawValue.split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { token ->
                val parts = token.split('=', limit = 2)
                if (parts.size != 2) return@forEach
                val aspect = runCatching { AstroAspectType.valueOf(parts[0].trim()) }.getOrNull()
                    ?: return@forEach
                val orb = parts[1].trim().toDoubleOrNull() ?: return@forEach
                if (orb !in ASTRO_MIN_ORB_DEGREES..ASTRO_MAX_ORB_DEGREES) return@forEach
                parsed[aspect] = orb
            }
        if (parsed.isEmpty()) return null
        return AstroAspectType.entries.associateWith { aspect ->
            parsed[aspect] ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(aspect)
        }
    }

}

