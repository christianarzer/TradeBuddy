package de.tradebuddy.data

import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AppThemeStyle
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.ASTRO_MAX_ORB_DEGREES
import de.tradebuddy.domain.model.ASTRO_MIN_ORB_DEGREES
import de.tradebuddy.domain.model.DEFAULT_ASTRO_ASPECT_ORBS
import de.tradebuddy.domain.model.UserSettings
import java.io.File
import java.util.Locale
import java.util.Properties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SettingsSnapshot(
    val themeStyle: AppThemeStyle?,
    val themeMode: AppThemeMode?,
    val selectedCityKeys: Set<String>?,
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

            val themeStyle = props.getProperty("themeStyle")?.let { AppThemeStyle.fromKey(it) }
            val themeMode = props.getProperty("themeMode")?.let { AppThemeMode.fromKey(it) }
            val darkTheme = props.getProperty("darkTheme")?.toBooleanStrictOrNull()
            val showUtcTime = props.getProperty("showUtcTime")?.toBooleanStrictOrNull()
            val showAzimuth = props.getProperty("showAzimuth")?.toBooleanStrictOrNull()
            val showSun = props.getProperty("showSun")?.toBooleanStrictOrNull()
            val showMoon = props.getProperty("showMoon")?.toBooleanStrictOrNull()
            val showRise = props.getProperty("showRise")?.toBooleanStrictOrNull()
            val showSet = props.getProperty("showSet")?.toBooleanStrictOrNull()
            val aspectOrbs = parseAspectOrbs(props.getProperty("astroAspectOrbs"))
            val rawCities = props.getProperty("selectedCities")
            val selectedCityKeys = rawCities
                ?.split(';')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()

            if (themeStyle == null && themeMode == null && darkTheme == null &&
                selectedCityKeys == null && showUtcTime == null && showAzimuth == null &&
                showSun == null && showMoon == null && showRise == null && showSet == null &&
                aspectOrbs == null
            ) null
            else SettingsSnapshot(
                themeStyle = themeStyle ?: darkTheme?.let { AppThemeStyle.Neon },
                themeMode = themeMode ?: darkTheme?.let {
                    if (it) AppThemeMode.Dark else AppThemeMode.Light
                },
                selectedCityKeys = selectedCityKeys,
                showUtcTime = showUtcTime,
                showAzimuth = showAzimuth,
                showSun = showSun,
                showMoon = showMoon,
                showRise = showRise,
                showSet = showSet,
                aspectOrbs = aspectOrbs
            )
        }.getOrNull()
    }

    override suspend fun saveSettings(settings: UserSettings) = withContext(dispatcher) {
        runCatching {
            settingsPath.parentFile?.mkdirs()
            val props = Properties()
            props.setProperty("themeStyle", settings.themeStyle.key)
            props.setProperty("themeMode", settings.themeMode.key)
            props.setProperty("selectedCities", settings.selectedCityKeys.joinToString(";"))
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
        }.getOrElse {  }
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

