package de.tradebuddy.data

import java.io.File

private const val DefaultAppName = "TradeBuddy"

object AppStoragePaths {
    fun settingsPath(appName: String = DefaultAppName): File =
        File(appStorageDir(appName), "settings.properties")

    fun statsPath(appName: String = DefaultAppName): File =
        File(appStorageDir(appName), "stats.csv")

    fun marketEventsWatchlistPath(appName: String = DefaultAppName): File =
        File(appStorageDir(appName), "market_events_watchlist.json")

    fun marketEventsCachePath(appName: String = DefaultAppName): File =
        File(appStorageDir(appName), "market_events_cache.json")

    fun portfolioPath(appName: String = DefaultAppName): File =
        File(appStorageDir(appName), "portfolio.json")

    fun tasksPath(appName: String = DefaultAppName): File =
        File(appStorageDir(appName), "tasks.json")

    fun backtestingHistoryPath(appName: String = DefaultAppName): File =
        File(appStorageDir(appName), "backtesting_history.json")

    fun backtestingCandleCachePath(appName: String = DefaultAppName): File =
        File(appStorageDir(appName), "backtesting_candles_cache.json")

    fun backtestingSymbolsCachePath(appName: String = DefaultAppName): File =
        File(appStorageDir(appName), "backtesting_symbols_cache.json")
}

expect fun appStorageDir(appName: String = DefaultAppName): File
