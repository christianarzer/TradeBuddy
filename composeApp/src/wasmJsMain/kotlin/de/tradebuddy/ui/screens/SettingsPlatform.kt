package de.tradebuddy.ui.screens

import de.tradebuddy.data.AppStoragePaths

internal fun settingsStorageSectionUi(): StorageSectionUi = StorageSectionUi(
    visible = true,
    settingsPath = AppStoragePaths.settingsPath(),
    statsPath = AppStoragePaths.statsPath(),
    canOpen = false
)

internal fun openSettingsStoragePath() = Unit

internal fun openStatsStoragePath() = Unit
