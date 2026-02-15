package de.tradebuddy.data

import java.io.File

private const val DefaultAppName = "TradeBuddy"

object AppStoragePaths {
    fun settingsPath(appName: String = DefaultAppName): File =
        File(appStorageDir(appName), "settings.properties")

    fun statsPath(appName: String = DefaultAppName): File =
        File(appStorageDir(appName), "stats.csv")
}

expect fun appStorageDir(appName: String = DefaultAppName): File
