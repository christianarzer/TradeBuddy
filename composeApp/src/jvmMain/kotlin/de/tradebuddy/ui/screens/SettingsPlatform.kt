package de.tradebuddy.ui.screens

import de.tradebuddy.data.AppStoragePaths
import java.io.File
import java.util.Locale

internal fun settingsStorageSectionUi(): StorageSectionUi {
    if (isAndroidRuntime()) {
        return StorageSectionUi(
            visible = false,
            settingsPath = "",
            statsPath = "",
            canOpen = false
        )
    }
    val settingsDir = AppStoragePaths.settingsPath().parentFile ?: AppStoragePaths.settingsPath()
    val statsDir = AppStoragePaths.statsPath().parentFile ?: AppStoragePaths.statsPath()
    return StorageSectionUi(
        visible = true,
        settingsPath = settingsDir.toString(),
        statsPath = statsDir.toString(),
        canOpen = true
    )
}

internal fun openSettingsStoragePath() {
    val directory = AppStoragePaths.settingsPath().parentFile ?: AppStoragePaths.settingsPath()
    openDirectory(directory)
}

internal fun openStatsStoragePath() {
    val directory = AppStoragePaths.statsPath().parentFile ?: AppStoragePaths.statsPath()
    openDirectory(directory)
}

private fun isAndroidRuntime(): Boolean {
    val runtime = System.getProperty("java.runtime.name") ?: return false
    return runtime.contains("Android", ignoreCase = true)
}

private fun openDirectory(path: File) {
    val os = System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT)
    val command = when {
        os.contains("win") -> listOf("explorer", path.path)
        os.contains("mac") -> listOf("open", path.path)
        else -> listOf("xdg-open", path.path)
    }
    runCatching { ProcessBuilder(command).start() }
}
