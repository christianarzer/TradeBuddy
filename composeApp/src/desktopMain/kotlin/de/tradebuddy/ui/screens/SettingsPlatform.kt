package de.tradebuddy.ui.screens

import de.tradebuddy.data.AppStoragePaths
import de.tradebuddy.logging.AppLog
import java.io.File
import java.util.Locale

internal actual fun settingsStorageSectionUi(): StorageSectionUi {
    val settingsDir = AppStoragePaths.settingsPath().parentFile ?: AppStoragePaths.settingsPath()
    val statsDir = AppStoragePaths.statsPath().parentFile ?: AppStoragePaths.statsPath()
    return StorageSectionUi(
        visible = true,
        settingsPath = settingsDir.toString(),
        statsPath = statsDir.toString(),
        canOpen = true
    )
}

internal actual fun openSettingsStoragePath() {
    val directory = AppStoragePaths.settingsPath().parentFile ?: AppStoragePaths.settingsPath()
    openDirectory(directory)
}

internal actual fun openStatsStoragePath() {
    val directory = AppStoragePaths.statsPath().parentFile ?: AppStoragePaths.statsPath()
    openDirectory(directory)
}

private fun openDirectory(path: File) {
    val os = System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT)
    val command = when {
        os.contains("win") -> listOf("explorer", path.path)
        os.contains("mac") -> listOf("open", path.path)
        else -> listOf("xdg-open", path.path)
    }
    runCatching { ProcessBuilder(command).start() }
        .onFailure { error ->
            AppLog.warn(
                tag = "SettingsPlatform",
                message = "Failed to open directory ${path.path}",
                throwable = error
            )
        }
}
