package de.tradebuddy.ui.screens

internal actual fun settingsStorageSectionUi(): StorageSectionUi = StorageSectionUi(
    visible = false,
    settingsPath = "",
    statsPath = "",
    canOpen = false
)

internal actual fun openSettingsStoragePath() = Unit

internal actual fun openStatsStoragePath() = Unit
