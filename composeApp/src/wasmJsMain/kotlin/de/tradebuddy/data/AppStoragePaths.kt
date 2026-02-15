package de.tradebuddy.data

private const val DefaultAppName = "TradeBuddy"

object AppStoragePaths {
    fun settingsPath(appName: String = DefaultAppName): String =
        "browser://localStorage/$appName/settings"

    fun statsPath(appName: String = DefaultAppName): String =
        "browser://localStorage/$appName/stats"
}

