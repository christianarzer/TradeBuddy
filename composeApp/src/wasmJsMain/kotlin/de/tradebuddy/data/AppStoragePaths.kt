package de.tradebuddy.data

private const val DefaultAppName = "TradeBuddy"

object AppStoragePaths {
    fun settingsPath(appName: String = DefaultAppName): String =
        "browser://localStorage/$appName/settings"

    fun statsPath(appName: String = DefaultAppName): String =
        "browser://localStorage/$appName/stats"

    fun marketEventsWatchlistPath(appName: String = DefaultAppName): String =
        "browser://localStorage/$appName/market_events_watchlist"

    fun marketEventsCachePath(appName: String = DefaultAppName): String =
        "browser://localStorage/$appName/market_events_cache"

    fun portfolioPath(appName: String = DefaultAppName): String =
        "browser://localStorage/$appName/portfolio"

    fun tasksPath(appName: String = DefaultAppName): String =
        "browser://localStorage/$appName/tasks"
}
