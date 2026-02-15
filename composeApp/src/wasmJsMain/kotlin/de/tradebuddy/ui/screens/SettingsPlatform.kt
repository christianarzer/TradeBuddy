package de.tradebuddy.ui.screens

import de.tradebuddy.data.AppStoragePaths

internal actual fun settingsStorageSectionUi(): StorageSectionUi = StorageSectionUi(
    visible = true,
    settingsPath = AppStoragePaths.settingsPath(),
    statsPath = AppStoragePaths.statsPath(),
    canOpen = false
)

internal actual fun openSettingsStoragePath() = Unit

internal actual fun openStatsStoragePath() = Unit
