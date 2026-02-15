package de.tradebuddy.ui.screens

internal data class StorageSectionUi(
    val visible: Boolean,
    val settingsPath: String,
    val statsPath: String,
    val canOpen: Boolean
)

internal expect fun settingsStorageSectionUi(): StorageSectionUi

internal expect fun openSettingsStoragePath()

internal expect fun openStatsStoragePath()
