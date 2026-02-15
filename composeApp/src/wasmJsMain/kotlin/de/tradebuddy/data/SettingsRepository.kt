package de.tradebuddy.data

import de.tradebuddy.domain.model.AppThemeMode
import de.tradebuddy.domain.model.AppThemeStyle
import de.tradebuddy.domain.model.AstroAspectType
import de.tradebuddy.domain.model.ASTRO_MAX_ORB_DEGREES
import de.tradebuddy.domain.model.ASTRO_MIN_ORB_DEGREES
import de.tradebuddy.domain.model.DEFAULT_ASTRO_ASPECT_ORBS
import de.tradebuddy.domain.model.UserSettings
import de.tradebuddy.logging.AppLog
import kotlin.math.round
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SettingsSnapshot(
    val themeStyle: AppThemeStyle?,
    val themeMode: AppThemeMode?,
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
    appName: String = "TradeBuddy",
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : SettingsRepository {

    private val storageKey = "${appName}.settings.v1"

    override suspend fun loadSettings(): SettingsSnapshot? = withContext(dispatcher) {
        runCatching {
            val raw = window.localStorage.getItem(storageKey) ?: return@withContext null
            val props = decodeMap(raw)

            val themeStyle = props["themeStyle"]?.let { AppThemeStyle.fromKey(it) }
            val themeMode = props["themeMode"]?.let { AppThemeMode.fromKey(it) }
            val showUtcTime = props["showUtcTime"]?.toBooleanStrictOrNull()
            val showAzimuth = props["showAzimuth"]?.toBooleanStrictOrNull()
            val showSun = props["showSun"]?.toBooleanStrictOrNull()
            val showMoon = props["showMoon"]?.toBooleanStrictOrNull()
            val showRise = props["showRise"]?.toBooleanStrictOrNull()
            val showSet = props["showSet"]?.toBooleanStrictOrNull()
            val sunTimeOffsetMinutes = props["sunTimeOffsetMinutes"]?.toIntOrNull()
            val moonTimeOffsetMinutes = props["moonTimeOffsetMinutes"]?.toIntOrNull()
            val astroTimeOffsetMinutes = props["astroTimeOffsetMinutes"]?.toIntOrNull()
            val selectedCityKeys = props["selectedCities"]
                ?.split(';')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
            val aspectOrbs = parseAspectOrbs(props["astroAspectOrbs"])

            SettingsSnapshot(
                themeStyle = themeStyle,
                themeMode = themeMode,
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
                message = "Failed to load settings from browser storage",
                throwable = error
            )
        }.getOrNull()
    }

    override suspend fun saveSettings(settings: UserSettings) = withContext(dispatcher) {
        runCatching {
            val map = linkedMapOf(
                "themeStyle" to settings.themeStyle.key,
                "themeMode" to settings.themeMode.key,
                "selectedCities" to settings.selectedCityKeys.joinToString(";"),
                "sunTimeOffsetMinutes" to settings.sunTimeOffsetMinutes.toString(),
                "moonTimeOffsetMinutes" to settings.moonTimeOffsetMinutes.toString(),
                "astroTimeOffsetMinutes" to settings.astroTimeOffsetMinutes.toString(),
                "showUtcTime" to settings.showUtcTime.toString(),
                "showAzimuth" to settings.showAzimuth.toString(),
                "showSun" to settings.showSun.toString(),
                "showMoon" to settings.showMoon.toString(),
                "showRise" to settings.showRise.toString(),
                "showSet" to settings.showSet.toString(),
                "astroAspectOrbs" to AstroAspectType.entries.joinToString(";") { aspect ->
                    val orb = settings.aspectOrbs[aspect] ?: DEFAULT_ASTRO_ASPECT_ORBS.getValue(aspect)
                    "${aspect.name}=${orb.asOneDecimal()}"
                }
            )
            window.localStorage.setItem(storageKey, encodeMap(map))
        }.onFailure { error ->
            AppLog.error(
                tag = "SettingsRepository",
                message = "Failed to save settings to browser storage",
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

private fun encodeMap(map: Map<String, String>): String =
    map.entries.joinToString("\n") { (key, value) -> "${escape(key)}=${escape(value)}" }

private fun decodeMap(raw: String): Map<String, String> =
    raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.contains('=') }
        .associate { line ->
            val idx = line.indexOf('=')
            val key = unescape(line.substring(0, idx))
            val value = unescape(line.substring(idx + 1))
            key to value
        }

private fun escape(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("=", "\\=")

private fun unescape(value: String): String {
    val out = StringBuilder()
    var escaped = false
    value.forEach { ch ->
        if (escaped) {
            out.append(
                when (ch) {
                    'n' -> '\n'
                    else -> ch
                }
            )
            escaped = false
        } else if (ch == '\\') {
            escaped = true
        } else {
            out.append(ch)
        }
    }
    if (escaped) out.append('\\')
    return out.toString()
}

private fun Double.asOneDecimal(): String = (round(this * 10.0) / 10.0).toString()
